# TextField

Text input field for user text entry.

## Syntax

### With Common.ui Template (Recommended)

```
$C = "../Common.ui";

$C.@TextField #MyInput {
    @Anchor = (Width: 300);
    PlaceholderText: "Enter text here";
}
```

### Without Template (Not Recommended)

```
TextField #MyInput {
    Anchor: (Height: 35);
    PlaceholderText: "Enter text here";
}
```

Note: Without template, the field may lack proper styling.

## Attributes

| Attribute | Syntax | Description |
|-----------|--------|-------------|
| `@Anchor` | `= (Width: N)` | Sets width (template syntax uses `=`) |
| `PlaceholderText` | `"string"` | Hint text shown when empty |

## Value Property

- Read/write via `#ElementId.Value`
- Type: `String`

## Java Integration

### Event Binding

```java
events.addEventBinding(
    CustomUIEventBindingType.ValueChanged,
    "#TextInput",
    EventData.of("@TextInput", "#TextInput.Value"),
    false
);
```

### Restoring Value After Rebuild

```java
// In build() method - required to persist value
cmd.set("#TextInput.Value", textValue);
```

### Handling Value Changes

```java
// In handleDataEvent() - store value WITHOUT rebuild to keep focus
if (data.textInput != null) {
    textValue = data.textInput;
    // Do NOT call rebuild() here - causes focus loss
}
```

### Codec Field

```java
.append(new KeyedCodec<>("@TextInput", Codec.STRING),
    (d, v) -> d.textInput = v, d -> d.textInput).add()
```

## Example Layout

```
Group #TextRow {
    LayoutMode: Left;
    Anchor: (Height: 45);

    Label {
        Text: "Text Input";
        Anchor: (Width: 120);
        Style: (FontSize: 14, TextColor: #96a9be, VerticalAlignment: Center);
    }

    $C.@TextField #TextInput {
        @Anchor = (Width: 300);
        PlaceholderText: "Enter text here";
    }
}
```

## Important Notes

- **Focus loss**: Calling `rebuild()` on ValueChanged causes focus loss. Store values without rebuilding.
- **Value persistence**: Must call `cmd.set("#ElementId.Value", value)` in `build()` to restore value after any rebuild.

## Tested In

- `GeneralPage.ui` - Text input with value persistence
