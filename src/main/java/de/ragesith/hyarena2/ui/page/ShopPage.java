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
import de.ragesith.hyarena2.economy.EconomyManager;
import de.ragesith.hyarena2.economy.HonorManager;
import de.ragesith.hyarena2.economy.PlayerEconomyData;
import de.ragesith.hyarena2.shop.PurchaseResult;
import de.ragesith.hyarena2.shop.ShopCategory;
import de.ragesith.hyarena2.shop.ShopItem;
import de.ragesith.hyarena2.shop.ShopManager;
import de.ragesith.hyarena2.ui.hud.HudManager;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Player-facing shop page with two-column layout:
 * - Left: category sidebar with "My Items" at bottom
 * - Right: header bar (AP, Rank, Honor) + scrollable card grid (2 per row)
 * - Two-click purchase confirmation flow
 */
public class ShopPage extends InteractiveCustomUIPage<ShopPage.PageEventData> implements CloseablePage {

    private final PlayerRef playerRef;
    private final UUID playerUuid;
    private final ShopManager shopManager;
    private final EconomyManager economyManager;
    private final HonorManager honorManager;
    private final HudManager hudManager;
    private final ScheduledExecutorService scheduler;

    // Category entries: real categories sorted by sort value + "My Items" sentinel
    private final List<CategoryEntry> categoryEntries;
    private int selectedCategoryIndex;

    // Two-click purchase state
    private String pendingPurchaseId;

    // Maps itemId -> row selector prefix (e.g. "#CardGrid[0] #Card0") for live updates
    private Map<String, String> itemSelectorMap;

    // Auto-refresh
    private ScheduledFuture<?> refreshTask;
    private volatile boolean active = true;
    private int lastAP;

    public ShopPage(PlayerRef playerRef, UUID playerUuid,
                    ShopManager shopManager, EconomyManager economyManager,
                    HonorManager honorManager, HudManager hudManager,
                    ScheduledExecutorService scheduler) {
        this(playerRef, playerUuid, shopManager, economyManager, honorManager, hudManager, scheduler, 0);
    }

    public ShopPage(PlayerRef playerRef, UUID playerUuid,
                    ShopManager shopManager, EconomyManager economyManager,
                    HonorManager honorManager, HudManager hudManager,
                    ScheduledExecutorService scheduler, int selectedCategoryIndex) {
        super(playerRef, CustomPageLifetime.CantClose, PageEventData.CODEC);
        this.playerRef = playerRef;
        this.playerUuid = playerUuid;
        this.shopManager = shopManager;
        this.economyManager = economyManager;
        this.honorManager = honorManager;
        this.hudManager = hudManager;
        this.scheduler = scheduler;

        // Build category entries: sorted real categories + "My Items"
        this.categoryEntries = new ArrayList<>();
        List<ShopCategory> sorted = new ArrayList<>(shopManager.getCategories());
        sorted.sort(Comparator.comparingInt(ShopCategory::getSort)
            .thenComparing(ShopCategory::getDisplayName));
        for (ShopCategory cat : sorted) {
            categoryEntries.add(new CategoryEntry(cat.getId(), cat.getDisplayName(), false));
        }
        categoryEntries.add(new CategoryEntry("MY_ITEMS", "My Items", true));

        // Clamp index
        if (selectedCategoryIndex < 0 || selectedCategoryIndex >= categoryEntries.size()) {
            selectedCategoryIndex = 0;
        }
        this.selectedCategoryIndex = selectedCategoryIndex;
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder cmd,
                      @Nonnull UIEventBuilder events, @Nonnull Store<EntityStore> store) {
        itemSelectorMap = new HashMap<>();
        pendingPurchaseId = null;

        cmd.append("Pages/ShopPage.ui");

        // Header bar
        int ap = economyManager.getArenaPoints(playerUuid);
        lastAP = ap;
        cmd.set("#HeaderAP.Text", ap + " AP");

        String rankName = honorManager.getRankDisplayName(playerUuid);
        String rankColor = honorManager.getRankColor(playerUuid);
        cmd.set("#HeaderRank.Text", rankName);
        cmd.set("#HeaderRank.Style.TextColor", rankColor);

        int honor = (int) economyManager.getHonor(playerUuid);
        cmd.set("#HeaderHonor.Text", honor + " Honor");

        // Category sidebar
        for (int i = 0; i < categoryEntries.size(); i++) {
            CategoryEntry entry = categoryEntries.get(i);

            cmd.append("#CategoryList", "Pages/ShopCategoryButton.ui");
            String row = "#CategoryList[" + i + "]";

            cmd.set(row + " #CatBtnName.Text", entry.displayName);

            // Highlight selected category with gold text
            if (i == selectedCategoryIndex) {
                cmd.set(row + " #CatBtnName.Style.TextColor", "#e8c872");
            }

            events.addEventBinding(
                CustomUIEventBindingType.Activating,
                row + " #CatBtnSelect",
                EventData.of("Action", "cat").append("Index", String.valueOf(i)),
                false
            );
        }

        // Selected category header
        CategoryEntry selected = categoryEntries.get(selectedCategoryIndex);
        cmd.set("#SelectedCatName.Text", selected.displayName);

        // Build item grid
        int gridChildIndex = 0;
        if (selected.isMyItems) {
            gridChildIndex = buildMyItemsGrid(cmd, events, ap);
        } else {
            gridChildIndex = buildCategoryGrid(cmd, events, selected.id, ap);
        }

        // Show/hide no items notice
        cmd.set("#NoItemsNotice.Visible", gridChildIndex == 0);

        // Bind close button
        events.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#CloseButton",
            EventData.of("Action", "close"),
            false
        );

        hudManager.registerPage(playerUuid, this);
        startAutoRefresh();
    }

    /**
     * Builds the card grid for a specific category.
     * Returns the number of card pair rows added.
     */
    private int buildCategoryGrid(UICommandBuilder cmd, UIEventBuilder events, String categoryId, int playerAP) {
        List<ShopItem> items = new ArrayList<>(shopManager.getItemsByCategory(categoryId));
        items.sort(Comparator.comparingInt(ShopItem::getSort)
            .thenComparing(ShopItem::getDisplayName));
        return buildCardPairs(cmd, events, items, playerAP, false, 0);
    }

    /**
     * Builds the "My Items" grid: owned one-time items grouped by category with gold headers.
     * Returns the number of card pair rows added.
     */
    private int buildMyItemsGrid(UICommandBuilder cmd, UIEventBuilder events, int playerAP) {
        PlayerEconomyData data = economyManager.getPlayerData(playerUuid);
        if (data == null || data.getPurchasedItems().isEmpty()) {
            return 0;
        }

        List<String> purchasedIds = data.getPurchasedItems();

        // Sort categories by sort then alpha
        List<ShopCategory> sortedCats = new ArrayList<>(shopManager.getCategories());
        sortedCats.sort(Comparator.comparingInt(ShopCategory::getSort)
            .thenComparing(ShopCategory::getDisplayName));

        int gridChildIndex = 0;
        for (ShopCategory cat : sortedCats) {
            // Filter to owned one-time items in this category
            List<ShopItem> ownedItems = new ArrayList<>();
            for (ShopItem item : cat.getItems()) {
                if (item.isOneTimePurchase() && purchasedIds.contains(item.getId())) {
                    ownedItems.add(item);
                }
            }
            if (ownedItems.isEmpty()) {
                continue;
            }

            ownedItems.sort(Comparator.comparingInt(ShopItem::getSort)
                .thenComparing(ShopItem::getDisplayName));

            // Append category header
            cmd.append("#CardGrid", "Pages/AdminShopCategoryHeader.ui");
            String headerRow = "#CardGrid[" + gridChildIndex + "]";
            cmd.set(headerRow + " #CategoryHeaderLabel.Text", cat.getDisplayName());
            gridChildIndex++;

            // Append card pairs for this category's owned items
            gridChildIndex = buildCardPairs(cmd, events, ownedItems, playerAP, true, gridChildIndex);
        }

        return gridChildIndex;
    }

    /**
     * Builds card pair rows from a list of items.
     * @param forceOwned If true, all items are shown as owned (for "My Items" tab)
     * @param startIndex The starting child index in #CardGrid
     * @return The next child index after all pairs are added
     */
    private int buildCardPairs(UICommandBuilder cmd, UIEventBuilder events,
                               List<ShopItem> items, int playerAP,
                               boolean forceOwned, int startIndex) {
        int gridChildIndex = startIndex;
        for (int i = 0; i < items.size(); i += 2) {
            cmd.append("#CardGrid", "Pages/ShopCardPair.ui");
            String row = "#CardGrid[" + gridChildIndex + "]";
            gridChildIndex++;

            // Card 0 (always present)
            populateCard(cmd, events, row, "0", items.get(i), playerAP, forceOwned);

            // Card 1 (if exists)
            if (i + 1 < items.size()) {
                cmd.set(row + " #Card1.Visible", true);
                populateCard(cmd, events, row, "1", items.get(i + 1), playerAP, forceOwned);
            }
        }
        return gridChildIndex;
    }

    /**
     * Populates a single card slot within a card pair row.
     */
    private void populateCard(UICommandBuilder cmd, UIEventBuilder events,
                              String rowSel, String slot, ShopItem item,
                              int playerAP, boolean forceOwned) {
        String prefix = "Card" + slot;
        cmd.set(rowSel + " #" + prefix + "Name.Text", item.getDisplayName());
        cmd.set(rowSel + " #" + prefix + "Desc.Text",
            item.getDescription() != null ? item.getDescription() : "");
        cmd.set(rowSel + " #" + prefix + "Cost.Text", item.getCost() + " AP");

        boolean owned = forceOwned || (item.isOneTimePurchase() && shopManager.ownsItem(playerUuid, item.getId()));

        if (owned) {
            cmd.set(rowSel + " #" + prefix + "Cost.Text", "Owned");
            cmd.set(rowSel + " #" + prefix + "Cost.Style.TextColor", "#2ecc71");
            cmd.set(rowSel + " #" + prefix + "BuyBtn.Visible", false);
        } else {
            if (playerAP < item.getCost()) {
                cmd.set(rowSel + " #" + prefix + "Cost.Style.TextColor", "#e74c3c");
            }
            events.addEventBinding(
                CustomUIEventBindingType.Activating,
                rowSel + " #" + prefix + "BuyBtn",
                EventData.of("Action", "buy").append("Item", item.getId()),
                false
            );
        }

        itemSelectorMap.put(item.getId(), rowSel + " #" + prefix);
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, PageEventData data) {
        try {
            if (data == null || data.action == null) {
                return;
            }

            Player player = store.getComponent(ref, Player.getComponentType());

            switch (data.action) {
                case "close":
                    stopAutoRefresh();
                    if (player != null) {
                        player.getPageManager().setPage(ref, store, Page.None);
                    }
                    break;

                case "cat":
                    if (data.index != null) {
                        try {
                            int newIndex = Integer.parseInt(data.index);
                            if (newIndex >= 0 && newIndex < categoryEntries.size() && newIndex != selectedCategoryIndex) {
                                selectedCategoryIndex = newIndex;
                                pendingPurchaseId = null;
                                stopAutoRefresh();
                                active = true;
                                rebuild();
                            }
                        } catch (NumberFormatException e) {
                            // Ignore invalid index
                        }
                    }
                    break;

                case "buy":
                    if (data.item != null) {
                        handleBuy(data.item);
                    }
                    break;
            }
        } catch (Exception e) {
            System.err.println("[ShopPage] Error handling event: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Two-click purchase flow:
     * First click: changes button to "Confirm? (X AP)"
     * Second click: executes purchase
     */
    private void handleBuy(String itemId) {
        if (itemId.equals(pendingPurchaseId)) {
            // Second click - execute purchase
            pendingPurchaseId = null;

            PurchaseResult result = shopManager.purchase(playerUuid, itemId);
            switch (result) {
                case SUCCESS:
                    showStatus("#2ecc71", "Purchase successful!");
                    // Rebuild to update AP, owned state
                    stopAutoRefresh();
                    active = true;
                    rebuild();
                    break;

                case INSUFFICIENT_FUNDS:
                    showStatus("#e74c3c", "Insufficient AP!");
                    resetBuyButton(itemId);
                    break;

                case ALREADY_OWNED:
                    showStatus("#f1c40f", "Already owned!");
                    resetBuyButton(itemId);
                    break;

                case ITEM_NOT_FOUND:
                    showStatus("#e74c3c", "Item not found!");
                    resetBuyButton(itemId);
                    break;
            }
        } else {
            // First click - show confirmation
            // Reset previous pending button if any
            if (pendingPurchaseId != null) {
                resetBuyButton(pendingPurchaseId);
            }

            pendingPurchaseId = itemId;

            ShopItem item = shopManager.getItem(itemId);
            if (item == null) return;

            String selector = itemSelectorMap.get(itemId);
            if (selector == null) return;

            UICommandBuilder cmd = new UICommandBuilder();
            cmd.set(selector + "BuyBtn.Text", "Confirm? (" + item.getCost() + " AP)");
            safeSendUpdate(cmd);
        }
    }

    /**
     * Resets a buy button back to "Buy" text.
     */
    private void resetBuyButton(String itemId) {
        String selector = itemSelectorMap.get(itemId);
        if (selector == null) return;

        UICommandBuilder cmd = new UICommandBuilder();
        cmd.set(selector + "BuyBtn.Text", "Buy");
        safeSendUpdate(cmd);
    }

    /**
     * Shows a temporary status message in the header bar.
     */
    private void showStatus(String color, String message) {
        UICommandBuilder cmd = new UICommandBuilder();
        cmd.set("#StatusMsg.Visible", true);
        cmd.set("#StatusMsg.Text", message);
        cmd.set("#StatusMsg.Style.TextColor", color);
        safeSendUpdate(cmd);

        // Auto-hide after 3 seconds
        if (scheduler != null) {
            scheduler.schedule(() -> {
                if (!active) return;
                UICommandBuilder hideCmd = new UICommandBuilder();
                hideCmd.set("#StatusMsg.Visible", false);
                safeSendUpdate(hideCmd);
            }, 3, TimeUnit.SECONDS);
        }
    }

    /**
     * Starts auto-refresh for AP balance in the header (every 2s).
     */
    private void startAutoRefresh() {
        if (refreshTask != null || scheduler == null) {
            return;
        }

        refreshTask = scheduler.scheduleAtFixedRate(() -> {
            if (!active) return;

            try {
                int currentAP = economyManager.getArenaPoints(playerUuid);
                if (currentAP != lastAP) {
                    lastAP = currentAP;
                    UICommandBuilder cmd = new UICommandBuilder();
                    cmd.set("#HeaderAP.Text", currentAP + " AP");
                    safeSendUpdate(cmd);
                }
            } catch (Exception e) {
                // Page might be closed
            }
        }, 2, 2, TimeUnit.SECONDS);
    }

    private void safeSendUpdate(UICommandBuilder cmd) {
        if (!active) return;
        try {
            sendUpdate(cmd, false);
        } catch (Exception e) {
            active = false;
        }
    }

    private void stopAutoRefresh() {
        active = false;
        if (refreshTask != null) {
            refreshTask.cancel(false);
            refreshTask = null;
        }
    }

    @Override
    public void onDismiss(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        shutdown();
    }

    @Override
    public void shutdown() {
        stopAutoRefresh();
        hudManager.unregisterPage(playerUuid, this);
    }

    @Override
    public void close() {
        shutdown();
    }

    private static class CategoryEntry {
        final String id;
        final String displayName;
        final boolean isMyItems;

        CategoryEntry(String id, String displayName, boolean isMyItems) {
            this.id = id;
            this.displayName = displayName;
            this.isMyItems = isMyItems;
        }
    }

    public static class PageEventData {
        public String action;
        public String index;
        public String item;

        public static final BuilderCodec<PageEventData> CODEC =
            BuilderCodec.builder(PageEventData.class, PageEventData::new)
                .append(new KeyedCodec<>("Action", Codec.STRING),
                    (d, v) -> d.action = v, d -> d.action).add()
                .append(new KeyedCodec<>("Index", Codec.STRING),
                    (d, v) -> d.index = v, d -> d.index).add()
                .append(new KeyedCodec<>("Item", Codec.STRING),
                    (d, v) -> d.item = v, d -> d.item).add()
                .build();
    }
}
