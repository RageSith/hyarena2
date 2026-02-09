# Entity Filters, Prioritisers, and Collectors

All `IEntityFilter`, `ISensorEntityPrioritiser`, and `ISensorEntityCollector` types from the NPC documentation.

---

## IEntityFilter Types

### Altitude (Stable)

Matches targets if they're within the defined range above the ground.

| Attribute | Type | Required | Default | Stability | Constraint |
|-----------|------|----------|---------|-----------|------------|
| Enabled | Boolean, Computable | Optional | `true` | Stable | -- |
| AltitudeRange | Array (Element: Double), Computable | **Required** | -- | Stable | Values must be >= 0, <= 1.7976931348623157e+308, in weakly ascending order |

---

### And (Stable)

Logical AND of a list of filters.

| Attribute | Type | Required | Default | Stability | Constraint |
|-----------|------|----------|---------|-----------|------------|
| Enabled | Boolean, Computable | Optional | `true` | Stable | -- |
| Filters | Array (Element: IEntityFilter) | **Required** | -- | Stable | Array must not be empty |

---

### Attitude (Stable)

Matches the attitude towards the locked target.

| Attribute | Type | Required | Default | Stability | Constraint |
|-----------|------|----------|---------|-----------|------------|
| Enabled | Boolean, Computable | Optional | `true` | Stable | -- |
| Attitudes | FlagSet, Computable | **Required** | -- | Stable | -- |

**Attitudes flag values:**

| Value | Description |
|-------|-------------|
| HOSTILE | Is hostile towards the target |
| REVERED | Reveres the target |
| FRIENDLY | Is friendly towards the target |
| IGNORE | Is ignoring the target |
| NEUTRAL | Is neutral towards the target |

---

### Combat (Stable)

Check the target's combat state. This includes whether they're attacking at all, if they're using a particular attack, etc.

| Attribute | Type | Required | Default | Stability | Constraint |
|-----------|------|----------|---------|-----------|------------|
| Enabled | Boolean, Computable | Optional | `true` | Stable | -- |
| Sequence | Asset, Computable | Optional | `null` | Stable | -- |
| TimeElapsedRange | Array (Element: Double), Computable | Optional | `[0.0, 3.4028234663852886E38]` | Stable | Values must be >= 0, <= 3.4028234663852886e+38, in weakly ascending order |
| Mode | Flag, Computable | Optional | `Attacking` | Stable | If Mode is `Sequence`, the asset `Sequence` must be provided |

**Mode flag values:**

| Value | Description |
|-------|-------------|
| Ranged | Ranged |
| Charging | Weapon charging |
| Melee | Melee |
| Blocking | Blocking |
| Sequence | Combat sequence |
| Attacking | Attacking |
| Any | Any |
| None | None |

---

### Flock (Stable)

Test for flock membership and related properties.

| Attribute | Type | Required | Default | Stability | Constraint |
|-----------|------|----------|---------|-----------|------------|
| Enabled | Boolean, Computable | Optional | `true` | Stable | -- |
| FlockStatus | Flag | Optional | `Any` | Stable | -- |
| FlockPlayerStatus | Flag | Optional | `Any` | Stable | -- |
| Size | Array (Element: integer) | Optional | `null` | Stable | -- |
| CheckCanJoin | Boolean | Optional | `false` | Stable | -- |

**FlockStatus flag values:**

| Value | Description |
|-------|-------------|
| Leader | Is leader of a flock |
| Follower | Is part of a flock but not leader |
| NotMember | Is not part of a flock |
| Member | Is part of a flock |
| Any | Don't care |

**FlockPlayerStatus flag values:**

| Value | Description |
|-------|-------------|
| NotMember | Player is not member of a flock |
| Member | Player is member of a flock |
| Any | Don't care |

---

### HeightDifference (Stable)

Matches entities within the given height range.

| Attribute | Type | Required | Default | Stability | Constraint |
|-----------|------|----------|---------|-----------|------------|
| Enabled | Boolean, Computable | Optional | `true` | Stable | -- |
| HeightDifference | Array (Element: Double), Computable | Optional | `[-1.7976931348623157E308, 1.7976931348623157E308]` | Stable | Values must be >= -1.7976931348623157e+308, <= 1.7976931348623157e+308, in weakly ascending order |
| UseEyePosition | Boolean, Computable | Optional | `true` | Stable | -- |

---

### InsideBlock (Stable)

Matches if the entity is inside any of the blocks in the BlockSet.

| Attribute | Type | Required | Default | Stability | Constraint |
|-----------|------|----------|---------|-----------|------------|
| Enabled | Boolean, Computable | Optional | `true` | Stable | -- |
| BlockSet | Asset, Computable | **Required** | -- | Stable | -- |

---

### Inventory (Stable)

Test various conditions relating to entity inventory. This includes whether it contains a specific item, item count, and free slots.

| Attribute | Type | Required | Default | Stability | Constraint |
|-----------|------|----------|---------|-----------|------------|
| Enabled | Boolean, Computable | Optional | `true` | Stable | -- |
| Items | AssetArray (Element: Item), Computable | Optional | `[*]` | Stable | -- |
| CountRange | Array (Element: integer), Computable | Optional | `[1, 2147483647]` | Stable | Values must be >= 0, <= 2147483647, in weakly ascending order |
| FreeSlotRange | Array (Element: integer), Computable | Optional | `[0, 2147483647]` | Stable | Values must be >= 0, <= 2147483647, in weakly ascending order |

---

### ItemInHand (Stable)

Check if entity is holding an item.

| Attribute | Type | Required | Default | Stability | Constraint |
|-----------|------|----------|---------|-----------|------------|
| Enabled | Boolean, Computable | Optional | `true` | Stable | -- |
| Items | AssetArray (Element: Item), Computable | **Required** | -- | Stable | -- |
| Hand | Flag, Computable | Optional | `Both` | Stable | -- |

**Hand flag values:**

| Value | Description |
|-------|-------------|
| OffHand | The off-hand |
| Main | The main hand |
| Both | Both hands |

---

### LineOfSight (Stable)

Matches if there is line of sight to the target.

| Attribute | Type | Required | Default | Stability | Constraint |
|-----------|------|----------|---------|-----------|------------|
| Enabled | Boolean, Computable | Optional | `true` | Stable | -- |

---

### MovementState (Stable)

Check if the entity is in the given movement state.

| Attribute | Type | Required | Default | Stability | Constraint |
|-----------|------|----------|---------|-----------|------------|
| Enabled | Boolean, Computable | Optional | `true` | Stable | -- |
| State | Flag | **Required** | -- | Stable | -- |

**State flag values:**

| Value | Description |
|-------|-------------|
| WALKING | Walking |
| FLYING | Flying |
| RUNNING | Running |
| FALLING | Falling |
| IDLE | Idle |
| SPRINTING | Sprinting |
| CROUCHING | Crouching |
| CLIMBING | Climbing |
| ANY | Any |
| JUMPING | Jumping |

---

### NPCGroup (Stable)

Returns whether the entity matches one of the provided NPCGroups.

| Attribute | Type | Required | Default | Stability | Constraint |
|-----------|------|----------|---------|-----------|------------|
| Enabled | Boolean, Computable | Optional | `true` | Stable | -- |
| IncludeGroups | AssetArray (Element: TagSet), Computable | Optional | `null` | Stable | -- |
| ExcludeGroups | AssetArray (Element: TagSet), Computable | Optional | `null` | Stable | -- |

**Constraints:** One (and only one) of `IncludeGroups`, `ExcludeGroups` must be provided.

---

### Not (WorkInProgress)

Invert filter test.

| Attribute | Type | Required | Default | Stability | Constraint |
|-----------|------|----------|---------|-----------|------------|
| Enabled | Boolean, Computable | Optional | `true` | Stable | -- |
| Filter | ObjectRef (Object: IEntityFilter) | **Required** | -- | Stable | -- |

---

### Or (Stable)

Logical OR of a list of filters.

| Attribute | Type | Required | Default | Stability | Constraint |
|-----------|------|----------|---------|-----------|------------|
| Enabled | Boolean, Computable | Optional | `true` | Stable | -- |
| Filters | Array (Element: IEntityFilter) | **Required** | -- | Stable | Array must not be empty |

---

### SpotsMe (Stable)

Checks if the entity can view the NPC in a given view sector or cone and without obstruction.

| Attribute | Type | Required | Default | Stability | Constraint |
|-----------|------|----------|---------|-----------|------------|
| Enabled | Boolean, Computable | Optional | `true` | Stable | -- |
| ViewAngle | Double | Optional | `90.0` | Stable | Value must be >= 0 and <= 360 |
| ViewTest | Flag | Optional | `VIEW_SECTOR` | Stable | -- |
| TestLineOfSight | Boolean | Optional | `true` | Stable | -- |

**ViewTest flag values:**

| Value | Description |
|-------|-------------|
| VIEW_SECTOR | View Sector |
| NONE | None |
| VIEW_CONE | View Cone |

---

### StandingOnBlock (Stable)

Matches the block directly beneath the entity against a BlockSet.

| Attribute | Type | Required | Default | Stability | Constraint |
|-----------|------|----------|---------|-----------|------------|
| Enabled | Boolean, Computable | Optional | `true` | Stable | -- |
| BlockSet | Asset, Computable | **Required** | -- | Stable | -- |

---

### Stat (Stable)

Match stat values of the entity.

| Attribute | Type | Required | Default | Stability | Constraint |
|-----------|------|----------|---------|-----------|------------|
| Enabled | Boolean, Computable | Optional | `true` | Stable | -- |
| Stat | Asset, Computable | **Required** | -- | Stable | -- |
| StatTarget | Flag, Computable | **Required** | -- | Stable | -- |
| RelativeTo | Asset, Computable | **Required** | -- | Stable | -- |
| RelativeToTarget | Flag, Computable | **Required** | -- | Stable | -- |
| ValueRange | Array (Element: Double), Computable | **Required** | -- | Stable | Values must be >= 0, <= 1.7976931348623157e+308, in weakly ascending order |

**StatTarget / RelativeToTarget flag values:**

| Value | Description |
|-------|-------------|
| Min | Min value |
| Max | Max value |
| Value | Current value |

---

### ViewSector (Stable)

Matches entities within the given view sector.

| Attribute | Type | Required | Default | Stability | Constraint |
|-----------|------|----------|---------|-----------|------------|
| Enabled | Boolean, Computable | Optional | `true` | Stable | -- |
| ViewSector | Double, Computable | Optional | `0.0` | Stable | Value must be >= 0 and <= 360 |

---

## ISensorEntityPrioritiser Types

### Attitude (Stable)

Prioritises return entities by attitude.

| Attribute | Type | Required | Default | Stability | Constraint |
|-----------|------|----------|---------|-----------|------------|
| AttitudesByPriority | FlagArray, Computable | **Required** | -- | Stable | Array must not contain duplicates |

**AttitudesByPriority flag values:**

| Value | Description |
|-------|-------------|
| HOSTILE | Is hostile towards the target |
| REVERED | Reveres the target |
| FRIENDLY | Is friendly towards the target |
| IGNORE | Is ignoring the target |
| NEUTRAL | Is neutral towards the target |

---

## ISensorEntityCollector Types

### CombatTargets (Stable)

A collector which processes matched friendly and hostile targets and adds them to the NPC's short-term combat memory.

*No attributes.*
