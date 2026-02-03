# Common.ui Template Reference

> **Note**: This file documents templates from the server-provided `Common.ui` asset. These templates are available at runtime and may change with game updates. This is reference material only.

## How to Use Templates

Import Common.ui at the top of your `.ui` file:

```
$C = "../Common.ui";
```

Then use templates with the `$C.@` prefix:

```
$C.@TextButton #MyButton {
    @Text = "Click Me";
    @Anchor = (Width: 140);
}
```

**Syntax notes:**
- Template properties use `@Property = value` (with `=`)
- Regular properties use `Property: value` (with `:`)

---

## Container Templates

### @PageOverlay

Semi-transparent overlay for modal dialogs.

```
$C.@PageOverlay {}
```

- Background: `#000000(0.45)` - 45% black opacity
- Use as first child to dim background

### @Container

Panel with title and content areas.

```
$C.@Container {
    @CloseButton = true;
    @ContentPadding = Padding(Full: 20);

    Group #Title {
        // Title content here
    }

    Group #Content {
        // Main content here
    }
}
```

| Property | Default | Description |
|----------|---------|-------------|
| `@CloseButton` | `false` | Show close button |
| `@ContentPadding` | `Padding(Full: 17)` | Content area padding |

### @DecoratedContainer

Fancy container with decorative elements.

```
$C.@DecoratedContainer {
    @CloseButton = true;
    Anchor: (Width: 650, Height: 720);

    Group #Title { }
    Group #Content { }
}
```

| Property | Default | Description |
|----------|---------|-------------|
| `@CloseButton` | `false` | Show close button |
| `@ContentPadding` | `Padding(Full: 17)` | Content area padding |

### @Panel

Simple background panel.

```
$C.@Panel {
    // Content
}
```

- Background: `Common/ContainerFullPatch.png` with Border: 20

---

## Button Templates

### @TextButton (Primary)

Primary action button. **Tested ✓**

```
$C.@TextButton #SubmitButton {
    @Text = "Submit";
    @Anchor = (Width: 140, Height: 40);
}
```

| Property | Default | Description |
|----------|---------|-------------|
| `@Text` | `""` | Button label |
| `@Anchor` | `Anchor()` | Size/position |
| `@Sounds` | `()` | Additional sounds |

Height: 44px (default)

### @SecondaryTextButton

Secondary action button.

```
$C.@SecondaryTextButton #CancelButton {
    @Text = "Cancel";
    @Anchor = (Width: 120);
}
```

Same properties as `@TextButton`.

### @TertiaryTextButton

Subtle/tertiary action button.

```
$C.@TertiaryTextButton #SkipButton {
    @Text = "Skip";
}
```

Same properties as `@TextButton`.

### @CancelTextButton

Destructive/cancel action button (different styling).

```
$C.@CancelTextButton #DeleteButton {
    @Text = "Delete";
}
```

Same properties as `@TextButton`.

### @SmallSecondaryTextButton

Smaller secondary button (32px height).

```
$C.@SmallSecondaryTextButton #SmallButton {
    @Text = "Small";
}
```

### @CloseButton

X button for closing dialogs.

```
$C.@CloseButton #Close {}
```

- Fixed size: 32x32
- Position: Top: -16, Right: -16

### @BackButton

Navigation back button.

```
$C.@BackButton #Back {}
```

- Position: Left: 50, Bottom: 50
- Size: 110x27

---

## Input Field Templates

### @TextField

Text input field. **Tested ✓**

```
$C.@TextField #NameInput {
    @Anchor = (Width: 300);
    PlaceholderText: "Enter name";
}
```

| Property | Default | Description |
|----------|---------|-------------|
| `@Anchor` | `Anchor()` | Size/position |

- Height: 38px (fixed)
- Value property: `#ElementId.Value` (String)

### @NumberField

Numeric input field. **Tested ✓**

```
$C.@NumberField #AgeInput {
    @Anchor = (Width: 100);
}
```

| Property | Default | Description |
|----------|---------|-------------|
| `@Anchor` | `Anchor()` | Size/position |

- Height: 38px (fixed)
- Value property: `#ElementId.Value` (Integer)

### @CheckBox

Boolean toggle. **Tested ✓**

```
$C.@CheckBox #EnabledCheckbox {}
```

- Fixed size: 22x22
- Value property: `#ElementId.Value` (Boolean)
- No built-in label (add separate Label element)

### @CheckBoxWithLabel

Checkbox with built-in label.

```
$C.@CheckBoxWithLabel #OptionCheckbox {
    @Text = "Enable this option";
    @Checked = false;
}
```

| Property | Default | Description |
|----------|---------|-------------|
| `@Text` | `""` | Label text |
| `@Checked` | `false` | Initial state |

### @DropdownBox

Selection dropdown. **Not yet tested**

```
$C.@DropdownBox #ModeDropdown {
    @Anchor = (Width: 200);
}
```

| Property | Default | Description |
|----------|---------|-------------|
| `@Anchor` | `Anchor()` | Size/position |

- Default size: 330x32
- Value property: `#ElementId.Value` (String)

---

## Label Templates

### @Title

Styled title label.

```
$C.@Title #PageTitle {
    @Text = "Settings";
    @Alignment = Center;
}
```

| Property | Default | Description |
|----------|---------|-------------|
| `@Text` | `""` | Title text |
| `@Alignment` | `Center` | Horizontal alignment |

Style: FontSize 15, Uppercase, Bold, Secondary font

### @Subtitle

Styled subtitle label.

```
$C.@Subtitle #SectionTitle {
    @Text = "General Options";
}
```

| Property | Default | Description |
|----------|---------|-------------|
| `@Text` | required | Subtitle text |

Style: FontSize 15, Uppercase, #96a9be color

### @TitleLabel

Large centered title.

```
$C.@TitleLabel #BigTitle {
    Text: "Welcome";
}
```

Style: FontSize 40, Center aligned

---

## Separator Templates

### @ContentSeparator

Simple 1px horizontal line.

```
$C.@ContentSeparator {
    @Anchor = (Top: 10);
}
```

- Height: 1px
- Color: #2b3542

### @PanelSeparatorFancy

Decorative separator with center icon.

```
$C.@PanelSeparatorFancy {}
```

- Height: 8px
- Decorative left line, center, right line

### @HeaderSeparator

Vertical separator for header tabs.

```
$C.@HeaderSeparator {}
```

- Size: 5x34
- Texture: `Common/HeaderTabSeparator.png`

### @VerticalSeparator

Decorative vertical separator.

```
$C.@VerticalSeparator {}
```

- Width: 6px
- Texture: `Common/ContainerVerticalSeparator.png`

---

## Utility Templates

### @DefaultSpinner

Loading animation sprite.

```
$C.@DefaultSpinner #Loading {
    @Anchor = (Top: 100);
}
```

| Property | Default | Description |
|----------|---------|-------------|
| `@Anchor` | `Anchor()` | Position |

- Size: 32x32
- 72 frames at 30 FPS

### @HeaderSearch

Collapsible search input.

```
$C.@HeaderSearch {
    @MarginRight = 10;
}
```

Contains `CompactTextField #SearchInput` that expands on focus.

### @ActionButtonContainer

Container for action buttons (right-aligned).

```
$C.@ActionButtonContainer {
    // Buttons here
}
```

- LayoutMode: Right
- Position: Right: 50, Bottom: 50

### @ActionButtonSeparator

Horizontal spacer between buttons.

```
$C.@ActionButtonSeparator {}
```

- Width: 35px

---

## Style Constants

| Constant | Value | Description |
|----------|-------|-------------|
| `@DefaultButtonHeight` | 44px | Standard button height |
| `@SmallButtonHeight` | 32px | Small button height |
| `@BigButtonHeight` | 48px | Large button height |
| `@ButtonBorder` | 12 | Button border size |
| `@ButtonPadding` | 24 | Button padding |
| `@DefaultButtonMinWidth` | 172 | Minimum button width |
| `@DropdownBoxHeight` | 32 | Dropdown height |
| `@ContentPaddingFull` | 17 | Content padding |
| `@TitleHeight` | 38 | Title area height |

---

## Color Reference

| Usage | Color |
|-------|-------|
| Default text | `#96a9be` |
| Title text | `#b4c8c9` |
| Placeholder text | `#6e7da1` |
| Separator line | `#2b3542` |
| Page overlay | `#000000(0.45)` |

---

## Element Types Used

| Type | Description |
|------|-------------|
| `Group` | Container element |
| `Label` | Text display |
| `Button` | Icon/square button |
| `TextButton` | Button with text label |
| `TextField` | Single-line text input |
| `NumberField` | Numeric input |
| `CheckBox` | Boolean toggle |
| `DropdownBox` | Selection list |
| `Slider` | Range selection |
| `Sprite` | Image/animation |
| `CompactTextField` | Collapsible text input |
| `ColorPicker` | Color selection |
| `FileDropdownBox` | File selection dropdown |

---

## Source

Templates extracted from:
`%APPDATA%\Hytale\install\release\package\game\build-5\Assets\Common\UI\Custom\Common.ui`

**Warning**: This is a server-provided asset that may change with game updates.
