# Instructions and Miscellaneous Types

All `Instruction` types and miscellaneous part types from the NPC documentation.

---

## Instruction Types

### Instruction (WorkInProgress)

An instruction with Sensor, and Motions and Actions, or a list of nested instructions.

| Attribute | Type | Required | Default | Stability | Constraint |
|-----------|------|----------|---------|-----------|------------|
| Name | String | Optional | `null` | Stable | -- |
| Tag | String | Optional | `null` | Experimental | String value must be either null or not empty |
| Enabled | Boolean, Computable | Optional | `true` | Stable | -- |
| Sensor | ObjectRef (Object: Sensor) | Optional | `null` | Stable | -- |
| BodyMotion | ObjectRef (Object: BodyMotion) | Optional | `null` | Stable | -- |
| HeadMotion | ObjectRef (Object: HeadMotion) | Optional | `null` | Stable | -- |
| Actions | ObjectRef (Object: ActionList) | Optional | `null` | Stable | -- |
| ActionsBlocking | Boolean | Optional | `false` | Stable | -- |
| ActionsAtomic | Boolean | Optional | `false` | Stable | -- |
| Instructions | Array (Element: Instruction) | Optional | `null` | Stable | -- |
| Continue | Boolean | Optional | `false` | WorkInProgress | -- |
| Weight | Double, Computable | Optional | `1.0` | Stable | Value must be > 0 |
| TreeMode | Boolean | Optional | `false` | Stable | -- |
| InvertTreeModeResult | Boolean, Computable | Optional | `false` | Stable | -- |

**Constraints:**
- At most one or none of `BodyMotion`, `Instructions` must be provided
- At most one or none of `HeadMotion`, `Instructions` must be provided
- At most one or none of `Actions`, `Instructions` must be provided
- If `TreeMode` is true, `Continue` must be false

---

### Random Instruction (Stable)

Randomised list of weighted instructions. One will be selected at random and executed until the NPC state changes.

| Attribute | Type | Required | Default | Stability | Constraint |
|-----------|------|----------|---------|-----------|------------|
| Name | String | Optional | `null` | Stable | -- |
| Tag | String | Optional | `null` | Experimental | String value must be either null or not empty |
| Enabled | Boolean, Computable | Optional | `true` | Stable | -- |
| Sensor | ObjectRef (Object: Sensor) | Optional | `null` | Stable | -- |
| Instructions | Array (Element: Instruction) | Optional | `null` | Stable | -- |
| Continue | Boolean | Optional | `false` | WorkInProgress | -- |
| Weight | Double, Computable | Optional | `1.0` | Stable | Value must be > 0 |
| TreeMode | Boolean | Optional | `false` | Stable | -- |
| InvertTreeModeResult | Boolean, Computable | Optional | `false` | Stable | -- |
| ResetOnStateChange | Boolean, Computable | Optional | `true` | Stable | -- |
| ExecuteFor | Array (Element: Double), Computable | Optional | `[1.7976931348623157E308, 1.7976931348623157E308]` | Stable | Values must be > 0, <= 1.7976931348623157e+308, in weakly ascending order |

**Constraints:**
- If `TreeMode` is true, `Continue` must be false

---

### Reference Instruction (Stable)

Prioritized instruction list that can be referenced from elsewhere in the file. Otherwise works like the default.

| Attribute | Type | Required | Default | Stability | Constraint |
|-----------|------|----------|---------|-----------|------------|
| Parameters | Parameters | Optional | *(empty)* | Stable | -- |
| Name | String | **Required** | -- | Stable | -- |
| Tag | String | Optional | `null` | Experimental | String value must be either null or not empty |
| Enabled | Boolean, Computable | Optional | `true` | Stable | -- |
| Sensor | ObjectRef (Object: Sensor) | Optional | `null` | Stable | -- |
| BodyMotion | ObjectRef (Object: BodyMotion) | Optional | `null` | Stable | -- |
| HeadMotion | ObjectRef (Object: HeadMotion) | Optional | `null` | Stable | -- |
| Actions | ObjectRef (Object: ActionList) | Optional | `null` | Stable | -- |
| ActionsBlocking | Boolean | Optional | `false` | Stable | -- |
| ActionsAtomic | Boolean | Optional | `false` | Stable | -- |
| Instructions | Array (Element: Instruction) | Optional | `null` | Stable | -- |
| Continue | Boolean | Optional | `false` | WorkInProgress | -- |
| Weight | Double, Computable | Optional | `1.0` | Stable | Value must be > 0 |
| TreeMode | Boolean | Optional | `false` | Stable | -- |
| InvertTreeModeResult | Boolean, Computable | Optional | `false` | Stable | -- |

**Constraints:**
- At most one or none of `BodyMotion`, `Instructions` must be provided
- At most one or none of `HeadMotion`, `Instructions` must be provided
- At most one or none of `Actions`, `Instructions` must be provided
- If `TreeMode` is true, `Continue` must be false

---

## Miscellaneous Types

### ActionList (Stable)

An array of actions to be executed.

| Attribute | Type | Required | Default | Stability | Constraint |
|-----------|------|----------|---------|-----------|------------|
| *(list of actions)* | Array (Element: Action) | **Required** | -- | Stable | -- |

---

### HashMap (Stable)

Non-empty list of motion controllers.

| Attribute | Type | Required | Default | Stability | Constraint |
|-----------|------|----------|---------|-----------|------------|
| *(list of motion controllers)* | Array (Element: MotionController) | **Required** | -- | Stable | Array must not be empty |

---

### Path (Stable)

List of transient path points.

| Attribute | Type | Required | Default | Stability | Constraint |
|-----------|------|----------|---------|-----------|------------|
| Waypoints | Array (Element: RelativeWaypointDefinition) | **Required** | -- | Stable | -- |
| Scale | Double, Computable | Optional | `1.0` | Stable | Value must be > 0 |

---

### RelativeWaypointDefinition (Stable)

A simple path waypoint definition where each waypoint is relative to the previous.

| Attribute | Type | Required | Default | Stability | Constraint |
|-----------|------|----------|---------|-----------|------------|
| Rotation | Double | Optional | `0.0` | Stable | Value must be > -360 and < 360 |
| Distance | Double | **Required** | -- | Stable | Value must be > 0 |

---

### StateTransition (Stable)

An entry containing a list of actions to execute when moving from one state to another.

| Attribute | Type | Required | Default | Stability | Constraint |
|-----------|------|----------|---------|-----------|------------|
| States | Array (Element: StateTransitionEdges) | **Required** | -- | Stable | -- |
| Actions | ObjectRef (Object: ActionList) | **Required** | -- | Stable | -- |
| Enabled | Boolean, Computable | Optional | `true` | Stable | -- |

---

### StateTransitionController (Stable)

A list of state transitions.

| Attribute | Type | Required | Default | Stability | Constraint |
|-----------|------|----------|---------|-----------|------------|
| *(list of state transition entries)* | Array (Element: StateTransition) | **Required** | -- | Stable | -- |

---

### StateTransitionEdges (Stable)

Sets of from and to states defining state transitions.

| Attribute | Type | Required | Default | Stability | Constraint |
|-----------|------|----------|---------|-----------|------------|
| Priority | Integer | Optional | `0` | Stable | Value must be >= 0 |
| From | StringList | **Required** | -- | Stable | Strings in array must not be empty |
| To | StringList | **Required** | -- | Stable | Strings in array must not be empty |
| Enabled | Boolean, Computable | Optional | `true` | Stable | -- |

---

### ValueToParameterMapping (Stable)

Maps a value from the value store to a named parameter override.

| Attribute | Type | Required | Default | Stability | Constraint |
|-----------|------|----------|---------|-----------|------------|
| ValueType | Flag | **Required** | -- | Stable | -- |
| FromValue | String | **Required** | -- | Stable | String must be not empty |
| ToParameter | String | **Required** | -- | Stable | String must be not empty |

**ValueType flag values:**

| Value | Description |
|-------|-------------|
| String | String value |
| Double | Double value |
| Int | Integer value |

---

### WeightedAction (Stable)

A wrapped and weighted action intended to be used for Random action lists.

| Attribute | Type | Required | Default | Stability | Constraint |
|-----------|------|----------|---------|-----------|------------|
| Action | ObjectRef (Object: Action) | **Required** | -- | Stable | -- |
| Weight | Double, Computable | **Required** | -- | Stable | Value must be > 0 |

---

## HeadMotion Types

### Aim (Stable)

Aim at target considering weapon in hand.

| Attribute | Type | Required | Default | Stability | Constraint |
|-----------|------|----------|---------|-----------|------------|
| Spread | Double, Computable | Optional | `1.0` | Experimental | Value must be >= 0 and <= 5 |
| HitProbability | Double, Computable | Optional | `0.33` | Experimental | Value must be >= 0 and <= 1 |
| Deflection | Boolean, Computable | Optional | `true` | Experimental | -- |
| RelativeTurnSpeed | Double, Computable | Optional | `1.0` | Stable | Value must be > 0 and <= 2 |

**Constraints:** Must be attached to a sensor that provides one of: player target, NPC target, dropped item target, vector position.

---

### Nothing (Stable)

Do nothing.

*No attributes.*

---

### Observe (Stable)

Observe surroundings in various ways. This includes looking in random directions within an angle, or sweeping the head left and right, etc. Angles are relative to body angle at any given time.

| Attribute | Type | Required | Default | Stability | Constraint |
|-----------|------|----------|---------|-----------|------------|
| AngleRange | Array (Element: Double), Computable | **Required** | -- | Stable | Values must be >= -180, <= 180, in weakly ascending order |
| PauseTimeRange | Array (Element: Double), Computable | Optional | `[2.0, 2.0]` | Stable | Values must be >= 0, <= 1.7976931348623157e+308, in weakly ascending order |
| PickRandomAngle | Boolean, Computable | Optional | `false` | Stable | -- |
| ViewSegments | Integer, Computable | Optional | `1.0` | Stable | Value must be > 0 |
| RelativeTurnSpeed | Double, Computable | Optional | `1.0` | Stable | Value must be > 0 and <= 2 |

---

### Sequence (Stable)

Sequence of motions. Can be used in conjunction with `Timer` to model more complex motions.

| Attribute | Type | Required | Default | Stability | Constraint |
|-----------|------|----------|---------|-----------|------------|
| Looped | Boolean | Optional | `true` | Stable | -- |
| RestartOnActivate | Boolean | Optional | `false` | Experimental | -- |
| Motions | Array (Element: HeadMotion) | **Required** | -- | Stable | Array must not be empty |

---

### Timer (Stable)

Execute a HeadMotion for a specific maximum time. If the motion finishes earlier the Timer also finishes.

| Attribute | Type | Required | Default | Stability | Constraint |
|-----------|------|----------|---------|-----------|------------|
| Time | Array (Element: Double), Computable | Optional | `[1.0, 1.0]` | Stable | Values must be >= 0, <= 1.7976931348623157e+308, in weakly ascending order |
| Motion | ObjectRef (Object: HeadMotion) | **Required** | -- | Stable | -- |

---

### Watch (Stable)

Rotate to target.

| Attribute | Type | Required | Default | Stability | Constraint |
|-----------|------|----------|---------|-----------|------------|
| RelativeTurnSpeed | Double, Computable | Optional | `1.0` | Stable | Value must be > 0 and <= 2 |

**Constraints:** Must be attached to a sensor that provides one of: player target, NPC target, dropped item target, vector position.
