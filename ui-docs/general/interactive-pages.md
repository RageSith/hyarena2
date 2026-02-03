# Interactive Pages

How to create UI pages that respond to user interactions.

## Page Types

| Type | Use Case |
|------|----------|
| `BasicCustomUIPage` | Static display, no event handling |
| `InteractiveCustomUIPage<T>` | Handles user interactions (clicks, input changes) |

## InteractiveCustomUIPage Setup

### 1. Class Declaration

```java
public class MyPage extends InteractiveCustomUIPage<MyPage.PageEventData> {
```

### 2. Constructor

```java
public MyPage(PlayerRef playerRef) {
    super(playerRef, CustomPageLifetime.CanDismiss, PageEventData.CODEC);
}
```

**CustomPageLifetime options:**
- `CanDismiss` - Player can close with ESC
- `CantClose` - Cannot be dismissed
- `CanDismissOrCloseThroughInteraction` - ESC or UI button closes

### 3. Build Method

```java
@Override
public void build(Ref<EntityStore> ref,
                  UICommandBuilder cmd,
                  UIEventBuilder events,
                  Store<EntityStore> store) {
    cmd.append("Pages/MyPage.ui");

    // Set initial values
    cmd.set("#OutputLabel.Text", outputText);

    // Register event bindings
    events.addEventBinding(
        CustomUIEventBindingType.Activating,
        "#SubmitButton",
        EventData.of("Submit", "true"),
        false
    );
}
```

### 4. Handle Events

```java
@Override
public void handleDataEvent(Ref<EntityStore> ref,
                            Store<EntityStore> store,
                            PageEventData data) {
    if (data == null) return;

    if (data.submit != null) {
        outputText = "Button clicked!";
        rebuild();  // Refresh UI
    }
}
```

### 5. Event Data Class with Codec

```java
public static class PageEventData {
    public String submit;

    public static final BuilderCodec<PageEventData> CODEC =
        BuilderCodec.builder(PageEventData.class, PageEventData::new)
            .append(new KeyedCodec<>("Submit", Codec.STRING),
                (d, v) -> d.submit = v, d -> d.submit).add()
            .build();
}
```

## Event Binding Pattern

```java
events.addEventBinding(
    CustomUIEventBindingType.Activating,  // Event type
    "#ElementId",                          // UI element selector
    EventData.of("KeyName", "value"),      // Data to send
    false                                  // Unknown flag
);
```

**Event Types:**
- `Activating` - Button clicks
- `ValueChanged` - Input field changes
- Others: `RightClicking`, `DoubleClicking`, `MouseEntered`, `MouseExited`, `Dismissing`, `FocusGained`, `FocusLost`, `KeyDown`

## UICommandBuilder Methods

| Method | Description |
|--------|-------------|
| `cmd.append(path)` | Load UI file |
| `cmd.set(selector, value)` | Update element property |
| `cmd.clear(selector)` | Clear container contents |
| `cmd.remove(selector)` | Remove element |

**Selector syntax:** `#ElementId.Property` (e.g., `#OutputLabel.Text`)

## State Management Pattern

1. Store state in instance variables
2. Update state in `handleDataEvent()`
3. Call `rebuild()` only when needed (button clicks, not input changes)
4. In `build()`, set values from state using `cmd.set()`

```java
private String outputText = "Initial value";
private String textValue = "";

// In build() - restore all values:
cmd.set("#OutputLabel.Text", outputText);
cmd.set("#TextInput.Value", textValue);

// In handleDataEvent:
if (data.textInput != null) {
    textValue = data.textInput;
    // Do NOT rebuild here - causes focus loss
}
if (data.submit != null) {
    outputText = "Submitted: " + textValue;
    rebuild();  // Safe to rebuild on button click
}
```

## Focus Management

**Critical**: Do NOT call `rebuild()` on input field ValueChanged events.

- `rebuild()` refreshes the entire UI, causing input fields to lose focus
- Store input values without rebuilding
- Only `rebuild()` on explicit actions (button clicks, form submission)

## Required Imports

```java
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
```

## Codec Types

| Codec | Java Type | UI Element |
|-------|-----------|------------|
| `Codec.STRING` | `String` | TextField, DropdownBox, Button events |
| `Codec.INTEGER` | `Integer` | NumberField, Slider |
| `Codec.FLOAT` | `Float` | FloatSlider |
| `Codec.BOOLEAN` | `Boolean` | CheckBox |
| `Codec.DOUBLE` | `Double` | ProgressBar (when reading) |

## Complete Example

```java
public class MyPage extends InteractiveCustomUIPage<MyPage.PageEventData> {

    private String textValue = "";
    private Integer sliderValue = 50;
    private Boolean checkboxValue = false;

    public MyPage(PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismiss, PageEventData.CODEC);
    }

    @Override
    public void build(Ref<EntityStore> ref, UICommandBuilder cmd,
                      UIEventBuilder events, Store<EntityStore> store) {
        cmd.append("Pages/MyPage.ui");

        // Restore state
        cmd.set("#TextInput.Value", textValue);
        cmd.set("#Slider.Value", sliderValue);
        cmd.set("#Checkbox.Value", checkboxValue);

        // Event bindings
        events.addEventBinding(CustomUIEventBindingType.ValueChanged,
            "#TextInput", EventData.of("@Text", "#TextInput.Value"), false);
        events.addEventBinding(CustomUIEventBindingType.ValueChanged,
            "#Slider", EventData.of("@Slider", "#Slider.Value"), false);
        events.addEventBinding(CustomUIEventBindingType.ValueChanged,
            "#Checkbox", EventData.of("@Check", "#Checkbox.Value"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating,
            "#SubmitBtn", EventData.of("Submit", "true"), false);
    }

    @Override
    public void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store,
                                PageEventData data) {
        if (data == null) return;

        if (data.text != null) textValue = data.text;
        if (data.slider != null) sliderValue = data.slider;
        if (data.check != null) {
            checkboxValue = data.check;
            rebuild(); // Checkbox needs rebuild to update visual
        }
        if (data.submit != null) {
            // Process form
            rebuild();
        }
    }

    public static class PageEventData {
        public String text;
        public Integer slider;
        public Boolean check;
        public String submit;

        public static final BuilderCodec<PageEventData> CODEC =
            BuilderCodec.builder(PageEventData.class, PageEventData::new)
                .append(new KeyedCodec<>("@Text", Codec.STRING),
                    (d, v) -> d.text = v, d -> d.text).add()
                .append(new KeyedCodec<>("@Slider", Codec.INTEGER),
                    (d, v) -> d.slider = v, d -> d.slider).add()
                .append(new KeyedCodec<>("@Check", Codec.BOOLEAN),
                    (d, v) -> d.check = v, d -> d.check).add()
                .append(new KeyedCodec<>("Submit", Codec.STRING),
                    (d, v) -> d.submit = v, d -> d.submit).add()
                .build();
    }
}
```

## Opening a Page

```java
// In your command or event handler
PlayerRef playerRef = ...; // Get from event
Store<EntityStore> store = ...; // Get from context

MyPage page = new MyPage(playerRef);
page.open(store);
```

## Tested In

- `GeneralPage.java` - Full showcase with all input types
