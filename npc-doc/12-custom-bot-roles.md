# Custom Bot Roles: HyArena_Bot_Objective

> How and why we built a custom NPC role for objective-aware bots

---

## Why a Custom Role Was Needed

Stock Hytale roles like `Template_Intelligent` and its variants (`_Melee`, `_Ranged`, etc.) are designed for combat AI. They have states like `Idle`, `Combat`, `Chase`, `Search`, and `Alert` — but **no `Follow` state**.

The `Follow` state is what makes an NPC pathfind to a `LockedTarget` entity using `Seek` body motion. Without it, setting a target entity only makes the NPC consider it for combat — it won't walk to an arbitrary position.

For objective modes like KOTH, bots need to walk to capture zones when no enemies are nearby. The only reliable way to move an NPC to a position is:

1. Spawn an invisible marker entity at the target position
2. Set it as the NPC's `LockedTarget`
3. Set the NPC state to `Follow`
4. The NPC's `Seek` body motion pathfinds to the marker

Stock roles can't do step 3 — the `Follow` state doesn't exist and calling `setState("Follow", ...)` silently fails. So we created `HyArena_Bot_Objective`, a Generic role with explicit `Follow` and `Idle` instructions.

---

## Role File Location

```
src/main/resources/
  Server/NPC/
    Roles/HyArena_Bot_Objective.json        # Role definition
    Attitude/Roles/HyArena_Bot_Objective.json  # Attitude groups
```

Both files are loaded automatically because `manifest.json` has `"IncludesAssetPack": true`.

---

## Role Structure Breakdown

### Top-Level Properties

```json
{
  "Type": "Generic",
  "StartState": "Idle",
  "DefaultSubState": "Default",
  "Appearance": "Blook_Skeleton_Pirate_Captain",
  "MaxHealth": 100,
  ...
}
```

| Property | Value | Purpose |
|----------|-------|---------|
| `Type` | `"Generic"` | Standalone role (not inheriting from a parent). Required for custom Instructions. |
| `StartState` | `"Idle"` | NPC begins in Idle state after spawning |
| `DefaultSubState` | `"Default"` | Sub-state used when transitioning states |
| `Appearance` | `"Blook_Skeleton_Pirate_Captain"` | Visual model. Can be any valid model ID. |
| `MaxHealth` | `100` | Base health (modified by BotDifficulty multiplier in Java) |
| `DefaultPlayerAttitude` | `"Hostile"` | Default attitude toward players |
| `DefaultNPCAttitude` | `"Neutral"` | Default attitude toward other NPCs |
| `Invulnerable` | `false` | Takes damage |
| `DropList` | `"Empty"` | No drops on death |

### Movement Properties

```json
{
  "ApplyAvoidance": true,
  "ApplySeparation": true,
  "SeparationDistance": 2.0,
  "Inertia": 1.0,
  "KnockbackScale": 1.0
}
```

| Property | Purpose |
|----------|---------|
| `ApplyAvoidance` | Bots steer around obstacles and each other |
| `ApplySeparation` | Bots maintain minimum distance from each other (prevents stacking) |
| `SeparationDistance` | Minimum distance in blocks between NPCs |
| `Inertia` | Movement momentum (1.0 = normal) |
| `KnockbackScale` | How much knockback this NPC receives (1.0 = full) |

### Inventory & Combat

```json
{
  "InventorySize": 3,
  "HotbarSize": 3,
  "HotbarItems": ["Weapon_Axe_Mithril"]
}
```

The bot spawns with an axe equipped. Combat damage is handled by Java (`BotManager.applyBotCombatDamage()`), not by the NPC's built-in combat AI.

### Spawn & Lifecycle

```json
{
  "DeathAnimationTime": 5.0,
  "DespawnAnimationTime": 0.8,
  "SpawnLockTime": 1.5,
  "SpawnViewDistance": 75,
  "CollisionDistance": 5
}
```

| Property | Purpose |
|----------|---------|
| `DeathAnimationTime` | Seconds the death animation plays before despawn |
| `DespawnAnimationTime` | Seconds the despawn animation plays |
| `SpawnLockTime` | Seconds the NPC is locked in place after spawning |
| `SpawnViewDistance` | Max distance (blocks) at which NPC is visible to clients |
| `CollisionDistance` | Distance for collision detection |

### Motion Controller

```json
{
  "MotionControllerList": [
    {
      "Type": "Walk",
      "MaxWalkSpeed": 5,
      "Acceleration": 25,
      "MaxRotationSpeed": 180,
      "MinJumpHeight": 1.2,
      "Gravity": 10,
      "MaxFallSpeed": 15
    }
  ]
}
```

A single `Walk` controller. The `MaxWalkSpeed` value is the ground speed during `Seek` body motion. `MinJumpHeight` determines the minimum height the NPC can jump over during pathfinding.

---

## State Registration (Critical)

This is the most important discovery from implementation. **Custom states must be registered via `InteractionInstruction` before they can be used in `Instructions` sensors.**

If you define a `Follow` state sensor in `Instructions` without registering it, Hytale rejects the role at startup:

```
FAIL: /Server/NPC/Roles/HyArena_Bot_Objective.json:
  State sensor or State setter action/motion exists without accompanying state/setter: Follow
```

### How to register a state

The `InteractionInstruction` block contains a `State` action that tells Hytale "Follow is a valid state for this role":

```json
{
  "InteractionInstruction": {
    "Instructions": [
      {
        "Sensor": {
          "Type": "And",
          "Sensors": [
            { "Type": "State", "State": "Idle" },
            { "Type": "Target" }
          ]
        },
        "Actions": [
          {
            "Type": "State",
            "State": "Follow"
          }
        ]
      }
    ]
  }
}
```

This reads as: "When the NPC is in `Idle` state AND has a `LockedTarget`, transition to `Follow` state." The `State` action registers `Follow` as a known state.

In practice, we transition states from Java via `role.getStateSupport().setState(ref, "Follow", "Default", store)` — but the InteractionInstruction is still required for Hytale to recognize the state at load time.

---

## Instructions: Follow + Idle

The `Instructions` array defines what the NPC does in each state:

### Follow Behavior

```json
{
  "Name": "FollowBehavior",
  "Sensor": {
    "Type": "And",
    "Sensors": [
      { "Type": "Target" },
      { "Type": "State", "State": "Follow" }
    ]
  },
  "BodyMotion": {
    "Type": "Seek",
    "StopDistance": 1.0,
    "SlowDownDistance": 3.0,
    "AbortDistance": 200.0
  },
  "HeadMotion": {
    "Type": "Observe",
    "AngleRange": [-45.0, 45.0]
  }
}
```

**When active:** NPC is in `Follow` state AND has a `LockedTarget`.

**What it does:**
- `Seek` body motion pathfinds toward the `LockedTarget` entity
- `StopDistance: 1.0` — stops 1 block away from target
- `SlowDownDistance: 3.0` — decelerates within 3 blocks
- `AbortDistance: 200.0` — gives up if target is >200 blocks away
- `Observe` head motion — looks around while walking

### Idle Behavior

```json
{
  "Name": "IdleBehavior",
  "Sensor": {
    "Type": "State",
    "State": "Idle"
  },
  "BodyMotion": {
    "Type": "Nothing"
  },
  "HeadMotion": {
    "Type": "Observe",
    "AngleRange": [-45.0, 45.0]
  }
}
```

**When active:** NPC is in `Idle` state.

**What it does:** Stands still, looks around. Used when the bot has reached the objective and no enemies are nearby.

---

## Attitude File

```json
{
  "Groups": {
    "Hostile": ["Player"]
  }
}
```

Located at `Server/NPC/Attitude/Roles/HyArena_Bot_Objective.json`. Declares that this role is hostile to players by default. Attitude overrides are applied per-target from Java via `role.getWorldSupport().overrideAttitude()`.

---

## How BotManager Uses the Role

### Role Selection

In `BotManager.spawnBotEntity()`, the role is chosen automatically:

```
if match.getGameMode().getBotObjective(config) != null:
    role = "HyArena_Bot_Objective"     // Custom role with Follow
else:
    role = arena.getBotModelId()        // Stock role (Template_Intelligent variant)
```

Non-objective modes (Duel, Deathmatch, LMS, Kit Roulette) are completely unaffected.

### State Transitions from Java

BotManager controls the NPC state explicitly:

| Situation | Java Call | NPC Behavior |
|-----------|-----------|--------------|
| Move to objective | `setState(ref, "Follow", "Default", store)` + set marker as LockedTarget | Pathfinds to invisible marker |
| Arrived at objective | `setState(ref, "Idle", "Default", store)` + clear LockedTarget | Stands still, looks around |
| Enemy nearby | Set enemy as LockedTarget + override attitude HOSTILE | Chases and attacks enemy |

### Per-Bot Invisible Markers

Each bot gets its own invisible marker entity positioned within the capture zone with a random XZ offset. This prevents bots from stacking on the exact same point.

Marker entities are spawned with:
- `ProjectileComponent` — lightweight entity base
- `TransformComponent` — position
- `UUIDComponent` — identity
- `Intangible` — no collision
- `MovementStatesComponent` — required for NPC target tracking
- `NetworkId` — spatial registration

Markers are cleaned up when bots despawn or matches end.

---

## How Instructions Work (General Reference)

An NPC role's behavior is driven by its `Instructions` array. Each instruction has:

| Field | Type | Purpose |
|-------|------|---------|
| `Name` | String | Identifier for debugging |
| `Sensor` | Sensor | Condition that activates this instruction |
| `BodyMotion` | BodyMotion | How the NPC moves its body |
| `HeadMotion` | HeadMotion | How the NPC moves its head |
| `Actions` | Action[] | Side effects when instruction activates |

Instructions are evaluated top-to-bottom each tick. The **first** instruction whose `Sensor` returns true becomes active. Only one instruction is active at a time.

### Common Sensor Types

| Sensor | Purpose |
|--------|---------|
| `State` | True when NPC is in the named state |
| `Target` | True when NPC has a `LockedTarget` |
| `And` | Logical AND of multiple sensors |
| `Or` | Logical OR of multiple sensors |
| `Not` | Inverts a sensor |
| `Player` | True when a player is within range |
| `Damage` | True when NPC received damage |
| `Timer` | True when a named timer is in range |

### Common Body Motions

| Motion | Purpose |
|--------|---------|
| `Seek` | Pathfind toward LockedTarget |
| `Flee` | Move away from LockedTarget |
| `Wander` | Random movement near spawn |
| `Nothing` | Stand still |
| `Path` | Follow a predefined path |
| `MaintainDistance` | Keep specific distance from target |

### Common Head Motions

| Motion | Purpose |
|--------|---------|
| `Observe` | Look around randomly within angle range |
| `Aim` | Look directly at target |
| `Watch` | Rotate to face target |
| `Nothing` | Don't move head |

---

## Extending to New Game Modes

To add objective behavior for a new game mode:

1. Override `getBotObjective(ArenaConfig config)` in your GameMode class
2. Return a `BotObjective` with the target position and zone bounds
3. BotManager handles everything else automatically

No new role files needed — `HyArena_Bot_Objective` is generic enough for any "go to position" objective.

---

## Lessons Learned

1. **State registration is mandatory.** Any state referenced in `Instructions` sensors must appear in a `State` action somewhere (typically in `InteractionInstruction`). Without this, the role fails to load.

2. **Generic vs Variant.** Only `Generic` roles can define `Instructions`. `Variant` roles inherit from a parent and can only modify parameters — they cannot add new states or instructions.

3. **Seek needs a LockedTarget.** The `Seek` body motion pathfinds to whatever entity is set as the `LockedTarget`. To make an NPC walk to a position, spawn an invisible entity at that position and set it as the target.

4. **Separation prevents stacking.** When multiple NPCs share similar objectives, `ApplySeparation` + `SeparationDistance` keeps them from overlapping. Per-bot random offsets add further variety.

5. **State transitions from Java work.** `role.getStateSupport().setState(ref, state, subState, store)` reliably transitions the NPC, as long as the state was registered in the role JSON.
