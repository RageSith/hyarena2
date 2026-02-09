# NPC Actions Reference

All Action types available for NPC behavior scripting. Actions are executable operations that NPCs can perform, grouped by category.

> Every action has the common attribute **Once** (Stable) — `Type: Boolean, Optional (Default: false)` — "Execute only once". This attribute is listed once here and omitted from individual action tables for brevity.

---

## Table of Contents

- [Audiovisual](#audiovisual)
  - [Appearance](#appearance)
  - [DisplayName](#displayname)
  - [ModelAttachment](#modelattachment)
  - [PlayAnimation](#playanimation)
  - [PlaySound](#playsound)
  - [SpawnParticles](#spawnparticles)
- [Combat](#combat)
  - [ApplyEntityEffect](#applyentityeffect)
  - [Attack](#attack)
- [Core Components](#core-components)
  - [AddToHostileTargetMemory](#addtohostiletargetmemory)
  - [CombatAbility](#combatability)
  - [FlockBeacon](#flockbeacon)
  - [FlockState](#flockstate)
  - [FlockTarget](#flocktarget)
  - [JoinFlock](#joinflock)
  - [LeaveFlock](#leaveflock)
  - [TriggerSpawnBeacon](#triggerspawnbeacon)
- [Debug](#debug)
  - [Log](#log)
  - [Test](#test)
- [Entity](#entity)
  - [IgnoreForAvoidance](#ignoreforavoidance)
  - [OverrideAttitude](#overrideattitude)
  - [ReleaseTarget](#releasetarget)
  - [SetMarkedTarget](#setmarkedtarget)
  - [SetStat](#setstat)
- [Interaction](#interaction)
  - [LockOnInteractionTarget](#lockoninteractiontarget)
  - [SetInteractable](#setinteractable)
- [Items](#items)
  - [DropItem](#dropitem)
  - [Inventory](#inventory)
  - [PickUpItem](#pickupitem)
- [Lifecycle](#lifecycle)
  - [DelayDespawn](#delaydespawn)
  - [Despawn](#despawn)
  - [Die](#die)
  - [Remove](#remove)
  - [Role](#role)
  - [Spawn](#spawn)
- [Message](#message)
  - [Beacon](#beacon)
  - [Notify](#notify)
- [Movement](#movement)
  - [Crouch](#crouch)
  - [OverrideAltitude](#overridealtitude)
  - [RecomputePath](#recomputepath)
- [NPC](#npc)
  - [CompleteTask](#completetask)
  - [Mount](#mount)
  - [OpenBarterShop](#openbartershop)
  - [OpenShop](#openshop)
  - [StartObjective](#startobjective)
- [Path](#path)
  - [MakePath](#makepath)
- [State Machine](#state-machine)
  - [ParentState](#parentstate)
  - [State](#state)
  - [ToggleStateEvaluator](#togglestateevaluator)
- [Timer](#timer)
  - [SetAlarm](#setalarm)
  - [TimerContinue](#timercontinue)
  - [TimerModify](#timermodify)
  - [TimerPause](#timerpause)
  - [TimerRestart](#timerrestart)
  - [TimerStart](#timerstart)
  - [TimerStop](#timerstop)
- [Utility](#utility)
  - [Nothing](#nothing)
  - [Random](#random)
  - [ResetInstructions](#resetinstructions)
  - [Sequence](#sequence)
  - [SetFlag](#setflag)
  - [Timeout](#timeout)
- [World](#world)
  - [PlaceBlock](#placeblock)
  - [ResetBlockSensors](#resetblocksensors)
  - [ResetPath](#resetpath)
  - [ResetSearchRays](#resetsearchrays)
  - [SetBlockToPlace](#setblocktoplace)
  - [SetLeashPosition](#setleashposition)
  - [StorePosition](#storeposition)
  - [TriggerSpawners](#triggerspawners)

---

## Audiovisual

### Appearance

**Stability:** Stable

Change model of NPC to given appearance.

| Attribute | Stability | Description | Type | Required | Default | Computable | Constraint |
|-----------|-----------|-------------|------|----------|---------|------------|------------|
| Appearance | Stable | Model name to use | Asset | Required | — | No | — |

---

### DisplayName

**Stability:** Stable

Set the name displayed above NPC.

| Attribute | Stability | Description | Type | Required | Default | Computable | Constraint |
|-----------|-----------|-------------|------|----------|---------|------------|------------|
| DisplayName | Stable | Name to display above NPC | String | Required | — | Yes | — |

---

### ModelAttachment

**Stability:** Stable

Set an attachment on the current NPC model.

| Attribute | Stability | Description | Type | Required | Default | Computable | Constraint |
|-----------|-----------|-------------|------|----------|---------|------------|------------|
| Slot | Stable | The attachment slot to set | String | Required | — | Yes | String must be not empty |
| Attachment | Stable | The attachment to set, or empty to remove | String | Required | — | Yes | — |

---

### PlayAnimation

**Stability:** Experimental

Play an animation.

| Attribute | Stability | Description | Type | Required | Default | Computable | Constraint |
|-----------|-----------|-------------|------|----------|---------|------------|------------|
| Slot | Stable | The animation slot to play on | Flag | Required | — | No | Flag Values: `Status`, `Action`, `Face` |
| Animation | Stable | The animation ID to play | String | Optional | null | Yes | — |

---

### PlaySound

**Stability:** Stable

Plays a sound to players within a specified range.

| Attribute | Stability | Description | Type | Required | Default | Computable | Constraint |
|-----------|-----------|-------------|------|----------|---------|------------|------------|
| SoundEventId | Stable | The sound event to play | Asset | Required | — | Yes | — |

---

### SpawnParticles

**Stability:** WorkInProgress

Spawn particle system visible within a given range with an offset relative to NPC heading.

| Attribute | Stability | Description | Type | Required | Default | Computable | Constraint |
|-----------|-----------|-------------|------|----------|---------|------------|------------|
| ParticleSystem | Stable | Particle system to spawn | Asset | Required | — | Yes | — |
| Range | Stable | Maximum visibility range | Double | Optional | 75.0 | Yes | Value must be greater than 0 |
| Offset | Stable | Offset relative to footpoint in view direction of NPC | Array (Double) | Optional | null | Yes | — |

---

## Combat

### ApplyEntityEffect

**Stability:** Stable

Applies an entity effect to the target or self.

| Attribute | Stability | Description | Type | Required | Default | Computable | Constraint |
|-----------|-----------|-------------|------|----------|---------|------------|------------|
| EntityEffect | Stable | The entity effect to apply | Asset | Required | — | Yes | — |
| UseTarget | Stable | Use the sensor-provided target for the action, self otherwise | Boolean | Optional | true | Yes | If true, must be attached to a sensor that provides one of player target, NPC target |

---

### Attack

**Stability:** Experimental

Let NPC start an attack. When an attack is running no new attack is started.

| Attribute | Stability | Description | Type | Required | Default | Computable | Constraint |
|-----------|-----------|-------------|------|----------|---------|------------|------------|
| Attack | Experimental | Attack pattern to use. If omitted, will cancel current attack | Asset | Optional | null | Yes | — |
| AttackType | Stable | The interaction type to use | Flag | Optional | Primary | Yes | Flag Values: `Primary`, `Secondary`, `Ability1`, `Ability2`, `Ability3` |
| ChargeFor | Stable | How long to charge for. 0 indicates no charging. Also doubles as how long to block for | Double | Optional | 0.0 | Yes | Value must be >= 0 |
| AttackPauseRange | Stable | Range of minimum pause between attacks | Array (Double) | Optional | [0.0, 0.0] | Yes | Values must be >= 0, <= MAX_DOUBLE, in weakly ascending order |
| AimingTimeRange | Stable | A range from which to pick a random value denoting the max time the NPC will wait for aiming before launching the attack | Array (Double) | Optional | [0.0, 0.0] | Yes | Values must be >= 0, <= MAX_DOUBLE, in weakly ascending order |
| LineOfSight | Experimental | Check Line of Sight before firing | Boolean | Optional | false | No | — |
| AvoidFriendlyFire | Experimental | Tries to avoid friendly fire if true | Boolean | Optional | true | No | — |
| BallisticMode | WorkInProgress | Trajectory to use | Flag | Optional | Short | No | Flag Values: `Short`, `Long`, `Random`, `Alternate` |
| MeleeConeAngle | WorkInProgress | Cone angle considered for on target for melee | Double | Optional | 30.0 | No | Value must be > 0 and <= 360 |
| DamageFriendlies | Stable | Whether this attack should bypass ignored damage groups and deal damage to the target | Boolean | Optional | false | No | — |
| SkipAiming | Stable | Whether aiming should be skipped and the attack just executed immediately | Boolean | Optional | false | No | — |
| ChargeDistance | Stable | If this is a charge attack, the distance required for the charge | Double | Optional | 0.0 | Yes | Value must be >= 0 |
| InteractionVars | Stable | Set of interaction vars for modifying the interaction | CodecObject | Optional | null | No | — |

**Cross-attribute constraints:**
- If `SkipAiming` is true, `LineOfSight` and `AvoidFriendlyFire` must be false.

---

## Core Components

### AddToHostileTargetMemory

**Stability:** Stable

Adds the passed target from the sensor to the hostile target memory.

| Attribute | Stability | Description | Type | Required | Default | Computable | Constraint |
|-----------|-----------|-------------|------|----------|---------|------------|------------|
| *(no extra attributes)* | | | | | | | |

**Constraints:** Must be attached to a sensor that provides one of player target, NPC target.

---

### CombatAbility

**Stability:** Stable

Starts the combat ability selected by the combat action evaluator.

| Attribute | Stability | Description | Type | Required | Default | Computable | Constraint |
|-----------|-----------|-------------|------|----------|---------|------------|------------|
| *(no extra attributes)* | | | | | | | |

---

### FlockBeacon

**Stability:** Experimental

Let the NPC send out a message to the flock members.

| Attribute | Stability | Description | Type | Required | Default | Computable | Constraint |
|-----------|-----------|-------------|------|----------|---------|------------|------------|
| Message | Stable | Message to send to targets | String | Required | — | Yes | String must be not empty |
| SendTargetSlot | Stable | The marked target slot to send. If omitted, sends own position | String | Optional | null | No | String value must be either null or not empty |
| ExpirationTime | Stable | The number of seconds that the message should last and be acknowledged by the receiving NPC. -1 represents infinite time | Double | Optional | 1.0 | No | Value must be >= 0 or equal -1 |
| SendToSelf | Stable | Send the message to self | Boolean | Optional | true | No | — |
| SendToLeaderOnly | Stable | Only send the message to the leader of the flock | Boolean | Optional | false | No | — |

---

### FlockState

**Stability:** Stable

Sets the state name for the flock the NPC is member of. The flock leader is explicitly excluded from this operation.

| Attribute | Stability | Description | Type | Required | Default | Computable | Constraint |
|-----------|-----------|-------------|------|----------|---------|------------|------------|
| State | Stable | State name to set | String | Required | — | Yes | String must be a valid state string (e.g. `Main.Test`). If nested within a substate, the main state may be omitted (e.g. `.Test`). |

---

### FlockTarget

**Stability:** Stable

Sets or clears the locked target for the flock the NPC is member of. If Clear flag is true, the locked target is cleared otherwise it is set to the target. The flock leader is explicitly excluded from this operation.

| Attribute | Stability | Description | Type | Required | Default | Computable | Constraint |
|-----------|-----------|-------------|------|----------|---------|------------|------------|
| Clear | Stable | If true, clear locked target. If false, set to current target | Boolean | Optional | false | No | — |
| TargetSlot | Stable | The target slot to use | String | Optional | LockedTarget | Yes | String must be not empty |

**Constraints:** Must be attached to a sensor that provides one of player target, NPC target.

---

### JoinFlock

**Stability:** Stable

Tries to build/join flock with target. Fails if both NPC and target are in a flock. If either NPC or target are in a flock, the one not in flock tries to join existing flock. If NPC and target are both not in a flock, a new flock with NPC is created and target is tried to be joined. Joining the flock can be rejected if the joining entity does have the correct type or the flock is full. This can be overridden by setting the ForceJoin flag to true.

| Attribute | Stability | Description | Type | Required | Default | Computable | Constraint |
|-----------|-----------|-------------|------|----------|---------|------------|------------|
| ForceJoin | Stable | Disables checking flock join conditions test and forces joining flock | Boolean | Optional | false | No | — |

**Constraints:** Must be attached to a sensor that provides one of player target, NPC target.

---

### LeaveFlock

**Stability:** Stable

NPC leaves flock currently in. Does nothing when not in flock.

| Attribute | Stability | Description | Type | Required | Default | Computable | Constraint |
|-----------|-----------|-------------|------|----------|---------|------------|------------|
| *(no extra attributes)* | | | | | | | |

---

### TriggerSpawnBeacon

**Stability:** Stable

Trigger the nearest spawn beacon matching the configuration id.

| Attribute | Stability | Description | Type | Required | Default | Computable | Constraint |
|-----------|-----------|-------------|------|----------|---------|------------|------------|
| BeaconSpawn | Stable | The beacon spawn config ID | Asset | Required | — | Yes | — |
| Range | Stable | The distance to search for a beacon to trigger | Integer | Required | — | Yes | Value must be greater than 0 |
| TargetSlot | Stable | A slot to use as the target for the spawned NPC. If omitted the NPC itself will be used | String | Optional | null | No | String value must be either null or not empty |

---

## Debug

### Log

**Stability:** Stable

Log a message to console.

| Attribute | Stability | Description | Type | Required | Default | Computable | Constraint |
|-----------|-----------|-------------|------|----------|---------|------------|------------|
| Message | Stable | Text to print to console | String | Required | — | Yes | — |

---

### Test

**Stability:** Experimental

Test action to exercise attribute evaluation (DO NOT USE).

| Attribute | Stability | Description | Type | Required | Default | Computable | Constraint |
|-----------|-----------|-------------|------|----------|---------|------------|------------|
| Boolean | Deprecated | Boolean True | Boolean | Optional | true | Yes | — |
| Double | Deprecated | Double 0 | Double | Optional | 0.0 | Yes | — |
| Float | Deprecated | Float 0 | Double | Optional | 0.0 | Yes | — |
| Int | Deprecated | Int 0 | Integer | Optional | 0.0 | Yes | — |
| String | Deprecated | String Test | String | Optional | Test | Yes | — |
| Enum | Deprecated | Enum RoleDebugFlags Collisions | Flag | Optional | Collisions | Yes | Flag Values: `Overlaps`, `ProbeBlockCollisions`, `SteeringRole`, `DisplayInternalId`, `Pathfinder`, `DisplayStamina`, `FlockDamage`, `Collisions`, `DisplayFreeSlots`, `DisplayTime`, `DisplayName`, `DisplayLightLevel`, `DisplayAnim`, `TraceSensorFailures`, `DisplayHP`, `DisplayFlock`, `DisplaySpeed`, `ValidateMath`, `VisSeparation`, `DisplayState`, `TraceSuccess`, `MotionControllerMove`, `TraceFail`, `DisplayCustom`, `VisAvoidance`, `Flock`, `MotionControllerSteer`, `BlockCollisions`, `ValidatePositions`, `DisplayTarget`, `BeaconMessages` |
| EnumSet | Deprecated | EnumSet Collisions Flock | FlagSet | Optional | [Flock, Collisions] | Yes | Same Flag Values as Enum above |
| Asset | Deprecated | Asset Sheep | Asset | Optional | Sheep | Yes | — |
| DoubleArray | Deprecated | DoubleArray [1,2] 0-10 | Array (Double) | Optional | [1.0, 2.0] | Yes | — |
| StringArray | Deprecated | StringArray [a,b] 0-10 | Array (String) | Optional | [a, b] | Yes | — |

---

## Entity

### IgnoreForAvoidance

**Stability:** Stable

Set the target slot of an entity that should be ignored during avoidance.

| Attribute | Stability | Description | Type | Required | Default | Computable | Constraint |
|-----------|-----------|-------------|------|----------|---------|------------|------------|
| TargetSlot | Stable | The target slot containing the entity to be ignored | String | Required | — | Yes | String must be not empty |

---

### OverrideAttitude

**Stability:** Stable

Override this NPC's attitude towards the provided target for a given duration.

| Attribute | Stability | Description | Type | Required | Default | Computable | Constraint |
|-----------|-----------|-------------|------|----------|---------|------------|------------|
| Attitude | Stable | The attitude to set | Flag | Required | — | Yes | Flag Values: `HOSTILE` (is hostile towards the target), `FRIENDLY` (is friendly towards the target), `NEUTRAL` (is neutral towards the target), `REVERED` (reveres the target), `IGNORE` (is ignoring the target) |
| Duration | Stable | The duration to override for | Double | Optional | 10.0 | Yes | Value must be greater than 0 |

**Constraints:** Must be attached to a sensor that provides one of player target, NPC target.

---

### ReleaseTarget

**Stability:** Stable

Clear locked target for NPC.

| Attribute | Stability | Description | Type | Required | Default | Computable | Constraint |
|-----------|-----------|-------------|------|----------|---------|------------|------------|
| TargetSlot | Stable | The target slot to release | String | Optional | LockedTarget | Yes | String must be not empty |

---

### SetMarkedTarget

**Stability:** Stable

Explicitly sets a marked target in a given slot.

| Attribute | Stability | Description | Type | Required | Default | Computable | Constraint |
|-----------|-----------|-------------|------|----------|---------|------------|------------|
| TargetSlot | Stable | The target slot to set a target to | String | Optional | LockedTarget | Yes | String must be not empty |

**Constraints:** Must be attached to a sensor that provides one of player target, NPC target.

---

### SetStat

**Stability:** Stable

Sets (or adds to) an entity stat on the NPC.

| Attribute | Stability | Description | Type | Required | Default | Computable | Constraint |
|-----------|-----------|-------------|------|----------|---------|------------|------------|
| Stat | Stable | The entity stat to affect | Asset | Required | — | Yes | — |
| Value | Stable | The value to set the stat to | Double | Required | — | Yes | — |
| Add | Stable | Add the value to the existing value instead of setting it | Boolean | Optional | false | Yes | — |

---

## Interaction

### LockOnInteractionTarget

**Stability:** Stable

Locks on to the currently iterated player in the interaction instruction.

| Attribute | Stability | Description | Type | Required | Default | Computable | Constraint |
|-----------|-----------|-------------|------|----------|---------|------------|------------|
| TargetSlot | Stable | The target slot to use | String | Optional | LockedTarget | Yes | String must be not empty |

---

### SetInteractable

**Stability:** Stable

Set whether the currently iterated player in the interaction instruction should be able to interact with this NPC.

| Attribute | Stability | Description | Type | Required | Default | Computable | Constraint |
|-----------|-----------|-------------|------|----------|---------|------------|------------|
| Interactable | Stable | Toggle whether the currently iterated player should be able to interact with this NPC | Boolean | Optional | true | Yes | — |
| Hint | Stable | The interaction hint translation key to show for this player (e.g. `interactionHints.trade`) | String | Optional | null | No | — |
| ShowPrompt | Stable | Whether to show the F-key interaction prompt. Set to false for contextual-only interactions (e.g. shearing with tools) | Boolean | Optional | true | No | — |

---

## Items

### DropItem

**Stability:** Stable

Drop an item. Can be a specific item, or from a drop table.

| Attribute | Stability | Description | Type | Required | Default | Computable | Constraint |
|-----------|-----------|-------------|------|----------|---------|------------|------------|
| Delay | Stable | Range of time to delay in seconds | Array (Double) | Optional | [1.0, 1.0] | Yes | Values must be >= 0, <= MAX_DOUBLE, in weakly ascending order |
| Item | Stable | A specific item to drop | Asset | Optional | null | Yes | — |
| DropList | Stable | A reference to an item drop list | Asset | Optional | null | Yes | — |
| ThrowSpeed | Stable | The throw speed to use | Double | Optional | 1.0 | No | Value must be > 0 and <= MAX_FLOAT |
| Distance | Stable | The range from which to pick a distance to throw the item | Array (Double) | Optional | [1.0, 1.0] | No | Values must be >= 0, <= MAX_DOUBLE, in weakly ascending order |
| DropSector | Stable | The sector to spread drops in relative to view direction of NPC in degrees | Array (Double) | Optional | [0.0, 0.0] | No | Values must be >= -360, <= 360, in weakly ascending order |
| PitchHigh | Stable | Whether to pitch high or pitch low instead | Boolean | Optional | false | No | — |

**Cross-attribute constraints:**
- One (and only one) of `Item`, `DropList` must be provided.

---

### Inventory

**Stability:** Stable

Add or remove a number of items from an inventory. Can also be used to equip them.

| Attribute | Stability | Description | Type | Required | Default | Computable | Constraint |
|-----------|-----------|-------------|------|----------|---------|------------|------------|
| Operation | Stable | Operation to perform | Flag | Optional | Add | Yes | Flag Values: `Add` (Add items to inventory), `Remove` (Remove items from inventory), `Equip` (Equip item as weapon or armour), `EquipHotbar` (Equips the item from a specific hotbar slot), `EquipOffHand` (Equips the item from a specific off-hand slot), `SetHotbar` (Sets the hotbar item in a specific slot), `SetOffHand` (Sets the off-hand item in a specific slot), `RemoveHeldItem` (Destroy the held item), `ClearHeldItem` (Clear the held item) |
| Count | Stable | Number of items to add/remove | Integer | Optional | 1 | Yes | Value must be greater than 0 |
| Item | Stable | Item type to add, remove, or equip | Asset | Optional | (empty) | Yes | — |
| UseTarget | Stable | Use the sensor-provided target for the action | Boolean | Optional | true | Yes | If true, must be attached to a sensor that provides one of player target, NPC target |
| Slot | Stable | The hotbar or off-hand to effect. Only valid for Hotbar/OffHand Set/Equip operations | Integer | Optional | 0 | Yes | — |

---

### PickUpItem

**Stability:** Stable

Pick up an item. In hoover mode, will match to the Item array. Otherwise, requires a target to be provided (e.g. by a DroppedItemSensor).

| Attribute | Stability | Description | Type | Required | Default | Computable | Constraint |
|-----------|-----------|-------------|------|----------|---------|------------|------------|
| Delay | Stable | Range of time to delay in seconds | Array (Double) | Optional | [1.0, 1.0] | Yes | Values must be >= 0, <= MAX_DOUBLE, in weakly ascending order |
| Range | Stable | The range the item will be picked up from | Double | Optional | 1.0 | Yes | Value must be greater than 0 |
| StorageTarget | Experimental | Where to prioritise putting the item | Flag | Optional | Hotbar | Yes | Flag Values: `Hotbar` (Prioritise hotbar), `Inventory` (Prioritise inventory), `Destroy` (Destroy the item) |
| Hoover | Stable | Suck up all items in range with optional cooldown. Can be filtered with a list of glob patterns. Ignored outside hoover mode | Boolean | Optional | false | No | — |
| Items | Stable | A list of glob item patterns to match for hoover mode. If omitted, will match any item. Ignored outside hoover mode | AssetArray (Item) | Optional | null | Yes | — |

**Cross-attribute constraints:**
- If `Hoover` is false, must be attached to a sensor that provides dropped item target.

---

## Lifecycle

### DelayDespawn

**Stability:** Stable

Delay the despawning cycle for some amount of time.

| Attribute | Stability | Description | Type | Required | Default | Computable | Constraint |
|-----------|-----------|-------------|------|----------|---------|------------|------------|
| Time | Stable | How long to set the delay | Double | Required | — | No | Value must be greater than 0 |
| Shorten | Stable | Set the delay to either the current delay or the given time, whatever is smaller | Boolean | Optional | false | No | — |

---

### Despawn

**Stability:** Stable

Trigger the NPC to start the despawning cycle. If the script contains a despawn sensor it will run that action/motion before removing.

| Attribute | Stability | Description | Type | Required | Default | Computable | Constraint |
|-----------|-----------|-------------|------|----------|---------|------------|------------|
| Force | Stable | Force the NPC to remove automatically | Boolean | Optional | false | No | — |

---

### Die

**Stability:** Stable

Kill the NPC.

| Attribute | Stability | Description | Type | Required | Default | Computable | Constraint |
|-----------|-----------|-------------|------|----------|---------|------------|------------|
| *(no extra attributes)* | | | | | | | |

---

### Remove

**Stability:** Stable

Erase the target entity from the world (no death animation).

| Attribute | Stability | Description | Type | Required | Default | Computable | Constraint |
|-----------|-----------|-------------|------|----------|---------|------------|------------|
| UseTarget | Stable | Use the sensor-provided target for the action | Boolean | Optional | true | Yes | If true, must be attached to a sensor that provides one of player target, NPC target |

---

### Role

**Stability:** Stable

Change the Role of the NPC.

| Attribute | Stability | Description | Type | Required | Default | Computable | Constraint |
|-----------|-----------|-------------|------|----------|---------|------------|------------|
| Role | Stable | The name of the Role to change to | Asset | Required | — | Yes | — |
| ChangeAppearance | Stable | Whether the appearance of the new Role should be used | Boolean | Optional | true | Yes | — |
| State | Stable | State name to set | String | Optional | null | Yes | String must be a valid state string (e.g. `Main.Test`). If nested within a substate, the main state may be omitted (e.g. `.Test`). |

---

### Spawn

**Stability:** Experimental

Spawn an NPC.

| Attribute | Stability | Description | Type | Required | Default | Computable | Constraint |
|-----------|-----------|-------------|------|----------|---------|------------|------------|
| SpawnDirection | Experimental | Direction of spawn cone relative to view direction (in degrees) | Double | Optional | 0.0 | Yes | Value must be >= -180 and <= 180 |
| SpawnAngle | Experimental | Cone width of spawn direction (in degrees) | Double | Optional | 360.0 | Yes | Value must be >= 0 and <= 360 |
| FanOut | Experimental | Fan NPCs out equally over angle | Boolean | Optional | false | Yes | — |
| DistanceRange | Stable | Distance from spawner to spawn | Array (Double) | Optional | [1.0, 1.0] | Yes | Values must be > 0, <= 128, in weakly ascending order |
| CountRange | Stable | Number of NPCs to spawn | Array (Integer) | Optional | [5, 5] | Yes | Values must be > 0, <= 100, in weakly ascending order |
| DelayRange | Stable | Time between consecutive spawns in seconds | Array (Double) | Optional | [0.25, 0.25] | Yes | Values must be >= 0, <= MAX_DOUBLE, in weakly ascending order |
| Kind | Experimental | NPC role to spawn | String | Required | — | Yes | String must be not empty |
| Flock | Stable | Flock definition to spawn | Asset | Optional | null | Yes | — |
| LaunchAtTarget | WorkInProgress | Launch the spawned NPC at target position/entity | Boolean | Optional | false | Yes | — |
| PitchHigh | Stable | If launching at a target, use high pitch | Boolean | Optional | true | Yes | — |
| LaunchSpread | Stable | The radius of the circle centred on the target within which to spread thrown NPCs | Double | Optional | 0.0 | Yes | Value must be >= 0 |
| JoinFlock | Stable | Whether to join the parent NPC's flock | Boolean | Optional | false | Yes | — |
| SpawnState | Stable | An optional state to set on the spawned NPC if it exists | String | Optional | null | Yes | String value must be either null or not empty |
| SpawnSubState | Stable | An optional substate to set on the spawned NPC if it exists | String | Optional | null | Yes | String value must be either null or not empty |

**Cross-attribute constraints:**
- If `LaunchAtTarget` is true, must be attached to a sensor that provides one of player target, NPC target, dropped item target, vector position.

---

## Message

### Beacon

**Stability:** Experimental

Let the NPC send out a message to a target group of entities within a certain distance.

| Attribute | Stability | Description | Type | Required | Default | Computable | Constraint |
|-----------|-----------|-------------|------|----------|---------|------------|------------|
| Message | Experimental | Message to send to targets | String | Required | — | Yes | String must be not empty |
| Range | Experimental | The maximum range to send the message | Double | Optional | 64.0 | No | Value must be greater than 0 |
| TargetGroups | Experimental | The target group(s) to send the message to | AssetArray (TagSet) | Required | — | Yes | — |
| SendTargetSlot | Stable | The target slot of the marked entity to send. Omit to send own position | String | Optional | null | Yes | String value must be either null or not empty |
| ExpirationTime | Experimental | The number of seconds that the message should last and be acknowledged by the receiving NPC. -1 represents infinite time | Double | Optional | 1.0 | No | Value must be >= 0 or equal -1 |
| SendCount | Experimental | The number of entities to send the message to. -1 will send to all. Entities will be chosen with a roughly even random distribution using reservoir sampling | Integer | Optional | -1 | No | Value must be > 0 or equal -1 |

---

### Notify

**Stability:** Stable

Directly notifies a target NPC with a beacon message.

| Attribute | Stability | Description | Type | Required | Default | Computable | Constraint |
|-----------|-----------|-------------|------|----------|---------|------------|------------|
| Message | Stable | The message to send | String | Required | — | Yes | String must be not empty |
| ExpirationTime | Experimental | The number of seconds that the message should last and be acknowledged by the receiving NPC. -1 represents infinite time | Double | Optional | 1.0 | No | Value must be >= 0 or equal -1 |
| UseTargetSlot | Stable | A marked target to send to instead of the target provided by a sensor. Omit to use the target provided by the sensor | String | Optional | null | No | String value must be either null or not empty |

**Constraints:** If `UseTargetSlot` is not set (false/null), must be attached to a sensor that provides NPC target.

---

## Movement

### Crouch

**Stability:** Stable

Set NPC crouching state.

| Attribute | Stability | Description | Type | Required | Default | Computable | Constraint |
|-----------|-----------|-------------|------|----------|---------|------------|------------|
| Crouch | Stable | True for crouching, false for non-crouching | Boolean | Optional | true | Yes | — |

---

### OverrideAltitude

**Stability:** Stable

Temporarily override the preferred altitude of a flying NPC. Must be refreshed each tick.

| Attribute | Stability | Description | Type | Required | Default | Computable | Constraint |
|-----------|-----------|-------------|------|----------|---------|------------|------------|
| DesiredAltitudeRange | Stable | The desired altitude range | Array (Double) | Required | — | Yes | Values must be >= 0, <= MAX_DOUBLE, in weakly ascending order |

---

### RecomputePath

**Stability:** Stable

Force recomputation of path finder solution.

| Attribute | Stability | Description | Type | Required | Default | Computable | Constraint |
|-----------|-----------|-------------|------|----------|---------|------------|------------|
| *(no extra attributes)* | | | | | | | |

---

## NPC

### CompleteTask

**Stability:** Stable

Complete a task. Tasks are picked based on those provided to SensorCanInteract.

| Attribute | Stability | Description | Type | Required | Default | Computable | Constraint |
|-----------|-----------|-------------|------|----------|---------|------------|------------|
| Slot | Stable | The animation slot to play on | Flag | Required | — | No | Flag Values: `Status`, `Action`, `Face` |
| Animation | Stable | The animation ID to play | String | Optional | null | Yes | — |
| PlayAnimation | Stable | Whether or not to play the animation associated with completing this task | Boolean | Optional | true | Yes | — |

---

### Mount

**Stability:** Stable

Enable the player to mount the entity.

| Attribute | Stability | Description | Type | Required | Default | Computable | Constraint |
|-----------|-----------|-------------|------|----------|---------|------------|------------|
| AnchorX | Stable | The X anchor pos | Double | Required | — | Yes | — |
| AnchorY | Stable | The Y anchor pos | Double | Required | — | Yes | — |
| AnchorZ | Stable | The Z anchor pos | Double | Required | — | Yes | — |
| MovementConfig | Stable | The MovementConfig to use for this mount | String | Required | — | Yes | — |

---

### OpenBarterShop

**Stability:** Stable

Open the barter shop UI for the current player.

| Attribute | Stability | Description | Type | Required | Default | Computable | Constraint |
|-----------|-----------|-------------|------|----------|---------|------------|------------|
| Shop | Stable | The barter shop to open | Asset | Required | — | Yes | — |

---

### OpenShop

**Stability:** Stable

Open the shop UI for the current player.

| Attribute | Stability | Description | Type | Required | Default | Computable | Constraint |
|-----------|-----------|-------------|------|----------|---------|------------|------------|
| Shop | Stable | The shop to open | Asset | Required | — | Yes | — |

---

### StartObjective

**Stability:** Stable

Start the given objective for the currently iterated player in the interaction instruction.

| Attribute | Stability | Description | Type | Required | Default | Computable | Constraint |
|-----------|-----------|-------------|------|----------|---------|------------|------------|
| Objective | Stable | The task to start | Asset | Required | — | Yes | — |

---

## Path

### MakePath

**Stability:** WorkInProgress

Constructs a transient path for the NPC based on a series of rotations and distances.

| Attribute | Stability | Description | Type | Required | Default | Computable | Constraint |
|-----------|-----------|-------------|------|----------|---------|------------|------------|
| Path | WorkInProgress | A transient path definition | ObjectRef (Path) | Required | — | No | — |

---

## State Machine

### ParentState

**Stability:** Stable

Set the main state of NPC from within a component.

| Attribute | Stability | Description | Type | Required | Default | Computable | Constraint |
|-----------|-----------|-------------|------|----------|---------|------------|------------|
| State | Stable | The alias of the external state to set, as defined by `_ImportStates` in parameters | String | Required | — | No | String must be a valid state string. A main state must be included before the period (e.g. `Main.Test`). Only the main state can be included. |

**Constraints:** May only be included within a component.

---

### State

**Stability:** Stable

Set state of NPC. The state can be queried with a sensor later on.

| Attribute | Stability | Description | Type | Required | Default | Computable | Constraint |
|-----------|-----------|-------------|------|----------|---------|------------|------------|
| State | Stable | State name to set | String | Required | — | No | String must be a valid state string (e.g. `Main.Test`). If nested within a substate, the main state may be omitted (e.g. `.Test`). |
| ClearState | Stable | Clear the state of things like set once flags on transition | Boolean | Optional | true | No | — |

---

### ToggleStateEvaluator

**Stability:** Stable

Enable or disable the NPC's state evaluator.

| Attribute | Stability | Description | Type | Required | Default | Computable | Constraint |
|-----------|-----------|-------------|------|----------|---------|------------|------------|
| Enabled | Stable | Whether or not to enable the state evaluator | Boolean | Required | — | No | — |

---

## Timer

### SetAlarm

**Stability:** Stable

Set a named alarm on the NPC.

| Attribute | Stability | Description | Type | Required | Default | Computable | Constraint |
|-----------|-----------|-------------|------|----------|---------|------------|------------|
| Name | Stable | The name of the alarm to set | String | Required | — | Yes | String must be not empty |
| DurationRange | Stable | The duration range from which to pick a duration to set the alarm for. `["P0D", "P0D"]` will unset the alarm | Array (TemporalAmount) | Required | — | Yes | Values must be >= a few seconds, <= 5879611 years, in weakly ascending order and either all Periods or all Durations |

---

### TimerContinue

**Stability:** Stable

Continue a timer.

| Attribute | Stability | Description | Type | Required | Default | Computable | Constraint |
|-----------|-----------|-------------|------|----------|---------|------------|------------|
| Name | Stable | The name of the timer | String | Required | — | Yes | String must be not empty |

---

### TimerModify

**Stability:** Stable

Modify values of a timer.

| Attribute | Stability | Description | Type | Required | Default | Computable | Constraint |
|-----------|-----------|-------------|------|----------|---------|------------|------------|
| Name | Stable | The name of the timer | String | Required | — | Yes | String must be not empty |
| AddValue | Stable | Add value to the timer | Double | Optional | 0.0 | Yes | Value must be >= 0 |
| MaxValue | Stable | Set the restart value range the timer can have. If [0, 0] (default) it will be ignored | Array (Double) | Optional | [0.0, 0.0] | Yes | Values must be >= 0, <= MAX_DOUBLE |
| Rate | Stable | Set the rate at which the timer will decrease. If 0 (default) it will be ignored | Double | Optional | 0.0 | Yes | Value must be >= 0 |
| SetValue | Stable | Set the value of the timer. If 0 (default) it will be ignored | Double | Optional | 0.0 | Yes | Value must be >= 0 |
| Repeating | Stable | Whether to repeat the timer when countdown finishes | Boolean | Optional | false | Yes | — |

---

### TimerPause

**Stability:** Stable

Pause a timer.

| Attribute | Stability | Description | Type | Required | Default | Computable | Constraint |
|-----------|-----------|-------------|------|----------|---------|------------|------------|
| Name | Stable | The name of the timer | String | Required | — | Yes | String must be not empty |

---

### TimerRestart

**Stability:** Stable

Restart a timer. Will be set to the original initial values.

| Attribute | Stability | Description | Type | Required | Default | Computable | Constraint |
|-----------|-----------|-------------|------|----------|---------|------------|------------|
| Name | Stable | The name of the timer | String | Required | — | Yes | String must be not empty |

---

### TimerStart

**Stability:** Stable

Start a timer.

| Attribute | Stability | Description | Type | Required | Default | Computable | Constraint |
|-----------|-----------|-------------|------|----------|---------|------------|------------|
| Name | Stable | The name of the timer | String | Required | — | Yes | String must be not empty |
| StartValueRange | Stable | The range from which to pick an initial value to start at | Array (Double) | Required | — | Yes | Values must be > 0, <= MAX_DOUBLE, in weakly ascending order |
| RestartValueRange | Stable | The range from which to pick a value when the timer is restarted. The upper bound is also the timer max | Array (Double) | Required | — | Yes | Values must be > 0, <= MAX_DOUBLE, in weakly ascending order |
| Rate | Stable | The rate at which the timer will decrease | Double | Optional | 1.0 | Yes | Value must be greater than 0 |
| Repeating | Stable | Whether to repeat the timer when countdown finishes | Boolean | Optional | false | Yes | — |

---

### TimerStop

**Stability:** Stable

Stop a timer.

| Attribute | Stability | Description | Type | Required | Default | Computable | Constraint |
|-----------|-----------|-------------|------|----------|---------|------------|------------|
| Name | Stable | The name of the timer | String | Required | — | Yes | String must be not empty |

---

## Utility

### Nothing

**Stability:** Stable

Do nothing. Used often as placeholder.

| Attribute | Stability | Description | Type | Required | Default | Computable | Constraint |
|-----------|-----------|-------------|------|----------|---------|------------|------------|
| *(no extra attributes)* | | | | | | | |

---

### Random

**Stability:** Stable

Execute a single random action from a list of weighted actions.

| Attribute | Stability | Description | Type | Required | Default | Computable | Constraint |
|-----------|-----------|-------------|------|----------|---------|------------|------------|
| Actions | Stable | List of possible actions | Array (WeightedAction) | Required | — | No | — |

---

### ResetInstructions

**Stability:** Stable

Force reset instructionList, either by name, or as a whole.

| Attribute | Stability | Description | Type | Required | Default | Computable | Constraint |
|-----------|-----------|-------------|------|----------|---------|------------|------------|
| Instructions | Stable | The instructionList to reset. If left empty, will reset all instructionList | Array (String) | Optional | null | Yes | Strings in array must not be empty |

---

### Sequence

**Stability:** Stable

Execute list of actions.

| Attribute | Stability | Description | Type | Required | Default | Computable | Constraint |
|-----------|-----------|-------------|------|----------|---------|------------|------------|
| Blocking | Stable | Do not execute an action unless the previous action could execute | Boolean | Optional | false | No | — |
| Atomic | Stable | Only execute actions if all actions can be executed | Boolean | Optional | false | No | — |
| Actions | Stable | List of actions | ObjectRef (ActionList) | Required | — | No | — |

---

### SetFlag

**Stability:** Stable

Set a named flag to a boolean value.

| Attribute | Stability | Description | Type | Required | Default | Computable | Constraint |
|-----------|-----------|-------------|------|----------|---------|------------|------------|
| Name | Stable | The name of the flag | String | Required | — | Yes | String must be not empty |
| SetTo | Stable | The value to set the flag to | Boolean | Optional | true | Yes | — |

---

### Timeout

**Stability:** Stable

Delay an action by a time which is randomly picked between a given minimum and maximum value.

| Attribute | Stability | Description | Type | Required | Default | Computable | Constraint |
|-----------|-----------|-------------|------|----------|---------|------------|------------|
| Delay | Stable | Range of time to delay in seconds | Array (Double) | Optional | [1.0, 1.0] | Yes | Values must be >= 0, <= MAX_DOUBLE, in weakly ascending order |
| DelayAfter | Stable | Delay after executing the action | Boolean | Optional | false | No | — |
| Action | Stable | Optional action to delay | ObjectRef (Action) | Optional | null | No | — |

---

## World

### PlaceBlock

**Stability:** Stable

Place a block (chosen by another action) at a position returned by a Sensor if close enough.

| Attribute | Stability | Description | Type | Required | Default | Computable | Constraint |
|-----------|-----------|-------------|------|----------|---------|------------|------------|
| Range | Stable | The range to target position before block will be placed | Double | Optional | 3.0 | Yes | Value must be greater than 0 |
| AllowEmptyMaterials | Stable | Whether it should be possible to replace blocks that have empty material | Boolean | Optional | false | Yes | — |

**Constraints:** Must be attached to a sensor that provides one of vector position.

---

### ResetBlockSensors

**Stability:** Stable

Resets a specific block sensor by name, or all block sensors by clearing the current targeted block.

| Attribute | Stability | Description | Type | Required | Default | Computable | Constraint |
|-----------|-----------|-------------|------|----------|---------|------------|------------|
| BlockSets | Stable | The searched blocksets to reset block sensors for. If left empty, will reset all block sensors and found blocks | AssetArray (BlockSet) | Optional | null | Yes | — |

---

### ResetPath

**Stability:** Stable

Resets the current patrol path this NPC follows.

| Attribute | Stability | Description | Type | Required | Default | Computable | Constraint |
|-----------|-----------|-------------|------|----------|---------|------------|------------|
| *(no extra attributes)* | | | | | | | |

---

### ResetSearchRays

**Stability:** Stable

Resets a specific search ray sensor cached position by name, or all search ray sensors.

| Attribute | Stability | Description | Type | Required | Default | Computable | Constraint |
|-----------|-----------|-------------|------|----------|---------|------------|------------|
| Names | Stable | The search ray sensor ids. If left empty, will reset all search ray sensors | Array (String) | Optional | null | Yes | Strings in array must not be empty |

---

### SetBlockToPlace

**Stability:** Stable

Set the block type the NPC will place.

| Attribute | Stability | Description | Type | Required | Default | Computable | Constraint |
|-----------|-----------|-------------|------|----------|---------|------------|------------|
| Block | Stable | The block item type | Asset | Required | — | Yes | — |

---

### SetLeashPosition

**Stability:** Stable

Sets the NPC's current position to the spawn/leash position to be used with the Leash Sensor.

| Attribute | Stability | Description | Type | Required | Default | Computable | Constraint |
|-----------|-----------|-------------|------|----------|---------|------------|------------|
| ToCurrent | Stable | Set to the NPC's current position | Boolean | Optional | false | No | — |
| ToTarget | Stable | Set to the target position | Boolean | Optional | false | No | — |

**Cross-attribute constraints:**
- At least one of `ToCurrent`, `ToTarget` must be true.
- If `ToTarget` is true, must be attached to a sensor that provides one of player target, NPC target, dropped item target.

---

### StorePosition

**Stability:** Stable

Store the position from the attached sensor.

| Attribute | Stability | Description | Type | Required | Default | Computable | Constraint |
|-----------|-----------|-------------|------|----------|---------|------------|------------|
| Slot | Stable | The slot to store the position in | String | Required | — | Yes | String must be not empty |

---

### TriggerSpawners

**Stability:** Stable

Trigger all, or up to a certain number of manual spawn markers in a radius around the NPC.

| Attribute | Stability | Description | Type | Required | Default | Computable | Constraint |
|-----------|-----------|-------------|------|----------|---------|------------|------------|
| SpawnMarker | Stable | The spawn marker type to trigger | Asset | Optional | null | Yes | — |
| Range | Stable | The range within which to trigger spawn markers | Double | Required | — | Yes | Value must be greater than 0 |
| Count | Stable | The number of markers to randomly trigger (0 will trigger all matching validators) | Integer | Optional | 0 | Yes | Value must be >= 0 |
