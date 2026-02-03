# TextButton

Clickable button element with text.

## Syntax

### With Common.ui Template (Recommended)

```
$C = "../Common.ui";

$C.@TextButton #MyButton {
    @Text = "Click Me";
    @Anchor = (Width: 140, Height: 40);
}
```

### Without Template

```
TextButton #MyButton {
    Anchor: (Height: 40);
    Text: "Click Me";
}
```

Note: Without template, renders as plain clickable text (no background/borders).

## Attributes

### With Template (`$C.@TextButton`)

| Attribute | Syntax | Description |
|-----------|--------|-------------|
| `@Text` | `= "string"` | Button label (template syntax uses `=`) |
| `@Anchor` | `= (Width: N, Height: N)` | Dimensions |

### Without Template

| Attribute | Syntax | Description |
|-----------|--------|-------------|
| `Text` | `"string"` | Button label |
| `Anchor` | `(Height: N)` | Sets height |

## Behavior

- Triggers `Activating` event when clicked
- Template version has full styling (background, borders, hover effects)
- Non-template version appears as plain text

## Java Integration

### Event Binding

```java
events.addEventBinding(
    CustomUIEventBindingType.Activating,
    "#SubmitButton",
    EventData.of("Submit", "true"),
    false
);
```

### Handling Click

```java
@Override
public void handleDataEvent(..., PageEventData data) {
    if (data.submit != null) {
        // Button was clicked - safe to rebuild here
        outputText = "Button clicked!";
        rebuild();
    }
}
```

### Codec Field

```java
.append(new KeyedCodec<>("Submit", Codec.STRING),
    (d, v) -> d.submit = v, d -> d.submit).add()
```

## Example

### Styled Button (Template)

```
$C.@TextButton #SubmitButton {
    @Text = "Submit";
    @Anchor = (Width: 140, Height: 40);
}
```

### Plain Button (No Template)

```
TextButton #SubmitButton {
    Anchor: (Height: 40);
    Text: "Submit";
}
```

## Important Notes

- **Template syntax**: Use `@Property = value` with templates, `Property: value` without
- **Rebuild safe**: Unlike input fields, calling `rebuild()` on button click is safe (no focus to lose)

## Tested In

- `GeneralPage.ui` - Submit button with and without template
