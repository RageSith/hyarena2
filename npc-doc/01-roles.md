# NPC Roles Reference

This document covers the three NPC Role types: Abstract, Generic, and Variant.

---

## Abstract Role (Stable)

Generic role for NPC with a core planner and list of Motion controllers.

### Health & Stats

#### MaxHealth
- **Stability:** Stable
- **Type:** Integer
- **Required** (no default)
- **Computable:** Yes
- **Description:** Max health
- **Constraint:** Value must be greater than 0

#### Invulnerable
- **Stability:** Stable
- **Type:** Boolean
- **Optional** (Default: `false`)
- **Computable:** Yes
- **Description:** Makes NPC ignore damage

#### KnockbackScale
- **Stability:** Stable
- **Type:** Double
- **Optional** (Default: `1.0`)
- **Computable:** Yes
- **Description:** Scale factor for knockback. Values greater than 1 increase knockback. Smaller values decrease it.
- **Constraint:** Value must be greater or equal than 0

#### Inertia
- **Stability:** Experimental
- **Type:** Double
- **Optional** (Default: `1.0`)
- **Computable:** No
- **Description:** Inertia
- **Constraint:** Value must be greater than 0.1

### Appearance

#### Appearance
- **Stability:** Stable
- **Type:** Asset
- **Required** (no default)
- **Computable:** Yes
- **Description:** Model to use for rendering

#### DisplayNames
- **Stability:** Stable
- **Type:** StringList
- **Optional** (Default: `null`)
- **Computable:** No
- **Description:** List of possible display names to choose from
- **Constraint:** Strings in array must not be empty

#### NameTranslationKey
- **Stability:** Stable
- **Type:** String
- **Required** (no default)
- **Computable:** Yes
- **Description:** The translation key for this NPC's name
- **Constraint:** String must be not empty

#### OverrideHeadPitchAngle
- **Stability:** Experimental
- **Type:** Boolean
- **Optional** (Default: `true`)
- **Computable:** Yes
- **Description:** Whether to override the head pitch angle range

#### HeadPitchAngleRange
- **Stability:** Experimental
- **Type:** Array (Element Type: Double)
- **Optional** (Default: `[-89.0, 89.0]`)
- **Computable:** Yes
- **Description:** Head rotation pitch range to be used instead of model camera settings
- **Constraint:** Values must be greater or equal than -90, less or equal than 90, and in weakly ascending order

### Inventory

#### InventorySize
- **Stability:** Stable
- **Type:** Integer
- **Optional** (Default: `0`)
- **Computable:** No
- **Description:** Number of available inventory slots
- **Constraint:** Value must be greater or equal than 0 and less or equal than 36

#### HotbarSize
- **Stability:** Stable
- **Type:** Integer
- **Optional** (Default: `3`)
- **Computable:** No
- **Description:** Number of available hotbar slots
- **Constraint:** Value must be greater or equal than 3 and less or equal than 8

#### OffHandSlots
- **Stability:** Stable
- **Type:** Integer
- **Optional** (Default: `0`)
- **Computable:** No
- **Description:** The number of slots for off-hand items
- **Constraint:** Value must be greater or equal than 0 and less or equal than 4

#### HotbarItems
- **Stability:** Stable
- **Type:** AssetArray (Element Type: Item)
- **Optional** (Default: `null`)
- **Computable:** Yes
- **Description:** Hotbar items (e.g. primary weapon, secondary weapon, etc)

#### OffHandItems
- **Stability:** Stable
- **Type:** AssetArray (Element Type: Item)
- **Optional** (Default: `null`)
- **Computable:** Yes
- **Description:** Off-hand items (e.g. shields, torches, etc)

#### DefaultOffHandSlot
- **Stability:** Stable
- **Type:** Integer
- **Optional** (Default: `-1`)
- **Computable:** Yes
- **Description:** The default off-hand item slot (-1 is empty)
- **Constraint:** Value must be greater or equal than -1 and less or equal than 4

#### PossibleInventoryItems
- **Stability:** Stable
- **Type:** Asset
- **Optional** (Default: `null`)
- **Computable:** Yes
- **Description:** A droplist defining the possible items the NPC's inventory could contain

#### Armor
- **Stability:** WorkInProgress
- **Type:** AssetArray (Element Type: Item)
- **Optional** (Default: `null`)
- **Computable:** No
- **Description:** Armor items

### Combat

#### CombatConfig
- **Stability:** Stable
- **Type:** CodecObject
- **Optional** (Default: `null`)
- **Computable:** No
- **Description:** The combat configuration providing optional combat action evaluator

#### DropList
- **Stability:** Stable
- **Type:** Asset
- **Optional** (Default: `null`)
- **Computable:** Yes
- **Description:** Drop list to spawn when killed

#### PickupDropOnDeath
- **Stability:** Stable
- **Type:** Boolean
- **Optional** (Default: `false`)
- **Computable:** No
- **Description:** Drop last picked item on death

### Attitudes

#### DefaultPlayerAttitude
- **Stability:** Stable
- **Type:** Flag
- **Optional** (Default: `HOSTILE`)
- **Computable:** Yes
- **Description:** The default attitude of this NPC towards players
- **Flag Values:**
  - `HOSTILE` -- is hostile towards the target
  - `REVERED` -- reveres the target
  - `FRIENDLY` -- is friendly towards the target
  - `IGNORE` -- is ignoring the target
  - `NEUTRAL` -- is neutral towards the target

#### DefaultNPCAttitude
- **Stability:** Stable
- **Type:** Flag
- **Optional** (Default: `NEUTRAL`)
- **Computable:** Yes
- **Description:** The default attitude of this NPC towards other NPCs
- **Flag Values:**
  - `HOSTILE` -- is hostile towards the target
  - `REVERED` -- reveres the target
  - `FRIENDLY` -- is friendly towards the target
  - `IGNORE` -- is ignoring the target
  - `NEUTRAL` -- is neutral towards the target

#### AttitudeGroup
- **Stability:** Stable
- **Type:** Asset
- **Optional** (Default: `null`)
- **Computable:** Yes
- **Description:** The attitude group towards other NPCs this NPC belongs to (often species related)

#### ItemAttitudeGroup
- **Stability:** Stable
- **Type:** Asset
- **Optional** (Default: `null`)
- **Computable:** Yes
- **Description:** This NPC's item attitudes

### Collision & Avoidance

#### CollisionDistance
- **Stability:** Stable
- **Type:** Double
- **Optional** (Default: `5.0`)
- **Computable:** No
- **Description:** Collision lookahead
- **Constraint:** Value must be greater than 0

#### CollisionForceFalloff
- **Stability:** Experimental
- **Type:** Double
- **Optional** (Default: `2.0`)
- **Computable:** No
- **Description:** Falloff rate for collision force
- **Constraint:** Value must be greater than 0

#### CollisionRadius
- **Stability:** Experimental
- **Type:** Double
- **Optional** (Default: `-1.0`)
- **Computable:** No
- **Description:** Collision radius override

#### CollisionViewAngle
- **Stability:** Experimental
- **Type:** Double
- **Optional** (Default: `320.0`)
- **Computable:** No
- **Description:** Collision detection view cone
- **Constraint:** Value must be greater or equal than 0 and less or equal than 360

#### ApplyAvoidance
- **Stability:** Experimental
- **Type:** Boolean
- **Optional** (Default: `false`)
- **Computable:** No
- **Description:** Apply avoidance steering force

#### AvoidanceMode
- **Stability:** Experimental
- **Type:** Flag
- **Optional** (Default: `Any`)
- **Computable:** No
- **Description:** Abilities to use for avoidance
- **Flag Values:**
  - `Evade` -- Only evade
  - `Slowdown` -- Only slow down NPC
  - `Any` -- Any avoidance allowed

#### EntityAvoidanceStrength
- **Stability:** Experimental
- **Type:** Double
- **Optional** (Default: `1.0`)
- **Computable:** No
- **Description:** Blending factor avoidance
- **Constraint:** Value must be greater or equal than 0

### Separation

#### ApplySeparation
- **Stability:** Experimental
- **Type:** Boolean
- **Optional** (Default: `false`)
- **Computable:** Yes
- **Description:** Apply separation steering force

#### SeparationDistance
- **Stability:** Experimental
- **Type:** Double
- **Optional** (Default: `3.0`)
- **Computable:** Yes
- **Description:** Desired separation distance
- **Constraint:** Value must be greater than 0

#### SeparationWeight
- **Stability:** Experimental
- **Type:** Double
- **Optional** (Default: `1.0`)
- **Computable:** Yes
- **Description:** Blend factor separation
- **Constraint:** Value must be greater or equal than 0

#### SeparationDistanceTarget
- **Stability:** Experimental
- **Type:** Double
- **Optional** (Default: `1.0`)
- **Computable:** Yes
- **Description:** Desired separation distance when close to target
- **Constraint:** Value must be greater or equal than 0

#### SeparationNearRadiusTarget
- **Stability:** Experimental
- **Type:** Double
- **Optional** (Default: `1.0`)
- **Computable:** Yes
- **Description:** Distance when using SeparationDistanceTarget
- **Constraint:** Value must be greater than 0

#### SeparationFarRadiusTarget
- **Stability:** Experimental
- **Type:** Double
- **Optional** (Default: `5.0`)
- **Computable:** Yes
- **Description:** Use normal separation distance from further than this distance
- **Constraint:** Value must be greater than 0

### Environment

#### OpaqueBlockSet
- **Stability:** Stable
- **Type:** Asset
- **Optional** (Default: `Opaque`)
- **Computable:** No
- **Description:** Blocks blocking line of sight

#### StayInEnvironment
- **Stability:** Experimental
- **Type:** Boolean
- **Optional** (Default: `false`)
- **Computable:** No
- **Description:** Stay in spawning environment

#### AllowedEnvironments
- **Stability:** Experimental
- **Type:** String
- **Optional** (Default: `null`)
- **Computable:** No
- **Description:** Allowed environment to walk in
- **Constraint:** String value must be either null or not empty

#### BreathesInAir
- **Stability:** WorkInProgress
- **Type:** Boolean
- **Optional** (Default: `true`)
- **Computable:** Yes
- **Description:** Can breathe in air

#### BreathesInWater
- **Stability:** WorkInProgress
- **Type:** Boolean
- **Optional** (Default: `false`)
- **Computable:** Yes
- **Description:** Can breathe in fluid/water

### Flock

#### FlockSpawnTypes
- **Stability:** WorkInProgress
- **Type:** Array (Element Type: String)
- **Optional** (Default: `null`)
- **Computable:** Yes
- **Description:** Types of NPC this flock should consist of
- **Constraint:** Strings in array must not be empty

#### FlockSpawnTypesRandom
- **Stability:** WorkInProgress
- **Type:** Boolean
- **Optional** (Default: `false`)
- **Computable:** Yes
- **Description:** Create a randomized flock if true, else spawn in order of FlockSpawnTypes

#### FlockAllowedNPC
- **Stability:** Experimental
- **Type:** Array (Element Type: String)
- **Optional** (Default: `null`)
- **Computable:** Yes
- **Description:** List of NPCs allowed in flock
- **Constraint:** Strings in array must not be empty

#### FlockCanLead
- **Stability:** Experimental
- **Type:** Boolean
- **Optional** (Default: `false`)
- **Computable:** Yes
- **Description:** This NPC can be flock leader

#### FlockWeightAlignment
- **Stability:** Experimental
- **Type:** Double
- **Optional** (Default: `1.0`)
- **Computable:** No
- **Description:** Blending flock alignment
- **Constraint:** Value must be greater than 0

#### FlockWeightSeparation
- **Stability:** Experimental
- **Type:** Double
- **Optional** (Default: `1.0`)
- **Computable:** No
- **Description:** Blending flock separation
- **Constraint:** Value must be greater than 0

#### FlockWeightCohesion
- **Stability:** Experimental
- **Type:** Double
- **Optional** (Default: `1.0`)
- **Computable:** No
- **Description:** Blending flock cohesion
- **Constraint:** Value must be greater than 0

#### FlockInfluenceRange
- **Stability:** Experimental
- **Type:** Double
- **Optional** (Default: `10.0`)
- **Computable:** No
- **Description:** Influence radius flock forces
- **Constraint:** Value must be greater than 0

#### CorpseStaysInFlock
- **Stability:** Stable
- **Type:** Boolean
- **Optional** (Default: `false`)
- **Computable:** No
- **Description:** Whether the NPC should stay in the flock until corpse removal or be removed at the moment of death

### Damage Groups

#### DisableDamageFlock
- **Stability:** WorkInProgress
- **Type:** Boolean
- **Optional** (Default: `true`)
- **Computable:** No
- **Description:** If true, disables combat damage from flock members

#### DisableDamageGroups
- **Stability:** WorkInProgress
- **Type:** AssetArray (Element Type: TagSet)
- **Optional** (Default: `null`)
- **Computable:** Yes
- **Description:** Members in this list of group won't cause damage

### State Machine

#### StartState
- **Stability:** Stable
- **Type:** String
- **Optional** (Default: `start`)
- **Computable:** No
- **Description:** Initial state
- **Constraint:** String must be not empty

#### DefaultSubState
- **Stability:** Stable
- **Type:** String
- **Optional** (Default: `Default`)
- **Computable:** No
- **Description:** The default sub state to reference when transitioning to a main state without a specified sub state
- **Constraint:** String must be not empty

#### BusyStates
- **Stability:** Stable
- **Type:** StringList
- **Required** (no default)
- **Computable:** No
- **Description:** States during which this NPC is busy and can't be interacted with
- **Constraint:** String must be a valid state string. A main state must be included before the period (e.g. `Main.Test`). State strings consist of a main state and a sub state (e.g. `Main.Test`). If nested within a substate, the main state may be omitted (e.g. `.Test`) when referencing.

#### StateTransitions
- **Stability:** Stable
- **Type:** ObjectRef (Object Type: StateTransitionController)
- **Optional** (Default: `null`)
- **Computable:** No
- **Description:** A set of state transitions and the actions that will be executed during them

#### StateEvaluator
- **Stability:** Stable
- **Type:** CodecObject
- **Optional** (Default: `null`)
- **Computable:** No
- **Description:** A state evaluator

### Motion & Controllers

#### MotionControllerList
- **Stability:** Stable
- **Type:** ObjectRef (Object Type: HashMap)
- **Required** (no default)
- **Computable:** No
- **Description:** Motion controllers

#### InitialMotionController
- **Stability:** Stable
- **Type:** String
- **Optional** (Default: `null`)
- **Computable:** Yes
- **Description:** The initial motion controller to set. If omitted and there are multiple, one will be chosen at random.
- **Constraint:** String value must be either null or not empty

### Instructions & Interactions

#### Instructions
- **Stability:** WorkInProgress
- **Type:** Array (Element Type: Instruction)
- **Optional** (Default: `null`)
- **Computable:** No
- **Description:** List of instructions

#### InteractionInstruction
- **Stability:** Stable
- **Type:** ObjectRef (Object Type: Instruction)
- **Optional** (Default: `null`)
- **Computable:** No
- **Description:** An instruction designed to evaluate and set which players can interact with an NPC, along with setting correct states upon interaction

#### DeathInstruction
- **Stability:** Stable
- **Type:** ObjectRef (Object Type: Instruction)
- **Optional** (Default: `null`)
- **Computable:** No
- **Description:** An instruction which will run only when the NPC is dead until they are removed

#### InteractionVars
- **Stability:** Stable
- **Type:** CodecObject
- **Optional** (Default: `null`)
- **Computable:** No
- **Description:** Interaction vars to be used in interactions.

### Death & Despawn

#### DeathAnimationTime
- **Stability:** Experimental
- **Type:** Double
- **Optional** (Default: `5.0`)
- **Computable:** No
- **Description:** How long to let the death animation play before removing
- **Constraint:** Value must be greater or equal than 0

#### DeathInteraction
- **Stability:** Experimental
- **Type:** Asset
- **Optional** (Default: `null`)
- **Computable:** Yes
- **Description:** Interaction to run on death

#### DespawnAnimationTime
- **Stability:** Experimental
- **Type:** Double
- **Optional** (Default: `0.8`)
- **Computable:** No
- **Description:** How long to let the despawn animation play before removing
- **Constraint:** Value must be greater or equal than 0

### Spawn

#### SpawnParticles
- **Stability:** Experimental
- **Type:** String
- **Optional** (Default: `null`)
- **Computable:** No
- **Description:** Particle system when spawning

#### SpawnParticlesOffset
- **Stability:** Experimental
- **Type:** Array (Element Type: Double)
- **Optional** (Default: `null`)
- **Computable:** No
- **Description:** Displacement from foot point to spawn relative to NPC heading

#### SpawnViewDistance
- **Stability:** Experimental
- **Type:** Double
- **Optional** (Default: `75.0`)
- **Computable:** No
- **Description:** View distance for spawn particle
- **Constraint:** Value must be greater than 0

#### SpawnLockTime
- **Stability:** Stable
- **Type:** Double
- **Optional** (Default: `1.5`)
- **Computable:** Yes
- **Description:** How long the NPC should be locked and unable to perform behavior when first spawned
- **Constraint:** Value must be greater or equal than 0

### Memories

#### IsMemory
- **Stability:** Stable
- **Type:** Boolean
- **Optional** (Default: `false`)
- **Computable:** Yes
- **Description:** Used to define if the NPC has a Memory to record.

#### MemoriesCategory
- **Stability:** Stable
- **Type:** String
- **Optional** (Default: `Other`)
- **Computable:** Yes
- **Description:** Category to put the NPC in, as part of the Memories Plugin
- **Constraint:** String value must be either null or not empty

#### MemoriesNameOverride
- **Stability:** Stable
- **Type:** String
- **Optional** (Default: empty string)
- **Computable:** Yes
- **Description:** Overrides the Memory name when set.

### Debug

#### Debug
- **Stability:** WorkInProgress
- **Type:** String
- **Optional** (Default: empty string)
- **Computable:** No
- **Description:** Debugging flags

### Abstract Role Constraints

> At least one of `BreathesInAir`, `BreathesInWater` must be `true`.

---

## Generic Role (Stable)

Generic role for NPC with a core planner and list of Motion controllers.

The Generic Role shares the same attribute set as the Abstract Role. All attributes, types, defaults, constraints, and computable flags are identical. The Generic Role is the concrete instantiation used when defining standard NPCs.

### Health & Stats

#### MaxHealth
- **Stability:** Stable
- **Type:** Integer
- **Required** (no default)
- **Computable:** Yes
- **Description:** Max health
- **Constraint:** Value must be greater than 0

#### Invulnerable
- **Stability:** Stable
- **Type:** Boolean
- **Optional** (Default: `false`)
- **Computable:** Yes
- **Description:** Makes NPC ignore damage

#### KnockbackScale
- **Stability:** Stable
- **Type:** Double
- **Optional** (Default: `1.0`)
- **Computable:** Yes
- **Description:** Scale factor for knockback. Values greater than 1 increase knockback. Smaller values decrease it.
- **Constraint:** Value must be greater or equal than 0

#### Inertia
- **Stability:** Experimental
- **Type:** Double
- **Optional** (Default: `1.0`)
- **Computable:** No
- **Description:** Inertia
- **Constraint:** Value must be greater than 0.1

### Appearance

#### Appearance
- **Stability:** Stable
- **Type:** Asset
- **Required** (no default)
- **Computable:** Yes
- **Description:** Model to use for rendering

#### DisplayNames
- **Stability:** Stable
- **Type:** StringList
- **Optional** (Default: `null`)
- **Computable:** No
- **Description:** List of possible display names to choose from
- **Constraint:** Strings in array must not be empty

#### NameTranslationKey
- **Stability:** Stable
- **Type:** String
- **Required** (no default)
- **Computable:** Yes
- **Description:** The translation key for this NPC's name
- **Constraint:** String must be not empty

#### OverrideHeadPitchAngle
- **Stability:** Experimental
- **Type:** Boolean
- **Optional** (Default: `true`)
- **Computable:** Yes
- **Description:** Whether to override the head pitch angle range

#### HeadPitchAngleRange
- **Stability:** Experimental
- **Type:** Array (Element Type: Double)
- **Optional** (Default: `[-89.0, 89.0]`)
- **Computable:** Yes
- **Description:** Head rotation pitch range to be used instead of model camera settings
- **Constraint:** Values must be greater or equal than -90, less or equal than 90, and in weakly ascending order

### Inventory

#### InventorySize
- **Stability:** Stable
- **Type:** Integer
- **Optional** (Default: `0`)
- **Computable:** No
- **Description:** Number of available inventory slots
- **Constraint:** Value must be greater or equal than 0 and less or equal than 36

#### HotbarSize
- **Stability:** Stable
- **Type:** Integer
- **Optional** (Default: `3`)
- **Computable:** No
- **Description:** Number of available hotbar slots
- **Constraint:** Value must be greater or equal than 3 and less or equal than 8

#### OffHandSlots
- **Stability:** Stable
- **Type:** Integer
- **Optional** (Default: `0`)
- **Computable:** No
- **Description:** The number of slots for off-hand items
- **Constraint:** Value must be greater or equal than 0 and less or equal than 4

#### HotbarItems
- **Stability:** Stable
- **Type:** AssetArray (Element Type: Item)
- **Optional** (Default: `null`)
- **Computable:** Yes
- **Description:** Hotbar items (e.g. primary weapon, secondary weapon, etc)

#### OffHandItems
- **Stability:** Stable
- **Type:** AssetArray (Element Type: Item)
- **Optional** (Default: `null`)
- **Computable:** Yes
- **Description:** Off-hand items (e.g. shields, torches, etc)

#### DefaultOffHandSlot
- **Stability:** Stable
- **Type:** Integer
- **Optional** (Default: `-1`)
- **Computable:** Yes
- **Description:** The default off-hand item slot (-1 is empty)
- **Constraint:** Value must be greater or equal than -1 and less or equal than 4

#### PossibleInventoryItems
- **Stability:** Stable
- **Type:** Asset
- **Optional** (Default: `null`)
- **Computable:** Yes
- **Description:** A droplist defining the possible items the NPC's inventory could contain

#### Armor
- **Stability:** WorkInProgress
- **Type:** AssetArray (Element Type: Item)
- **Optional** (Default: `null`)
- **Computable:** No
- **Description:** Armor items

### Combat

#### CombatConfig
- **Stability:** Stable
- **Type:** CodecObject
- **Optional** (Default: `null`)
- **Computable:** No
- **Description:** The combat configuration providing optional combat action evaluator

#### DropList
- **Stability:** Stable
- **Type:** Asset
- **Optional** (Default: `null`)
- **Computable:** Yes
- **Description:** Drop list to spawn when killed

#### PickupDropOnDeath
- **Stability:** Stable
- **Type:** Boolean
- **Optional** (Default: `false`)
- **Computable:** No
- **Description:** Drop last picked item on death

### Attitudes

#### DefaultPlayerAttitude
- **Stability:** Stable
- **Type:** Flag
- **Optional** (Default: `HOSTILE`)
- **Computable:** Yes
- **Description:** The default attitude of this NPC towards players
- **Flag Values:**
  - `HOSTILE` -- is hostile towards the target
  - `REVERED` -- reveres the target
  - `FRIENDLY` -- is friendly towards the target
  - `IGNORE` -- is ignoring the target
  - `NEUTRAL` -- is neutral towards the target

#### DefaultNPCAttitude
- **Stability:** Stable
- **Type:** Flag
- **Optional** (Default: `NEUTRAL`)
- **Computable:** Yes
- **Description:** The default attitude of this NPC towards other NPCs
- **Flag Values:**
  - `HOSTILE` -- is hostile towards the target
  - `REVERED` -- reveres the target
  - `FRIENDLY` -- is friendly towards the target
  - `IGNORE` -- is ignoring the target
  - `NEUTRAL` -- is neutral towards the target

#### AttitudeGroup
- **Stability:** Stable
- **Type:** Asset
- **Optional** (Default: `null`)
- **Computable:** Yes
- **Description:** The attitude group towards other NPCs this NPC belongs to (often species related)

#### ItemAttitudeGroup
- **Stability:** Stable
- **Type:** Asset
- **Optional** (Default: `null`)
- **Computable:** Yes
- **Description:** This NPC's item attitudes

### Collision & Avoidance

#### CollisionDistance
- **Stability:** Stable
- **Type:** Double
- **Optional** (Default: `5.0`)
- **Computable:** No
- **Description:** Collision lookahead
- **Constraint:** Value must be greater than 0

#### CollisionForceFalloff
- **Stability:** Experimental
- **Type:** Double
- **Optional** (Default: `2.0`)
- **Computable:** No
- **Description:** Falloff rate for collision force
- **Constraint:** Value must be greater than 0

#### CollisionRadius
- **Stability:** Experimental
- **Type:** Double
- **Optional** (Default: `-1.0`)
- **Computable:** No
- **Description:** Collision radius override

#### CollisionViewAngle
- **Stability:** Experimental
- **Type:** Double
- **Optional** (Default: `320.0`)
- **Computable:** No
- **Description:** Collision detection view cone
- **Constraint:** Value must be greater or equal than 0 and less or equal than 360

#### ApplyAvoidance
- **Stability:** Experimental
- **Type:** Boolean
- **Optional** (Default: `false`)
- **Computable:** No
- **Description:** Apply avoidance steering force

#### AvoidanceMode
- **Stability:** Experimental
- **Type:** Flag
- **Optional** (Default: `Any`)
- **Computable:** No
- **Description:** Abilities to use for avoidance
- **Flag Values:**
  - `Evade` -- Only evade
  - `Slowdown` -- Only slow down NPC
  - `Any` -- Any avoidance allowed

#### EntityAvoidanceStrength
- **Stability:** Experimental
- **Type:** Double
- **Optional** (Default: `1.0`)
- **Computable:** No
- **Description:** Blending factor avoidance
- **Constraint:** Value must be greater or equal than 0

### Separation

#### ApplySeparation
- **Stability:** Experimental
- **Type:** Boolean
- **Optional** (Default: `false`)
- **Computable:** Yes
- **Description:** Apply separation steering force

#### SeparationDistance
- **Stability:** Experimental
- **Type:** Double
- **Optional** (Default: `3.0`)
- **Computable:** Yes
- **Description:** Desired separation distance
- **Constraint:** Value must be greater than 0

#### SeparationWeight
- **Stability:** Experimental
- **Type:** Double
- **Optional** (Default: `1.0`)
- **Computable:** Yes
- **Description:** Blend factor separation
- **Constraint:** Value must be greater or equal than 0

#### SeparationDistanceTarget
- **Stability:** Experimental
- **Type:** Double
- **Optional** (Default: `1.0`)
- **Computable:** Yes
- **Description:** Desired separation distance when close to target
- **Constraint:** Value must be greater or equal than 0

#### SeparationNearRadiusTarget
- **Stability:** Experimental
- **Type:** Double
- **Optional** (Default: `1.0`)
- **Computable:** Yes
- **Description:** Distance when using SeparationDistanceTarget
- **Constraint:** Value must be greater than 0

#### SeparationFarRadiusTarget
- **Stability:** Experimental
- **Type:** Double
- **Optional** (Default: `5.0`)
- **Computable:** Yes
- **Description:** Use normal separation distance from further than this distance
- **Constraint:** Value must be greater than 0

### Environment

#### OpaqueBlockSet
- **Stability:** Stable
- **Type:** Asset
- **Optional** (Default: `Opaque`)
- **Computable:** No
- **Description:** Blocks blocking line of sight

#### StayInEnvironment
- **Stability:** Experimental
- **Type:** Boolean
- **Optional** (Default: `false`)
- **Computable:** No
- **Description:** Stay in spawning environment

#### AllowedEnvironments
- **Stability:** Experimental
- **Type:** String
- **Optional** (Default: `null`)
- **Computable:** No
- **Description:** Allowed environment to walk in
- **Constraint:** String value must be either null or not empty

#### BreathesInAir
- **Stability:** WorkInProgress
- **Type:** Boolean
- **Optional** (Default: `true`)
- **Computable:** Yes
- **Description:** Can breathe in air

#### BreathesInWater
- **Stability:** WorkInProgress
- **Type:** Boolean
- **Optional** (Default: `false`)
- **Computable:** Yes
- **Description:** Can breathe in fluid/water

### Flock

#### FlockSpawnTypes
- **Stability:** WorkInProgress
- **Type:** Array (Element Type: String)
- **Optional** (Default: `null`)
- **Computable:** Yes
- **Description:** Types of NPC this flock should consist of
- **Constraint:** Strings in array must not be empty

#### FlockSpawnTypesRandom
- **Stability:** WorkInProgress
- **Type:** Boolean
- **Optional** (Default: `false`)
- **Computable:** Yes
- **Description:** Create a randomized flock if true, else spawn in order of FlockSpawnTypes

#### FlockAllowedNPC
- **Stability:** Experimental
- **Type:** Array (Element Type: String)
- **Optional** (Default: `null`)
- **Computable:** Yes
- **Description:** List of NPCs allowed in flock
- **Constraint:** Strings in array must not be empty

#### FlockCanLead
- **Stability:** Experimental
- **Type:** Boolean
- **Optional** (Default: `false`)
- **Computable:** Yes
- **Description:** This NPC can be flock leader

#### FlockWeightAlignment
- **Stability:** Experimental
- **Type:** Double
- **Optional** (Default: `1.0`)
- **Computable:** No
- **Description:** Blending flock alignment
- **Constraint:** Value must be greater than 0

#### FlockWeightSeparation
- **Stability:** Experimental
- **Type:** Double
- **Optional** (Default: `1.0`)
- **Computable:** No
- **Description:** Blending flock separation
- **Constraint:** Value must be greater than 0

#### FlockWeightCohesion
- **Stability:** Experimental
- **Type:** Double
- **Optional** (Default: `1.0`)
- **Computable:** No
- **Description:** Blending flock cohesion
- **Constraint:** Value must be greater than 0

#### FlockInfluenceRange
- **Stability:** Experimental
- **Type:** Double
- **Optional** (Default: `10.0`)
- **Computable:** No
- **Description:** Influence radius flock forces
- **Constraint:** Value must be greater than 0

#### CorpseStaysInFlock
- **Stability:** Stable
- **Type:** Boolean
- **Optional** (Default: `false`)
- **Computable:** No
- **Description:** Whether the NPC should stay in the flock until corpse removal or be removed at the moment of death

### Damage Groups

#### DisableDamageFlock
- **Stability:** WorkInProgress
- **Type:** Boolean
- **Optional** (Default: `true`)
- **Computable:** No
- **Description:** If true, disables combat damage from flock members

#### DisableDamageGroups
- **Stability:** WorkInProgress
- **Type:** AssetArray (Element Type: TagSet)
- **Optional** (Default: `null`)
- **Computable:** Yes
- **Description:** Members in this list of group won't cause damage

### State Machine

#### StartState
- **Stability:** Stable
- **Type:** String
- **Optional** (Default: `start`)
- **Computable:** No
- **Description:** Initial state
- **Constraint:** String must be not empty

#### DefaultSubState
- **Stability:** Stable
- **Type:** String
- **Optional** (Default: `Default`)
- **Computable:** No
- **Description:** The default sub state to reference when transitioning to a main state without a specified sub state
- **Constraint:** String must be not empty

#### BusyStates
- **Stability:** Stable
- **Type:** StringList
- **Required** (no default)
- **Computable:** No
- **Description:** States during which this NPC is busy and can't be interacted with
- **Constraint:** String must be a valid state string. A main state must be included before the period (e.g. `Main.Test`). State strings consist of a main state and a sub state (e.g. `Main.Test`). If nested within a substate, the main state may be omitted (e.g. `.Test`) when referencing.

#### StateTransitions
- **Stability:** Stable
- **Type:** ObjectRef (Object Type: StateTransitionController)
- **Optional** (Default: `null`)
- **Computable:** No
- **Description:** A set of state transitions and the actions that will be executed during them

#### StateEvaluator
- **Stability:** Stable
- **Type:** CodecObject
- **Optional** (Default: `null`)
- **Computable:** No
- **Description:** A state evaluator

### Motion & Controllers

#### MotionControllerList
- **Stability:** Stable
- **Type:** ObjectRef (Object Type: HashMap)
- **Required** (no default)
- **Computable:** No
- **Description:** Motion controllers

#### InitialMotionController
- **Stability:** Stable
- **Type:** String
- **Optional** (Default: `null`)
- **Computable:** Yes
- **Description:** The initial motion controller to set. If omitted and there are multiple, one will be chosen at random.
- **Constraint:** String value must be either null or not empty

### Instructions & Interactions

#### Instructions
- **Stability:** WorkInProgress
- **Type:** Array (Element Type: Instruction)
- **Optional** (Default: `null`)
- **Computable:** No
- **Description:** List of instructions

#### InteractionInstruction
- **Stability:** Stable
- **Type:** ObjectRef (Object Type: Instruction)
- **Optional** (Default: `null`)
- **Computable:** No
- **Description:** An instruction designed to evaluate and set which players can interact with an NPC, along with setting correct states upon interaction

#### DeathInstruction
- **Stability:** Stable
- **Type:** ObjectRef (Object Type: Instruction)
- **Optional** (Default: `null`)
- **Computable:** No
- **Description:** An instruction which will run only when the NPC is dead until they are removed

#### InteractionVars
- **Stability:** Stable
- **Type:** CodecObject
- **Optional** (Default: `null`)
- **Computable:** No
- **Description:** Interaction vars to be used in interactions.

### Death & Despawn

#### DeathAnimationTime
- **Stability:** Experimental
- **Type:** Double
- **Optional** (Default: `5.0`)
- **Computable:** No
- **Description:** How long to let the death animation play before removing
- **Constraint:** Value must be greater or equal than 0

#### DeathInteraction
- **Stability:** Experimental
- **Type:** Asset
- **Optional** (Default: `null`)
- **Computable:** Yes
- **Description:** Interaction to run on death

#### DespawnAnimationTime
- **Stability:** Experimental
- **Type:** Double
- **Optional** (Default: `0.8`)
- **Computable:** No
- **Description:** How long to let the despawn animation play before removing
- **Constraint:** Value must be greater or equal than 0

### Spawn

#### SpawnParticles
- **Stability:** Experimental
- **Type:** String
- **Optional** (Default: `null`)
- **Computable:** No
- **Description:** Particle system when spawning

#### SpawnParticlesOffset
- **Stability:** Experimental
- **Type:** Array (Element Type: Double)
- **Optional** (Default: `null`)
- **Computable:** No
- **Description:** Displacement from foot point to spawn relative to NPC heading

#### SpawnViewDistance
- **Stability:** Experimental
- **Type:** Double
- **Optional** (Default: `75.0`)
- **Computable:** No
- **Description:** View distance for spawn particle
- **Constraint:** Value must be greater than 0

#### SpawnLockTime
- **Stability:** Stable
- **Type:** Double
- **Optional** (Default: `1.5`)
- **Computable:** Yes
- **Description:** How long the NPC should be locked and unable to perform behavior when first spawned
- **Constraint:** Value must be greater or equal than 0

### Memories

#### IsMemory
- **Stability:** Stable
- **Type:** Boolean
- **Optional** (Default: `false`)
- **Computable:** Yes
- **Description:** Used to define if the NPC has a Memory to record.

#### MemoriesCategory
- **Stability:** Stable
- **Type:** String
- **Optional** (Default: `Other`)
- **Computable:** Yes
- **Description:** Category to put the NPC in, as part of the Memories Plugin
- **Constraint:** String value must be either null or not empty

#### MemoriesNameOverride
- **Stability:** Stable
- **Type:** String
- **Optional** (Default: empty string)
- **Computable:** Yes
- **Description:** Overrides the Memory name when set.

### Debug

#### Debug
- **Stability:** WorkInProgress
- **Type:** String
- **Optional** (Default: empty string)
- **Computable:** No
- **Description:** Debugging flags

### Generic Role Constraints

> At least one of `BreathesInAir`, `BreathesInWater` must be `true`.

---

## Variant Role (WorkInProgress)

Create a variant from an existing NPC JSON file.

The Variant Role allows you to derive a new NPC definition from an existing one, inheriting its configuration and overriding specific attributes as needed. This role is currently marked as **WorkInProgress** and has no documented attributes beyond the base NPC JSON reference.
