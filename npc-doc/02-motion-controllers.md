# NPC Motion Controllers

This document covers the three Motion Controller types available for NPC movement: **Dive**, **Fly**, and **Walk**. Each controller defines how an NPC moves through different environments (underwater, airborne, or ground-based).

---

## Dive: MotionController (WorkInProgress)

Provides diving abilities for NPCs, controlling underwater movement, surface interaction, and depth preferences.

### Common Attributes

| Attribute | Type | Default | Stability | Computable | Constraint | Description |
|-----------|------|---------|-----------|------------|------------|-------------|
| Type | String | *Required* | Stable | No | Must be non-empty | Type field |
| EpsilonSpeed | Double | 1.0E-5 | Experimental | No | > 0 | Minimum speed considered non-zero |
| EpsilonAngle | Double | 3.0 | Experimental | No | > 0 | Minimum angle difference considered non-zero (degrees) |
| MaxHeadRotationSpeed | Double | 360.0 | Stable | Yes | >= 0, <= 360 | Maximum rotation speed of the head (degrees) |
| ForceVelocityDamping | Double | 0.5 | Experimental | No | > 0 | Damping of external force/velocity over time |
| RunThreshold | Double | 0.7 | WorkInProgress | Yes | >= 0, <= 1 | Relative threshold when running animation should be used |
| RunThresholdRange | Double | 0.15 | WorkInProgress | No | >= 0, <= 1 | Relative threshold range for switching between running/walking |

### Speed Attributes

| Attribute | Type | Default | Stability | Computable | Constraint | Description |
|-----------|------|---------|-----------|------------|------------|-------------|
| MaxSwimSpeed | Double | 3.0 | Stable | Yes | > 0 | Maximum horizontal speed |
| MaxDiveSpeed | Double | 8.0 | Stable | No | > 0 | Maximum vertical speed |
| MaxFallSpeed | Double | 10.0 | Stable | No | > 0 | Terminal velocity falling in air |
| MaxSinkSpeed | Double | 4.0 | Stable | No | > 0 | Terminal velocity sinking in water |
| Acceleration | Double | 3.0 | Stable | No | > 0 | Acceleration |

### Physics & Rotation

| Attribute | Type | Default | Stability | Computable | Constraint | Description |
|-----------|------|---------|-----------|------------|------------|-------------|
| Gravity | Double | 10.0 | Stable | No | > 0 | Gravity |
| MaxRotationSpeed | Double | 360.0 | Stable | No | > 0 | Maximum rotational speed (degrees) |
| MaxSwimTurnAngle | Double | 90.0 | WorkInProgress | No | >= 0, <= 180 | Maximum angle NPC can swim without explicit turning (degrees) |

### Animation

| Attribute | Type | Default | Stability | Computable | Constraint | Description |
|-----------|------|---------|-----------|------------|------------|-------------|
| FastSwimThreshold | Double | 0.6 | WorkInProgress | No | >= 0, <= 1 | Relative threshold when fast swimming animation should be used |

### Depth Control

| Attribute | Type | Default | Stability | Computable | Constraint | Description |
|-----------|------|---------|-----------|------------|------------|-------------|
| SwimDepth | Double | 0.4 | WorkInProgress | No | >= -1, <= 1 | Swim depth relative to bounding box. 0 = eye height, -1 = bottom, +1 = top. Values scale linearly. |
| SinkRatio | Double | 1.0 | WorkInProgress | No | >= 0 | Relative sink or climb speed while wandering |
| MinDiveDepth | Double | 0.0 | Unknown | No | >= 0 | *(No description provided)* |
| MaxDiveDepth | Double | 1.7976931348623157E308 | WorkInProgress | No | > 0 | Maximum dive depth below surface desired |
| MinDepthAboveGround | Double | 1.0 | WorkInProgress | No | >= 0 | Minimum distance from ground desired |
| MinDepthBelowSurface | Double | 1.0 | WorkInProgress | No | >= 0 | Minimum distance from water surface desired |
| MinWaterDepth | Double | 1.0 | Unknown | No | >= 0 | *(No description provided)* |
| MaxWaterDepth | Double | 0.0 | Unknown | No | >= 0 | *(No description provided)* |
| DesiredDepthWeight | Double | 0.0 | Stable | No | >= 0, <= 1 | How much the NPC prefers being within the desired height range. 0 = doesn't care, 1 = will do its best to get there fast. |

### Constraints

- `MinSwimSpeed` must be less than or equal to `MaxSwimSpeed`

---

## Fly: MotionController (WorkInProgress)

Flight motion controller for airborne NPCs, controlling flight dynamics, roll, altitude preferences, and fluid interactions.

### Common Attributes

| Attribute | Type | Default | Stability | Computable | Constraint | Description |
|-----------|------|---------|-----------|------------|------------|-------------|
| Type | String | *Required* | Stable | No | Must be non-empty | Type field |
| EpsilonSpeed | Double | 1.0E-5 | Experimental | No | > 0 | Minimum speed considered non-zero |
| EpsilonAngle | Double | 3.0 | Experimental | No | > 0 | Minimum angle difference considered non-zero (degrees) |
| MaxHeadRotationSpeed | Double | 360.0 | Stable | Yes | >= 0, <= 360 | Maximum rotation speed of the head (degrees) |
| ForceVelocityDamping | Double | 0.5 | Experimental | No | > 0 | Damping of external force/velocity over time |
| RunThreshold | Double | 0.7 | WorkInProgress | Yes | >= 0, <= 1 | Relative threshold when running animation should be used |
| RunThresholdRange | Double | 0.15 | WorkInProgress | No | >= 0, <= 1 | Relative threshold range for switching between running/walking |

### Speed Attributes

| Attribute | Type | Default | Stability | Computable | Constraint | Description |
|-----------|------|---------|-----------|------------|------------|-------------|
| MinAirSpeed | Double | 0.1 | WorkInProgress | No | >= 0 | Minimum in-air speed |
| MaxHorizontalSpeed | Double | 8.0 | Stable | Yes | > 0 | Maximum horizontal speed |
| MaxClimbSpeed | Double | 6.0 | Stable | No | > 0 | Maximum climbing speed |
| MaxSinkSpeed | Double | 10.0 | Stable | No | > 0 | Maximum sink/drop speed |
| MaxFallSpeed | Double | 40.0 | Stable | No | > 0 | Maximum fall speed |
| MaxSinkSpeedFluid | Double | 4.0 | Stable | No | >= 0 | Maximum sink/fall speed in fluids |
| Acceleration | Double | 4.0 | Stable | No | > 0 | Maximum acceleration |
| Deceleration | Double | 4.0 | Stable | No | > 0 | Maximum deceleration |

### Physics & Gravity

| Attribute | Type | Default | Stability | Computable | Constraint | Description |
|-----------|------|---------|-----------|------------|------------|-------------|
| Gravity | Double | 40.0 | Stable | No | > 0 | Gravity |
| MaxClimbAngle | Double | 45.0 | Stable | No | > 0 | Maximum climb angle (degrees) |
| MaxSinkAngle | Double | 85.0 | Stable | No | > 0 | Maximum sink angle (degrees) |

### Rotation & Roll

| Attribute | Type | Default | Stability | Computable | Constraint | Description |
|-----------|------|---------|-----------|------------|------------|-------------|
| MaxTurnSpeed | Double | 180.0 | Stable | No | > 0 | Maximum turn speed (degrees per second) |
| MaxRollAngle | Double | 45.0 | Stable | No | > 0 | Maximum roll angle (degrees) |
| MaxRollSpeed | Double | 180.0 | Stable | No | > 0 | Maximum roll speed (degrees per second) |
| RollDamping | Double | 0.9 | Stable | No | >= 0, <= 1 | Roll damping factor |
| AutoLevel | Boolean | true | Stable | No | -- | Set pitch to 0 when no steering forces are applied |

### Altitude Control

| Attribute | Type | Default | Stability | Computable | Constraint | Description |
|-----------|------|---------|-----------|------------|------------|-------------|
| MinHeightOverGround | Double | 1.0 | Stable | Yes | >= 0 | Minimum height over ground |
| MaxHeightOverGround | Double | 20.0 | Stable | Yes | > 0 | Maximum height over ground |
| DesiredAltitudeWeight | Double | 0.0 | Stable | No | >= 0, <= 1 | How much the NPC prefers being within the desired height range. 0 = doesn't care, 1 = will do its best to get there fast. |

### Animation

| Attribute | Type | Default | Stability | Computable | Constraint | Description |
|-----------|------|---------|-----------|------------|------------|-------------|
| FastFlyThreshold | Double | 0.6 | WorkInProgress | No | >= 0, <= 1 | Relative threshold when fast flying animation should be used |

---

## Walk: MotionController (WorkInProgress)

Provides ground-based walking abilities for NPCs, including climbing, jumping, descent, hovering, and combat slowdown mechanics.

### Common Attributes

| Attribute | Type | Default | Stability | Computable | Constraint | Description |
|-----------|------|---------|-----------|------------|------------|-------------|
| Type | String | *Required* | Stable | No | Must be non-empty | Type field |
| EpsilonSpeed | Double | 1.0E-5 | Experimental | No | > 0 | Minimum speed considered non-zero |
| EpsilonAngle | Double | 3.0 | Experimental | No | > 0 | Minimum angle difference considered non-zero (degrees) |
| MaxHeadRotationSpeed | Double | 360.0 | Stable | Yes | >= 0, <= 360 | Maximum rotation speed of the head (degrees) |
| ForceVelocityDamping | Double | 0.5 | Experimental | No | > 0 | Damping of external force/velocity over time |
| RunThreshold | Double | 0.7 | WorkInProgress | Yes | >= 0, <= 1 | Relative threshold when running animation should be used |
| RunThresholdRange | Double | 0.15 | WorkInProgress | No | >= 0, <= 1 | Relative threshold range for switching between running/walking |

### Speed Attributes

| Attribute | Type | Default | Stability | Computable | Constraint | Description |
|-----------|------|---------|-----------|------------|------------|-------------|
| MaxWalkSpeed | Double | 3.0 | Stable | Yes | > 0 | Maximum horizontal speed |
| MinWalkSpeed | Double | 0.1 | WorkInProgress | No | >= 0 | Minimum horizontal speed |
| MaxFallSpeed | Double | 8.0 | Stable | No | > 0 | Maximum fall speed |
| MaxSinkSpeedFluid | Double | 4.0 | Stable | No | > 0 | Maximum sink speed in fluids |
| Acceleration | Double | 3.0 | Stable | Yes | > 0 | Acceleration |

### Physics & Gravity

| Attribute | Type | Default | Stability | Computable | Constraint | Description |
|-----------|------|---------|-----------|------------|------------|-------------|
| Gravity | Double | 10.0 | Stable | No | > 0 | Gravity |

### Rotation & Heading

| Attribute | Type | Default | Stability | Computable | Constraint | Description |
|-----------|------|---------|-----------|------------|------------|-------------|
| MaxRotationSpeed | Double | 360.0 | Stable | Yes | > 0 | Maximum rotational speed (degrees) |
| MaxWalkTurnAngle | Double | 90.0 | WorkInProgress | Yes | >= 0, <= 180 | Maximum angle NPC can walk without explicit turning (degrees) |
| BlendRestTurnAngle | Double | 60.0 | WorkInProgress | Yes | >= 0, <= 180 | When blending heading and turn angle required is larger than this, speed is reduced |
| BlendRestRelativeSpeed | Double | 0.2 | WorkInProgress | Yes | >= 0, <= 1 | Relative speed used when reducing speed during heading blend |

### Climbing

| Attribute | Type | Default | Stability | Computable | Constraint | Description |
|-----------|------|---------|-----------|------------|------------|-------------|
| MaxClimbHeight | Double | 1.3 | Stable | Yes | > 0 | Maximum height NPC can climb |
| ClimbSpeedConst | Double | 5.0 | WorkInProgress | Yes | > 0 | Climb speed constant in formula: `const + multiplier * walkspeed ^ power` |
| ClimbSpeedMult | Double | 0.0 | WorkInProgress | Yes | -- | Climb speed multiplier in formula: `const + multiplier * walkspeed ^ power` |
| ClimbSpeedPow | Double | 1.0 | WorkInProgress | Yes | -- | Climb speed power in formula: `const + multiplier * walkspeed ^ power` |
| AscentAnimationType | Flag | Walk | Stable | Yes | Walk, Fly, Idle, Climb, Jump | Animation to play when walking up a block |
| FenceBlockSet | Asset | Fence | Stable | No | -- | Unclimbable blocks |

### Jumping

| Attribute | Type | Default | Stability | Computable | Constraint | Description |
|-----------|------|---------|-----------|------------|------------|-------------|
| JumpHeight | Double | 0.5 | Experimental | Yes | >= 0 | How high the NPC jumps above climb height |
| MinJumpHeight | Double | 0.6 | Experimental | Yes | >= 0 | Minimum height above which a jump will be attempted |
| MinJumpDistance | Double | 0.2 | Experimental | Yes | > 0 | Minimum distance above which a jump will be executed |
| JumpForce | Double | 1.5 | Experimental | Yes | > 0 | Force multiplier for the upward motion of the jump |
| JumpBlending | Double | 1.0 | Experimental | Yes | >= 0, <= 1 | Blending of the upward jump pattern. 0 = more curved, 1 = linear |
| JumpDescentBlending | Double | 1.0 | Experimental | Yes | >= 0 | Blending of the jump descent pattern. 0 = linear, higher = more curved |
| JumpDescentSteepness | Double | 1.0 | Experimental | Yes | > 0 | Steepness of the descent portion of the jump |
| JumpRange | Array(Double) | [0.0, 0.0] | WorkInProgress | Yes | Elements >= 0, <= 10, weakly ascending | Jump distance range |

### Descent

| Attribute | Type | Default | Stability | Computable | Constraint | Description |
|-----------|------|---------|-----------|------------|------------|-------------|
| MaxDropHeight | Double | 3.0 | WorkInProgress | Yes | > 0 | Maximum height NPC considers a safe drop |
| MinDescentAnimationHeight | Double | 1.0 | Stable | Yes | >= 0 | Minimum drop distance to switch from walking animation to descent animation |
| DescendFlatness | Double | 0.7 | WorkInProgress | Yes | >= 0, <= 1 | Relative scale of how fast NPC moves forward while climbing down |
| DescendSpeedCompensation | Double | 0.9 | WorkInProgress | Yes | >= 0, <= 1 | Factor to compensate forward speed reduction while moving downwards |
| DescentAnimationType | Flag | Fall | Experimental | Yes | Walk, Idle, Fall | Animation to play when moving down a block |
| DescentSteepness | Double | 1.4 | Experimental | Yes | > 0 | Relative steepness of the descent |
| DescentBlending | Double | 1.8 | Experimental | Yes | >= 0 | Blending of the descent pattern. 0 = linear, higher = more curved |

### Hover

| Attribute | Type | Default | Stability | Computable | Constraint | Description |
|-----------|------|---------|-----------|------------|------------|-------------|
| MinHover | Double | 0.0 | WorkInProgress | No | >= 0 | Minimum hover height over ground |
| MinHoverClimb | Double | 0.0 | WorkInProgress | No | >= 0 | Minimum hover height over ground when climbing |
| MinHoverDrop | Double | 0.0 | WorkInProgress | No | >= 0 | Minimum hover height over ground when dropping |
| MaxHover | Double | 0.0 | WorkInProgress | No | >= 0 | Maximum hover height over ground |
| HoverFreq | Double | 0.0 | WorkInProgress | No | >= 0 | Hover frequency |
| FloatsDown | Boolean | true | WorkInProgress | No | -- | If true, NPC floats down when hovering is enabled; otherwise gravity decides |

### Combat

| Attribute | Type | Default | Stability | Computable | Constraint | Description |
|-----------|------|---------|-----------|------------|------------|-------------|
| MinHitSlowdown | Double | 0.1 | Stable | No | >= 0, <= 1 | Minimum percentage to slow down by when attacked from behind |

### Constraints

- `MinHover` must be less than or equal to `MaxHover`
- `MinHoverClimb` must be less than or equal to `MinHover`
- `MinHoverDrop` must be less than or equal to `MinHover`
- `MinWalkSpeed` must be less than or equal to `MaxWalkSpeed`
