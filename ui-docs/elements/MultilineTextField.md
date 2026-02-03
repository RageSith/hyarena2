# MultilineTextField

Multi-line text input field with scrolling support.

## Status

**Works with bug** - Functional but has rendering issues inside scrollable containers.

## Basic Syntax

```
MultilineTextField #MyMultiline {
    Anchor: (Width: 350, Height: 100);
    Value: "";
    PlaceholderText: "Enter text...";
    ScrollbarStyle: @DefaultScrollbarStyle;
    Background: @InputBoxBackground;
    Style: @DefaultInputFieldStyle;
    ContentPadding: (Horizontal: 10, Vertical: 8);
}
```

## Properties

| Property | Type | Description |
|----------|------|-------------|
| `Value` | String | Current text content |
| `PlaceholderText` | String | Placeholder when empty |
| `ScrollbarStyle` | Style ref | Scrollbar for long content |
| `Background` | Style ref | Input box background |
| `Style` | Style ref | Text styling |
| `ContentPadding` | Padding | Padding inside the field |
| `MaxLength` | Integer | Maximum character limit |
| `MaxVisibleLines` | Integer | Limit visible lines |
| `ReadOnly` | Boolean | Disable editing |
| `AutoGrow` | Boolean | Auto-expand with content |

## Java Usage

### Setting Value

```java
cmd.set("#MyMultiline.Value", "Line 1\nLine 2\nLine 3");
```

### Event Binding

```java
events.addEventBinding(CustomUIEventBindingType.ValueChanged,
    "#MyMultiline",
    EventData.of("@MultilineKey", "#MyMultiline.Value"),
    false);
```

### Codec

```java
// Use Codec.STRING
.append(new KeyedCodec<>("@MultilineKey", Codec.STRING),
    (d, v) -> d.multilineValue = v, d -> d.multilineValue).add()
```

### Reading Value

```java
if (data.multilineValue != null) {
    String text = data.multilineValue;
    // Contains newlines for multi-line content
}
```

## Known Bug

**Text doesn't follow scroll in TopScrolling containers**

When MultilineTextField is placed inside a `LayoutMode: TopScrolling` container, the text content stays visually fixed while the element frame scrolls. This causes the text to appear outside the element bounds when the parent container is scrolled.

Same bug affects: ProgressBar

### Workaround

Place MultilineTextField outside scrollable areas:

```
$C.@DecoratedContainer {
    // Scrollable content area
    Group #Content {
        Anchor: (Top: 38, Bottom: 120);  // Leave room at bottom
        LayoutMode: TopScrolling;
        // ... scrollable content ...
    }

    // MultilineTextField outside scroll area
    Group #InputSection {
        Anchor: (Bottom: 15, Height: 100, Left: 15, Right: 15);

        MultilineTextField #Notes {
            Anchor: (Width: 350, Height: 90);
            ScrollbarStyle: @DefaultScrollbarStyle;
            Background: @InputBoxBackground;
            Style: @DefaultInputFieldStyle;
            ContentPadding: (Horizontal: 10, Vertical: 8);
        }
    }
}
```

## Complete Example

### UI File

```
$C = "../Common.ui";

Group {
    $C.@DecoratedContainer {
        Anchor: (Width: 500, Height: 400);

        Group #Content {
            Anchor: (Top: 38, Bottom: 130);
            Padding: (Full: 15);
            LayoutMode: Top;

            Label { Text: "Other content here..."; }
        }

        // Outside scrollable area
        Group {
            Anchor: (Bottom: 15, Height: 100, Left: 15, Right: 15);
            LayoutMode: Left;

            Label {
                Text: "Notes:";
                Anchor: (Width: 60);
                Style: (FontSize: 14, TextColor: #96a9be);
            }

            MultilineTextField #Notes {
                Anchor: (Width: 350, Height: 90);
                PlaceholderText: "Enter notes...";
                ScrollbarStyle: @DefaultScrollbarStyle;
                Background: @InputBoxBackground;
                Style: @DefaultInputFieldStyle;
                ContentPadding: (Horizontal: 10, Vertical: 8);
            }
        }
    }
}
```

### Java File

```java
public class NotesPage extends InteractiveCustomUIPage<NotesPage.PageEventData> {

    private String notes = "";

    public NotesPage(PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismiss, PageEventData.CODEC);
    }

    @Override
    public void build(Ref<EntityStore> ref, UICommandBuilder cmd,
                      UIEventBuilder events, Store<EntityStore> store) {
        cmd.append("Pages/NotesPage.ui");

        cmd.set("#Notes.Value", notes);

        events.addEventBinding(CustomUIEventBindingType.ValueChanged,
            "#Notes", EventData.of("@Notes", "#Notes.Value"), false);
    }

    @Override
    public void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store,
                                PageEventData data) {
        if (data != null && data.notes != null) {
            notes = data.notes;
            // Don't rebuild - keeps focus
        }
    }

    public static class PageEventData {
        public String notes;

        public static final BuilderCodec<PageEventData> CODEC =
            BuilderCodec.builder(PageEventData.class, PageEventData::new)
                .append(new KeyedCodec<>("@Notes", Codec.STRING),
                    (d, v) -> d.notes = v, d -> d.notes).add()
                .build();
    }
}
```

## Differences from TextField

| Feature | TextField | MultilineTextField |
|---------|-----------|-------------------|
| Lines | Single line | Multiple lines |
| Enter key | May submit/close | Creates new line |
| ScrollbarStyle | Not applicable | For long content |
| Background | Template handles | Must specify |
| ContentPadding | Not applicable | Configurable |
| Template | `$C.@TextField` | None (use element directly) |

## Related

- [TextField](TextField.md) - Single-line input
- [ProgressBar](ProgressBar.md) - Has same scrollable container bug
