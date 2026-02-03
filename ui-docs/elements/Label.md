# Label

Text display element.

## Syntax

```
Label {
    Text: "Your text here";
}
```

With ID:
```
Label #MyId {
    Text: "Your text here";
}
```

## Attributes

| Attribute | Syntax | Description |
|-----------|--------|-------------|
| `Text` | `"string"` | The text content to display |
| `Anchor` | `(Height: N)` | Sets fixed height in pixels |
| `Style` | `(...)` | Styling properties (see below) |

### Style Properties

| Property | Values | Description |
|----------|--------|-------------|
| `FontSize` | Number | Font size in pixels |
| `TextColor` | `#hex` | Text color |
| `Alignment` | `Center` | Text alignment |

## Example

```
Label #Title {
    Text: "Test Window";
    Anchor: (Height: 40);
    Style: (FontSize: 24, TextColor: #ffffff, Alignment: Center);
}
```

## Tested In

- `GeneralPage.ui` - Title label in test window
