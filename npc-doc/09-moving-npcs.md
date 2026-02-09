# Moving NPCs Programmatically

> Patterns extracted from working Hytale server code (HycompanionAdapter)

---

## Core Concept

Hytale NPCs are driven by their **AI state machine**. You don't move them directly — you tell their AI *what to follow* and *what state to be in*, and the built-in pathfinding handles the rest.

The two key APIs:

```java
Role role = npcEntity.getRole();

// 1. Tell the NPC what to target
role.getMarkedEntitySupport().setMarkedEntity("LockedTarget", entityRef);

// 2. Tell the NPC what behavior to use
role.getStateSupport().setState(entityRef, "StateName", "SubState", store);
```

---

## Method 1: Move to Position (Invisible Target Entity)

The primary technique for sending an NPC to a specific coordinate. Spawn an invisible entity at the destination and make the NPC follow it.

### Step 1 — Spawn invisible target entity

```java
Holder<EntityStore> holder = EntityStore.REGISTRY.newHolder();

// Use ProjectileComponent as a lightweight entity base
ProjectileComponent projectile = new ProjectileComponent("Projectile");
holder.putComponent(ProjectileComponent.getComponentType(), projectile);

// Set target position
Vector3d targetPos = new Vector3d(x, y, z);
holder.putComponent(TransformComponent.getComponentType(),
    new TransformComponent(targetPos, new Vector3f(0, 0, 0)));

// Required components
holder.ensureComponent(UUIDComponent.getComponentType());
holder.ensureComponent(Intangible.getComponentType()); // No collision

// NetworkId is required for spatial systems to register the entity
holder.addComponent(NetworkId.getComponentType(),
    new NetworkId(store.getExternalData().takeNextNetworkId()));

projectile.initialize();

// Spawn into the world
Ref<EntityStore> targetRef = store.addEntity(holder, AddReason.SPAWN);
```

### Step 2 — Set NPC to follow the target

```java
Role role = npcEntity.getRole();

// Point the NPC at the invisible target
role.getMarkedEntitySupport().setMarkedEntity("LockedTarget", targetRef);

// Activate Follow state — NPC pathfinds to the target
role.getStateSupport().setState(entityRef, "Follow", "Default", store);
```

### Step 3 — Monitor arrival

Poll the NPC's position on a timer. When close enough to the target, clean up:

```java
TransformComponent t = store.getComponent(entityRef, TransformComponent.getComponentType());
Vector3d currentPos = t.getPosition();

double dx = currentPos.getX() - targetX;
double dz = currentPos.getZ() - targetZ;
double distSq = dx * dx + dz * dz;

// Arrival threshold: 3 blocks (9.0 squared)
if (distSq < 9.0) {
    // NPC arrived — clean up
}
```

### Step 4 — Cleanup

Always clean up both the NPC state and the invisible entity:

```java
// Reset NPC to idle
role.getStateSupport().setState(entityRef, "Idle", "Default", store);
role.getMarkedEntitySupport().setMarkedEntity("LockedTarget", null);

// Remove the invisible target entity
store.removeEntity(targetRef, RemoveReason.REMOVE);
```

### Stuck Detection

If the NPC hasn't moved more than 0.2 blocks in 3 seconds, consider it stuck:

```java
double movedDist = currentPos.distanceTo(lastRecordedPos);
if (movedDist < 0.2 && timeSinceLastMove > 3000) {
    // NPC is stuck — abort or teleport
}
```

---

## Method 2: Follow a Player

Same pattern but using the player's entity reference directly instead of an invisible entity.

```java
// Get the player's entity reference
PlayerRef targetPlayer = Universe.get().getPlayerByUsername(name, NameMatching.EXACT_IGNORE_CASE);
Ref<EntityStore> playerEntityRef = targetPlayer.getReference();

// Set as follow target
role.getMarkedEntitySupport().setMarkedEntity("LockedTarget", playerEntityRef);
role.getStateSupport().setState(entityRef, "Follow", null, store);
```

No invisible entity needed — the NPC follows the player in real time.

### Stop following

```java
role.getMarkedEntitySupport().setMarkedEntity("LockedTarget", null);
role.getStateSupport().setState(entityRef, "Idle", "Default", store);
```

---

## Method 3: Attack / Chase a Target

Same LockedTarget mechanism, but use a combat state instead of Follow.

```java
// Set attack target
role.getMarkedEntitySupport().setMarkedEntity("LockedTarget", targetEntityRef);

// Try combat states in order of preference
StateMappingHelper stateHelper = role.getStateSupport().getStateHelper();

if (stateHelper.getStateIndex("Combat") >= 0) {
    role.getStateSupport().setState(entityRef, "Combat", "Attack", store);
}
else if (stateHelper.getStateIndex("Chase") >= 0) {
    role.getStateSupport().setState(entityRef, "Chase", "Default", store);
}
else if (stateHelper.getStateIndex("Attack") >= 0) {
    role.getStateSupport().setState(entityRef, "Attack", "Default", store);
}
```

### Stop attacking

```java
// Clear combat overrides if any
CombatSupport combatSupport = role.getCombatSupport();
if (combatSupport != null) {
    combatSupport.clearAttackOverrides();
}

role.getMarkedEntitySupport().setMarkedEntity("LockedTarget", null);
role.getStateSupport().setState(entityRef, "Idle", "Default", store);
```

---

## Method 4: Instant Teleport

For immediate repositioning without pathfinding.

```java
world.execute(() -> {
    TransformComponent transform = store.getComponent(entityRef, TransformComponent.getComponentType());
    Vector3d newPos = new Vector3d(x, y, z);
    transform.setPosition(newPos);

    // Update leash point so NPC wanders around new location
    npcEntity.setLeashPoint(newPos);
});
```

**Important:** Always update the leash point after teleporting, otherwise the NPC's AI will try to walk back to its old leash position.

---

## Method 5: Smooth Rotation

Rotate the NPC's body to face a direction over multiple ticks.

### Calculate target yaw

```java
double dx = targetX - npcPos.getX();
double dz = targetZ - npcPos.getZ();
float targetYaw = TrigMathUtil.atan2(-dx, -dz);
```

### Lerp body rotation each tick

```java
// Run every 50ms for ~15 ticks
TransformComponent transform = store.getComponent(entityRef, TransformComponent.getComponentType());
Vector3f bodyRotation = transform.getRotation();
float currentYaw = bodyRotation.getYaw();

// Shortest angle difference
float diff = targetYaw - currentYaw;
while (diff > Math.PI)  diff -= 2 * Math.PI;
while (diff < -Math.PI) diff += 2 * Math.PI;

// Lerp 15% per tick
float newYaw = currentYaw + diff * 0.15f;
bodyRotation.setYaw(newYaw);
transform.setRotation(bodyRotation);

// Update leash heading so AI knows facing direction
npcEntity.setLeashHeading(newYaw);

// Stop when aligned (within ~9 degrees)
if (Math.abs(diff) < 0.20f) {
    // Done rotating
}
```

---

## Method 6: Leash Point (Passive Wander Center)

Not direct movement, but controls where the NPC idles and returns to.

```java
Vector3d newCenter = new Vector3d(x, y, z);
npcEntity.setLeashPoint(newCenter);
npcEntity.setLeashHeading(yaw);
npcEntity.setLeashPitch(pitch);
```

The NPC will wander within `WanderRadius` of the leash point and return within `LeashTimer` seconds if it strays beyond `LeashDistance`.

---

## State Reference

| State | SubState | Behavior |
|-------|----------|----------|
| `Idle` | `Default` | Wander near leash point |
| `Follow` | `Default` | Pathfind toward LockedTarget |
| `Combat` | `Attack` | Chase and attack LockedTarget |
| `Chase` | `Default` | Pursue LockedTarget |
| `Chase` | `Attack` | Pursue and attack LockedTarget |
| `Attack` | `Default` | Attack LockedTarget (simple NPCs) |

**Note:** Available states depend on the NPC's role template. Use `StateHelper.getStateIndex("StateName")` to check if a state exists before setting it.

---

## Important Rules

1. **All entity operations must run on the world thread** — wrap in `world.execute(() -> { ... })`
2. **Always clean up invisible entities** — leaked entities persist in the world
3. **Reset animation before state changes** — prevents visual glitches:
   ```java
   AnimationUtils.stopAnimation(entityRef, AnimationSlot.Action, true, store);
   npcEntity.playAnimation(entityRef, AnimationSlot.Action, "Idle", store);
   ```
4. **Check `entityRef.isValid()`** before any operation — references can go stale
5. **Update leash point after teleports** — otherwise the NPC walks back
6. **LockedTarget is the universal target slot** — used for follow, combat, and move-to

---

## Import Reference

```java
import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.math.util.TrigMathUtil;
import com.hypixel.hytale.protocol.AnimationSlot;
import com.hypixel.hytale.server.core.entity.AnimationUtils;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.component.Intangible;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.tracker.NetworkId;
import com.hypixel.hytale.server.core.entity.entities.ProjectileComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.role.Role;
import com.hypixel.hytale.server.npc.role.support.MarkedEntitySupport;
```
