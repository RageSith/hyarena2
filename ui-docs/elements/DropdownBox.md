# DropdownBox

Selection dropdown for choosing from a list of options.

## Syntax

### With Common.ui Template (Recommended)

```
$C = "../Common.ui";

$C.@DropdownBox #ModeDropdown {
    @Anchor = (Width: 200);
}
```

### Without Template

```
DropdownBox #ModeDropdown {
    Anchor: (Width: 200, Height: 32);
}
```

## Attributes

| Attribute | Syntax | Description |
|-----------|--------|-------------|
| `@Anchor` | `= (Width: N)` | Size (template syntax) |
| `Anchor` | `(Width: N, Height: N)` | Size (non-template) |

Default height: 32px

## Properties

| Property | Type | Description |
|----------|------|-------------|
| `.Value` | String | Currently selected value |
| `.Entries` | List&lt;DropdownEntryInfo&gt; | Available options |

## Java Integration

### Required Imports

```java
import com.hypixel.hytale.server.core.ui.DropdownEntryInfo;
import com.hypixel.hytale.server.core.ui.LocalizableString;
import java.util.ArrayList;
```

### Populating Entries

```java
// Create entries list
var entries = new ArrayList<DropdownEntryInfo>();
String[] options = {"Option 1", "Option 2", "Option 3"};

for (int i = 0; i < options.length; i++) {
    entries.add(new DropdownEntryInfo(
        LocalizableString.fromString(options[i]),  // Display text
        String.valueOf(i)                           // Value (string)
    ));
}

// Set entries and selected value
cmd.set("#ModeDropdown.Entries", entries);
cmd.set("#ModeDropdown.Value", "0");  // Select first option
```

### Event Binding

```java
events.addEventBinding(
    CustomUIEventBindingType.ValueChanged,
    "#ModeDropdown",
    EventData.of("@ModeDropdown", "#ModeDropdown.Value"),
    false
);
```

### Handling Selection

```java
if (data.modeDropdown != null) {
    selectedMode = data.modeDropdown;  // Value is the string passed to DropdownEntryInfo
}
```

### Codec Field

```java
.append(new KeyedCodec<>("@ModeDropdown", Codec.STRING),
    (d, v) -> d.modeDropdown = v, d -> d.modeDropdown).add()
```

## Example Layout

```
Group #DropdownRow {
    LayoutMode: Left;
    Anchor: (Height: 45);

    Label {
        Text: "Game Mode";
        Anchor: (Width: 100);
        Style: (FontSize: 14, TextColor: #96a9be, VerticalAlignment: Center);
    }

    $C.@DropdownBox #ModeDropdown {
        @Anchor = (Width: 200);
    }
}
```

## Important Notes

- **Entries must be set from Java** - Cannot define options in .ui file
- **Value is a string** - Even if using numeric indices, the value is passed as string
- **DropdownEntryInfo** takes display text (LocalizableString) and value (String)

## Tested In

- `GeneralPage.ui` - Game mode selection with 4 options
