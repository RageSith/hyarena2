# Slider

Draggable range input for integer values.

## Syntax

```
Slider #VolumeSlider {
    Anchor: (Width: 200, Height: 20);
    Min: 0;
    Max: 100;
    Step: 1;
    Value: 50;
    Style: @DefaultSliderStyle;
}
```

## Attributes

| Attribute | Type | Description |
|-----------|------|-------------|
| `Min` | Integer | Minimum value |
| `Max` | Integer | Maximum value |
| `Step` | Integer | Increment step |
| `Value` | Integer | Current/initial value |
| `Style` | Style | Use `@DefaultSliderStyle` from Common.ui |
| `Anchor` | Anchor | Size and position |

## Value Property

- Read/write via `#ElementId.Value`
- Type: `Integer`

## Java Integration

### Event Binding

```java
events.addEventBinding(
    CustomUIEventBindingType.ValueChanged,
    "#VolumeSlider",
    EventData.of("@VolumeSlider", "#VolumeSlider.Value"),
    false
);
```

### Setting Value

```java
cmd.set("#VolumeSlider.Value", sliderValue);
```

### Codec Field

```java
.append(new KeyedCodec<>("@VolumeSlider", Codec.INTEGER),
    (d, v) -> d.sliderValue = v, d -> d.sliderValue).add()
```

## Example with Value Label

Display current value next to slider:

```
Group #SliderRow {
    LayoutMode: Left;
    Anchor: (Height: 45);

    Label {
        Text: "Volume";
        Anchor: (Width: 100);
        Style: (FontSize: 14, TextColor: #96a9be, VerticalAlignment: Center);
    }

    Slider #VolumeSlider {
        Anchor: (Width: 200, Height: 20);
        Min: 0;
        Max: 100;
        Step: 1;
        Value: 50;
        Style: @DefaultSliderStyle;
    }

    Label #VolumeValue {
        Text: "50";
        Anchor: (Width: 50, Left: 10);
        Style: (FontSize: 14, TextColor: #ffffff, VerticalAlignment: Center);
    }
}
```

**Note**: To update the value label live, you would need to rebuild or find a way to update without losing slider focus.

## Related

- [FloatSlider](FloatSlider.md) - For decimal values

## Tested In

- `GeneralPage.ui` - Volume slider with min/max/step
