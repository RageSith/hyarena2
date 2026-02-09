# Objective-Aware Bots for Game Modes

> Design document: how to make bots play objectives instead of just chasing enemies

---

## Problem

Currently, `BotManager.updateBotTarget()` always finds the nearest alive participant and sets them as the NPC's `LockedTarget`. This works for Duel and Deathmatch, but in game modes with objectives (KOTH, future CTF, Domination), bots just fight randomly and never actually play the objective.

---

## Current Architecture

### What happens every tick

```
Match.tick()
  → BotManager.tickBotsForMatch(match)
    → tickBot(bot) for each alive bot
      → syncBotPosition()              // Read NPC position into BotParticipant
      → ai.tick()                       // BotAI state machine (IDLE/CHASE/ATTACK/etc.)
      → updateBotTarget(bot, match)     // Find nearest enemy → set MarkedEntity
      → applyBotCombatDamage(bot, match) // Handle damage
```

### What updateBotTarget does now

1. Iterates all alive participants in the match
2. Calculates distance from bot to each one
3. Picks the nearest
4. Sets it as the NPC's `MarkedEntity("LockedTarget")` + `HOSTILE` attitude
5. The NPC's built-in AI (Template_Intelligent) handles pathfinding and combat

### What BotAI does now

BotAI tracks a simple state machine: `IDLE → PATROL → CHASE → ATTACK → RETREAT → DEAD`

But the actual NPC movement is driven by the role's AI (Template_Intelligent), not by BotAI. BotAI mainly:
- Tracks which target the bot is focused on
- Decides when to attack (accuracy rolls, cooldowns)
- Fires damage callbacks

---

## The Goal

Bots should:
1. **Walk to objectives** (capture zones, flag positions) when no enemies are nearby
2. **Fight enemies near objectives** — prioritize enemies in/near the zone over distant enemies
3. **Balance fighting vs. objective** — harder bots prioritize objectives more
4. **Work generically** — game modes provide objective data, BotManager doesn't know game-specific logic

---

## Design: GameMode Provides Objective Info

### New method on GameMode interface

```java
/**
 * Returns objective information for bot AI decision-making.
 * Bots will try to move toward the objective position when not in combat.
 * Returns null if the game mode has no spatial objectives (Duel, Deathmatch).
 */
default BotObjective getBotObjective(ArenaConfig config) {
    return null;
}
```

### BotObjective data class

```java
public record BotObjective(
    Position position,    // Where bots should go (zone center, flag location)
    double radius,        // How close = "at objective" (zone half-width)
    String type           // "capture", "defend", "escort" — for future behavior tuning
) {}
```

### KOTH override

```java
// In KingOfTheHillGameMode
@Override
public BotObjective getBotObjective(ArenaConfig config) {
    List<ArenaConfig.CaptureZone> zones = config.getCaptureZones();
    if (zones == null || zones.isEmpty()) return null;

    ArenaConfig.CaptureZone zone = zones.get(activeZoneIndex);
    double cx = (zone.getMinX() + zone.getMaxX()) / 2.0;
    double cy = zone.getMinY(); // Ground level
    double cz = (zone.getMinZ() + zone.getMaxZ()) / 2.0;
    double rx = (zone.getMaxX() - zone.getMinX()) / 2.0;

    return new BotObjective(new Position(cx, cy, cz), rx, "capture");
}
```

Other modes return null → bots fight normally (no change needed).

---

## Design: Bot Movement via Invisible Target Entity

From the Hycompanion code (see `09-moving-npcs.md`), we learned that the most reliable way to move an NPC to a position is:

1. Spawn an **invisible entity** (Projectile + Intangible + NetworkId) at the target position
2. Set it as the NPC's `LockedTarget`
3. Set NPC state to `Follow`
4. The NPC pathfinds there using its built-in AI

This is better than the leash-based approach (which relies on the NPC's idle wander AI returning it to the leash point — slow and unreliable).

### Lifecycle

```
Bot decides to go to objective
  → Spawn invisible marker at zone center (if not already spawned)
  → Set marker as LockedTarget
  → NPC walks there via built-in Follow behavior

Bot sees enemy nearby
  → Set enemy as LockedTarget (replaces marker)
  → NPC chases and fights enemy
  → Marker stays alive (reused later)

Bot kills enemy / no enemies
  → Set marker as LockedTarget again
  → NPC resumes walking to objective

Match ends
  → Despawn all markers
```

### Key advantage

No custom NPC role files needed. The same `Blook_Skeleton_Pirate_Gunner_Blunderbuss` role works for all game modes. The bot model doesn't need a `_KOTH` variant — objective behavior is driven entirely from Java.

---

## Design: Decision Logic in updateBotTarget

### New flow

```
updateBotTarget(bot, match, store):
    objective = match.getGameMode().getBotObjective(config)
    botPos = bot.getCurrentPosition()

    if objective == null:
        → EXISTING BEHAVIOR: find nearest enemy, set as target
        return

    // Find enemies
    nearestEnemy = findNearest(alive participants, botPos)
    nearestEnemyNearZone = findNearest(alive participants near objective, botPos)

    distToObjective = botPos.distanceTo(objective.position)
    inZone = (distToObjective <= objective.radius)

    // Priority decision
    if nearestEnemy != null AND nearestEnemy.distance <= THREAT_RANGE:
        → TARGET ENEMY (fight nearby threats)
        setNpcTarget(enemy)
    else if inZone:
        → HOLD POSITION (we're on the objective, no threats)
        clearNpcTarget()
    else:
        → MOVE TO OBJECTIVE
        setObjectiveTarget(objective.position)
```

### Constants

```java
THREAT_RANGE = 10.0   // Enemies within 10 blocks always trigger combat
```

### objectiveFocus per difficulty

Not needed in this approach. The `THREAT_RANGE` constant is the same for all difficulties — the difference is that harder bots have wider `chaseRange` in BotDifficulty, so they notice enemies further away. Easy bots with `chaseRange: 15` barely see enemies, so they naturally spend more time on objectives.

---

## Implementation: Invisible Marker Management

### Per-match marker tracking

```java
// In BotManager
private final Map<UUID, Ref<EntityStore>> objectiveMarkers = new ConcurrentHashMap<>();
// Key: match ID, Value: invisible entity ref at objective position
```

### Spawning a marker

```java
private Ref<EntityStore> getOrCreateObjectiveMarker(Match match, Position target, Store<EntityStore> store) {
    UUID matchId = match.getMatchId();
    Ref<EntityStore> existing = objectiveMarkers.get(matchId);

    if (existing != null && existing.isValid()) {
        // Update position if objective moved (zone rotation)
        TransformComponent t = store.getComponent(existing, TransformComponent.getComponentType());
        if (t != null) {
            t.setPosition(new Vector3d(target.getX(), target.getY(), target.getZ()));
        }
        return existing;
    }

    // Spawn new invisible marker
    Holder<EntityStore> holder = EntityStore.REGISTRY.newHolder();
    ProjectileComponent proj = new ProjectileComponent("Projectile");
    holder.putComponent(ProjectileComponent.getComponentType(), proj);
    holder.putComponent(TransformComponent.getComponentType(),
        new TransformComponent(
            new Vector3d(target.getX(), target.getY(), target.getZ()),
            new Vector3f(0, 0, 0)));
    holder.ensureComponent(UUIDComponent.getComponentType());
    holder.ensureComponent(Intangible.getComponentType());
    holder.addComponent(NetworkId.getComponentType(),
        new NetworkId(store.getExternalData().takeNextNetworkId()));
    proj.initialize();

    Ref<EntityStore> ref = store.addEntity(holder, AddReason.SPAWN);
    if (ref != null) {
        objectiveMarkers.put(matchId, ref);
    }
    return ref;
}
```

### Cleanup on match end

```java
// In despawnAllBotsInMatch() — add:
Ref<EntityStore> marker = objectiveMarkers.remove(match.getMatchId());
if (marker != null && marker.isValid()) {
    marker.getStore().removeEntity(marker, RemoveReason.REMOVE);
}
```

---

## Setting NPC Target: Two Modes

### Combat mode (target an enemy)

```java
private void setNpcCombatTarget(Role role, Ref<EntityStore> targetRef) {
    MarkedEntitySupport marked = role.getMarkedEntitySupport();
    marked.setMarkedEntity(MarkedEntitySupport.DEFAULT_TARGET_SLOT, targetRef);

    WorldSupport worldSupport = role.getWorldSupport();
    worldSupport.overrideAttitude(targetRef, Attitude.HOSTILE, 60.0);
}
```

### Objective mode (move to position)

```java
private void setNpcObjectiveTarget(Role role, Ref<EntityStore> markerRef) {
    MarkedEntitySupport marked = role.getMarkedEntitySupport();
    marked.setMarkedEntity(MarkedEntitySupport.DEFAULT_TARGET_SLOT, markerRef);
    // No hostile attitude — marker is not an enemy, NPC just walks toward it
}
```

### Clear target (hold position)

```java
private void clearNpcTarget(Role role) {
    MarkedEntitySupport marked = role.getMarkedEntitySupport();
    marked.setMarkedEntity(MarkedEntitySupport.DEFAULT_TARGET_SLOT, null);
}
```

---

## Zone Rotation Handling

When KOTH rotates zones, `getBotObjective()` returns the new zone center automatically (it reads `activeZoneIndex`). On the next tick, `getOrCreateObjectiveMarker()` updates the marker's position via `TransformComponent.setPosition()`. Bots naturally redirect.

No special zone-rotation event needed.

---

## What Stays The Same

- **BotAI state machine** — unchanged. It still tracks IDLE/CHASE/ATTACK states for damage/accuracy logic.
- **applyBotCombatDamage()** — unchanged. Still handles manual bot-on-bot damage.
- **NPC role files** — unchanged. Same `Blook_Skeleton_Pirate_Gunner_Blunderbuss` for all modes. No `_KOTH` suffix needed.
- **Bot spawning** — unchanged. No special setup for objective modes.
- **BotDifficulty** — unchanged. No `objectiveFocus` param needed.

---

## What Changes

| File | Change |
|------|--------|
| `GameMode.java` | Add `default BotObjective getBotObjective(ArenaConfig)` returning null |
| `KingOfTheHillGameMode.java` | Override `getBotObjective()` → return active zone center + radius |
| `BotObjective.java` | New record: `position`, `radius`, `type` |
| `BotManager.java` | Refactor `updateBotTarget()` with objective check, add marker management |

---

## Summary

```
Game mode has objectives?
  ├── No (Duel, Deathmatch, LMS, Kit Roulette)
  │     → getBotObjective() returns null
  │     → updateBotTarget() finds nearest enemy (existing behavior)
  │
  └── Yes (KOTH, future CTF, Domination)
        → getBotObjective() returns position + radius
        → updateBotTarget() checks:
            ├── Enemy within 10 blocks? → Fight enemy
            ├── Bot in zone, no threats? → Hold position (clear target)
            └── Bot not in zone? → Walk to zone (invisible marker)
```

No NPC role file changes. No BotAI changes. No difficulty param changes. The entire feature lives in `updateBotTarget()` + one invisible marker entity per match.

---

## Appendix: Hytale Server API Reference

Every Hytale API class and method used in this design, with package paths and signatures.

### Entity Store & References

```java
import com.hypixel.hytale.component.Ref;              // Entity reference (pointer into ECS store)
import com.hypixel.hytale.component.Store;             // ECS store for a world
import com.hypixel.hytale.component.Holder;            // Entity builder (pre-spawn component holder)
import com.hypixel.hytale.component.AddReason;         // Enum: SPAWN, LOAD, etc.
import com.hypixel.hytale.component.RemoveReason;      // Enum: REMOVE, DESPAWN, etc.
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore; // World entity storage
```

| Method | Signature | Description |
|--------|-----------|-------------|
| `EntityStore.REGISTRY.newHolder()` | `Holder<EntityStore> newHolder()` | Create empty entity builder |
| `holder.putComponent(type, instance)` | `void putComponent(ComponentType, Component)` | Set a component on the builder |
| `holder.ensureComponent(type)` | `void ensureComponent(ComponentType)` | Add component with defaults if missing |
| `holder.addComponent(type, instance)` | `void addComponent(ComponentType, Component)` | Add component (fails if exists) |
| `store.addEntity(holder, reason)` | `Ref<EntityStore> addEntity(Holder, AddReason)` | Spawn entity into world, returns ref |
| `store.removeEntity(ref, reason)` | `void removeEntity(Ref, RemoveReason)` | Remove entity from world |
| `store.getComponent(ref, type)` | `<T> T getComponent(Ref, ComponentType<T>)` | Read component from entity |
| `store.getExternalData()` | `EntityStore getExternalData()` | Access world-level data |
| `store.getExternalData().getWorld()` | `World getWorld()` | Get the World this store belongs to |
| `store.getExternalData().takeNextNetworkId()` | `int takeNextNetworkId()` | Allocate next network ID for client sync |
| `ref.isValid()` | `boolean isValid()` | Check if entity still exists |
| `ref.getStore()` | `Store<EntityStore> getStore()` | Get the store this ref belongs to |

### Transform & Position

```java
import com.hypixel.hytale.math.vector.Vector3d;        // 3D position (double precision)
import com.hypixel.hytale.math.vector.Vector3f;        // 3D rotation / float vector
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent; // Entity position+rotation
```

| Method | Signature | Description |
|--------|-----------|-------------|
| `new Vector3d(x, y, z)` | `Vector3d(double, double, double)` | Create position |
| `new Vector3f(pitch, yaw, roll)` | `Vector3f(float, float, float)` | Create rotation |
| `new TransformComponent(pos, rot)` | `TransformComponent(Vector3d, Vector3f)` | Create transform for entity builder |
| `TransformComponent.getComponentType()` | `static ComponentType<TransformComponent>` | Component type key for store access |
| `transform.getPosition()` | `Vector3d getPosition()` | Read entity position |
| `transform.setPosition(vec)` | `void setPosition(Vector3d)` | Move entity (must be on world thread) |
| `transform.getRotation()` | `Vector3f getRotation()` | Read entity rotation |
| `pos.distanceTo(other)` | `double distanceTo(Vector3d)` | Euclidean distance between two points |

### NPC Entity & Role

```java
import com.hypixel.hytale.server.npc.entities.NPCEntity;                          // NPC entity component
import com.hypixel.hytale.server.npc.role.Role;                                    // NPC role (AI brain)
import com.hypixel.hytale.server.npc.role.support.MarkedEntitySupport;             // Target management
import com.hypixel.hytale.server.npc.role.support.WorldSupport;                    // Attitude overrides
import com.hypixel.hytale.server.npc.role.support.CombatSupport;                   // Combat state
import com.hypixel.hytale.server.core.asset.type.attitude.Attitude;                // HOSTILE, NEUTRAL, FRIENDLY
```

| Method | Signature | Description |
|--------|-----------|-------------|
| `NPCEntity.getComponentType()` | `static ComponentType<NPCEntity>` | Component type key |
| `npcEntity.getRole()` | `Role getRole()` | Get the NPC's active role (AI brain) |
| `npcEntity.getRoleName()` | `String getRoleName()` | Role ID string (e.g. "Blook_Skeleton_...") |
| `npcEntity.getRoleIndex()` | `int getRoleIndex()` | Numeric role index |
| `npcEntity.setLeashPoint(pos)` | `void setLeashPoint(Vector3d)` | Set wander/return center |
| `npcEntity.getLeashPoint()` | `Vector3d getLeashPoint()` | Get current leash position |
| `role.getMarkedEntitySupport()` | `MarkedEntitySupport getMarkedEntitySupport()` | Access target slots |
| `role.getWorldSupport()` | `WorldSupport getWorldSupport()` | Access attitude overrides |
| `role.getCombatSupport()` | `CombatSupport getCombatSupport()` | Access combat state |
| `role.getActiveMotionController()` | `MotionController getActiveMotionController()` | Current movement controller |
| `role.getStateSupport()` | `StateSupport getStateSupport()` | Access state machine |

### MarkedEntitySupport (Target Management)

```java
// No additional import — accessed via role.getMarkedEntitySupport()
```

| Method | Signature | Description |
|--------|-----------|-------------|
| `marked.setMarkedEntity(slot, ref)` | `void setMarkedEntity(String, Ref<EntityStore>)` | Set target entity (null to clear) |
| `marked.getMarkedEntityRef(slot)` | `Ref<EntityStore> getMarkedEntityRef(String)` | Get current target ref |
| `MarkedEntitySupport.DEFAULT_TARGET_SLOT` | `static final String` | = `"LockedTarget"` — the standard target slot |

### WorldSupport (Attitude Overrides)

```java
// No additional import — accessed via role.getWorldSupport()
```

| Method | Signature | Description |
|--------|-----------|-------------|
| `world.overrideAttitude(ref, attitude, duration)` | `void overrideAttitude(Ref, Attitude, double)` | Override attitude toward entity for N seconds |

### StateSupport (State Machine)

```java
import com.hypixel.hytale.server.npc.asset.builder.StateMappingHelper; // State index lookups
```

| Method | Signature | Description |
|--------|-----------|-------------|
| `stateSupport.getStateName()` | `String getStateName()` | Get current state name |
| `stateSupport.setState(ref, state, sub, store)` | `void setState(Ref, String, String, Store)` | Transition to state.subState |
| `stateSupport.isInBusyState()` | `boolean isInBusyState()` | Check if NPC is busy (combat, follow) |
| `stateSupport.getStateHelper()` | `StateMappingHelper getStateHelper()` | Access state index lookups |
| `helper.getStateIndex(name)` | `int getStateIndex(String)` | Returns >= 0 if state exists, -1 otherwise |
| `helper.getSubStateIndex(idx, name)` | `int getSubStateIndex(int, String)` | Check sub-state existence |

### Lightweight Entity Components (for invisible markers)

```java
import com.hypixel.hytale.server.core.entity.entities.ProjectileComponent;        // Lightweight entity base
import com.hypixel.hytale.server.core.modules.entity.component.Intangible;         // No collision flag
import com.hypixel.hytale.server.core.modules.entity.tracker.NetworkId;             // Client visibility
import com.hypixel.hytale.server.core.entity.UUIDComponent;                        // Entity UUID
```

| Method | Signature | Description |
|--------|-----------|-------------|
| `new ProjectileComponent(name)` | `ProjectileComponent(String)` | Create projectile base (invisible) |
| `proj.initialize()` | `void initialize()` | Required init call before spawning |
| `ProjectileComponent.getComponentType()` | `static ComponentType<ProjectileComponent>` | Component type key |
| `Intangible.getComponentType()` | `static ComponentType<Intangible>` | No-collision component type |
| `new NetworkId(id)` | `NetworkId(int)` | Wrap a network ID for client sync |
| `NetworkId.getComponentType()` | `static ComponentType<NetworkId>` | Component type key |
| `UUIDComponent.getComponentType()` | `static ComponentType<UUIDComponent>` | UUID component type |

### NPC Spawning

```java
import com.hypixel.hytale.server.npc.NPCPlugin;       // NPC system entry point
```

| Method | Signature | Description |
|--------|-----------|-------------|
| `NPCPlugin.get()` | `static NPCPlugin get()` | Get singleton NPC plugin |
| `npcPlugin.getIndex(roleName)` | `int getIndex(String)` | Lookup role index by name (-1 if not found) |
| `npcPlugin.spawnNPC(store, model, null, pos, rot)` | `Pair<Ref<EntityStore>, INonPlayerCharacter> spawnNPC(Store, String, String, Vector3d, Vector3f)` | Spawn NPC by model name string |
| `npcPlugin.spawnEntity(store, idx, pos, rot, model, cb)` | `Pair<Ref<EntityStore>, NPCEntity> spawnEntity(Store, int, Vector3d, Vector3f, String, Consumer)` | Spawn NPC by role index |

### World & Universe

```java
import com.hypixel.hytale.server.core.universe.Universe;          // Server universe (all worlds)
import com.hypixel.hytale.server.core.universe.world.World;       // Single world
import com.hypixel.hytale.server.core.universe.PlayerRef;         // Player reference
```

| Method | Signature | Description |
|--------|-----------|-------------|
| `Universe.get()` | `static Universe get()` | Get server universe singleton |
| `Universe.get().getWorld(name)` | `World getWorld(String)` | Get world by name |
| `Universe.get().getPlayer(uuid)` | `PlayerRef getPlayer(UUID)` | Get player by UUID |
| `world.execute(runnable)` | `void execute(Runnable)` | Schedule task on world thread |
| `world.getEntityStore()` | `WorldEntityStore getEntityStore()` | Access world entity storage |
| `world.getEntityStore().getStore()` | `Store<EntityStore> getStore()` | Get ECS store |
| `playerRef.getReference()` | `Ref<EntityStore> getReference()` | Get player's entity ref |

### CombatSupport

```java
// No additional import — accessed via role.getCombatSupport()
```

| Method | Signature | Description |
|--------|-----------|-------------|
| `combat.isExecutingAttack()` | `boolean isExecutingAttack()` | Check if NPC is mid-attack |
| `combat.clearAttackOverrides()` | `void clearAttackOverrides()` | Clear forced attack type |
| `combat.addAttackOverride(str)` | `void addAttackOverride(String)` | Force attack type: `"Attack=Melee"` or `"Attack=Ranged"` |
