# DecoratedContainer

Ornamental container with title bar and optional close button. Part of Common.ui templates.

## Syntax

```
$C = "../Common.ui";

$C.@DecoratedContainer {
    @CloseButton = true;
    Anchor: (Width: 600, Height: 500);

    Group #Title {
        $C.@Title #TitleLabel {
            @Text = "Window Title";
            Anchor: (Top: 12, Height: 24);
        }
    }

    Group #Content {
        Padding: (Full: 15);
        LayoutMode: Top;

        // Your content here
    }
}
```

## Template Properties

| Property | Default | Description |
|----------|---------|-------------|
| `@CloseButton` | `false` | Show X button in corner |
| `@ContentPadding` | `Padding(Full: 17)` | Padding for content area |

## Structure

The container has two predefined child groups:

### #Title
- Height: 38px
- Place your title label here
- Background: Decorative header texture

### #Content
- Main content area
- Set `LayoutMode: Top` for vertical stacking
- Add your UI elements here

## Close Button

When `@CloseButton = true`:
- X button appears at top-right
- Clicking closes the page (ESC behavior)
- Position: Top: -8, Right: -8

## Example

```
$C.@DecoratedContainer {
    @CloseButton = true;
    Anchor: (Width: 500, Height: 400);

    Group #Title {
        $C.@Title #TitleLabel {
            @Text = "Settings";
            Anchor: (Top: 12, Height: 24);
        }
    }

    Group #Content {
        Padding: (Full: 20);
        LayoutMode: Top;

        $C.@Subtitle { @Text = "General"; }

        // ... form elements ...

        $C.@TextButton #SaveButton {
            @Text = "Save";
        }
    }
}
```

## Bottom-Anchored Elements

To pin elements (like buttons) to the bottom of the container, place them **outside** `#Content` but **inside** `@DecoratedContainer`:

```
$C.@DecoratedContainer {
    Anchor: (Width: 600, Height: 620);

    Group #Title { ... }

    Group #Content {
        Anchor: (Top: 38);
        // Scrollable/flowing content here
    }

    // Buttons pinned to bottom
    Group #ButtonRow {
        Anchor: (Bottom: 20, Height: 50, Left: 15);
        LayoutMode: Left;

        $C.@TextButton #SaveButton { @Text = "Save"; }
    }
}
```

**Important**: Elements outside the container bounds are not interactable. Always ensure container height accommodates all content.

## Related Templates

| Template | Description |
|----------|-------------|
| `@Container` | Simpler container without decorations |
| `@PageOverlay` | Semi-transparent background overlay |
| `@Title` | Styled title label |
| `@Subtitle` | Section header label |

## Tested In

- `GeneralPage.ui` - Main test page container
