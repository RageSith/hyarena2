# HyML — HyArena Markup Language Reference

## Overview

HyML is a custom XML markup system that generates styled Hytale UI pages from `.hyml` files. It eliminates the need to write a Java class and `.ui` template for every content page (rules, help, info screens, patch notes, etc.). One method call parses a `.hyml` file, builds the page, and opens it for the player.

---

## File Locations

| Location | Method | Use Case |
|----------|--------|----------|
| `config/hyml/*.hyml` | `showHyMLPage()` | Server-operator editable pages |
| `src/main/resources/hyml/*.hyml` | `showHyMLResourcePage()` | Pages bundled inside the plugin JAR |

---

## Opening a Page (Java)

```java
// From config directory (server operator can edit)
Map<String, String> vars = new HashMap<>();
vars.put("player_name", player.getDisplayName());
vars.put("ap", String.valueOf(economyManager.getArenaPoints(uuid)));
vars.put("rank", honorManager.getRankDisplayName(uuid));
hudManager.showHyMLPage(uuid, "welcome.hyml", vars);

// From JAR resources (bundled with plugin)
hudManager.showHyMLResourcePage(uuid, "hyml/rules.hyml", vars);
```

The `vars` map is optional (pass `null` if no placeholders are needed).

---

## Page Syntax

### Simple Page

No `<section>` tags — renders as a single scrollable content pane.

```xml
<page title="Arena Rules" width="550" height="400">
  <title>Arena Rules</title>
  <text>Follow these rules at all times.</text>
  <divider/>
  <heading>General</heading>
  <bullet>No spawn camping</bullet>
  <bullet>No exploiting glitches</bullet>
  <bullet color="#e74c3c">Breaking rules = ban</bullet>
  <gap/>
  <stat label="Your Name" value="{player_name}"/>
  <stat label="Arena Points" value="{ap}" color="#f1c40f"/>
  <text align="center" size="11" color="#596673">Press ESC to close</text>
</page>
```

### Sectioned Page

Contains `<section>` tags — renders as a two-column layout with a sidebar on the left and section content on the right. Clicking a section button switches the content pane.

```xml
<page title="Help" width="700" height="480">
  <section name="Getting Started">
    <title>Welcome, {player_name}!</title>
    <text>Use /arena to open the menu.</text>
  </section>
  <section name="Commands">
    <stat label="/arena" value="Open arena menu"/>
    <stat label="/shop" value="Open the shop"/>
  </section>
</page>
```

Layout mode is **auto-detected**: if any `<section>` tags exist → sectioned, otherwise → simple.

---

## Page Attributes

Set on the `<page>` root element:

| Attribute | Default | Description |
|-----------|---------|-------------|
| `title` | `"Page"` | Window title shown in the decorated header bar |
| `width` | `600` | Page width in pixels |
| `height` | `450` | Page height in pixels |

---

## Elements

| Tag | Description | Default Style |
|-----|-------------|---------------|
| `<title>` | Big headline | 18px, bold, `#e8c872`, uppercase |
| `<heading>` | Section header | 14px, bold, `#e8c872` |
| `<text>` | Body paragraph | 13px, `#b7cedd` |
| `<bullet>` | List item (auto-prefixed with •) | 13px, `#96a9be` |
| `<stat label="" value=""/>` | Key-value row (label left, value right) | label: 13px `#7f8c8d`, value: 13px bold `#b7cedd` |
| `<divider/>` | Horizontal line with vertical padding | 1px line, `#2b3542`, 8px padding top and bottom |
| `<gap/>` | Vertical spacer | 8px height |

### Void Elements

`<divider/>` and `<gap/>` are self-closing — they have no text content.

### Text Content

All other elements accept text between their tags:

```xml
<text>This is the body text.</text>
<bullet>This is a list item.</bullet>
```

---

## Optional Attributes

These can be added to any text element (`<title>`, `<heading>`, `<text>`, `<bullet>`, `<stat>`):

| Attribute | Example | Description |
|-----------|---------|-------------|
| `color` | `color="#e74c3c"` | Override text color (hex) |
| `size` | `size="16"` | Override font size |
| `bold` | `bold` | Force bold rendering (flag, no value) |
| `align` | `align="center"` | Text alignment (left, center, right) |
| `height` | `height="30"` | Force a fixed element height (overrides auto-sizing) |

`<divider/>` also accepts `color` to change the line color.

`<gap/>` accepts `height` to change the spacer size.

**Note:** By default, text elements auto-size their height to fit wrapped text. Only set `height` if you explicitly need a fixed size.

---

## Placeholders

Use `{key}` in text content or attribute values. They are substituted from the `Map<String, String>` passed at render time.

```xml
<title>Welcome, {player_name}!</title>
<stat label="Arena Points" value="{ap}" color="#f1c40f"/>
```

Unresolved placeholders are left as-is (e.g. `{unknown}` will display literally).

---

## Examples

### Welcome Page (shown on join)

```xml
<page title="Welcome" width="500" height="380">
  <title>Welcome, {player_name}!</title>
  <text>Ready to fight? Open the arena menu and jump into a match.</text>
  <divider/>
  <stat label="Arena Points" value="{ap}" color="#f1c40f"/>
  <stat label="Rank" value="{rank}"/>
  <divider/>
  <heading>Quick Start</heading>
  <bullet>Use /arena or interact with the matchmaking statue</bullet>
  <bullet>Pick an arena, select a kit, and join the queue</bullet>
  <bullet>Win matches to earn Arena Points and climb the ranks</bullet>
  <gap/>
  <text color="#596673" size="11" align="center">Press ESC to close</text>
</page>
```

### Multi-Section Info Page

```xml
<page title="HyArena Info" width="700" height="480">
  <section name="Getting Started">
    <title>Arena Points</title>
    <text>Earn AP by playing matches. Spend them in the shop.</text>
    <divider/>
    <heading>How to Earn AP</heading>
    <bullet>Win a match: +15 AP</bullet>
    <bullet>Lose a match: +10 AP</bullet>
  </section>
  <section name="Ranks">
    <title>Honor Ranks</title>
    <stat label="Champion" value="1000+ Honor" color="#f1c40f"/>
    <stat label="Gladiator" value="600+ Honor" color="#9b59b6"/>
    <stat label="Warrior" value="300+ Honor" color="#3498db"/>
    <stat label="Apprentice" value="100+ Honor" color="#2ecc71"/>
    <stat label="Novice" value="0+ Honor" color="#7f8c8d"/>
  </section>
</page>
```

---

## Testing

Use the `/thyml` command (requires `hyarena.debug` permission):

```
/thyml welcome.hyml
/thyml info.hyml
/thyml test.hyml
/thyml test_sections.hyml
```

Automatically passes `{player_name}`, `{ap}`, and `{rank}` as variables.

---

## Current Pages

| File | Type | Shown When |
|------|------|------------|
| `welcome.hyml` | Simple | Fresh player join (after hub teleport) |
| `info.hyml` | Sectioned | Via `/thyml info.hyml` (or future info command) |
| `test.hyml` | Simple | Testing only |
| `test_sections.hyml` | Sectioned | Testing only |
