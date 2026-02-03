# NumberField

Numeric input field for integer entry.

## Syntax

### With Common.ui Template (Recommended)

```
$C = "../Common.ui";

$C.@NumberField #MyNumber {
    @Anchor = (Width: 100);
}
```

### Without Template (Not Recommended)

```
NumberField #MyNumber {
    Anchor: (Height: 35);
}
```

Note: Without template, the field may lack proper styling.

## Attributes

| Attribute | Syntax | Description |
|-----------|--------|-------------|
| `@Anchor` | `= (Width: N)` | Sets width (template syntax uses `=`) |

## Value Property

- Read/write via `#ElementId.Value`
- Type: `Integer`
- Non-numeric input defaults to `0`

## Java Integration

### Event Binding

```java
events.addEventBinding(
    CustomUIEventBindingType.ValueChanged,
    "#NumberInput",
    EventData.of("@NumberInput", "#NumberInput.Value"),
    false
);
```

### Restoring Value After Rebuild

```java
// In build() method - required to persist value
cmd.set("#NumberInput.Value", numberValue);
```

### Handling Value Changes

```java
// In handleDataEvent() - store value WITHOUT rebuild to keep focus
if (data.numberInput != null) {
    numberValue = data.numberInput;
    // Do NOT call rebuild() here - causes focus loss
}
```

### Codec Field

```java
.append(new KeyedCodec<>("@NumberInput", Codec.INTEGER),
    (d, v) -> d.numberInput = v, d -> d.numberInput).add()
```

## Example Layout

```
Group #NumberRow {
    LayoutMode: Left;
    Anchor: (Height: 45);

    Label {
        Text: "Number Input";
        Anchor: (Width: 120);
        Style: (FontSize: 14, TextColor: #96a9be, VerticalAlignment: Center);
    }

    $C.@NumberField #NumberInput {
        @Anchor = (Width: 100);
    }
}
```

## Important Notes

- **Integer only**: Accepts integer values. Non-numeric text input results in `0`.
- **Focus loss**: Calling `rebuild()` on ValueChanged causes focus loss. Store values without rebuilding.
- **Value persistence**: Must call `cmd.set("#ElementId.Value", value)` in `build()` to restore value after any rebuild.

## Tested In

- `GeneralPage.ui` - Number input with value persistence
