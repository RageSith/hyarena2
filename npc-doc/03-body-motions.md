# BodyMotion Types

All available `BodyMotion` types for NPC behavior configuration. Each BodyMotion is assigned to an instruction's `BodyMotion` field and controls how the NPC physically moves its body.

---

## Table of Contents

- [AimCharge](#aimcharge) -- Aim for charge attack
- [Flee](#flee) -- Move away from target using pathfinding
- [Flock](#flock) -- Flocking (WIP)
- [Land](#land) -- Land at position
- [Leave](#leave) -- Get away from current position
- [MaintainDistance](#maintaindistance) -- Maintain distance from target
- [MatchLook](#matchlook) -- Body rotate to match look
- [Nothing](#nothing) -- Do nothing
- [Path](#path) -- Walk along a path
- [Seek](#seek) -- Chase target using pathfinding
- [Sequence](#sequence) -- Sequence of motions
- [TakeOff](#takeoff) -- Switch to flying
- [Teleport](#teleport) -- Teleport to position
- [TestProbe](#testprobe) -- Debug probing
- [Timer](#timer) -- Execute motion for specific time
- [Wander](#wander) -- Random movement in short linear pieces
- [WanderInCircle](#wanderincircle) -- Random movement in circle
- [WanderInRect](#wanderinrect) -- Random movement in rectangle

---

## AimCharge

**Stability:** Stable

Aim the NPC at a target position for performing a charge based on aiming information and ensure that the charge is possible before it is executed.

### Attributes

| Attribute | Type | Default | Stability | Computable | Constraint | Description |
|-----------|------|---------|-----------|------------|------------|-------------|
| RelativeTurnSpeed | Double | 1.0 | Stable | Yes | > 0, <= 2 | The relative turn speed modifier |

### Constraints

- Must be attached to a sensor that provides one of: player target, NPC target, dropped item target, vector position.

---

## Flee

**Stability:** Experimental

Move away from a target using pathfinding or steering. The NPC will flee from the sensor's locked target, changing direction periodically and behaving more erratically when the target is close.

### Speed Attributes

| Attribute | Type | Default | Stability | Computable | Constraint | Description |
|-----------|------|---------|-----------|------------|------------|-------------|
| RelativeSpeed | Double | 1.0 | Stable | Yes | > 0, <= 2 | Maximum relative speed the NPC should move |
| RelativeSpeedWaypoint | Double | 0.5 | Stable | Yes | > 0, <= 1 | Maximum relative speed the NPC should move close to waypoints |
| WaypointRadius | Double | 0.5 | Stable | Yes | > 0.1 | Radius to slow down around waypoints |
| BlendHeading | Double | 0.5 | Stable | Yes | >= 0, <= 1 | Relative rotation angle into next waypoint when arriving at current waypoint |

### Pathfinding Attributes

| Attribute | Type | Default | Stability | Computable | Constraint | Description |
|-----------|------|---------|-----------|------------|------------|-------------|
| UsePathfinder | Boolean | true | Stable | Yes | -- | Use path finder |
| UseBestPath | Boolean | true | Stable | Yes | -- | Use best partial path if goal can't be reached |
| BuildOptimisedPath | Boolean | true | Stable | Yes | -- | Try to reduce number of nodes of generated path |
| DiagonalMoves | Boolean | true | Stable | Yes | -- | Allow diagonal moves |
| PathSmoothing | Integer | 2 | Stable | Yes | >= 0 | Try to smooth followed path. Larger values smooth more |
| RejectionWeight | Double | 3.0 | Stable | Yes | > 0 | Weight of rejection vector pushing entity closer to original path |
| MinPathLength | Double | 2.0 | Experimental | Yes | >= 0 | Minimum length of path required when not able to reach target (should be >= 2) |
| StepsPerTick | Integer | 50 | Stable | Yes | > 0 | Steps per iteration |
| MaxPathLength | Integer | 200 | Stable | Yes | > 0 | Max path steps before aborting path finding |
| MaxOpenNodes | Integer | 200 | Stable | Yes | > 0 | Max open nodes before aborting path finding |
| MaxTotalNodes | Integer | 900 | Stable | Yes | > 0 | Max total nodes before aborting path finding |
| ThrottleDelayRange | Array\<Double\> | [3.0, 5.0] | Stable | Yes | >= 0, ascending | Time to delay after no pathfinding solution found |
| ThrottleIgnoreCount | Integer | 3 | Stable | Yes | >= 0 | How often no valid path solution can be found before throttling delay is applied |
| AvoidBlockDamage | Boolean | true | Stable | Yes | -- | Should avoid environmental damage from blocks |
| RelaxedMoveConstraints | Boolean | true | Stable | Yes | -- | NPC can do movements like wading (depends on motion controller type) |

### Steering Attributes

| Attribute | Type | Default | Stability | Computable | Constraint | Description |
|-----------|------|---------|-----------|------------|------------|-------------|
| UseSteering | Boolean | true | Stable | Yes | -- | Use simple/cheap steering if available |
| SkipSteering | Boolean | true | Experimental | Yes | -- | Skip steering if target not reachable |

### Distance Thresholds

| Attribute | Type | Default | Stability | Computable | Constraint | Description |
|-----------|------|---------|-----------|------------|------------|-------------|
| SlowDownDistance | Double | 8.0 | Stable | Yes | > 0 | Distance from target when NPC should start to slow down |
| StopDistance | Double | 10.0 | Stable | Yes | > 0 | Distance from target when NPC should halt |
| Falloff | Double | 3.0 | Stable | Yes | > 0 | Rate how fast the slowdown should happen relative to distance |
| AdjustRangeByHitboxSize | Boolean | false | Stable | Yes | -- | Correct range by hitbox sizes of involved entities |

### Recomputation Attributes

| Attribute | Type | Default | Stability | Computable | Constraint | Description |
|-----------|------|---------|-----------|------------|------------|-------------|
| WaitDistance | Double | 1.0 | Experimental | Yes | > 0 | Minimum distance target needs to move before recomputing path when no path can be found |
| RecomputeDistance | Double | 10.0 | Experimental | Yes | >= 0 | Maximum distance target can move before path is recomputed (0 to suppress recomputation) |
| ReprojectDistance | Double | 0.5 | Experimental | Yes | >= 0 | Maximum distance target can move before position is reprojected |
| RecomputeConeAngle | Double | 0.0 | Experimental | Yes | >= 0, <= 360 | Recompute path when target leaves cone from initial position to target |

### Flee-Specific Direction Attributes

| Attribute | Type | Default | Stability | Computable | Constraint | Description |
|-----------|------|---------|-----------|------------|------------|-------------|
| HoldDirectionTimeRange | Array\<Double\> | [2.0, 5.0] | Stable | Yes | >= 0, ascending | How often to change heading |
| ChangeDirectionViewSector | Double | 230.0 | Stable | Yes | >= 0, <= 360 | The view sector the NPC uses to decide if it should switch direction |
| DirectionJitter | Double | 45.0 | Stable | Yes | >= 0, <= 180 | How much jitter in degrees to add to the heading |
| ErraticDistance | Double | 4.0 | Stable | Yes | > 0 | If the player is closer than this distance, the NPC will behave more erratically |
| ErraticExtraJitter | Double | 45.0 | Stable | Yes | >= 0, <= 180 | Extra jitter added on top of standard when the target is too close |
| ErraticChangeDurationMultiplier | Double | 0.5 | Stable | Yes | > 0, <= 1 | Multiplier to decrease duration between direction changes when target is too close |

### Other Attributes

| Attribute | Type | Default | Stability | Computable | Constraint | Description |
|-----------|------|---------|-----------|------------|------------|-------------|
| DesiredAltitudeWeight | Double | -1.0 | Stable | Yes | >= -1, <= 1 | How much the NPC prefers being within the desired height range (0 = don't care, 1 = strongly prefer, <0 = use motion controller default) |
| Debug | String | *(empty)* | Stable | No | -- | Debugging flags |

### Constraints

- Must be attached to a sensor that provides one of: player target, NPC target, dropped item target, vector position.
- SlowDownDistance must be less or equal than StopDistance.

---

## Flock

**Stability:** Experimental

Flocking -- Work in Progress. No attributes.

### Attributes

*(None)*

### Constraints

*(None)*

---

## Land

**Stability:** Experimental

Try to land at the given position using a seek-like motion. Used for flying NPCs to transition from air to ground.

### Speed Attributes

| Attribute | Type | Default | Stability | Computable | Constraint | Description |
|-----------|------|---------|-----------|------------|------------|-------------|
| RelativeSpeed | Double | 1.0 | Stable | Yes | > 0, <= 2 | Maximum relative speed the NPC should move |
| RelativeSpeedWaypoint | Double | 0.5 | Stable | Yes | > 0, <= 1 | Maximum relative speed the NPC should move close to waypoints |
| WaypointRadius | Double | 0.5 | Stable | Yes | > 0.1 | Radius to slow down around waypoints |
| BlendHeading | Double | 0.5 | Stable | Yes | >= 0, <= 1 | Relative rotation angle into next waypoint when arriving at current waypoint |

### Pathfinding Attributes

| Attribute | Type | Default | Stability | Computable | Constraint | Description |
|-----------|------|---------|-----------|------------|------------|-------------|
| UsePathfinder | Boolean | true | Stable | Yes | -- | Use path finder |
| UseBestPath | Boolean | true | Stable | Yes | -- | Use best partial path if goal can't be reached |
| BuildOptimisedPath | Boolean | true | Stable | Yes | -- | Try to reduce number of nodes of generated path |
| DiagonalMoves | Boolean | true | Stable | Yes | -- | Allow diagonal moves |
| PathSmoothing | Integer | 2 | Stable | Yes | >= 0 | Try to smooth followed path. Larger values smooth more |
| RejectionWeight | Double | 3.0 | Stable | Yes | > 0 | Weight of rejection vector pushing entity closer to original path |
| MinPathLength | Double | 2.0 | Experimental | Yes | >= 0 | Minimum length of path required when not able to reach target (should be >= 2) |
| StepsPerTick | Integer | 50 | Stable | Yes | > 0 | Steps per iteration |
| MaxPathLength | Integer | 200 | Stable | Yes | > 0 | Max path steps before aborting path finding |
| MaxOpenNodes | Integer | 200 | Stable | Yes | > 0 | Max open nodes before aborting path finding |
| MaxTotalNodes | Integer | 900 | Stable | Yes | > 0 | Max total nodes before aborting path finding |
| ThrottleDelayRange | Array\<Double\> | [3.0, 5.0] | Stable | Yes | >= 0, ascending | Time to delay after no pathfinding solution found |
| ThrottleIgnoreCount | Integer | 3 | Stable | Yes | >= 0 | How often no valid path solution can be found before throttling delay is applied |
| AvoidBlockDamage | Boolean | true | Stable | Yes | -- | Should avoid environmental damage from blocks |
| RelaxedMoveConstraints | Boolean | true | Stable | Yes | -- | NPC can do movements like wading (depends on motion controller type) |

### Steering Attributes

| Attribute | Type | Default | Stability | Computable | Constraint | Description |
|-----------|------|---------|-----------|------------|------------|-------------|
| UseSteering | Boolean | true | Stable | Yes | -- | Use simple/cheap steering if available |
| SkipSteering | Boolean | true | Experimental | Yes | -- | Skip steering if target not reachable |

### Distance Thresholds

| Attribute | Type | Default | Stability | Computable | Constraint | Description |
|-----------|------|---------|-----------|------------|------------|-------------|
| SlowDownDistance | Double | 8.0 | Stable | Yes | > 0 | Distance when to slow down when approaching |
| StopDistance | Double | 10.0 | Stable | Yes | > 0 | Distance to stop at |
| AbortDistance | Double | 96.0 | Stable | Yes | > 0 | Distance to abort behaviour |
| Falloff | Double | 3.0 | Stable | Yes | > 0 | Deceleration when approaching target |
| SwitchToSteeringDistance | Double | 20.0 | Stable | Yes | > 0 | Distance below which NPC can test if target is reachable and abort existing path |
| AdjustRangeByHitboxSize | Boolean | false | Stable | Yes | -- | Correct range by hitbox sizes of involved entities |

### Recomputation Attributes

| Attribute | Type | Default | Stability | Computable | Constraint | Description |
|-----------|------|---------|-----------|------------|------------|-------------|
| WaitDistance | Double | 1.0 | Experimental | Yes | > 0 | Minimum distance target needs to move before recomputing path when no path can be found |
| RecomputeDistance | Double | 10.0 | Experimental | Yes | >= 0 | Maximum distance target can move before path is recomputed (0 to suppress recomputation) |
| ReprojectDistance | Double | 0.5 | Experimental | Yes | >= 0 | Maximum distance target can move before position is reprojected |
| RecomputeConeAngle | Double | 0.0 | Experimental | Yes | >= 0, <= 360 | Recompute path when target leaves cone from initial position to target |

### Land-Specific Attributes

| Attribute | Type | Default | Stability | Computable | Constraint | Description |
|-----------|------|---------|-----------|------------|------------|-------------|
| Reachable | Boolean | false | Experimental | Yes | -- | Target must be reachable so that hitboxes can overlap |
| HeightDifference | Array\<Double\> | [-1.0, 1.0] | Experimental | Yes | ascending (strict) | Height difference allowed to target |
| GoalLenience | Double | 2.0 | Experimental | Yes | > 0 | The distance from the target landing point that is acceptable to land at |

### Other Attributes

| Attribute | Type | Default | Stability | Computable | Constraint | Description |
|-----------|------|---------|-----------|------------|------------|-------------|
| DesiredAltitudeWeight | Double | -1.0 | Stable | Yes | >= -1, <= 1 | How much the NPC prefers being within the desired height range (0 = don't care, 1 = strongly prefer, <0 = use motion controller default) |
| Debug | String | *(empty)* | Stable | No | -- | Debugging flags |

### Constraints

- Must be attached to a sensor that provides one of: player target, NPC target, dropped item target, vector position.
- SlowDownDistance must be greater or equal than StopDistance.

---

## Leave

**Stability:** Experimental

Get away from the NPC's current position using pathfinding. Unlike Flee, this does not require a target to flee from -- it simply moves away from wherever the NPC currently is.

### Speed Attributes

| Attribute | Type | Default | Stability | Computable | Constraint | Description |
|-----------|------|---------|-----------|------------|------------|-------------|
| RelativeSpeed | Double | 1.0 | Stable | Yes | > 0, <= 2 | Maximum relative speed the NPC should move |
| RelativeSpeedWaypoint | Double | 0.5 | Stable | Yes | > 0, <= 1 | Maximum relative speed the NPC should move close to waypoints |
| WaypointRadius | Double | 0.5 | Stable | Yes | > 0.1 | Radius to slow down around waypoints |
| BlendHeading | Double | 0.5 | Stable | Yes | >= 0, <= 1 | Relative rotation angle into next waypoint when arriving at current waypoint |

### Pathfinding Attributes

| Attribute | Type | Default | Stability | Computable | Constraint | Description |
|-----------|------|---------|-----------|------------|------------|-------------|
| UseBestPath | Boolean | true | Stable | Yes | -- | Use best partial path if goal can't be reached |
| BuildOptimisedPath | Boolean | true | Stable | Yes | -- | Try to reduce number of nodes of generated path |
| DiagonalMoves | Boolean | true | Stable | Yes | -- | Allow diagonal moves |
| PathSmoothing | Integer | 2 | Stable | Yes | >= 0 | Try to smooth followed path. Larger values smooth more |
| RejectionWeight | Double | 3.0 | Stable | Yes | > 0 | Weight of rejection vector pushing entity closer to original path |
| MinPathLength | Double | 2.0 | Experimental | Yes | >= 0 | Minimum length of path required when not able to reach target (should be >= 2) |
| StepsPerTick | Integer | 50 | Stable | Yes | > 0 | Steps per iteration |
| MaxPathLength | Integer | 200 | Stable | Yes | > 0 | Max path steps before aborting path finding |
| MaxOpenNodes | Integer | 200 | Stable | Yes | > 0 | Max open nodes before aborting path finding |
| MaxTotalNodes | Integer | 900 | Stable | Yes | > 0 | Max total nodes before aborting path finding |
| ThrottleDelayRange | Array\<Double\> | [3.0, 5.0] | Stable | Yes | >= 0, ascending | Time to delay after no pathfinding solution found |
| ThrottleIgnoreCount | Integer | 3 | Stable | Yes | >= 0 | How often no valid path solution can be found before throttling delay is applied |
| AvoidBlockDamage | Boolean | true | Stable | Yes | -- | Should avoid environmental damage from blocks |
| RelaxedMoveConstraints | Boolean | true | Stable | Yes | -- | NPC can do movements like wading (depends on motion controller type) |

### Leave-Specific Attributes

| Attribute | Type | Default | Stability | Computable | Constraint | Description |
|-----------|------|---------|-----------|------------|------------|-------------|
| Distance | Double | *(required)* | Experimental | Yes | > 0 | Minimum distance required to move away |

### Other Attributes

| Attribute | Type | Default | Stability | Computable | Constraint | Description |
|-----------|------|---------|-----------|------------|------------|-------------|
| DesiredAltitudeWeight | Double | -1.0 | Stable | Yes | >= -1, <= 1 | How much the NPC prefers being within the desired height range (0 = don't care, 1 = strongly prefer, <0 = use motion controller default) |
| Debug | String | *(empty)* | Stable | No | -- | Debugging flags |

### Constraints

*(None documented)*

---

## MaintainDistance

**Stability:** Stable

Maintain distance from a given position. The NPC will try to stay within a desired distance range from its target, moving forwards or backwards as needed. Optionally supports strafing (lateral movement around the target).

### Attributes

| Attribute | Type | Default | Stability | Computable | Constraint | Description |
|-----------|------|---------|-----------|------------|------------|-------------|
| DesiredDistanceRange | Array\<Double\> | *(required)* | Stable | Yes | ascending | The desired distance to remain at |
| TargetDistanceFactor | Double | 0.5 | Stable | Yes | >= 0, <= 1 | Factor for where to move within the target range when outside it (0 = shortest distance into range, 1 = furthest, 0.5 = middle) |
| MoveThreshold | Double | 1.0 | Stable | Yes | > 0 | Extra threshold distance on either side of the desired range before movement is triggered |
| RelativeForwardsSpeed | Double | 1.0 | Stable | Yes | > 0, <= 2 | Maximum relative speed for the NPC moving forwards |
| RelativeBackwardsSpeed | Double | 1.0 | Stable | Yes | > 0, <= 2 | Maximum relative speed for the NPC moving backwards |
| MoveTowardsSlowdownThreshold | Double | 2.0 | Stable | Yes | >= 0 | Distance away from the target stopping point at which the NPC will start to slow down while moving towards the target |
| StrafingDurationRange | Array\<Double\> | [0.0, 0.0] | Stable | Yes | >= 0, ascending | How long to strafe for (moving left or right around the target). Set to [0, 0] to disable horizontal movement |
| StrafingFrequencyRange | Array\<Double\> | [2.0, 2.0] | Stable | Yes | > 0, ascending | How frequently to execute strafing |

### Constraints

- Must be attached to a sensor that provides one of: player target, NPC target, dropped item target, vector position.

---

## MatchLook

**Stability:** Stable

Make the NPC body rotate to match look direction. No attributes.

### Attributes

*(None)*

### Constraints

*(None)*

---

## Nothing

**Stability:** Stable

Do nothing. The NPC will stand still. Useful as a placeholder or in sequences/timers.

### Attributes

*(None)*

### Constraints

*(None)*

---

## Path

**Stability:** Stable

Walk along a predefined path of waypoints. Supports various path shapes (loop, line, chain, points), walking directions, and observation behavior at nodes.

### Speed Attributes

| Attribute | Type | Default | Stability | Computable | Constraint | Description |
|-----------|------|---------|-----------|------------|------------|-------------|
| MinRelSpeed | Double | 0.5 | Stable | No | > 0, <= 1 | Minimum relative walk speed |
| MaxRelSpeed | Double | 0.5 | Stable | No | > 0, <= 1 | Maximum relative walk speed |

### Path Configuration

| Attribute | Type | Default | Stability | Computable | Constraint | Description |
|-----------|------|---------|-----------|------------|------------|-------------|
| StartAtNearestNode | Boolean | true | Stable | No | -- | Start at closest waypoint |
| PathWidth | Double | 0.0 | Experimental | No | >= 0 | Walking corridor width |
| NodeWidth | Double | 0.2 | Experimental | No | > 0 | Radius of waypoint node |
| MinWalkDistance | Double | 0.0 | Experimental | No | >= 0 | Minimum walk distance when PathWidth > 0 |
| MaxWalkDistance | Double | 0.0 | Experimental | No | >= 0 | Maximum walk distance when PathWidth > 0 |
| Direction | Flag | FORWARD | Stable | No | -- | Walking direction relative to order of nodes |
| Shape | Flag | LOOP | Stable | Yes | -- | Shape of the path |

**Direction flag values:**

| Value | Description |
|-------|-------------|
| FORWARD | Start visiting nodes in order |
| BACKWARD | Start visiting nodes in reverse order |
| RANDOM | Can change direction between nodes and randomly pick target node in Points shape mode |
| ANY | Pick any start direction |

**Shape flag values:**

| Value | Description |
|-------|-------------|
| LOOP | Nodes form a closed loop (last node leads to first node) |
| LINE | Nodes form an open path of line segments |
| CHAIN | Nodes form an open path and will chain together with the next nearest path upon reaching the final node |
| POINTS | Any path between nodes is possible |

### Node Pause Attributes

| Attribute | Type | Default | Stability | Computable | Constraint | Description |
|-----------|------|---------|-----------|------------|------------|-------------|
| MinNodeDelay | Double | 0.0 | Stable | No | >= 0 | Minimum resting time at a node |
| MaxNodeDelay | Double | 0.0 | Stable | No | >= 0 | Maximum resting time at a node |
| UseNodeViewDirection | Boolean | false | Stable | No | -- | Look into next node direction at node |
| NodePauseScaleRange | Array\<Double\> | [0.2, 0.4] | Stable | Yes | > 0, ascending | Portion of total node pause time spent facing a direction before turning |
| NodePauseExtraPercentRange | Array\<Double\> | [0.0, 0.2] | Stable | Yes | >= 0, <= 1, ascending | Additional percentage of directional pause time to add |

### Observation Attributes

| Attribute | Type | Default | Stability | Computable | Constraint | Description |
|-----------|------|---------|-----------|------------|------------|-------------|
| PickRandomAngle | Boolean | false | Stable | No | -- | Whether to pick random angles within the observation range, or sweep left and right |
| ViewSegments | Integer | 1 | Stable | Yes | > 0 | Number of distinct segments to stop at when sweeping from left to right |

### Constraints

- MinRelativeSpeed must be less or equal than MaxRelativeSpeed.
- MinWalkDistance must be less or equal than MaxWalkDistance.
- MinNodeDelay must be less or equal than MaxNodeDelay.
- Must be attached to a sensor that provides a path.

---

## Seek

**Stability:** Experimental

Move towards a target using pathfinding or steering. The primary "chase" motion -- the NPC will navigate towards its sensor's locked target.

### Speed Attributes

| Attribute | Type | Default | Stability | Computable | Constraint | Description |
|-----------|------|---------|-----------|------------|------------|-------------|
| RelativeSpeed | Double | 1.0 | Stable | Yes | > 0, <= 2 | Maximum relative speed the NPC should move |
| RelativeSpeedWaypoint | Double | 0.5 | Stable | Yes | > 0, <= 1 | Maximum relative speed the NPC should move close to waypoints |
| WaypointRadius | Double | 0.5 | Stable | Yes | > 0.1 | Radius to slow down around waypoints |
| BlendHeading | Double | 0.5 | Stable | Yes | >= 0, <= 1 | Relative rotation angle into next waypoint when arriving at current waypoint |

### Pathfinding Attributes

| Attribute | Type | Default | Stability | Computable | Constraint | Description |
|-----------|------|---------|-----------|------------|------------|-------------|
| UsePathfinder | Boolean | true | Stable | Yes | -- | Use path finder |
| UseBestPath | Boolean | true | Stable | Yes | -- | Use best partial path if goal can't be reached |
| BuildOptimisedPath | Boolean | true | Stable | Yes | -- | Try to reduce number of nodes of generated path |
| DiagonalMoves | Boolean | true | Stable | Yes | -- | Allow diagonal moves |
| PathSmoothing | Integer | 2 | Stable | Yes | >= 0 | Try to smooth followed path. Larger values smooth more |
| RejectionWeight | Double | 3.0 | Stable | Yes | > 0 | Weight of rejection vector pushing entity closer to original path |
| MinPathLength | Double | 2.0 | Experimental | Yes | >= 0 | Minimum length of path required when not able to reach target (should be >= 2) |
| StepsPerTick | Integer | 50 | Stable | Yes | > 0 | Steps per iteration |
| MaxPathLength | Integer | 200 | Stable | Yes | > 0 | Max path steps before aborting path finding |
| MaxOpenNodes | Integer | 200 | Stable | Yes | > 0 | Max open nodes before aborting path finding |
| MaxTotalNodes | Integer | 900 | Stable | Yes | > 0 | Max total nodes before aborting path finding |
| ThrottleDelayRange | Array\<Double\> | [3.0, 5.0] | Stable | Yes | >= 0, ascending | Time to delay after no pathfinding solution found |
| ThrottleIgnoreCount | Integer | 3 | Stable | Yes | >= 0 | How often no valid path solution can be found before throttling delay is applied |
| AvoidBlockDamage | Boolean | true | Stable | Yes | -- | Should avoid environmental damage from blocks |
| RelaxedMoveConstraints | Boolean | true | Stable | Yes | -- | NPC can do movements like wading (depends on motion controller type) |

### Steering Attributes

| Attribute | Type | Default | Stability | Computable | Constraint | Description |
|-----------|------|---------|-----------|------------|------------|-------------|
| UseSteering | Boolean | true | Stable | Yes | -- | Use simple/cheap steering if available |
| SkipSteering | Boolean | true | Experimental | Yes | -- | Skip steering if target not reachable |
| SwitchToSteeringDistance | Double | 20.0 | Stable | Yes | > 0 | Distance below which NPC can test if target is reachable and abort existing path |

### Distance Thresholds

| Attribute | Type | Default | Stability | Computable | Constraint | Description |
|-----------|------|---------|-----------|------------|------------|-------------|
| SlowDownDistance | Double | 8.0 | Stable | Yes | > 0 | Distance when to slow down when approaching |
| StopDistance | Double | 10.0 | Stable | Yes | > 0 | Distance to stop at |
| AbortDistance | Double | 96.0 | Stable | Yes | > 0 | Distance to abort behaviour |
| Falloff | Double | 3.0 | Stable | Yes | > 0 | Deceleration when approaching target |
| AdjustRangeByHitboxSize | Boolean | false | Stable | Yes | -- | Correct range by hitbox sizes of involved entities |
| Reachable | Boolean | false | Experimental | Yes | -- | Target must be reachable so that hitboxes can overlap |
| HeightDifference | Array\<Double\> | [-1.0, 1.0] | Experimental | Yes | strictly ascending | Height difference allowed to target |

### Recomputation Attributes

| Attribute | Type | Default | Stability | Computable | Constraint | Description |
|-----------|------|---------|-----------|------------|------------|-------------|
| WaitDistance | Double | 1.0 | Experimental | Yes | > 0 | Minimum distance target needs to move before recomputing path when no path can be found |
| RecomputeDistance | Double | 10.0 | Experimental | Yes | >= 0 | Maximum distance target can move before path is recomputed (0 to suppress recomputation) |
| ReprojectDistance | Double | 0.5 | Experimental | Yes | >= 0 | Maximum distance target can move before position is reprojected |
| RecomputeConeAngle | Double | 0.0 | Experimental | Yes | >= 0, <= 360 | Recompute path when target leaves cone from initial position to target |

### Other Attributes

| Attribute | Type | Default | Stability | Computable | Constraint | Description |
|-----------|------|---------|-----------|------------|------------|-------------|
| DesiredAltitudeWeight | Double | -1.0 | Stable | Yes | >= -1, <= 1 | How much the NPC prefers being within the desired height range (0 = don't care, 1 = strongly prefer, <0 = use motion controller default) |
| Debug | String | *(empty)* | Stable | No | -- | Debugging flags |

### Constraints

- Must be attached to a sensor that provides one of: player target, NPC target, dropped item target, vector position.
- SlowDownDistance must be greater or equal than StopDistance.

---

## Sequence

**Stability:** Stable

Sequence of body motions. Executes motions in order, one after another. Can be used in conjunction with `Timer` to model more complex motions.

### Attributes

| Attribute | Type | Default | Stability | Computable | Constraint | Description |
|-----------|------|---------|-----------|------------|------------|-------------|
| Looped | Boolean | true | Stable | No | -- | When true, restart after last motion is finished |
| RestartOnActivate | Boolean | false | Experimental | No | -- | Restart from first motion when NPC is activated |
| Motions | Array\<BodyMotion\> | *(required)* | Stable | No | Must not be empty | Array of motions to execute in order |

### Constraints

*(None)*

---

## TakeOff

**Stability:** Experimental

Switch NPC from walking to flying motion controller. Triggers a vertical jump to initiate flight.

### Attributes

| Attribute | Type | Default | Stability | Computable | Constraint | Description |
|-----------|------|---------|-----------|------------|------------|-------------|
| JumpSpeed | Double | 0.0 | Experimental | No | >= 0 | Speed to jump off |

### Constraints

*(None)*

---

## Teleport

**Stability:** Experimental

Teleport NPC to a position given by a sensor, or to a random position nearby with an optional minimum offset up to a maximum offset.

### Attributes

| Attribute | Type | Default | Stability | Computable | Constraint | Description |
|-----------|------|---------|-----------|------------|------------|-------------|
| OffsetRange | Array\<Double\> | [0.0, 0.0] | Experimental | No | >= 0, ascending | The minimum and maximum offset the NPC can be spawned from the target position |
| MaxYOffset | Double | 5.0 | Experimental | No | > 0 | Maximum vertical offset from the target position in case of terrain obstacles |
| OffsetSector | Double | 0.0 | Experimental | No | >= 0, <= 360 | The sector around the target in which to teleport to. Origin point is directly between the target and the NPC |
| Orientation | Flag | Unchanged | Experimental | No | -- | The direction to face after teleporting |

**Orientation flag values:**

| Value | Description |
|-------|-------------|
| Unchanged | Do not change orientation |
| TowardsTarget | Face towards the target |
| UseTarget | Use the target's orientation |

### Constraints

- Must be attached to a sensor that provides one of: player target, NPC target, dropped item target, vector position.
- If Orientation is `UseTarget`, must be attached to a sensor that provides one of: player target, NPC target, dropped item target.

---

## TestProbe

**Stability:** Experimental

Debugging tool for test probing. Used for internal development and testing of NPC movement systems.

### Attributes

| Attribute | Type | Default | Stability | Computable | Constraint | Description |
|-----------|------|---------|-----------|------------|------------|-------------|
| AdjustX | Double | -1.0 | Experimental | No | -- | X block position adjustment |
| AdjustZ | Double | -1.0 | Experimental | No | -- | Z block position adjustment |
| AdjustDistance | Double | -1.0 | Experimental | No | -- | Set probe direction length for debugging |
| SnapAngle | Double | -1.0 | Experimental | No | -- | Snap angle to multiples of value for debugging |
| AvoidBlockDamage | Boolean | true | Stable | No | -- | Should avoid environmental damage from blocks |
| RelaxedMoveConstraints | Boolean | false | Stable | No | -- | NPC can do movements like wading (depends on motion controller type) |

### Constraints

*(None)*

---

## Timer

**Stability:** Stable

Execute a body motion for a specific maximum time. If the wrapped motion finishes earlier, the Timer also finishes. Useful inside `Sequence` to limit how long a motion runs.

### Attributes

| Attribute | Type | Default | Stability | Computable | Constraint | Description |
|-----------|------|---------|-----------|------------|------------|-------------|
| Time | Array\<Double\> | [1.0, 1.0] | Stable | Yes | >= 0, ascending | Range of time (seconds) from which the random timer length is chosen |
| Motion | BodyMotion (ObjectRef) | *(required)* | Stable | No | -- | Motion to execute |

### Constraints

*(None)*

---

## Wander

**Stability:** Stable

Random movement in short linear pieces. The NPC picks a direction, walks for a time, then picks a new direction. No boundary -- the NPC can wander indefinitely.

### Attributes

| Attribute | Type | Default | Stability | Computable | Constraint | Description |
|-----------|------|---------|-----------|------------|------------|-------------|
| MinWalkTime | Double | 2.0 | Stable | Yes | >= 0 | Minimum time to wander for a segment |
| MaxWalkTime | Double | 4.0 | Stable | Yes | > 0 | Maximum time to wander for a segment |
| MinHeadingChange | Double | 0.0 | Stable | Yes | >= 0, <= 180 | Approximate minimum heading change between segments |
| MaxHeadingChange | Double | 90.0 | Stable | Yes | >= 0, <= 180 | Approximate maximum heading change between segments |
| RelaxHeadingChange | Boolean | true | Stable | Yes | -- | Allow other directions when preferred directions are blocked |
| RelativeSpeed | Double | 0.5 | Stable | Yes | > 0, <= 2 | Relative wander speed |
| MinMoveDistance | Double | 0.5 | Stable | Yes | > 0 | Minimum distance to move in a segment |
| StopDistance | Double | 0.5 | Stable | Yes | > 0 | Distance to stop at target |
| TestsPerTick | Integer | 1 | Stable | Yes | > 0 | Direction tests per tick |
| AvoidBlockDamage | Boolean | true | Stable | Yes | -- | Should avoid environmental damage from blocks |
| RelaxedMoveConstraints | Boolean | false | Stable | Yes | -- | NPC can do movements like wading (depends on motion controller type) |
| DesiredAltitudeWeight | Double | -1.0 | Stable | Yes | >= -1, <= 1 | How much the NPC prefers being within the desired height range (0 = don't care, 1 = strongly prefer, <0 = use motion controller default) |

### Constraints

- MinWalkTime must be less or equal than MaxWalkTime.
- MinHeadingChange must be less or equal than MaxHeadingChange.

---

## WanderInCircle

**Stability:** Stable

Random movement in short linear pieces inside a circle (or sphere) around the NPC's spawn position. Same wandering behavior as `Wander`, but confined to a circular area.

### Wander Attributes

| Attribute | Type | Default | Stability | Computable | Constraint | Description |
|-----------|------|---------|-----------|------------|------------|-------------|
| MinWalkTime | Double | 2.0 | Stable | Yes | >= 0 | Minimum time to wander for a segment |
| MaxWalkTime | Double | 4.0 | Stable | Yes | > 0 | Maximum time to wander for a segment |
| MinHeadingChange | Double | 0.0 | Stable | Yes | >= 0, <= 180 | Approximate minimum heading change between segments |
| MaxHeadingChange | Double | 90.0 | Stable | Yes | >= 0, <= 180 | Approximate maximum heading change between segments |
| RelaxHeadingChange | Boolean | true | Stable | Yes | -- | Allow other directions when preferred directions are blocked |
| RelativeSpeed | Double | 0.5 | Stable | Yes | > 0, <= 2 | Relative wander speed |
| MinMoveDistance | Double | 0.5 | Stable | Yes | > 0 | Minimum distance to move in a segment |
| StopDistance | Double | 0.5 | Stable | Yes | > 0 | Distance to stop at target |
| TestsPerTick | Integer | 1 | Stable | Yes | > 0 | Direction tests per tick |
| AvoidBlockDamage | Boolean | true | Stable | Yes | -- | Should avoid environmental damage from blocks |
| RelaxedMoveConstraints | Boolean | false | Stable | Yes | -- | NPC can do movements like wading (depends on motion controller type) |
| DesiredAltitudeWeight | Double | -1.0 | Stable | Yes | >= -1, <= 1 | How much the NPC prefers being within the desired height range (0 = don't care, 1 = strongly prefer, <0 = use motion controller default) |

### Circle-Specific Attributes

| Attribute | Type | Default | Stability | Computable | Constraint | Description |
|-----------|------|---------|-----------|------------|------------|-------------|
| Radius | Double | 10.0 | Stable | Yes | > 0 | Radius of circle to wander in |
| UseSphere | Boolean | false | Stable | No | -- | Use a sphere instead of circle cylinder |
| Flock | Boolean | false | Experimental | No | -- | Do not use (internal/WIP) |

### Constraints

- MinWalkTime must be less or equal than MaxWalkTime.
- MinHeadingChange must be less or equal than MaxHeadingChange.

---

## WanderInRect

**Stability:** Stable

Random movement in short linear pieces inside a rectangle around the NPC's spawn position. Same wandering behavior as `Wander`, but confined to a rectangular area.

### Wander Attributes

| Attribute | Type | Default | Stability | Computable | Constraint | Description |
|-----------|------|---------|-----------|------------|------------|-------------|
| MinWalkTime | Double | 2.0 | Stable | Yes | >= 0 | Minimum time to wander for a segment |
| MaxWalkTime | Double | 4.0 | Stable | Yes | > 0 | Maximum time to wander for a segment |
| MinHeadingChange | Double | 0.0 | Stable | Yes | >= 0, <= 180 | Approximate minimum heading change between segments |
| MaxHeadingChange | Double | 90.0 | Stable | Yes | >= 0, <= 180 | Approximate maximum heading change between segments |
| RelaxHeadingChange | Boolean | true | Stable | Yes | -- | Allow other directions when preferred directions are blocked |
| RelativeSpeed | Double | 0.5 | Stable | Yes | > 0, <= 2 | Relative wander speed |
| MinMoveDistance | Double | 0.5 | Stable | Yes | > 0 | Minimum distance to move in a segment |
| StopDistance | Double | 0.5 | Stable | Yes | > 0 | Distance to stop at target |
| TestsPerTick | Integer | 1 | Stable | Yes | > 0 | Direction tests per tick |
| AvoidBlockDamage | Boolean | true | Stable | Yes | -- | Should avoid environmental damage from blocks |
| RelaxedMoveConstraints | Boolean | false | Stable | Yes | -- | NPC can do movements like wading (depends on motion controller type) |
| DesiredAltitudeWeight | Double | -1.0 | Stable | Yes | >= -1, <= 1 | How much the NPC prefers being within the desired height range (0 = don't care, 1 = strongly prefer, <0 = use motion controller default) |

### Rectangle-Specific Attributes

| Attribute | Type | Default | Stability | Computable | Constraint | Description |
|-----------|------|---------|-----------|------------|------------|-------------|
| Width | Double | 10.0 | Stable | No | > 0 | Rectangle width |
| Depth | Double | 10.0 | Stable | No | > 0 | Rectangle depth |

### Constraints

- MinWalkTime must be less or equal than MaxWalkTime.
- MinHeadingChange must be less or equal than MaxHeadingChange.
