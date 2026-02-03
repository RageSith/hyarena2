# CheckBox

Boolean toggle control for on/off states.

## Syntax

### With Common.ui Template (Recommended)

```
$C = "../Common.ui";

$C.@CheckBox #MyCheckbox {
}
```

### Without Template (Not Recommended)

```
CheckBox #MyCheckbox {
}
```

Note: Without template, the checkbox may lack proper styling.

## Attributes

The CheckBox element itself has no text property. Use a separate Label for the description.

## Value Property

- Read/write via `#ElementId.Value`
- Type: `Boolean` (`true`/`false`)

## Java Integration

### Event Binding

```java
events.addEventBinding(
    CustomUIEventBindingType.ValueChanged,
    "#CheckboxTest",
    EventData.of("@CheckboxTest", "#CheckboxTest.Value"),
    false
);
```

### Restoring Value After Rebuild

```java
// In build() method - required to persist value
cmd.set("#CheckboxTest.Value", checkboxValue);
```

### Handling Value Changes

```java
// In handleDataEvent() - store value WITHOUT rebuild to keep focus
if (data.checkboxTest != null) {
    checkboxValue = data.checkboxTest;
    // Do NOT call rebuild() here - causes visual reset
}
```

### Codec Field

```java
.append(new KeyedCodec<>("@CheckboxTest", Codec.BOOLEAN),
    (d, v) -> d.checkboxTest = v, d -> d.checkboxTest).add()
```

## Example Layout

CheckBox requires a separate Label for text. Use a horizontal Group with spacing:

```
Group #CheckboxRow {
    LayoutMode: Left;
    Anchor: (Height: 35);
    Padding: (Left: 10);

    $C.@CheckBox #CheckboxTest {
    }

    Label {
        Text: "Enable this option";
        Anchor: (Left: 10, Width: 200);
        Style: (FontSize: 14, TextColor: #ffffff, VerticalAlignment: Center);
    }
}
```

## Spacing Notes

- CheckBox has no built-in label - must add separate Label element
- Use `Anchor: (Left: N)` on the Label to add space between checkbox and text
- Use `Padding: (Left: N)` on the row Group for left margin

## Important Notes

- **No Text property**: Unlike some UI frameworks, CheckBox does not have a built-in text label.
- **Value persistence**: Must call `cmd.set("#ElementId.Value", value)` in `build()` to restore state after rebuild.
- **Avoid rebuild on change**: Calling `rebuild()` on ValueChanged causes the checkbox to visually reset.

## Tested In

- `GeneralPage.ui` - Checkbox with label and value persistence
