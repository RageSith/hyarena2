# NPC Sensors Reference

All sensor types available for NPC behavior scripting. Sensors evaluate conditions and optionally provide targets or positions to attached actions and motions.

Every sensor has two common attributes:

| Attribute | Stability | Description | Type | Default |
|-----------|-----------|-------------|------|---------|
| Once | Stable | Sensor only triggers once | Boolean, Optional | `false` |
| Enabled | Stable | Whether this sensor should be enabled on the NPC | Boolean, Optional, Computable | `true` |

These common attributes are omitted from individual sensor entries below to avoid repetition.

---

## Audiovisual

### Animation (Stable)

Check if a given animation is being played.

**Attributes:**

| Attribute | Stability | Description | Type | Default | Constraints |
|-----------|-----------|-------------|------|---------|-------------|
| Slot | Stable | The animation slot to check | Flag, Required, Computable | -- | Flag values: `Status`, `Action`, `Face` |
| Animation | Stable | The animation ID to check for | String, Required, Computable | -- | String must be not empty |

**Provides:** Nothing

---

## Combat

### Damage (Stable)

Test if NPC suffered damage. A position is only returned when NPC suffered combat damage.

**Attributes:**

| Attribute | Stability | Description | Type | Default | Constraints |
|-----------|-----------|-------------|------|---------|-------------|
| Combat | Stable | Test for combat damage | Boolean, Optional | `true` | -- |
| Friendly | Stable | Test for damage from usually disabled damage groups | Boolean, Optional | `false` | -- |
| Drowning | Stable | Test for damage from drowning | Boolean, Optional | `false` | -- |
| Environment | Stable | Test for damage from environment | Boolean, Optional | `false` | -- |
| Other | Stable | Test for other damage | Boolean, Optional | `false` | -- |
| TargetSlot | Stable | The slot to use for locking on the target if damage is taken. If omitted, target will not be locked | String, Optional | `null` | String value must be either null or not empty |

**Constraints:**
- At least one of `Combat`, `Drowning`, `Environment`, `Other` must be true
- If `TargetSlot` is true, `Drowning`, `Environment`, `Other` must be false

**Provides:** Player target, NPC target, Dropped item target

---

### IsBackingAway (Stable)

Test if the NPC is currently backing away from something.

**Attributes:** None (only common attributes)

**Provides:** Nothing

---

### CombatActionEvaluator (Experimental)

A sensor which handles funnelling information to actions and motions from the combat action evaluator. Delivers the current attack target and desired range for supported direct child motions.

**Attributes:**

| Attribute | Stability | Description | Type | Default | Constraints |
|-----------|-----------|-------------|------|---------|-------------|
| TargetInRange | Stable | Whether to match on target being in or out of range | Boolean, Required, Computable | -- | -- |
| AllowableDeviation | Stable | The allowable deviation from the desired attack range | Double, Optional, Computable | `0.5` | Value must be greater than 0 |

**Provides:** Player target, NPC target

---

## Core Components

### FlockCombatDamage (Stable)

Return true if flock with NPC received combat damage. Target position is entity which did most damage.

**Attributes:**

| Attribute | Stability | Description | Type | Default | Constraints |
|-----------|-----------|-------------|------|---------|-------------|
| LeaderOnly | Stable | Only test for damage to flock leader | Boolean, Optional | `true` | -- |

**Provides:** Player target, NPC target

---

### FlockLeader (Stable)

Test for the presence and provide position of the flock leader.

**Attributes:** None (only common attributes)

**Provides:** Player target, NPC target

---

### HasHostileTargetMemory (Stable)

Checks if there is currently a hostile target in the target memory.

**Attributes:** None (only common attributes)

**Provides:** Nothing

---

### InflictedDamage (Stable)

Return true if an individual or the flock it belongs to inflicted combat damage. Target position is entity which received most damage.

**Attributes:**

| Attribute | Stability | Description | Type | Default | Constraints |
|-----------|-----------|-------------|------|---------|-------------|
| Target | Stable | Who to check has inflicted damage | Flag, Optional | `Self` | Flag values: `FlockLeader` (Check flock leader only), `Self` (Check self), `Flock` (Check flock) |
| FriendlyFire | Stable | Consider friendly fire too | Boolean, Optional | `false` | -- |

**Provides:** Player target, NPC target

---

## Entity

### Beacon (Stable)

Checks to see if any messages have been broadcasted by nearby NPCs.

**Attributes:**

| Attribute | Stability | Description | Type | Default | Constraints |
|-----------|-----------|-------------|------|---------|-------------|
| Message | Experimental | The message to listen for | String, Required, Computable | -- | String must be not empty |
| Range | Experimental | The max distance beacons should be received from | Double, Optional, Computable | `64.0` | Value must be greater than 0 |
| TargetSlot | Stable | A slot to store the sender as a target. If omitted no target will be stored | String, Optional | `null` | String value must be either null or not empty |
| ConsumeMessage | Stable | Whether the message should be consumed by this sensor | Boolean, Optional | `true` | -- |

**Provides:** Player target, NPC target, Dropped item target

---

### Count (Stable)

Check if there is a certain number of NPCs or players within a specific range.

**Attributes:**

| Attribute | Stability | Description | Type | Default | Constraints |
|-----------|-----------|-------------|------|---------|-------------|
| Count | Stable | Specifies the allowed number of entities (inclusive) | Array of Integer, Required, Computable | -- | Values must be >= 0, <= 2147483647, in weakly ascending order |
| Range | Stable | Range to find entities in (inclusive) | Array of Double, Required, Computable | -- | Values must be >= 0, in weakly ascending order |
| IncludeGroups | Stable | Match for NPCs in these groups | AssetArray (TagSet), Optional | `null` | -- |
| ExcludeGroups | Stable | Never match NPCs in these groups | AssetArray (TagSet), Optional | `null` | -- |

**Provides:** Nothing

---

### Kill (Stable)

Return true if NPC made a kill. Target position is killed entity position.

**Attributes:**

| Attribute | Stability | Description | Type | Default | Constraints |
|-----------|-----------|-------------|------|---------|-------------|
| TargetSlot | Stable | The target slot to check if killed. If omitted, will accept any entity killed | String, Optional, Computable | `null` | String value must be either null or not empty |

**Provides:** Vector position

---

### Mob (Stable)

Return true if entity matching specific attributes and filters is in range. Target is entity.

**Attributes:**

| Attribute | Stability | Description | Type | Default | Constraints |
|-----------|-----------|-------------|------|---------|-------------|
| MinRange | Stable | Minimum range to test entities in | Double, Optional, Computable | `0.0` | Value must be >= 0 |
| Range | Stable | Maximum range to test entities in | Double, Required, Computable | -- | Value must be > 0 |
| LockOnTarget | Stable | Matched target becomes locked target | Boolean, Optional, Computable | `false` | -- |
| LockedTargetSlot | Stable | The target slot to use for locking on or unlocking | String, Optional, Computable | `LockedTarget` | String value must be either null or not empty |
| AutoUnlockTarget | Stable | Unlock locked target when sensor not matching it anymore | Boolean, Optional, Computable | `false` | -- |
| OnlyLockedTarget | Stable | Test only locked target | Boolean, Optional, Computable | `false` | -- |
| IgnoredTargetSlot | Stable | The target slot to use for ignoring | String, Optional, Computable | `null` | String value must be either null or not empty |
| UseProjectedDistance | Stable | Use the projected movement direction vector for distance, rather than the Euclidean distance | Boolean, Optional, Computable | `false` | -- |
| Prioritiser | Stable | A prioritiser for selecting results based on additional parameters | ObjectRef (ISensorEntityPrioritiser), Optional | `null` | -- |
| Collector | Stable | A collector which can process all checked entities and act on them based on whether they match or not | ObjectRef (ISensorEntityCollector), Optional | `null` | -- |
| Filters | Stable | A series of entity filter sensors to test | Array of IEntityFilter, Optional | `null` | -- |
| GetPlayers | Stable | Test players | Boolean, Optional, Computable | `false` | -- |
| GetNPCs | Stable | Test mobs/NPCs | Boolean, Optional, Computable | `true` | -- |
| ExcludeOwnType | Stable | Exclude NPCs of same type as current NPC | Boolean, Optional, Computable | `true` | -- |

**Constraints:**
- `Range` must be greater or equal than `MinRange`
- At least one of `GetPlayers`, `GetNPCs` must be true

**Provides:** Player target, NPC target

---

### Player (Stable)

Return true if player matching specific attributes and filters is in range. Target is player.

**Attributes:**

| Attribute | Stability | Description | Type | Default | Constraints |
|-----------|-----------|-------------|------|---------|-------------|
| MinRange | Stable | Minimum range to test entities in | Double, Optional, Computable | `0.0` | Value must be >= 0 |
| Range | Stable | Maximum range to test entities in | Double, Required, Computable | -- | Value must be > 0 |
| LockOnTarget | Stable | Matched target becomes locked target | Boolean, Optional, Computable | `false` | -- |
| LockedTargetSlot | Stable | The target slot to use for locking on or unlocking | String, Optional, Computable | `LockedTarget` | String value must be either null or not empty |
| AutoUnlockTarget | Stable | Unlock locked target when sensor not matching it anymore | Boolean, Optional, Computable | `false` | -- |
| OnlyLockedTarget | Stable | Test only locked target | Boolean, Optional, Computable | `false` | -- |
| IgnoredTargetSlot | Stable | The target slot to use for ignoring | String, Optional, Computable | `null` | String value must be either null or not empty |
| UseProjectedDistance | Stable | Use the projected movement direction vector for distance, rather than the Euclidean distance | Boolean, Optional, Computable | `false` | -- |
| Prioritiser | Stable | A prioritiser for selecting results based on additional parameters | ObjectRef (ISensorEntityPrioritiser), Optional | `null` | -- |
| Collector | Stable | A collector which can process all checked entities and act on them based on whether they match or not | ObjectRef (ISensorEntityCollector), Optional | `null` | -- |
| Filters | Stable | A series of entity filter sensors to test | Array of IEntityFilter, Optional | `null` | -- |

**Constraints:**
- `Range` must be greater or equal than `MinRange`

**Provides:** Player target, NPC target

---

### Self (Stable)

Test if the NPC itself matches a set of entity filters.

**Attributes:**

| Attribute | Stability | Description | Type | Default | Constraints |
|-----------|-----------|-------------|------|---------|-------------|
| Filters | Stable | A series of entity filter sensors to test | Array of IEntityFilter, Required | -- | -- |

**Provides:** Vector position

---

### Target (Stable)

Test if given target matches a series of criteria and optional entity filters.

**Attributes:**

| Attribute | Stability | Description | Type | Default | Constraints |
|-----------|-----------|-------------|------|---------|-------------|
| TargetSlot | Stable | The target slot to check | String, Optional, Computable | `LockedTarget` | String must be not empty |
| Range | Stable | Maximum range of locked target | Double, Optional, Computable | `1.7976931348623157E308` (max double) | Value must be > 0 |
| AutoUnlockTarget | Stable | Unlock locked target if match fails | Boolean, Optional, Computable | `false` | -- |
| Filters | Stable | A series of entity filter sensors to test | Array of IEntityFilter, Optional | `null` | -- |

**Provides:** Player target, NPC target

---

## Interaction

### CanInteract (Stable)

Checks whether or not the player being iterated by the interaction instruction can interact with this NPC.

**Attributes:**

| Attribute | Stability | Description | Type | Default | Constraints |
|-----------|-----------|-------------|------|---------|-------------|
| ViewSector | Stable | View sector to test the player in | Double, Optional, Computable | `0.0` | Value must be >= 0 and <= 360 |
| Attitudes | Stable | A set of attitudes to match | FlagSet, Optional, Computable | `[NEUTRAL, FRIENDLY, REVERED]` | Flag values: `HOSTILE`, `REVERED`, `FRIENDLY`, `IGNORE`, `NEUTRAL` |

**Provides:** Nothing

---

### HasInteracted (Stable)

Checks whether the currently iterated player in the interaction instruction has interacted with this NPC.

**Attributes:** None (only common attributes)

**Provides:** Nothing

---

### InteractionContext (Stable)

Checks whether the currently iterated player in the interaction instruction has interacted with this NPC in the given context.

**Attributes:**

| Attribute | Stability | Description | Type | Default | Constraints |
|-----------|-----------|-------------|------|---------|-------------|
| Context | Stable | The context of the interaction | String, Required, Computable | -- | String must be not empty |

**Provides:** Nothing

---

### HasTask (Stable)

Checks whether or not the player being iterated by the interaction instruction has any of the given tasks.

**Attributes:**

| Attribute | Stability | Description | Type | Default | Constraints |
|-----------|-----------|-------------|------|---------|-------------|
| TasksById | Stable | Completable tasks to match by name | Array of String, Required, Computable | -- | String array must not be empty |

**Provides:** Nothing

---

## Lifecycle

### Age (Stable)

Triggers when the age of the NPC falls between a certain range. Range is defined in terms of period (e.g. `1Y2M3W4D` - 1 year, 2 months, 3 weeks, 4 days) or duration (e.g. `2DT3H4M` - 2 days, 3 hours, 4 minutes).

**Attributes:**

| Attribute | Stability | Description | Type | Default | Constraints |
|-----------|-----------|-------------|------|---------|-------------|
| AgeRange | Stable | The age range within which to trigger | Array of TemporalAmount, Required, Computable | -- | Values must be >= a few seconds, <= 5879611 years, in strictly ascending order, and either all Periods or all Durations |

**Provides:** Nothing

---

## Movement

### InAir (Stable)

Return true if NPC is not on ground. No target is returned.

**Attributes:** None (only common attributes)

**Provides:** Nothing

---

### MotionController (Experimental)

Test if specific motion controller is active.

**Attributes:**

| Attribute | Stability | Description | Type | Default | Constraints |
|-----------|-----------|-------------|------|---------|-------------|
| MotionController | Experimental | Motion controller name to test for | String, Required | -- | String must be not empty |

**Provides:** Nothing

---

### Nav (Stable)

Queries navigation state.

**Attributes:**

| Attribute | Stability | Description | Type | Default | Constraints |
|-----------|-----------|-------------|------|---------|-------------|
| NavStates | Stable | Trigger when path finder is in one of the states or empty to match all | FlagSet, Optional, Computable | `[]` | Flag values: `PROGRESSING` (Moving or computing a path), `INIT` (Doing nothing), `AT_GOAL` (Reached target), `BLOCKED` (Can't advance any further), `ABORTED` (Search stopped but target not reached), `DEFER` (Delaying/unable to advance) |
| ThrottleDuration | Stable | Minimum time in seconds the path finder isn't able to reach target or 0 to ignore | Double, Optional, Computable | `0.0` | Value must be >= 0 |
| TargetDelta | Stable | Minimum distance target has moved since path was computed or 0 to ignore | Double, Optional, Computable | `0.0` | Value must be >= 0 |

**Provides:** Nothing

---

### OnGround (Stable)

Return true if NPC is on ground. No target is returned.

**Attributes:** None (only common attributes)

**Provides:** Nothing

---

## State Machine

### IsBusy (Stable)

Tests if an NPC is in one of the defined Busy States.

**Attributes:** None (only common attributes)

**Provides:** Nothing

---

### State (Stable)

Signal if NPC is set to specific state.

**Attributes:**

| Attribute | Stability | Description | Type | Default | Constraints |
|-----------|-----------|-------------|------|---------|-------------|
| State | Stable | State to compare to | String, Required | -- | String must be a valid state string. State strings consist of a main state and a sub state (e.g. `Main.Test`). If nested within a substate, the main state may be omitted (e.g. `.Test`) when referencing. |
| IgnoreMissingSetState | Stable | Override and ignore checks for matching setter action that sets this state. Intended for use in cases such as the FlockState action which sets the state via another NPC | Boolean, Optional | `false` | -- |

**Provides:** Nothing

---

## Timer

### Alarm (Stable)

Check the state of a named alarm and optionally clear it if the time has passed.

**Attributes:**

| Attribute | Stability | Description | Type | Default | Constraints |
|-----------|-----------|-------------|------|---------|-------------|
| Name | Stable | The name of the alarm to check | String, Required, Computable | -- | String must be not empty |
| State | Stable | The state to check for | Flag, Required, Computable | -- | Flag values: `SET`, `UNSET` (Not set), `PASSED` |
| Clear | Stable | Whether to clear the alarm (unset it) if it has passed | Boolean, Optional, Computable | `false` | -- |

**Provides:** Nothing

---

### Timer (Stable)

Tests if a timer exists and the value is within a certain range.

**Attributes:**

| Attribute | Stability | Description | Type | Default | Constraints |
|-----------|-----------|-------------|------|---------|-------------|
| Name | Stable | The name of the timer | String, Required, Computable | -- | String must be not empty |
| State | Stable | The timer's state to check | Flag, Optional | `ANY` | Flag values: `PAUSED`, `RUNNING`, `STOPPED`, `ANY` |
| TimeRemainingRange | Stable | The acceptable remaining time on the timer | Array of Double, Optional, Computable | `[0.0, 1.7976931348623157E308]` | Values must be >= 0, in weakly ascending order |

**Provides:** Nothing

---

## Utility

### AdjustPosition (Stable)

Perform adjustments to the wrapped sensor's returned position.

**Attributes:**

| Attribute | Stability | Description | Type | Default | Constraints |
|-----------|-----------|-------------|------|---------|-------------|
| Sensor | Stable | Sensor to wrap | ObjectRef (Sensor), Required | -- | -- |
| Offset | Stable | The offset to apply to the returned position from the sensor | Array of Double, Required, Computable | -- | -- |

**Provides:** Vector position

---

### And (Stable)

Evaluate all sensors and execute action only when all sensors signal true. Target is provided by first sensor.

**Attributes:**

| Attribute | Stability | Description | Type | Default | Constraints |
|-----------|-----------|-------------|------|---------|-------------|
| Sensors | Stable | List of sensors | Array of Sensor, Required | -- | Array must not be empty |
| AutoUnlockTargetSlot | Stable | A target slot to unlock when sensor doesn't match anymore | String, Optional, Computable | `null` | String value must be either null or not empty |

**Provides:** Inherited from first sensor in the list

---

### Any (Stable)

Sensor always signals true but doesn't return a target.

**Attributes:** None (only common attributes)

**Provides:** Nothing

---

### Eval (Experimental)

Evaluate javascript expression and test truth value. Current values accessible are `health` and `blocked`.

**Attributes:**

| Attribute | Stability | Description | Type | Default | Constraints |
|-----------|-----------|-------------|------|---------|-------------|
| Expression | Experimental | Javascript expression | String, Required | -- | String must be not empty |

**Provides:** Nothing

---

### Flag (Stable)

Test if a named flag is set or not.

**Attributes:**

| Attribute | Stability | Description | Type | Default | Constraints |
|-----------|-----------|-------------|------|---------|-------------|
| Name | Stable | The name of the flag | String, Required, Computable | -- | String must be not empty |
| Set | Stable | Whether the flag should be set or not | Boolean, Optional, Computable | `true` | -- |

**Provides:** Nothing

---

### Not (WorkInProgress)

Return true when the given sensor test fails.

**Attributes:**

| Attribute | Stability | Description | Type | Default | Constraints |
|-----------|-----------|-------------|------|---------|-------------|
| Sensor | Stable | Sensor to test | ObjectRef (Sensor), Required | -- | -- |
| UseTargetSlot | Stable | A locked target slot to feed to action (if available) | String, Optional, Computable | `null` | String value must be either null or not empty |
| AutoUnlockTargetSlot | Stable | A target slot to unlock when sensor doesn't match anymore | String, Optional, Computable | `null` | String value must be either null or not empty |

**Provides:** Nothing

---

### Or (Stable)

Evaluate sensors and execute action when at least one sensor signals true. Target is provided by first sensor signalling true.

**Attributes:**

| Attribute | Stability | Description | Type | Default | Constraints |
|-----------|-----------|-------------|------|---------|-------------|
| Sensors | Stable | List of sensors | Array of Sensor, Required | -- | Array must not be empty |
| AutoUnlockTargetSlot | Stable | A target slot to unlock when sensor doesn't match anymore | String, Optional, Computable | `null` | String value must be either null or not empty |

**Provides:** Inherited from first matching sensor

---

### Random (Stable)

Alternates between returning true and false for specified random durations.

**Attributes:**

| Attribute | Stability | Description | Type | Default | Constraints |
|-----------|-----------|-------------|------|---------|-------------|
| TrueDurationRange | Stable | The duration range to pick a random period to return true | Array of Double, Required, Computable | -- | Values must be > 0, in weakly ascending order |
| FalseDurationRange | Stable | The duration range to pick a random period to return false | Array of Double, Required, Computable | -- | Values must be > 0, in weakly ascending order |

**Provides:** Nothing

---

### Switch (Stable)

Check if a computed boolean is true.

**Attributes:**

| Attribute | Stability | Description | Type | Default | Constraints |
|-----------|-----------|-------------|------|---------|-------------|
| Switch | Stable | The switch to check | Boolean, Required, Computable | -- | -- |

**Provides:** Nothing

---

### ValueProviderWrapper (Stable)

Wraps a sensor and passes down some additional parameter overrides pulled from the value store.

**Attributes:**

| Attribute | Stability | Description | Type | Default | Constraints |
|-----------|-----------|-------------|------|---------|-------------|
| PassValues | Stable | Used to enable/disable passing of values in components | Boolean, Optional, Computable | `true` | -- |
| Sensor | Stable | Sensor to wrap | ObjectRef (Sensor), Required | -- | -- |
| ValueToParameterMappings | Stable | The mappings of values to override parameters | Array of ValueToParameterMapping, Required | -- | Array must not be empty |

**Provides:** Inherited from wrapped sensor

---

## World

### Block (Experimental)

Checks for one of a set of blocks in the nearby area and caches the result until explicitly reset or the targeted block changes/is removed. All block sensors with the same sought blockset share the same targeted block once found.

**Attributes:**

| Attribute | Stability | Description | Type | Default | Constraints |
|-----------|-----------|-------------|------|---------|-------------|
| Range | Stable | The range to search for the blocks in | Double, Required, Computable | -- | Value must be > 0 |
| MaxHeight | Stable | The vertical range to search for the blocks in | Double, Optional, Computable | `4.0` | Value must be > 0 |
| Blocks | Stable | The set of blocks to search for | Asset, Required, Computable | -- | -- |
| Random | Stable | Whether to pick at random from within the matched blocks or pick the closest | Boolean, Optional, Computable | `false` | -- |
| Reserve | Stable | Whether to reserve the found block to prevent other NPCs selecting it | Boolean, Optional, Computable | `false` | -- |

**Provides:** Vector position

---

### BlockChange (Stable)

Matches when a block from a blockset within a certain range is changed or interacted with.

**Attributes:**

| Attribute | Stability | Description | Type | Default | Constraints |
|-----------|-----------|-------------|------|---------|-------------|
| Range | Stable | Max range to listen in | Double, Required, Computable | -- | Value must be > 0 |
| SearchType | Stable | Whether to listen for events triggered by players, npcs, or both in a certain order | Flag, Optional, Computable | `PlayerOnly` | Flag values: `NpcOnly`, `PlayerFirst`, `PlayerOnly`, `NpcFirst` |
| TargetSlot | Stable | A target slot to place the target in. If omitted, no slot will be used | String, Optional, Computable | `null` | String value must be either null or not empty |
| BlockSet | Stable | Block set to listen for | Asset, Required, Computable | -- | -- |
| EventType | Stable | The event type to listen for | Flag, Optional, Computable | `DAMAGE` | Flag values: `DESTRUCTION` (On block destruction), `INTERACTION` (On block use interaction), `DAMAGE` (On block damage) |

**Provides:** Player target, NPC target

---

### BlockType (Stable)

Checks if the block at the given position matches the provided block set.

**Attributes:**

| Attribute | Stability | Description | Type | Default | Constraints |
|-----------|-----------|-------------|------|---------|-------------|
| Sensor | Stable | Sensor to wrap | ObjectRef (Sensor), Required | -- | -- |
| BlockSet | Stable | Block set to check against | Asset, Required, Computable | -- | -- |

**Provides:** Nothing

---

### CanPlaceBlock (Stable)

Test if the currently set block can be placed at the relative position given direction and offset.

**Attributes:**

| Attribute | Stability | Description | Type | Default | Constraints |
|-----------|-----------|-------------|------|---------|-------------|
| Direction | Stable | The direction to place relative to heading | Flag, Optional, Computable | `Forward` | Flag values: `Left`, `Backward`, `Right`, `Forward` |
| Offset | Stable | The offset to place at | Flag, Optional, Computable | `BodyPosition` | Flag values: `BodyPosition`, `FootPosition`, `HeadPosition` |
| RetryDelay | Stable | The amount of time to delay if a placement fails before trying to place something again | Double, Optional, Computable | `5.0` | Value must be > 0 |
| AllowEmptyMaterials | Stable | Whether it should be possible to replace blocks that have empty material | Boolean, Optional, Computable | `false` | -- |

**Provides:** Vector position

---

### EntityEvent (Stable)

Matches when an entity from a specific NPC group within a certain range is damaged, killed, or interacted with.

**Attributes:**

| Attribute | Stability | Description | Type | Default | Constraints |
|-----------|-----------|-------------|------|---------|-------------|
| Range | Stable | Max range to listen in | Double, Required, Computable | -- | Value must be > 0 |
| SearchType | Stable | Whether to listen for events triggered by players, npcs, or both in a certain order | Flag, Optional, Computable | `PlayerOnly` | Flag values: `NpcOnly`, `PlayerFirst`, `PlayerOnly`, `NpcFirst` |
| TargetSlot | Stable | A target slot to place the target in. If omitted, no slot will be used | String, Optional, Computable | `null` | String value must be either null or not empty |
| NPCGroup | Stable | NPC group to listen for | Asset, Required, Computable | -- | -- |
| EventType | Stable | The event type to listen for | Flag, Optional, Computable | `DAMAGE` | Flag values: `DEATH` (On dying), `INTERACTION` (On use interaction), `DAMAGE` (On taking damage) |
| FlockOnly | Stable | Whether to only listen for flock events | Boolean, Optional, Computable | `false` | -- |

**Provides:** Player target, NPC target

---

### InWater (Stable)

Check if NPC is currently in water.

**Attributes:** None (only common attributes)

**Provides:** Nothing

---

### Leash (Stable)

Triggers when the NPC is outside a specified range from the leash point.

**Attributes:**

| Attribute | Stability | Description | Type | Default | Constraints |
|-----------|-----------|-------------|------|---------|-------------|
| Range | Stable | The farthest distance allowed from the leash point | Double, Required, Computable | -- | Value must be > 0 |

**Provides:** Vector position

---

### Light (Stable)

Check the light levels of the block an entity is standing on. Can test light intensity, sky light or block channel levels.

**Attributes:**

| Attribute | Stability | Description | Type | Default | Constraints |
|-----------|-----------|-------------|------|---------|-------------|
| LightRange | Stable | The light intensity percentage range | Array of Double, Optional, Computable | `[0.0, 100.0]` | Values must be >= 0, <= 100, in weakly ascending order |
| SkyLightRange | Stable | The sky light percentage range | Array of Double, Optional, Computable | `[0.0, 100.0]` | Values must be >= 0, <= 100, in weakly ascending order |
| SunlightRange | Stable | The sunlight percentage range | Array of Double, Optional, Computable | `[0.0, 100.0]` | Values must be >= 0, <= 100, in weakly ascending order |
| RedLightRange | Stable | The red light percentage range | Array of Double, Optional, Computable | `[0.0, 100.0]` | Values must be >= 0, <= 100, in weakly ascending order |
| GreenLightRange | Stable | The green light percentage range | Array of Double, Optional, Computable | `[0.0, 100.0]` | Values must be >= 0, <= 100, in weakly ascending order |
| BlueLightRange | Stable | The blue light percentage range | Array of Double, Optional, Computable | `[0.0, 100.0]` | Values must be >= 0, <= 100, in weakly ascending order |
| UseTargetSlot | Stable | A target slot to check. If omitted, will check self | String, Optional, Computable | `null` | String value must be either null or not empty |

**Provides:** Nothing

---

### ReadPosition (Stable)

Read a stored position with some conditions.

**Attributes:**

| Attribute | Stability | Description | Type | Default | Constraints |
|-----------|-----------|-------------|------|---------|-------------|
| Slot | Stable | The slot to read the position from | String, Required, Computable | -- | String must be not empty |
| MinRange | Stable | Minimum range from stored position | Double, Optional, Computable | `0.0` | Value must be >= 0 |
| Range | Stable | Maximum range from stored position | Double, Required, Computable | -- | Value must be > 0 |
| UseMarkedTarget | Stable | Whether to read from a marked target slot instead of a position slot | Boolean, Optional, Computable | `false` | -- |

**Provides:** Vector position

---

### SearchRay (Stable)

Fire a ray at a specific angle to see if what it hits matches a given sought block.

**Attributes:**

| Attribute | Stability | Description | Type | Default | Constraints |
|-----------|-----------|-------------|------|---------|-------------|
| Name | Stable | The id of this search ray sensor so the position can be cached | String, Required, Computable | -- | String must be not empty |
| Angle | Stable | Angle to fire the ray. Horizontal is 0. Positive is downwards | Double, Required, Computable | -- | Value must be >= -90 and <= 90 |
| Range | Stable | How far to search | Double, Required, Computable | -- | Value must be > 0 and <= 96 |
| Blocks | Stable | The blockset to search for | Asset, Required, Computable | -- | -- |
| MinRetestAngle | Stable | The minimum change in NPC rotation before rays stop being throttled | Double, Optional, Computable | `5.0` | Value must be >= 0 and <= 360 |
| MinRetestMove | Stable | The minimum distance the NPC needs to move while facing the same direction before rays stop being throttled | Double, Optional, Computable | `1.0` | Value must be > 0 |
| ThrottleTime | Stable | The delay between retests when an NPC is facing the same direction | Double, Optional, Computable | `0.5` | Value must be > 0 |

**Provides:** Vector position

---

### Time (Stable)

Check if the day/year time is within some specified time. If you want to check a range of time which crosses through midnight and switches to the next day, use the greater time as the min value and the lesser value as the max value.

**Attributes:**

| Attribute | Stability | Description | Type | Default | Constraints |
|-----------|-----------|-------------|------|---------|-------------|
| Period | Stable | The time period to trigger within | Array of Double, Required, Computable | -- | Values must be >= 0, <= 24 |
| CheckDay | Stable | Check the day time. When using a double the values go from [.00, .99]. Don't get confused with there only being 60 minutes in an hour | Boolean, Optional | `true` | -- |
| CheckYear | WorkInProgress | Check the year time. When using a double the values go from [.00, .99]. Don't get confused with there only being 60 minutes in an hour | Boolean, Optional | `false` | -- |
| ScaleDayTimeRange | Stable | Whether to use a relative scale for the day time. Sunrise will be at relative 6, Noon at 12, and Sunset at 18, regardless of actual in-game time | Boolean, Optional | `true` | -- |

**Provides:** Nothing

---

### Weather (Stable)

Matches the current weather at the NPCs position against a set of weather globs.

**Attributes:**

| Attribute | Stability | Description | Type | Default | Constraints |
|-----------|-----------|-------------|------|---------|-------------|
| Weathers | Stable | The glob patterns to match against weather | AssetArray (Weather), Required, Computable | -- | -- |

**Provides:** Nothing

---

## Path

### Path (Stable)

Find a path based on various criteria. Provides the position of the nearest waypoint and the path itself.

**Attributes:**

| Attribute | Stability | Description | Type | Default | Constraints |
|-----------|-----------|-------------|------|---------|-------------|
| Path | Stable | The name of the path. If left blank, will find the nearest path | String, Optional, Computable | `null` | -- |
| Range | Stable | The range to test to nearest waypoint. 0 is unlimited | Double, Optional, Computable | `10.0` | Value must be > 0 |
| PathType | Stable | The type of path to search for | Flag, Optional, Computable | `AnyPrefabPath` | Flag values: `CurrentPrefabPath` (a path from the prefab the NPC spawned in), `TransientPath` (a transient path, testing purposes only), `AnyPrefabPath` (a path from any prefab), `WorldPath` (named world path) |

**Constraints:**
- If `PathType` is `WorldPath`, `Path` string must be not empty

**Provides:** Vector position, Path
