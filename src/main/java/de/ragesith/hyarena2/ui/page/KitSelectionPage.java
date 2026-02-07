package de.ragesith.hyarena2.ui.page;

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
import de.ragesith.hyarena2.kit.KitAccessInfo;
import de.ragesith.hyarena2.kit.KitConfig;
import de.ragesith.hyarena2.ui.hud.HudManager;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Kit selection page with two-column layout:
 * - Left: scrollable list of kit buttons (dynamic templates)
 * - Right: detail panel showing selected kit info, items, and unlock status
 */
public class KitSelectionPage extends InteractiveCustomUIPage<KitSelectionPage.PageEventData> implements CloseablePage {

    private final PlayerRef playerRef;
    private final UUID playerUuid;
    private final List<KitAccessInfo> availableKits;
    private final HudManager hudManager;
    private final Consumer<String> onKitSelected;
    private final Runnable onBack;

    private int selectedIndex;

    public KitSelectionPage(PlayerRef playerRef, UUID playerUuid,
                            List<KitAccessInfo> availableKits, String currentKitId,
                            HudManager hudManager,
                            Consumer<String> onKitSelected, Runnable onBack) {
        super(playerRef, CustomPageLifetime.CanDismiss, PageEventData.CODEC);
        this.playerRef = playerRef;
        this.playerUuid = playerUuid;
        this.availableKits = new ArrayList<>(availableKits);
        this.availableKits.sort((a, b) -> a.getDisplayName().compareToIgnoreCase(b.getDisplayName()));
        this.hudManager = hudManager;
        this.onKitSelected = onKitSelected;
        this.onBack = onBack;

        // Find current selection index
        this.selectedIndex = 0;
        if (currentKitId != null) {
            for (int i = 0; i < availableKits.size(); i++) {
                if (availableKits.get(i).getKitId().equals(currentKitId)) {
                    this.selectedIndex = i;
                    break;
                }
            }
        }
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder cmd,
                      @Nonnull UIEventBuilder events, @Nonnull Store<EntityStore> store) {
        cmd.append("Pages/KitSelectionPage.ui");

        // Populate kit list (left column)
        for (int i = 0; i < availableKits.size(); i++) {
            KitAccessInfo info = availableKits.get(i);
            KitConfig kit = info.getKit();

            cmd.append("#KitList", "Pages/KitButton.ui");
            String row = "#KitList[" + i + "]";

            cmd.set(row + " #KitBtnName.Text", kit.getDisplayName());

            // Status icon and color
            if (info.isUnlocked()) {
                cmd.set(row + " #KitBtnStatus.Text", "");
            } else {
                cmd.set(row + " #KitBtnStatus.Text", "LOCKED");
                cmd.set(row + " #KitBtnStatus.Style.TextColor", "#e74c3c");
                cmd.set(row + " #KitBtnStatus.Style.FontSize", 9);
                cmd.set(row + " #KitBtnStatus.Style.RenderBold", true);
                cmd.set(row + " #KitBtnStatus.Style.RenderUppercase", true);
            }

            // Highlight selected kit
            if (i == selectedIndex) {
                cmd.set(row + " #KitBtnName.Style.TextColor", "#e8c872");
            }

            // Bind click event — row IS the #KitBtn root element
            events.addEventBinding(
                CustomUIEventBindingType.Activating,
                row,
                EventData.of("Action", "kit").append("Index", String.valueOf(i)),
                false
            );
        }

        // Populate detail panel (right column)
        populateDetail(cmd);

        // Bind bottom buttons
        events.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#SelectButton",
            EventData.of("Action", "select"),
            false
        );

        events.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#BackButton",
            EventData.of("Action", "back"),
            false
        );

        events.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#CloseButton",
            EventData.of("Action", "close"),
            false
        );

        // Register for cleanup
        hudManager.registerPage(playerUuid, this);
    }

    /**
     * Populates the right-side detail panel for the currently selected kit.
     */
    private void populateDetail(UICommandBuilder cmd) {
        if (availableKits.isEmpty() || selectedIndex < 0 || selectedIndex >= availableKits.size()) {
            cmd.set("#DetailName.Text", "No kits available");
            cmd.set("#DetailDescription.Text", "");
            cmd.set("#ItemsHeader.Visible", false);
            cmd.set("#UnlockSection.Visible", false);
            cmd.set("#SelectButton.Visible", false);
            return;
        }

        KitAccessInfo info = availableKits.get(selectedIndex);
        KitConfig kit = info.getKit();

        // Kit name
        cmd.set("#DetailName.Text", kit.getDisplayName());

        // Description
        String desc = kit.getDescription();
        cmd.set("#DetailDescription.Text", desc != null && !desc.isEmpty() ? desc : "No description available");

        // Items
        List<String> items = kit.getItems();
        Map<String, String> armor = kit.getArmor();
        String offhand = kit.getOffhand();

        boolean hasItems = !items.isEmpty() || !armor.isEmpty() || (offhand != null && !offhand.isEmpty());
        cmd.set("#ItemsHeader.Visible", hasItems);

        if (hasItems) {
            int itemIndex = 0;

            // Main items
            for (String itemEntry : items) {
                cmd.append("#ItemList", "Pages/KitItemRow.ui");
                String itemRow = "#ItemList[" + itemIndex + "]";
                cmd.set(itemRow + " #ItemName.Text", "\u2022 " + formatItemName(itemEntry));
                itemIndex++;
            }

            // Armor items
            for (Map.Entry<String, String> entry : armor.entrySet()) {
                cmd.append("#ItemList", "Pages/KitItemRow.ui");
                String itemRow = "#ItemList[" + itemIndex + "]";
                cmd.set(itemRow + " #ItemName.Text", "\u2022 " + formatArmorName(entry.getKey(), entry.getValue()));
                itemIndex++;
            }

            // Offhand
            if (offhand != null && !offhand.isEmpty()) {
                cmd.append("#ItemList", "Pages/KitItemRow.ui");
                String itemRow = "#ItemList[" + itemIndex + "]";
                cmd.set(itemRow + " #ItemName.Text", "\u2022 " + capitalize(offhand) + " (Offhand)");
            }
        }

        // Unlock section
        if (!info.isUnlocked()) {
            cmd.set("#UnlockSection.Visible", true);
            String hint = getUnlockHint(kit.getPermission());
            cmd.set("#UnlockText.Text", hint != null ? hint : "Locked");
            cmd.set("#SelectButton.Visible", false);
        } else {
            cmd.set("#UnlockSection.Visible", false);
            cmd.set("#SelectButton.Visible", true);
        }
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, PageEventData data) {
        try {
            if (data == null) {
                return;
            }

            Player player = store.getComponent(ref, Player.getComponentType());

            if (data.action == null) {
                return;
            }

            switch (data.action) {
                case "kit":
                    if (data.index != null) {
                        try {
                            int newIndex = Integer.parseInt(data.index);
                            if (newIndex >= 0 && newIndex < availableKits.size() && newIndex != selectedIndex) {
                                selectedIndex = newIndex;
                                rebuild();
                            }
                        } catch (NumberFormatException e) {
                            // Ignore invalid index
                        }
                    }
                    break;

                case "select":
                    if (selectedIndex >= 0 && selectedIndex < availableKits.size()) {
                        KitAccessInfo selected = availableKits.get(selectedIndex);
                        if (selected.isUnlocked()) {
                            shutdown();
                            if (onKitSelected != null) {
                                onKitSelected.accept(selected.getKitId());
                            }
                        }
                    }
                    break;

                case "back":
                    shutdown();
                    if (onBack != null) {
                        onBack.run();
                    }
                    break;

                case "close":
                    shutdown();
                    if (player != null) {
                        player.getPageManager().setPage(ref, store, Page.None);
                    }
                    break;
            }
        } catch (Exception e) {
            System.err.println("[KitSelectionPage] Error handling event: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Derives an unlock hint from the kit permission string.
     */
    static String getUnlockHint(String permission) {
        if (permission == null || permission.isEmpty()) {
            return null;
        }
        if (permission.equals("hyarena.classes.premium")) {
            return "Requires Premium membership";
        }
        if (permission.startsWith("hyarena.classes.rank.")) {
            String rank = permission.substring("hyarena.classes.rank.".length());
            return "Requires " + capitalize(rank) + " rank";
        }
        if (permission.startsWith("hyarena.classes.purchased.")) {
            return "Available for purchase in the store";
        }
        return "Locked";
    }

    /**
     * Formats an item entry like "iron_sword:1" into "Iron Sword x1".
     */
    private static String formatItemName(String itemEntry) {
        String itemId;
        int quantity = 1;

        if (itemEntry.contains(":")) {
            String[] parts = itemEntry.split(":");
            itemId = parts[0];
            try {
                quantity = Integer.parseInt(parts[1]);
            } catch (NumberFormatException e) {
                quantity = 1;
            }
        } else {
            itemId = itemEntry;
        }

        String name = capitalize(itemId.replace('_', ' '));
        return quantity > 1 ? name + " x" + quantity : name;
    }

    /**
     * Formats an armor entry like ("helmet", "iron_helmet") into "Iron Helmet (Helmet)".
     */
    private static String formatArmorName(String slot, String itemId) {
        String name = capitalize(itemId.replace('_', ' '));
        String slotName = capitalize(slot);
        return name + " (" + slotName + ")";
    }

    /**
     * Capitalizes the first letter of each word.
     */
    private static String capitalize(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        StringBuilder sb = new StringBuilder();
        boolean capitalizeNext = true;
        for (char c : text.toCharArray()) {
            if (c == ' ' || c == '_') {
                sb.append(' ');
                capitalizeNext = true;
            } else if (capitalizeNext) {
                sb.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
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
