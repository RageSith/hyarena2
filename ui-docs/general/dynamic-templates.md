# Dynamic UI Templates

How to create reusable UI templates and add them programmatically at runtime.

## Overview

Dynamic templates allow you to:
- Define a reusable UI component once (e.g., a skill row, player entry, item card)
- Append multiple instances to a container from Java
- Set unique values on each instance using bracket notation

## Creating a Template

### 1. Define the Template File

Create a `.ui` file with a root element containing your component structure:

**`Pages/SkillRow.ui`**
```
Group #SkillRow {
    Anchor: (Height: 50);
    LayoutMode: Left;
    Padding: (Full: 8);
    Background: #1a2633;

    Label #SkillName {
        Text: "Skill";
        Anchor: (Width: 150);
        Style: (FontSize: 14, TextColor: #ffffff, VerticalAlignment: Center);
    }

    Label #SkillLevel {
        Text: "Lv. 1";
        Anchor: (Width: 60);
        Style: (FontSize: 14, TextColor: #4a9eff, RenderBold: true);
    }

    Group {
        Anchor: (Width: 150, Height: 8);
        Background: "../Common/ProgressBar.png";

        ProgressBar #SkillProgress {
            BarTexturePath: "../Common/ProgressBarFill.png";
            Value: 0.0;
        }
    }

    Label #SkillXp {
        Text: "0 / 100";
        Anchor: (Width: 100, Left: 10);
        Style: (FontSize: 12, TextColor: #96a9be);
    }
}
```

### 2. Create a Container in Your Page

**`Pages/MyPage.ui`**
```
Group #Content {
    LayoutMode: Top;
    Padding: (Full: 15);

    $C.@Subtitle { @Text = "Skills"; }

    // Container where templates will be appended
    Group #SkillList {
        LayoutMode: Top;
    }
}
```

## Appending Templates from Java

### Basic Pattern

```java
@Override
public void build(Ref<EntityStore> ref, UICommandBuilder cmd,
                  UIEventBuilder events, Store<EntityStore> store) {

    cmd.append("Pages/MyPage.ui");

    // Loop through your data
    for (int i = 0; i < items.size(); i++) {
        Item item = items.get(i);

        // 1. Append template to container
        cmd.append("#SkillList", "Pages/SkillRow.ui");

        // 2. Reference this instance with bracket notation
        String row = "#SkillList[" + i + "]";

        // 3. Set values on child elements
        cmd.set(row + " #SkillName.Text", item.name);
        cmd.set(row + " #SkillLevel.Text", "Lv. " + item.level);
        cmd.set(row + " #SkillXp.Text", item.xp + " / " + item.maxXp);

        // Set progress bar (0.0 to 1.0)
        double progress = (double) item.xp / item.maxXp;
        cmd.set(row + " #SkillProgress.Value", progress);
    }
}
```

### Bracket Notation Syntax

```java
// First appended item
"#Container[0]"

// Fifth appended item
"#Container[4]"

// Child element in nth item
"#Container[n] #ChildElement"

// Property on child element
"#Container[n] #ChildElement.Text"
"#Container[n] #ChildElement.Value"
"#Container[n] #ChildElement.Visible"

// Nested property
"#Container[n] #Element.Style.TextColor"
```

## Complete Example

### Data Class

```java
private static class SkillData {
    String name;
    int level;
    int xp;
    int maxXp;

    SkillData(String name, int level, int xp, int maxXp) {
        this.name = name;
        this.level = level;
        this.xp = xp;
        this.maxXp = maxXp;
    }
}
```

### Page Class

```java
public class DynamicListPage extends InteractiveCustomUIPage<DynamicListPage.PageEventData> {

    private List<SkillData> skills = new ArrayList<>();

    public DynamicListPage(PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismiss, PageEventData.CODEC);

        // Initialize data
        skills.add(new SkillData("Mining", 5, 450, 1000));
        skills.add(new SkillData("Woodcutting", 3, 200, 500));
        skills.add(new SkillData("Fishing", 7, 800, 1200));
    }

    @Override
    public void build(Ref<EntityStore> ref, UICommandBuilder cmd,
                      UIEventBuilder events, Store<EntityStore> store) {

        cmd.append("Pages/DynamicListPage.ui");

        // Append a row for each skill
        for (int i = 0; i < skills.size(); i++) {
            SkillData skill = skills.get(i);

            cmd.append("#SkillList", "Pages/SkillRow.ui");

            String row = "#SkillList[" + i + "]";
            cmd.set(row + " #SkillName.Text", skill.name);
            cmd.set(row + " #SkillLevel.Text", "Lv. " + skill.level);
            cmd.set(row + " #SkillXp.Text", skill.xp + " / " + skill.maxXp);
            cmd.set(row + " #SkillProgress.Value", (double) skill.xp / skill.maxXp);

            // Alternate row colors
            if (i % 2 == 1) {
                cmd.set(row + ".Background", "#232d3a");
            }
        }

        // Button events
        events.addEventBinding(CustomUIEventBindingType.Activating,
            "#AddBtn", EventData.of("Action", "add"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating,
            "#ClearBtn", EventData.of("Action", "clear"), false);
    }

    @Override
    public void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store,
                                PageEventData data) {
        if (data == null) return;

        if ("add".equals(data.action)) {
            skills.add(new SkillData("NewSkill", 1, 50, 100));
            rebuild();  // Rebuilds entire page with new data
        }

        if ("clear".equals(data.action)) {
            skills.clear();
            rebuild();
        }
    }

    public static class PageEventData {
        public String action;

        public static final BuilderCodec<PageEventData> CODEC =
            BuilderCodec.builder(PageEventData.class, PageEventData::new)
                .append(new KeyedCodec<>("Action", Codec.STRING),
                    (d, v) -> d.action = v, d -> d.action).add()
                .build();
    }
}
```

## Event Bindings on Dynamic Elements

You can bind events to elements inside dynamically appended templates:

```java
for (int i = 0; i < items.size(); i++) {
    cmd.append("#List", "Pages/ItemRow.ui");
    String row = "#List[" + i + "]";

    // Bind click event to button in this row
    events.addEventBinding(
        CustomUIEventBindingType.Activating,
        row + " #ActionBtn",
        EventData.of("Action", "select").append("Index", String.valueOf(i)),
        false
    );
}
```

Handle in `handleDataEvent`:

```java
if ("select".equals(data.action) && data.index != null) {
    int selectedIndex = Integer.parseInt(data.index);
    // Handle selection
}
```

## Nested Templates

You can append templates inside already-appended templates:

```java
// Append outer template
cmd.append("#TierList", "Pages/TierRow.ui");
String tierRow = "#TierList[" + tierIndex + "]";

// Append inner templates to a container inside the tier row
for (int c = 0; c < choices.size(); c++) {
    cmd.append(tierRow + " #ChoicesContainer", "Pages/ChoiceButton.ui");
    String btn = tierRow + " #ChoicesContainer[" + c + "]";
    cmd.set(btn + " #Btn.Text", choices.get(c).name);
}
```

## Clearing and Rebuilding

To update the dynamic list:

1. Modify your data (add/remove items)
2. Call `rebuild()`

The `rebuild()` method calls `build()` again, which loads the page fresh and appends all items based on current data.

```java
// Add item
skills.add(new SkillData("New", 1, 0, 100));
rebuild();

// Remove item
skills.remove(index);
rebuild();

// Clear all
skills.clear();
rebuild();
```

## Best Practices

1. **Keep templates small** - One component per file
2. **Use meaningful IDs** - `#SkillRow`, `#SkillName`, not `#Row1`, `#Label`
3. **Store data in instance variables** - Survives rebuild
4. **Alternate row colors** - Improves readability for lists
5. **Limit list size** - Very long lists may impact performance

## Limitations

- No partial updates - `rebuild()` refreshes the entire page
- Input focus lost on rebuild
- ProgressBar in scrollable containers has rendering bug

## Tested In

- `DynamicListPage.java` - Skill list with add/clear functionality
- `SkillRow.ui` - Reusable row template

## Related

- [UI Basics](ui-basics.md) - Syntax and layout
- [Interactive Pages](interactive-pages.md) - Event handling
