# Hytale NPC Documentation

> Based on the NPC system by **Hypixel Studios Canada Inc.**

---

## Table of Contents

| # | Document | Description |
|---|----------|-------------|
| 1 | [01-roles.md](01-roles.md) | Role Types (Abstract, Generic, Variant) |
| 2 | [02-motion-controllers.md](02-motion-controllers.md) | Motion Controllers (Walk, Fly, Dive) |
| 3 | [03-body-motions.md](03-body-motions.md) | Body Motions (Seek, Flee, Wander, Path, Land, Leave, MaintainDistance, Teleport, etc.) |
| 4 | [04-head-motions.md](04-head-motions.md) | Head Motions (Aim, Observe, Watch) |
| 5 | [05-sensors.md](05-sensors.md) | Sensors (Player, Mob, Target, Damage, Leash, State, Timer, Path, etc.) |
| 6 | [06-actions.md](06-actions.md) | Actions (Attack, State, Spawn, Die, Inventory, OverrideAttitude, etc.) |
| 7 | [07-entity-filters.md](07-entity-filters.md) | Entity Filters (Attitude, Combat, LineOfSight, ViewSector, etc.) |
| 8 | [08-instructions.md](08-instructions.md) | Instructions & Misc (Instruction, Reference, Random, StateTransitions, etc.) |

---

## Complete Builder Reference

All 183 NPC builders available in the Hytale NPC system.

| # | Name | Type | Description |
|---|------|------|-------------|
| 1 | Abstract | Role | Generic role for NPC |
| 2 | ActionList | ActionList | An array of actions to be executed |
| 3 | AddToHostileTargetMemory | Action | Adds the passed target from the sensor to the hostile target memory |
| 4 | AdjustPosition | Sensor | Perform adjustments to the wrapped sensor's returned position |
| 5 | Age | Sensor | Triggers when the age of the NPC falls between a certain range |
| 6 | Aim | HeadMotion | Aim at target |
| 7 | AimCharge | BodyMotion | Aim the NPC at a target position for performing a charge |
| 8 | Alarm | Sensor | Check the state of a named alarm |
| 9 | Altitude | IEntityFilter | Matches targets if they're within the defined range above the ground |
| 10 | And | Sensor | Logical AND of list of sensors |
| 11 | And | IEntityFilter | Logical AND of a list of filters |
| 12 | Animation | Sensor | Check if a given animation is being played |
| 13 | Any | Sensor | Return always true |
| 14 | Appearance | Action | Set model displayed for NPC |
| 15 | ApplyEntityEffect | Action | Applies an entity effect to the target or self |
| 16 | Attack | Action | Starts attack |
| 17 | Attitude | ISensorEntityPrioritiser | Prioritises return entities by attitude |
| 18 | Attitude | IEntityFilter | Matches the attitude towards the locked target |
| 19 | Beacon | Action | Send Beacon Message |
| 20 | Beacon | Sensor | Checks to see if any messages have been broadcasted by nearby NPCs |
| 21 | Block | Sensor | Checks for one of a set of blocks in the nearby area |
| 22 | BlockChange | Sensor | Matches when a block from a blockset within a certain range is changed or interacted with |
| 23 | BlockType | Sensor | Checks if the block at the given position matches the provided block set |
| 24 | CanInteract | Sensor | Checks whether or not the player can interact with this NPC |
| 25 | CanPlaceBlock | Sensor | Test if the currently set block can be placed |
| 26 | Combat | IEntityFilter | Check the target's combat state |
| 27 | CombatAbility | Action | Starts the combat ability selected by the combat action evaluator |
| 28 | CombatActionEvaluator | Sensor | Handles funnelling information from the combat action evaluator |
| 29 | CombatTargets | ISensorEntityCollector | Processes matched friendly and hostile targets |
| 30 | CompleteTask | Action | Complete a task |
| 31 | Count | Sensor | Check if there is a certain number of NPCs or players within a specific range |
| 32 | Crouch | Action | Set NPC crouching state |
| 33 | Damage | Sensor | Test if NPC suffered damage |
| 34 | DelayDespawn | Action | Delay the despawning cycle |
| 35 | Despawn | Action | Trigger the NPC to despawn |
| 36 | Die | Action | Kill the NPC |
| 37 | DisplayName | Action | Set display name |
| 38 | Dive | MotionController | Provide diving abilities for NPC |
| 39 | DropItem | Action | Drop an item |
| 40 | DroppedItem | Sensor | Triggers if a given item is within a certain range |
| 41 | EntityEvent | Sensor | Matches when an entity from a specific NPC group is damaged/killed/interacted |
| 42 | Eval | Sensor | Evaluate javascript expression and test if true |
| 43 | Flag | Sensor | Test if a named flag is set or not |
| 44 | Flee | BodyMotion | Move away from target |
| 45 | Flock | BodyMotion | Flocking - WIP |
| 46 | Flock | IEntityFilter | Test for flock membership and related properties |
| 47 | FlockBeacon | Action | Send beacon message to flock |
| 48 | FlockCombatDamage | Sensor | Test if flock with NPC received combat damage |
| 49 | FlockLeader | Sensor | Test for the presence of the flock leader |
| 50 | FlockState | Action | Set state name for flock |
| 51 | FlockTarget | Action | Set or clear locked target for flock |
| 52 | Fly | MotionController | Flight motion controller |
| 53 | Generic | Role | Generic role for NPC |
| 54 | HasHostileTargetMemory | Sensor | Checks if there is currently a hostile target in the target memory |
| 55 | HasInteracted | Sensor | Checks whether player has interacted with this NPC |
| 56 | HasTask | Sensor | Checks whether player has any of the given tasks |
| 57 | HashMap | HashMap | List of motion controllers |
| 58 | HeightDifference | IEntityFilter | Matches entities within the given height range |
| 59 | IgnoreForAvoidance | Action | Set entity to be ignored during avoidance |
| 60 | InAir | Sensor | Test if NPC is not on ground |
| 61 | InWater | Sensor | Check if NPC is currently in water |
| 62 | InflictedDamage | Sensor | Test if NPC inflicted combat damage |
| 63 | InsideBlock | IEntityFilter | Matches if the entity is inside any of the blocks in the BlockSet |
| 64 | Instruction | Instruction | An instruction with Sensor, Motions and Actions |
| 65 | InteractionContext | Sensor | Checks interaction context |
| 66 | Inventory | Action | Add or remove items from inventory |
| 67 | Inventory | IEntityFilter | Test conditions relating to entity inventory |
| 68 | IsBackingAway | Sensor | Test if the NPC is currently backing away |
| 69 | IsBusy | Sensor | Tests if NPC is in one of the defined Busy States |
| 70 | ItemInHand | IEntityFilter | Check if entity is holding an item |
| 71 | JoinFlock | Action | Join/build a flock with other entity |
| 72 | Kill | Sensor | Test if NPC made a kill |
| 73 | Land | BodyMotion | Try to land at the given position |
| 74 | Leash | Sensor | Triggers when NPC is outside range from leash point |
| 75 | Leave | BodyMotion | Leave place |
| 76 | LeaveFlock | Action | Leave flock |
| 77 | Light | Sensor | Check the light levels |
| 78 | LineOfSight | IEntityFilter | Matches if there is line of sight to target |
| 79 | LockOnInteractionTarget | Action | Locks on to the interaction target player |
| 80 | Log | Action | Log a message to console |
| 81 | MaintainDistance | BodyMotion | Maintain distance from a given position |
| 82 | MakePath | Action | Constructs a transient path for the NPC |
| 83 | MatchLook | BodyMotion | Make NPC body rotate to match look direction |
| 84 | Mob | Sensor | Test if entity matching specific attributes is in range |
| 85 | ModelAttachment | Action | Set an attachment on the current NPC model |
| 86 | MotionController | Sensor | Test if specific motion controller is active |
| 87 | Mount | Action | Enable player to mount the entity |
| 88 | MovementState | IEntityFilter | Check if entity is in the given movement state |
| 89 | NPCGroup | IEntityFilter | Returns whether entity matches NPCGroups |
| 90 | Nav | Sensor | Queries navigation state |
| 91 | Not | Sensor | Invert sensor test |
| 92 | Not | IEntityFilter | Invert filter test |
| 93 | Nothing | Action | Do nothing |
| 94 | Nothing | BodyMotion | Do nothing |
| 95 | Nothing | HeadMotion | Do nothing |
| 96 | Notify | Action | Directly notifies a target NPC with beacon message |
| 97 | Observe | HeadMotion | Observe surroundings in various ways |
| 98 | OnGround | Sensor | Test if NPC is on ground |
| 99 | OpenBarterShop | Action | Open the barter shop UI |
| 100 | OpenShop | Action | Open the shop UI |
| 101 | Or | Sensor | Logical OR of list of sensors |
| 102 | Or | IEntityFilter | Logical OR of a list of filters |
| 103 | OverrideAltitude | Action | Temporarily override altitude of flying NPC |
| 104 | OverrideAttitude | Action | Override NPC attitude towards target |
| 105 | ParentState | Action | Set main state from within a component |
| 106 | Path | Path | List of transient path points |
| 107 | Path | Sensor | Find a path based on various criteria |
| 108 | Path | BodyMotion | Walk along a path |
| 109 | PickUpItem | Action | Pick up an item |
| 110 | PlaceBlock | Action | Place a block at a position |
| 111 | PlayAnimation | Action | Play an animation |
| 112 | PlaySound | Action | Plays a sound within specified range |
| 113 | Player | Sensor | Test if player matching attributes is in range |
| 114 | Random | Action | Execute random action from weighted list |
| 115 | Random | Sensor | Alternates between returning true and false |
| 116 | Random | Instruction | Randomised list of weighted instructions |
| 117 | ReadPosition | Sensor | Read a stored position with conditions |
| 118 | RecomputePath | Action | Force recomputation of path finder solution |
| 119 | Reference | Instruction | Prioritized instruction list that can be referenced |
| 120 | RelativeWaypointDefinition | RelativeWaypointDefinition | A relative path waypoint |
| 121 | ReleaseTarget | Action | Clear locked target |
| 122 | Remove | Action | Erase entity from world (no death animation) |
| 123 | ResetBlockSensors | Action | Resets block sensors |
| 124 | ResetInstructions | Action | Force reset instructionList |
| 125 | ResetPath | Action | Resets the current patrol path |
| 126 | ResetSearchRays | Action | Resets search ray sensors |
| 127 | Role | Action | Change the Role of the NPC |
| 128 | SearchRay | Sensor | Fire a ray to check block matches |
| 129 | Seek | BodyMotion | Chase target |
| 130 | Self | Sensor | Test if the NPC matches entity filters |
| 131 | Sequence | Action | List of actions |
| 132 | Sequence | BodyMotion | (Looped) Sequence of motions |
| 133 | Sequence | HeadMotion | (Looped) Sequence of motions |
| 134 | SetAlarm | Action | Set a named alarm |
| 135 | SetBlockToPlace | Action | Set the block type to place |
| 136 | SetFlag | Action | Set a named flag |
| 137 | SetInteractable | Action | Set whether player can interact with NPC |
| 138 | SetLeashPosition | Action | Sets the NPC leash/spawn position |
| 139 | SetMarkedTarget | Action | Explicitly sets a marked target |
| 140 | SetStat | Action | Sets or adds to an entity stat |
| 141 | Spawn | Action | Spawn an NPC |
| 142 | SpawnParticles | Action | Spawn particle system |
| 143 | SpotsMe | IEntityFilter | Checks if entity can view the NPC |
| 144 | StandingOnBlock | IEntityFilter | Matches block beneath entity |
| 145 | StartObjective | Action | Start an objective for the player |
| 146 | Stat | IEntityFilter | Match stat values of entity |
| 147 | State | Action | Set state of NPC |
| 148 | State | Sensor | Test for a specific state |
| 149 | StateTransition | StateTransition | State transition with actions |
| 150 | StateTransitionController | StateTransitionController | List of state transitions |
| 151 | StateTransitionEdges | StateTransitionEdges | From/to state transition definitions |
| 152 | StorePosition | Action | Store position from sensor |
| 153 | Switch | Sensor | Check if computed boolean is true |
| 154 | TakeOff | BodyMotion | Switch from walking to flying |
| 155 | Target | Sensor | Test if target matches criteria |
| 156 | Teleport | BodyMotion | Teleport NPC to sensor position |
| 157 | Test | Action | Test action (DO NOT USE) |
| 158 | TestProbe | BodyMotion | Debugging test probing |
| 159 | Time | Sensor | Check day/year time |
| 160 | Timeout | Action | Delay an action |
| 161 | Timer | Sensor | Tests if timer exists and value in range |
| 162 | Timer | BodyMotion | Execute Motion for specific time |
| 163 | Timer | HeadMotion | Execute Motion for specific time |
| 164 | TimerContinue | Action | Continue a timer |
| 165 | TimerModify | Action | Modify timer values |
| 166 | TimerPause | Action | Pause a timer |
| 167 | TimerRestart | Action | Restart a timer |
| 168 | TimerStart | Action | Start a timer |
| 169 | TimerStop | Action | Stop a timer |
| 170 | ToggleStateEvaluator | Action | Enable/disable state evaluator |
| 171 | TriggerSpawnBeacon | Action | Trigger nearest spawn beacon |
| 172 | TriggerSpawners | Action | Trigger manual spawn markers |
| 173 | ValueProviderWrapper | Sensor | Wraps sensor with parameter overrides |
| 174 | ValueToParameterMapping | ValueToParameterMapping | Value to parameter mapping entry |
| 175 | Variant | Role | Create variant from existing NPC JSON |
| 176 | ViewSector | IEntityFilter | Matches entities within view sector |
| 177 | Walk | MotionController | Walk on ground abilities |
| 178 | Wander | BodyMotion | Random movement |
| 179 | WanderInCircle | BodyMotion | Random movement in circle around spawn |
| 180 | WanderInRect | BodyMotion | Random movement in rectangle around spawn |
| 181 | Watch | HeadMotion | Rotate to target |
| 182 | Weather | Sensor | Matches current weather against patterns |
| 183 | WeightedAction | WeightedAction | Wrapped and weighted action for Random lists |
