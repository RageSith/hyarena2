# ProgressBar

Visual progress indicator bar that fills based on a value.

## Syntax

```
Group {
    Anchor: (Height: 8);
    Background: "../Common/ProgressBar.png";

    ProgressBar #MyProgress {
        BarTexturePath: "../Common/ProgressBarFill.png";
        Value: 0.65;
    }
}
```

**Important:** ProgressBar must be wrapped in a Group with `Background` for the empty bar track.

## Attributes

| Attribute | Type | Required | Description |
|-----------|------|----------|-------------|
| `BarTexturePath` | String | Yes | Path to the fill texture |
| `Value` | Float | Yes | Progress value (0.0 to 1.0) |
| `Direction` | Enum | No | Fill direction: `Start` or `End` |
| `EffectTexturePath` | String | No | Path to animated effect overlay |
| `EffectWidth` | Integer | No | Width of effect texture |
| `EffectHeight` | Integer | No | Height of effect texture |
| `EffectOffset` | Integer | No | Offset position of effect |

## Value Property

- Read/write via `#ElementId.Value`
- Type: `Double` (0.0 to 1.0)
- 0.0 = empty, 0.5 = 50%, 1.0 = full

## Direction Property

| Value | Description |
|-------|-------------|
| `Start` | Fill from right to left (inverted) |
| `End` | Fill from left to right (normal) |
| *omitted* | Default left to right |

## Java Integration

### Setting Value

```java
// Must use double (D suffix), not float
cmd.set("#MyProgress.Value", 0.65D);
```

### Example from Working Plugin

```java
double progress = calculateProgress(); // returns 0.0 to 1.0
cmd.set("#SkillProgress.Value", progress);
```

## Required Structure

ProgressBar requires a parent Group with the background texture:

```
Group {
    Anchor: (Height: 8);
    Background: "../Common/ProgressBar.png";

    ProgressBar #MyProgress {
        BarTexturePath: "../Common/ProgressBarFill.png";
        Value: 0.0;
    }
}
```

## Server Textures

Standard textures available from server assets:

| Texture | Path | Usage |
|---------|------|-------|
| Track (background) | `../Common/ProgressBar.png` | Empty bar background |
| Fill | `../Common/ProgressBarFill.png` | Filled portion |
| Effect | `../Common/ProgressBarEffect.png` | Optional animated overlay |

## Full Example with Effect

```
Group {
    Anchor: (Height: 8);
    Background: "../Common/ProgressBar.png";

    ProgressBar #SkillProgress {
        BarTexturePath: "../Common/ProgressBarFill.png";
        EffectTexturePath: "../Common/ProgressBarEffect.png";
        EffectWidth: 102;
        EffectHeight: 58;
        EffectOffset: 74;
        Value: 0.0;
    }
}
```

## Known Limitations

### Scrollable Container Bug

**ProgressBar fill disappears when placed inside a `TopScrolling` container that has been scrolled.**

- Fill renders correctly at initial scroll position (top)
- Scrolling down causes fill to disappear
- Fill reappears when scrolled back to top
- This is a Hytale UI system bug, not a syntax issue

**Workaround:** Place ProgressBar outside scrollable areas, or accept the visual glitch.

### Properties NOT Needed

- `Alignment` - Not required, may cause issues
- `Anchor` on ProgressBar itself - Not needed, inherits from parent Group

## Related

- [Slider](Slider.md) - Interactive range input
- [FloatSlider](FloatSlider.md) - Interactive decimal range input

## Tested In

- `GeneralPage.ui` - Basic progress bar test
- `MMOSkillTree` plugin - Production usage with skill XP progress
