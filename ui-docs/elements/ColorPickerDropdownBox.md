# ColorPickerDropdownBox

Compact color picker that opens a full color selection panel when clicked.

## Syntax

```
ColorPickerDropdownBox #ColorPicker {
    Anchor: (Width: 120, Height: 32);
    Format: Rgb;
    Style: $C.@DefaultColorPickerDropdownBoxStyle;
    DisplayTextField: true;
}
```

## Attributes

| Attribute | Value | Description |
|-----------|-------|-------------|
| `Anchor` | `(Width: N, Height: N)` | Size of the collapsed element |
| `Format` | `Rgb` | Color format |
| `Style` | `$C.@DefaultColorPickerDropdownBoxStyle` | Required styling from Common.ui |
| `DisplayTextField` | `true`/`false` | Show hex input field in picker |

## Properties

| Property | Type | Description |
|----------|------|-------------|
| `.Color` | String (hex) | The selected color value |

## Java Integration

### Setting Initial Color

```java
cmd.set("#ColorPicker.Color", "#ff5500");
```

### Reading Color Value

**Limitation**: `ValueChanged` event does NOT work with ColorPickerDropdownBox.

```java
// This does NOT work:
events.addEventBinding(
    CustomUIEventBindingType.ValueChanged,
    "#ColorPicker",
    EventData.of("@ColorPicker", "#ColorPicker.Color"),
    false
);
// Error: event binding has no compatible ValueChanged event
```

## Visual Behavior

- Collapsed: Shows small color swatch (or checkered if no color set)
- Expanded: Full color picker with:
  - Color gradient area
  - Hue slider
  - Hex input field (if `DisplayTextField: true`)

## Example

```
Group #ColorRow {
    LayoutMode: Left;
    Anchor: (Height: 45);

    Label {
        Text: "Tint Color";
        Anchor: (Width: 100);
        Style: (FontSize: 14, TextColor: #96a9be, VerticalAlignment: Center);
    }

    ColorPickerDropdownBox #ColorPicker {
        Anchor: (Width: 120, Height: 32);
        Format: Rgb;
        Style: $C.@DefaultColorPickerDropdownBoxStyle;
        DisplayTextField: true;
    }
}
```

## Known Limitations

- Cannot read selected color via `ValueChanged` event
- Must use `$C.@DefaultColorPickerDropdownBoxStyle` for proper styling
- If no color is set, displays checkered pattern

## Related

- `ColorPicker` - Full standalone color picker (not dropdown)

## Tested In

- `GeneralPage.ui` - Color picker with initial color set
