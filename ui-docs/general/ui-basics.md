# Hytale UI Basics

This guide covers the fundamentals of building custom UI in Hytale plugins.

## File Structure

```
your-plugin/
├── src/main/java/
│   └── com/yourplugin/
│       └── page/
│           └── YourPage.java       # Java page class
└── src/main/resources/
    ├── manifest.json               # Must have IncludesAssetPack: true
    └── Common/UI/Custom/
        ├── Pages/
        │   └── YourPage.ui         # UI definition file
        └── Huds/
            └── YourHud.ui          # HUD definition file
```

## manifest.json

```json
{
  "Name": "YourPlugin",
  "EntryPoint": "com.yourplugin.YourPlugin",
  "IncludesAssetPack": true
}
```

**Important:** `IncludesAssetPack: true` is required for UI files to be loaded.

## UI File Syntax (.ui)

UI files use a custom syntax similar to CSS/JSON hybrid.

### Basic Structure

```
$C = "../Common.ui";  // Import server templates

Group {
    // Root element

    $C.@PageOverlay {}  // Dim background

    $C.@DecoratedContainer {
        @CloseButton = true;
        Anchor: (Width: 600, Height: 400);

        Group #Title {
            $C.@Title #TitleLabel {
                @Text = "Page Title";
            }
        }

        Group #Content {
            // Your content here
        }
    }
}
```

### Property Syntax

Two syntax styles exist:

```
// Regular properties (colon)
Anchor: (Width: 200, Height: 50);
Text: "Hello";
Value: 50;

// Template properties (equals sign with @)
@Text = "Hello";
@Anchor = (Width: 200);
@CloseButton = true;
```

**Rule:** Use `@Property = value` for template properties, `Property: value` for regular properties.

### Element IDs

```
Label #MyLabel {
    Text: "Hello";
}
```

Reference in Java: `cmd.set("#MyLabel.Text", "New text");`

### Comments

```
// Single line comment

/* Multi-line
   comment */
```

## Common.ui Import

Server provides templates via Common.ui:

```
$C = "../Common.ui";

// Use templates
$C.@TextField #Input { }
$C.@TextButton #Button { @Text = "Click"; }
$C.@DecoratedContainer { }
```

## Anchor System

Controls size and position:

```
Anchor: (
    Width: 200,          // Fixed width
    Height: 50,          // Fixed height
    Left: 10,            // Offset from left
    Right: 10,           // Offset from right
    Top: 10,             // Offset from top
    Bottom: 10,          // Offset from bottom
    Full: 0,             // All sides (shorthand)
    Horizontal: 10,      // Left + Right
    Vertical: 10         // Top + Bottom
);
```

## Padding System

Internal spacing:

```
Padding: (
    Full: 15,            // All sides
    Left: 10,
    Right: 10,
    Top: 10,
    Bottom: 10,
    Horizontal: 10,      // Left + Right
    Vertical: 10         // Top + Bottom
);
```

## Layout Modes

Controls child element arrangement:

| Mode | Description |
|------|-------------|
| `Top` | Stack vertically from top |
| `TopScrolling` | Vertical with scrollbar |
| `Left` | Stack horizontally from left |
| `LeftScrolling` | Horizontal with scrollbar |
| `Right` | Stack from right |
| `RightScrolling` | Right with scrollbar |

```
Group {
    LayoutMode: TopScrolling;
    ScrollbarStyle: @DefaultScrollbarStyle;

    // Children stack vertically with scroll
}
```

## Style System

Text styling:

```
Style: (
    FontSize: 14,
    TextColor: #ffffff,
    RenderBold: true,
    RenderItalics: false,
    RenderUppercase: true,
    HorizontalAlignment: Center,
    VerticalAlignment: Center,
    Wrap: true
);
```

### Color Format

```
TextColor: #ffffff          // RGB hex
TextColor: #ffffff(0.5)     // RGBA with alpha
Background: #000000(0.45)   // 45% opacity black
```

## FlexWeight

For flexible sizing in layouts:

```
Group {
    LayoutMode: Left;

    Label { Anchor: (Width: 100); }     // Fixed 100px
    Group { FlexWeight: 1; }             // Takes remaining space
    Label { Anchor: (Width: 100); }     // Fixed 100px
}
```

## Visibility

```
Visible: true;   // or false
```

Set from Java:
```java
cmd.set("#Element.Visible", false);
```

## Background

### Solid Color
```
Background: #1a2633;
Background: #000000(0.5);  // With alpha
```

### Texture
```
Background: "../Common/ProgressBar.png";
```

## Common Patterns

### Row Layout
```
Group {
    LayoutMode: Left;
    Anchor: (Height: 45);

    Label { Text: "Label"; Anchor: (Width: 100); }
    $C.@TextField #Input { @Anchor = (Width: 200); }
}
```

### Scrollable Content
```
Group #Content {
    LayoutMode: TopScrolling;
    ScrollbarStyle: @DefaultScrollbarStyle;

    // Many children that overflow
}
```

### Spacer
```
Group { Anchor: (Width: 20); }   // Horizontal spacer
Group { Anchor: (Height: 20); }  // Vertical spacer
```

### Section Header
```
$C.@Subtitle { @Text = "Section Name"; }
$C.@ContentSeparator { @Anchor = (Top: 10, Bottom: 10); }
```

## Texture Paths

Relative to your .ui file location:

```
// From Pages/YourPage.ui to Common folder
Background: "../Common/ProgressBar.png";

// Server-provided textures (Common.ui templates handle this)
```

## Known Limitations

- `LayoutMode: Center` - Does not work
- `Margin` - Does not work (use Padding instead)
- `ClipChildren` - Does not work reliably
- ProgressBar fill disappears in scrollable containers when scrolled
- Sprite element crashes or doesn't render

## Next Steps

- [Interactive Pages](interactive-pages.md) - Event handling and state
- [Import Reference](import-reference.md) - Java import paths
- [Common.ui Reference](common-ui-reference.md) - Available templates
