package de.ragesith.hyarena2.ui.page.admin;

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
import de.ragesith.hyarena2.shop.ShopCategory;
import de.ragesith.hyarena2.shop.ShopItem;
import de.ragesith.hyarena2.shop.ShopManager;
import de.ragesith.hyarena2.ui.hud.HudManager;
import de.ragesith.hyarena2.ui.page.CloseablePage;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Admin page listing all shop items with Edit/Delete/Create buttons.
 */
public class ShopListPage extends InteractiveCustomUIPage<ShopListPage.PageEventData> implements CloseablePage {

    private final PlayerRef playerRef;
    private final UUID playerUuid;
    private final ShopManager shopManager;
    private final HudManager hudManager;
    private final ScheduledExecutorService scheduler;
    private final Runnable onBack;

    private volatile boolean active = true;
    private List<ShopItem> itemList;
    private Map<String, Integer> itemIndexMap; // itemId -> child index in #ItemList
    private String pendingDeleteId;

    public ShopListPage(PlayerRef playerRef, UUID playerUuid,
                        ShopManager shopManager, HudManager hudManager,
                        ScheduledExecutorService scheduler, Runnable onBack) {
        super(playerRef, CustomPageLifetime.CanDismiss, PageEventData.CODEC);
        this.playerRef = playerRef;
        this.playerUuid = playerUuid;
        this.shopManager = shopManager;
        this.hudManager = hudManager;
        this.scheduler = scheduler;
        this.onBack = onBack;
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder cmd,
                      @Nonnull UIEventBuilder events, @Nonnull Store<EntityStore> store) {
        cmd.append("Pages/ShopListPage.ui");

        // Sort categories by sort value, then alphabetically
        List<ShopCategory> categories = new ArrayList<>(shopManager.getCategories());
        categories.sort((a, b) -> {
            int cmp = Integer.compare(a.getSort(), b.getSort());
            return cmp != 0 ? cmp : a.getDisplayName().compareToIgnoreCase(b.getDisplayName());
        });

        itemList = new ArrayList<>();
        itemIndexMap = new HashMap<>();
        int childIndex = 0;
        int totalItems = 0;

        for (ShopCategory cat : categories) {
            List<ShopItem> catItems = new ArrayList<>(cat.getItems());
            if (catItems.isEmpty()) continue;

            catItems.sort((a, b) -> {
                int cmp = Integer.compare(a.getSort(), b.getSort());
                return cmp != 0 ? cmp : a.getDisplayName().compareToIgnoreCase(b.getDisplayName());
            });

            // Category header
            cmd.append("#ItemList", "Pages/AdminShopCategoryHeader.ui");
            cmd.set("#ItemList[" + childIndex + "] #CategoryHeaderLabel.Text", cat.getDisplayName());
            childIndex++;

            // Items in this category
            for (ShopItem item : catItems) {
                itemList.add(item);

                cmd.append("#ItemList", "Pages/AdminShopItemRow.ui");
                String row = "#ItemList[" + childIndex + "]";
                itemIndexMap.put(item.getId(), childIndex);

                cmd.set(row + " #RowItemName.Text", item.getDisplayName());
                cmd.set(row + " #RowItemInfo.Text", item.getId() + " | " + item.getCost() + " AP | sort: " + item.getSort());

                events.addEventBinding(CustomUIEventBindingType.Activating, row + " #RowEditBtn",
                    EventData.of("Action", "edit").append("Item", item.getId()), false);

                if (item.getId().equals(pendingDeleteId)) {
                    cmd.set(row + " #RowDeleteBtn.Text", "Confirm?");
                }

                events.addEventBinding(CustomUIEventBindingType.Activating, row + " #RowDeleteBtn",
                    EventData.of("Action", "delete").append("Item", item.getId()), false);

                childIndex++;
                totalItems++;
            }
        }

        cmd.set("#ItemCountLabel.Text", totalItems + " items loaded");

        events.addEventBinding(CustomUIEventBindingType.Activating, "#ManageCategoriesBtn",
            EventData.of("Action", "categories"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#CreateNewBtn",
            EventData.of("Action", "create"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#BackBtn",
            EventData.of("Action", "back"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#CloseButton",
            EventData.of("Action", "close"), false);

        hudManager.registerPage(playerUuid, this);
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, PageEventData data) {
        try {
            if (data == null || data.action == null) return;

            Player player = store.getComponent(ref, Player.getComponentType());
            if (player == null) return;

            switch (data.action) {
                case "close":
                    player.getPageManager().setPage(ref, store, Page.None);
                    break;

                case "back":
                    shutdown();
                    if (onBack != null) onBack.run();
                    break;

                case "categories":
                    openCategoryList(ref, store, player);
                    break;

                case "create":
                    openEditor(ref, store, player, null);
                    break;

                case "edit":
                    if (data.item != null) {
                        ShopItem item = shopManager.getItem(data.item);
                        if (item != null) {
                            openEditor(ref, store, player, item);
                        } else {
                            showStatus("Item not found: " + data.item, "#e74c3c");
                        }
                    }
                    break;

                case "delete":
                    handleDelete(data.item);
                    break;
            }
        } catch (Exception e) {
            System.err.println("[ShopListPage] Error handling event: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void openEditor(Ref<EntityStore> ref, Store<EntityStore> store, Player player, ShopItem item) {
        shutdown();
        ShopItemEditorPage editorPage = new ShopItemEditorPage(
            playerRef, playerUuid, item, shopManager,
            hudManager, scheduler,
            this::reopenSelf
        );
        player.getPageManager().openCustomPage(ref, store, editorPage);
    }

    private void openCategoryList(Ref<EntityStore> ref, Store<EntityStore> store, Player player) {
        shutdown();
        ShopCategoryListPage page = new ShopCategoryListPage(
            playerRef, playerUuid, shopManager,
            hudManager, scheduler,
            this::reopenSelf
        );
        player.getPageManager().openCustomPage(ref, store, page);
    }

    private void handleDelete(String itemId) {
        if (itemId == null) return;

        if (!itemId.equals(pendingDeleteId)) {
            pendingDeleteId = itemId;
            Integer idx = itemIndexMap.get(itemId);
            if (idx != null) {
                UICommandBuilder cmd = new UICommandBuilder();
                cmd.set("#ItemList[" + idx + "] #RowDeleteBtn.Text", "Confirm?");
                safeSendUpdate(cmd);
            }
            return;
        }

        boolean deleted = shopManager.deleteItem(itemId);
        pendingDeleteId = null;

        if (deleted) {
            reopenSelf();
        } else {
            showStatus("Failed to delete item", "#e74c3c");
        }
    }

    private void showStatus(String message, String color) {
        UICommandBuilder cmd = new UICommandBuilder();
        cmd.set("#ItemCountLabel.Text", message);
        cmd.set("#ItemCountLabel.Style.TextColor", color);
        safeSendUpdate(cmd);
    }

    private void reopenSelf() {
        Ref<EntityStore> pRef = playerRef.getReference();
        if (pRef == null) return;
        Store<EntityStore> pStore = pRef.getStore();
        Player p = pStore.getComponent(pRef, Player.getComponentType());
        if (p == null) return;

        ShopListPage page = new ShopListPage(
            playerRef, playerUuid, shopManager,
            hudManager, scheduler, onBack
        );
        p.getPageManager().openCustomPage(pRef, pStore, page);
    }

    private void safeSendUpdate(UICommandBuilder cmd) {
        if (!active) return;
        try { sendUpdate(cmd, false); } catch (Exception e) { active = false; }
    }

    @Override
    public void onDismiss(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        shutdown();
    }

    @Override
    public void shutdown() {
        active = false;
        hudManager.unregisterPage(playerUuid, this);
    }

    @Override
    public void close() {
        shutdown();
    }

    public static class PageEventData {
        public String action;
        public String item;

        public static final BuilderCodec<PageEventData> CODEC =
            BuilderCodec.builder(PageEventData.class, PageEventData::new)
                .append(new KeyedCodec<>("Action", Codec.STRING),
                    (d, v) -> d.action = v, d -> d.action).add()
                .append(new KeyedCodec<>("Item", Codec.STRING),
                    (d, v) -> d.item = v, d -> d.item).add()
                .build();
    }
}
