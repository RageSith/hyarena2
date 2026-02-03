# FloatSlider

Draggable range input for decimal/float values.

## Syntax

```
FloatSlider #ScaleSlider {
    Anchor: (Width: 200, Height: 20);
    Min: 0.0;
    Max: 2.0;
    Step: 0.1;
    Value: 1.0;
    Style: @DefaultSliderStyle;
}
```

## Attributes

| Attribute | Type | Description |
|-----------|------|-------------|
| `Min` | Float | Minimum value |
| `Max` | Float | Maximum value |
| `Step` | Float | Increment step (e.g., 0.1, 0.01) |
| `Value` | Float | Current/initial value |
| `Style` | Style | Use `@DefaultSliderStyle` from Common.ui |
| `Anchor` | Anchor | Size and position |

## Value Property

- Read/write via `#ElementId.Value`
- Type: `Float`

## Java Integration

### Event Binding

```java
events.addEventBinding(
    CustomUIEventBindingType.ValueChanged,
    "#ScaleSlider",
    EventData.of("@ScaleSlider", "#ScaleSlider.Value"),
    false
);
```

### Setting Value

```java
cmd.set("#ScaleSlider.Value", scaleValue);
```

### Codec Field

```java
.append(new KeyedCodec<>("@ScaleSlider", Codec.FLOAT),
    (d, v) -> d.scaleSlider = v, d -> d.scaleSlider).add()
```

## Example with Value Label

Display current value next to slider:

```
Group #FloatSliderRow {
    LayoutMode: Left;
    Anchor: (Height: 45);

    Label {
        Text: "Scale";
        Anchor: (Width: 100);
        Style: (FontSize: 14, TextColor: #96a9be, VerticalAlignment: Center);
    }

    FloatSlider #ScaleSlider {
        Anchor: (Width: 200, Height: 20);
        Min: 0.0;
        Max: 2.0;
        Step: 0.1;
        Value: 1.0;
        Style: @DefaultSliderStyle;
    }

    Label #ScaleValue {
        Text: "1.0";
        Anchor: (Width: 50, Left: 10);
        Style: (FontSize: 14, TextColor: #ffffff, VerticalAlignment: Center);
    }
}
```

## Differences from Slider

| Aspect | Slider | FloatSlider |
|--------|--------|-------------|
| Value Type | Integer | Float |
| Codec | `Codec.INTEGER` | `Codec.FLOAT` |
| Step | Whole numbers | Decimals (0.1, 0.01, etc.) |

## Related

- [Slider](Slider.md) - For integer values

## Tested In

- `GeneralPage.ui` - Scale slider with 0.1 step increments
