# Group

Container element for grouping other UI elements.

## Syntax

```
Group {
    // attributes
    // child elements
}
```

With ID:
```
Group #MyId {
    // ...
}
```

## Attributes

| Attribute | Syntax | Description |
|-----------|--------|-------------|
| `Anchor` | `(Width: N, Height: N)` | Sets fixed dimensions in pixels |
| `Background` | `#hex(alpha)` | Background color with transparency (0.0-1.0) |
| `LayoutMode` | `Top`, `Bottom`, `Left`, `Right`, `TopScrolling` | Stacks children in specified direction |
| `Padding` | `(Full: N)` | Inner spacing on all sides |
| `ScrollbarStyle` | `@DefaultScrollbarStyle` | Scrollbar style (use with TopScrolling) |

## Behavior

- Root-level Group is centered on screen by default
- Can contain child elements (Label, other Groups, etc.)
- `LayoutMode: Top` stacks children vertically from top to bottom
- `LayoutMode: Left` stacks children horizontally from left to right

## Scrollable Content

Use `LayoutMode: TopScrolling` for vertical scrolling when content exceeds container:

```
Group #Content {
    Anchor: (Top: 38, Bottom: 80);
    LayoutMode: TopScrolling;
    ScrollbarStyle: @DefaultScrollbarStyle;

    // Content here - scrollbar appears if it overflows
}
```

**Important**:
- Scrollbar only appears when content exceeds visible area
- Constrain the group with `Top`/`Bottom` anchors to define scrollable region

## Known Limitations

- `LayoutMode: Center` does not work
- `Margin` does not work (use `Padding` instead)
- Elements outside container bounds are not interactable

## Example

```
Group {
    Anchor: (Width: 500, Height: 400);
    Background: #1a1a2e(0.95);
    Padding: (Full: 20);
    LayoutMode: Top;

    Label #Title {
        Text: "Test Window";
    }
}
```

## Tested In

- `GeneralPage.ui` - Basic window container with layout
