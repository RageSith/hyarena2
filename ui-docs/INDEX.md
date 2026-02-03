# Hytale UI Documentation Index

Complete reference for building custom UI in Hytale plugins.

## Quick Start

1. Read [UI Basics](general/ui-basics.md) - File structure, syntax, layout
2. Read [Interactive Pages](general/interactive-pages.md) - Event handling
3. Check [Import Reference](general/import-reference.md) - Java imports
4. Browse element documentation below

## General Guides

| Guide | Description |
|-------|-------------|
| [UI Basics](general/ui-basics.md) | File structure, .ui syntax, layout modes, styling |
| [Interactive Pages](general/interactive-pages.md) | Event handling, state management, codecs |
| [Dynamic Templates](general/dynamic-templates.md) | Reusable templates, dynamic lists, bracket notation |
| [Import Reference](general/import-reference.md) | Java import paths for Hytale API |
| [Common.ui Reference](general/common-ui-reference.md) | Server-provided templates (55+) |

## Elements

### Container Elements

| Element | Description | Status |
|---------|-------------|--------|
| [Group](elements/Group.md) | Container for layout, supports scrolling | Confirmed |
| [DecoratedContainer](elements/DecoratedContainer.md) | Ornamental window frame | Confirmed |
| `@Container` | Simple container with title | Confirmed |
| `@PageOverlay` | Background dimming | Confirmed |

### Text Display

| Element | Description | Status |
|---------|-------------|--------|
| [Label](elements/Label.md) | Static text display | Confirmed |
| `@Title` | Styled title label | Confirmed |
| `@Subtitle` | Section header label | Confirmed |

### Input Elements

| Element | Description | Status |
|---------|-------------|--------|
| [TextField](elements/TextField.md) | Single-line text input | Confirmed |
| [MultilineTextField](elements/MultilineTextField.md) | Multi-line text input | Confirmed (with bug) |
| [NumberField](elements/NumberField.md) | Numeric input | Confirmed |
| [Slider](elements/Slider.md) | Integer range input | Confirmed |
| [FloatSlider](elements/FloatSlider.md) | Decimal range input | Confirmed |
| [CheckBox](elements/CheckBox.md) | Boolean toggle | Confirmed |
| [DropdownBox](elements/DropdownBox.md) | Selection dropdown | Confirmed |

### Button Elements

| Element | Description | Status |
|---------|-------------|--------|
| [TextButton](elements/TextButton.md) | Primary action button | Confirmed |
| `@SecondaryTextButton` | Secondary style button | Confirmed |
| `@TertiaryTextButton` | Tertiary/subtle button | Confirmed |
| `@CancelTextButton` | Destructive action button | Not tested |
| `@CloseButton` | X button for dialogs | Confirmed |

### Progress & Display

| Element | Description | Status |
|---------|-------------|--------|
| [ProgressBar](elements/ProgressBar.md) | Visual progress indicator | Confirmed (with bug) |
| `@ContentSeparator` | Horizontal line | Confirmed |

### Partially Working / Limitations

| Element | Description | Limitation |
|---------|-------------|------------|
| [ColorPickerDropdownBox](elements/ColorPickerDropdownBox.md) | Compact color picker | Can set color, can't read via events |
| [ProgressBar](elements/ProgressBar.md) | Progress indicator | Fill disappears when scrolled in TopScrolling |
| [MultilineTextField](elements/MultilineTextField.md) | Multi-line text input | Text doesn't follow scroll in TopScrolling |

### Broken / Not Working

| Element | Description | Issue |
|---------|-------------|-------|
| Sprite | Image/animation display | Crashes game or doesn't render |
| ColorPicker | Full color selection | Not tested |

## Element Quick Reference

### Group (Container)
```
Group #MyGroup {
    LayoutMode: Top;           // Top, TopScrolling, Left, LeftScrolling
    Anchor: (Width: 400, Height: 300);
    Padding: (Full: 15);
    Background: #1a2633;
    ScrollbarStyle: @DefaultScrollbarStyle;  // For scrolling modes
}
```

### Label
```
Label #MyLabel {
    Text: "Hello World";
    Anchor: (Height: 30);
    Style: (FontSize: 14, TextColor: #ffffff, RenderBold: true);
}
```

### TextField
```
$C.@TextField #Input {
    @Anchor = (Width: 250);
    PlaceholderText: "Enter text...";
}
```

### MultilineTextField
```
MultilineTextField #Notes {
    Anchor: (Width: 350, Height: 100);
    PlaceholderText: "Enter text...";
    ScrollbarStyle: @DefaultScrollbarStyle;
    Background: @InputBoxBackground;
    Style: @DefaultInputFieldStyle;
    ContentPadding: (Horizontal: 10, Vertical: 8);
}
```
**Note**: Place outside TopScrolling containers to avoid rendering bug.

### Slider
```
Slider #Volume {
    Anchor: (Width: 200, Height: 20);
    Min: 0;
    Max: 100;
    Step: 5;
    Value: 50;
    Style: @DefaultSliderStyle;
}
```

### FloatSlider
```
FloatSlider #Scale {
    Anchor: (Width: 200, Height: 20);
    Min: 0.0;
    Max: 1.0;
    Step: 0.05;
    Value: 0.5;
    Style: @DefaultSliderStyle;
}
```

### CheckBox
```
$C.@CheckBox #MyCheck {}
Label { Text: "Option"; Anchor: (Left: 10); }
```

### DropdownBox
```
$C.@DropdownBox #Select {
    @Anchor = (Width: 200);
}
// Entries set from Java
```

### TextButton
```
$C.@TextButton #Submit {
    @Text = "Submit";
    @Anchor = (Width: 120);
}
```

### ProgressBar
```
Group {
    Anchor: (Width: 200, Height: 8);
    Background: "../Common/ProgressBar.png";

    ProgressBar #Progress {
        BarTexturePath: "../Common/ProgressBarFill.png";
        Value: 0.5;
    }
}
```

## Java Quick Reference

### Setting Values
```java
cmd.set("#Label.Text", "New text");
cmd.set("#Input.Value", "text value");
cmd.set("#Slider.Value", 50);           // Integer
cmd.set("#FloatSlider.Value", 0.5f);    // Float
cmd.set("#Checkbox.Value", true);       // Boolean
cmd.set("#Progress.Value", 0.65D);      // Double (0.0-1.0)
```

### Event Bindings
```java
// Button click
events.addEventBinding(CustomUIEventBindingType.Activating,
    "#Button", EventData.of("BtnKey", "value"), false);

// Value change
events.addEventBinding(CustomUIEventBindingType.ValueChanged,
    "#Input", EventData.of("@InputKey", "#Input.Value"), false);
```

### Codec Types
```java
Codec.STRING   // TextField, DropdownBox
Codec.INTEGER  // NumberField, Slider
Codec.FLOAT    // FloatSlider
Codec.BOOLEAN  // CheckBox
```

### DropdownBox Entries
```java
var entries = new ArrayList<DropdownEntryInfo>();
entries.add(new DropdownEntryInfo(
    LocalizableString.fromString("Display Text"),
    "value"
));
cmd.set("#Dropdown.Entries", entries);
cmd.set("#Dropdown.Value", "value");
```

## Known Limitations

| Issue | Description |
|-------|-------------|
| `LayoutMode: Center` | Does not work |
| `Margin` | Does not work (use Padding) |
| `ClipChildren` | Does not work reliably |
| Input focus | `rebuild()` on ValueChanged causes focus loss |
| ProgressBar in scroll | Fill disappears when container is scrolled |
| Sprite | Crashes or doesn't render |

## Template Syntax

```
$C = "../Common.ui";  // Import

// Template properties use = sign
$C.@TextField #Input {
    @Anchor = (Width: 200);        // Template property
    PlaceholderText: "Enter...";   // Regular property
}
```

## File Locations

- UI Files: `src/main/resources/Common/UI/Custom/Pages/`
- Java Pages: `src/main/java/.../page/`
- Server Templates: `../Common.ui` (imported, not in your project)
- Server Textures: `../Common/` (relative paths)
