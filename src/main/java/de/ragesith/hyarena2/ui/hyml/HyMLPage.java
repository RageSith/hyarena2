package de.ragesith.hyarena2.ui.hyml;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import de.ragesith.hyarena2.ui.hud.HudManager;
import de.ragesith.hyarena2.ui.page.CloseablePage;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Generic page renderer for HyML documents.
 * Generates the entire page structure as inline UI code so dimensions
 * from the HyML document are baked in at build time.
 */
public class HyMLPage extends InteractiveCustomUIPage<HyMLPage.PageEventData> implements CloseablePage {

    private static final String SCROLLBAR_STYLE =
        "ScrollbarStyle(Spacing: 6, Size: 6, " +
        "Background: (TexturePath: \"Common/Scrollbar.png\", Border: 3), " +
        "Handle: (TexturePath: \"Common/ScrollbarHandle.png\", Border: 3), " +
        "HoveredHandle: (TexturePath: \"Common/ScrollbarHandleHovered.png\", Border: 3), " +
        "DraggedHandle: (TexturePath: \"Common/ScrollbarHandleDragged.png\", Border: 3))";

    private static final String CLOSE_BTN_STYLE =
        "Default: (Background: \"Common/ContainerCloseButton.png\"), " +
        "Hovered: (Background: \"Common/ContainerCloseButtonHovered.png\"), " +
        "Pressed: (Background: \"Common/ContainerCloseButtonPressed.png\")";

    private static final String SECONDARY_BTN_STYLE =
        "ButtonStyle(" +
        "Default: (Background: PatchStyle(TexturePath: \"Common/Buttons/Secondary.png\", Border: 12)), " +
        "Hovered: (Background: PatchStyle(TexturePath: \"Common/Buttons/Secondary_Hovered.png\", Border: 12)), " +
        "Pressed: (Background: PatchStyle(TexturePath: \"Common/Buttons/Secondary_Pressed.png\", Border: 12)), " +
        "Disabled: (Background: PatchStyle(TexturePath: \"Common/Buttons/Disabled.png\", Border: 12)))";

    private final PlayerRef playerRef;
    private final UUID playerUuid;
    private final HyMLDocument document;
    private final HudManager hudManager;

    private int selectedSectionIndex = 0;

    public HyMLPage(PlayerRef playerRef, UUID playerUuid, HyMLDocument document, HudManager hudManager) {
        super(playerRef, CustomPageLifetime.CanDismiss, PageEventData.CODEC);
        this.playerRef = playerRef;
        this.playerUuid = playerUuid;
        this.document = document;
        this.hudManager = hudManager;
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder cmd,
                      @Nonnull UIEventBuilder events, @Nonnull Store<EntityStore> store) {
        cmd.append("Pages/HyMLPage.ui");

        // Generate the entire decorated container inline with baked dimensions
        String containerCode = buildContainerInline();
        cmd.appendInline("#HyMLRoot", containerCode);

        // Bind close button
        events.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#CloseButton",
            EventData.of("Action", "close"),
            false
        );

        if (document.isSectioned()) {
            buildSectioned(cmd, events);
        } else {
            buildSimple(cmd);
        }

        // Register with HudManager for proper cleanup
        hudManager.registerPage(playerUuid, this);
    }

    /**
     * Generates the full decorated container as inline UI code with document dimensions.
     */
    private String buildContainerInline() {
        int w = document.getWidth();
        int h = document.getHeight();
        String title = escapeUIString(document.getPageTitle());
        boolean sectioned = document.isSectioned();

        StringBuilder sb = new StringBuilder();

        // Outer container with dimensions
        sb.append("Group #HyMLContainer { Anchor: (Width: ").append(w).append(", Height: ").append(h).append(");");

        // Title bar (replicates @DecoratedContainer #Title)
        sb.append(" Group #Title {");
        sb.append("   Anchor: (Height: 38, Top: 0);");
        sb.append("   Background: (TexturePath: \"Common/ContainerHeader.png\", HorizontalBorder: 50, VerticalBorder: 0);");
        sb.append("   Padding: (Top: 7);");
        sb.append("   Group #ContainerDecorationTop { Anchor: (Width: 236, Height: 11, Top: -12); Background: \"Common/ContainerDecorationTop.png\"; }");
        sb.append("   Label #TitleLabel { Text: \"").append(title).append("\";");
        sb.append("     Style: (FontSize: 15, VerticalAlignment: Center, RenderUppercase: true, TextColor: #b4c8c9, FontName: \"Secondary\", RenderBold: true, LetterSpacing: 0, HorizontalAlignment: Center);");
        sb.append("     Padding: (Horizontal: 19);");
        sb.append("   }");
        sb.append(" }");

        // Content area (replicates @DecoratedContainer #Content)
        sb.append(" Group #Content {");
        sb.append("   LayoutMode: Top;");
        sb.append("   Anchor: (Top: 38, Bottom: 0);");
        sb.append("   Padding: (Full: 17);");
        sb.append("   Background: (TexturePath: \"Common/ContainerPatch.png\", Border: 23);");

        // Inner content layout
        sb.append("   Group #InnerContent {");
        sb.append("     Padding: (Full: 3);");
        sb.append("     FlexWeight: 1;");
        sb.append("     LayoutMode: Left;");

        if (sectioned) {
            // Sidebar
            sb.append("   Group #SectionSidebar { Anchor: (Width: 160); LayoutMode: Top;");
            sb.append("     Label #SectionHeader { Text: \"Sections\"; Anchor: (Height: 24);");
            sb.append("       Style: (FontSize: 14, TextColor: #e8c872, RenderBold: true, RenderUppercase: true); }");
            sb.append("     Group #SectionList { Anchor: (Top: 6); FlexWeight: 1; LayoutMode: TopScrolling;");
            sb.append("       ScrollbarStyle: ").append(SCROLLBAR_STYLE).append("; }");
            sb.append("   }");

            // Vertical separator
            sb.append("   Group #SectionSeparator {");
            sb.append("     Background: (TexturePath: \"Common/ContainerVerticalSeparator.png\");");
            sb.append("     Anchor: (Width: 6, Top: -2, Left: 12, Right: 12);");
            sb.append("   }");
        }

        // Main content area (scrollable)
        sb.append("   Group #ContentArea { FlexWeight: 1; LayoutMode: TopScrolling;");
        sb.append("     ScrollbarStyle: ").append(SCROLLBAR_STYLE).append("; }");

        sb.append("   }"); // InnerContent

        sb.append(" }"); // Content

        // Bottom decoration
        sb.append(" Group #ContainerDecorationBottom { Anchor: (Width: 236, Height: 11, Bottom: -6); Background: \"Common/ContainerDecorationBottom.png\"; }");

        // Close button
        sb.append(" Button #CloseButton { Anchor: (Width: 32, Height: 32, Top: -8, Right: -8); Style: (").append(CLOSE_BTN_STYLE).append("); }");

        sb.append(" }"); // HyMLContainer

        return sb.toString();
    }

    /**
     * Simple mode: compile all elements into the content area.
     */
    private void buildSimple(UICommandBuilder cmd) {
        String uiCode = compileElements(document.getElements());
        cmd.appendInline("#ContentArea", uiCode);
    }

    /**
     * Sectioned mode: populate sidebar with section buttons, compile selected section's content.
     */
    private void buildSectioned(UICommandBuilder cmd, UIEventBuilder events) {
        List<HyMLDocument.HyMLSection> sections = document.getSections();

        // Populate section buttons via inline code
        for (int i = 0; i < sections.size(); i++) {
            HyMLDocument.HyMLSection section = sections.get(i);
            boolean selected = (i == selectedSectionIndex);

            String btnCode = compileSectionButton(section.getName(), i, selected);
            cmd.appendInline("#SectionList", btnCode);

            // Bind click event on the button
            String selector = "#SectionList[" + i + "] #SectionSelect";
            events.addEventBinding(
                CustomUIEventBindingType.Activating,
                selector,
                EventData.of("Action", "section").append("Index", String.valueOf(i)),
                false
            );
        }

        // Compile selected section's content
        if (selectedSectionIndex >= 0 && selectedSectionIndex < sections.size()) {
            String uiCode = compileElements(sections.get(selectedSectionIndex).getElements());
            cmd.appendInline("#ContentArea", uiCode);
        }
    }

    /**
     * Compiles a section button as inline UI code.
     */
    private static String compileSectionButton(String name, int index, boolean selected) {
        StringBuilder sb = new StringBuilder();
        sb.append("Group { Anchor: (Height: 42, Top: 4); LayoutMode: Left;");

        // Indicator bar
        sb.append(" Group #SectionIndicator { Anchor: (Width: 3);");
        if (selected) {
            sb.append(" Visible: true; Background: #e8c872;");
        } else {
            sb.append(" Visible: false; Background: #e8c872;");
        }
        sb.append(" }");

        // Button
        sb.append(" Button #SectionSelect { FlexWeight: 1;");
        sb.append("   Style: ").append(SECONDARY_BTN_STYLE).append(";");
        sb.append("   Padding: (Horizontal: 12, Vertical: 6);");
        sb.append("   Label #SectionName { Text: \"").append(escapeUIString(name)).append("\"; FlexWeight: 1;");
        String textColor = selected ? "#e8c872" : "#b7cedd";
        sb.append("     Style: (FontSize: 14, TextColor: ").append(textColor).append(", VerticalAlignment: Center); }");
        sb.append(" }");

        sb.append(" }");
        return sb.toString();
    }

    /**
     * Compiles a list of HyML elements into a raw Hytale UI code string.
     */
    static String compileElements(List<HyMLDocument.HyMLElement> elements) {
        StringBuilder sb = new StringBuilder();
        sb.append("Group { LayoutMode: Top;");

        for (HyMLDocument.HyMLElement elem : elements) {
            sb.append(compileElement(elem));
        }

        sb.append(" }");
        return sb.toString();
    }

    /**
     * Compiles a single HyML element to UI code.
     */
    private static String compileElement(HyMLDocument.HyMLElement elem) {
        Map<String, String> attrs = elem.getAttributes();

        switch (elem.getTag()) {
            case "title":
                return compileLabel(elem.getTextContent(), attrs, 26, 4,
                    18, "#e8c872", true, true);

            case "heading":
                return compileLabel(elem.getTextContent(), attrs, 22, 10,
                    14, "#e8c872", true, false);

            case "text":
                return compileLabel(elem.getTextContent(), attrs, 21, 2,
                    13, "#b7cedd", false, false);

            case "bullet":
                String bulletText = "\u2022 " + (elem.getTextContent() != null ? elem.getTextContent() : "");
                return compileLabel(bulletText, attrs, 21, 2,
                    13, "#96a9be", false, false);

            case "stat":
                return compileStat(attrs);

            case "divider":
                return compileDivider(attrs);

            case "gap":
                return compileGap(attrs);

            default:
                return "";
        }
    }

    /**
     * Compiles a label element (title, heading, text, bullet).
     */
    private static String compileLabel(String text, Map<String, String> attrs,
                                       int defaultHeight, int defaultTop,
                                       int defaultSize, String defaultColor,
                                       boolean defaultBold, boolean defaultUppercase) {
        int top = defaultTop;
        int size = getIntAttr(attrs, "size", defaultSize);
        String color = attrs.getOrDefault("color", defaultColor);
        boolean bold = defaultBold || attrs.containsKey("bold");
        String align = attrs.get("align");
        boolean hasExplicitHeight = attrs.containsKey("height");

        StringBuilder sb = new StringBuilder();
        sb.append(" Label { Text: \"").append(escapeUIString(text)).append("\";");
        // Only set fixed height if explicitly specified — otherwise auto-size for wrapping text
        if (hasExplicitHeight) {
            sb.append(" Anchor: (Height: ").append(getIntAttr(attrs, "height", defaultHeight));
            sb.append(", Top: ").append(top).append(");");
        } else {
            sb.append(" Anchor: (Top: ").append(top).append(");");
        }
        sb.append(" Style: (FontSize: ").append(size);
        sb.append(", TextColor: ").append(color);
        if (bold) sb.append(", RenderBold: true");
        if (defaultUppercase) sb.append(", RenderUppercase: true");
        if (align != null) sb.append(", Alignment: ").append(capitalize(align));
        sb.append(", Wrap: true);");
        sb.append(" }");

        return sb.toString();
    }

    /**
     * Compiles a stat element (key-value row).
     */
    private static String compileStat(Map<String, String> attrs) {
        String label = attrs.getOrDefault("label", "");
        String value = attrs.getOrDefault("value", "");
        String labelColor = attrs.getOrDefault("color", "#7f8c8d");
        String valueColor = "#b7cedd";
        boolean hasExplicitHeight = attrs.containsKey("height");

        StringBuilder sb = new StringBuilder();
        if (hasExplicitHeight) {
            sb.append(" Group { Anchor: (Height: ").append(getIntAttr(attrs, "height", 24)).append("); LayoutMode: Left;");
        } else {
            sb.append(" Group { LayoutMode: Left;");
        }
        sb.append(" Label { Anchor: (Width: 140);");
        sb.append(" Text: \"").append(escapeUIString(label)).append("\";");
        sb.append(" Style: (FontSize: 13, TextColor: ").append(labelColor).append(", Wrap: true); }");
        sb.append(" Label { FlexWeight: 1;");
        sb.append(" Text: \"").append(escapeUIString(value)).append("\";");
        sb.append(" Style: (FontSize: 13, TextColor: ").append(valueColor).append(", RenderBold: true, Wrap: true); }");
        sb.append(" }");

        return sb.toString();
    }

    /**
     * Compiles a divider element.
     */
    private static String compileDivider(Map<String, String> attrs) {
        String color = attrs.getOrDefault("color", "#2b3542");
        return " Group { Anchor: (Height: 17); Padding: (Top: 8, Bottom: 8); Group { Anchor: (Height: 1); Background: " + color + "; } }";
    }

    /**
     * Compiles a gap element.
     */
    private static String compileGap(Map<String, String> attrs) {
        int height = getIntAttr(attrs, "height", 8);
        return " Group { Anchor: (Height: " + height + "); }";
    }

    // ========== Event Handling ==========

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, PageEventData data) {
        try {
            if (data == null || data.action == null) return;

            Player player = store.getComponent(ref, Player.getComponentType());

            switch (data.action) {
                case "close":
                    shutdown();
                    if (player != null) {
                        player.getPageManager().setPage(ref, store, Page.None);
                    }
                    break;

                case "section":
                    if (data.index != null && document.isSectioned()) {
                        try {
                            int newIndex = Integer.parseInt(data.index);
                            if (newIndex >= 0 && newIndex < document.getSections().size()
                                    && newIndex != selectedSectionIndex) {
                                selectedSectionIndex = newIndex;
                                rebuild();
                            }
                        } catch (NumberFormatException e) {
                            // Ignore invalid index
                        }
                    }
                    break;
            }
        } catch (Exception e) {
            System.err.println("[HyMLPage] Error handling event: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void onDismiss(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        shutdown();
    }

    @Override
    public void shutdown() {
        hudManager.unregisterPage(playerUuid, this);
    }

    @Override
    public void close() {
        shutdown();
    }

    // ========== Utilities ==========

    private static String escapeUIString(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static int getIntAttr(Map<String, String> attrs, String name, int defaultValue) {
        String val = attrs.get(name);
        if (val == null) return defaultValue;
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }

    // ========== Event Data ==========

    public static class PageEventData {
        public String action;
        public String index;

        public static final BuilderCodec<PageEventData> CODEC =
            BuilderCodec.builder(PageEventData.class, PageEventData::new)
                .append(new KeyedCodec<>("Action", Codec.STRING),
                    (d, v) -> d.action = v, d -> d.action).add()
                .append(new KeyedCodec<>("Index", Codec.STRING),
                    (d, v) -> d.index = v, d -> d.index).add()
                .build();
    }
}
