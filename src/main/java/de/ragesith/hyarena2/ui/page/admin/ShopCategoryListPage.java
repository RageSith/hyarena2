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
import de.ragesith.hyarena2.shop.ShopManager;
import de.ragesith.hyarena2.ui.hud.HudManager;
import de.ragesith.hyarena2.ui.page.CloseablePage;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Admin page listing all shop categories with Edit/Delete/Create buttons.
 */
public class ShopCategoryListPage extends InteractiveCustomUIPage<ShopCategoryListPage.PageEventData> implements CloseablePage {

    private final PlayerRef playerRef;
    private final UUID playerUuid;
    private final ShopManager shopManager;
    private final HudManager hudManager;
    private final ScheduledExecutorService scheduler;
    private final Runnable onBack;

    private volatile boolean active = true;
    private List<ShopCategory> categoryList;
    private String pendingDeleteId;

    public ShopCategoryListPage(PlayerRef playerRef, UUID playerUuid,
                                ShopManager shopManager, HudManager hudManager,
                                ScheduledExecutorService scheduler, Runnable onBack) {
        super(playerRef, CustomPageLifetime.CantClose, PageEventData.CODEC);
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
        cmd.append("Pages/ShopCategoryListPage.ui");

        categoryList = new ArrayList<>(shopManager.getCategories());
        categoryList.sort((a, b) -> {
            int cmp = Integer.compare(a.getSort(), b.getSort());
            return cmp != 0 ? cmp : a.getDisplayName().compareToIgnoreCase(b.getDisplayName());
        });

        cmd.set("#CategoryCountLabel.Text", categoryList.size() + " categories loaded");

        for (int i = 0; i < categoryList.size(); i++) {
            ShopCategory cat = categoryList.get(i);

            cmd.append("#CategoryList", "Pages/AdminShopCategoryRow.ui");
            String row = "#CategoryList[" + i + "]";

            cmd.set(row + " #RowCategoryName.Text", cat.getDisplayName());
            cmd.set(row + " #RowCategoryInfo.Text",
                cat.getId() + " | sort: " + cat.getSort() + " | " + cat.getItems().size() + " items");

            events.addEventBinding(CustomUIEventBindingType.Activating, row + " #RowEditBtn",
                EventData.of("Action", "edit").append("Category", cat.getId()), false);

            if (cat.getId().equals(pendingDeleteId)) {
                cmd.set(row + " #RowDeleteBtn.Text", "Confirm?");
            }

            events.addEventBinding(CustomUIEventBindingType.Activating, row + " #RowDeleteBtn",
                EventData.of("Action", "delete").append("Category", cat.getId()), false);
        }

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

                case "create":
                    openEditor(ref, store, player, null);
                    break;

                case "edit":
                    if (data.category != null) {
                        ShopCategory cat = shopManager.getCategory(data.category);
                        if (cat != null) {
                            openEditor(ref, store, player, cat);
                        } else {
                            showStatus("Category not found: " + data.category, "#e74c3c");
                        }
                    }
                    break;

                case "delete":
                    handleDelete(data.category);
                    break;
            }
        } catch (Exception e) {
            System.err.println("[ShopCategoryListPage] Error handling event: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void openEditor(Ref<EntityStore> ref, Store<EntityStore> store, Player player, ShopCategory category) {
        shutdown();
        ShopCategoryEditorPage editorPage = new ShopCategoryEditorPage(
            playerRef, playerUuid, category, shopManager,
            hudManager, scheduler,
            this::reopenSelf
        );
        player.getPageManager().openCustomPage(ref, store, editorPage);
    }

    private void handleDelete(String categoryId) {
        if (categoryId == null) return;

        if (!categoryId.equals(pendingDeleteId)) {
            pendingDeleteId = categoryId;
            for (int i = 0; i < categoryList.size(); i++) {
                if (categoryList.get(i).getId().equals(categoryId)) {
                    UICommandBuilder cmd = new UICommandBuilder();
                    cmd.set("#CategoryList[" + i + "] #RowDeleteBtn.Text", "Confirm?");
                    safeSendUpdate(cmd);
                    break;
                }
            }
            return;
        }

        // Check if category has items
        ShopCategory cat = shopManager.getCategory(categoryId);
        if (cat != null && !cat.getItems().isEmpty()) {
            pendingDeleteId = null;
            showStatus("Cannot delete: category has " + cat.getItems().size() + " items", "#e74c3c");
            reopenSelf();
            return;
        }

        boolean deleted = shopManager.deleteCategory(categoryId);
        pendingDeleteId = null;

        if (deleted) {
            reopenSelf();
        } else {
            showStatus("Failed to delete category", "#e74c3c");
        }
    }

    private void showStatus(String message, String color) {
        UICommandBuilder cmd = new UICommandBuilder();
        cmd.set("#CategoryCountLabel.Text", message);
        cmd.set("#CategoryCountLabel.Style.TextColor", color);
        safeSendUpdate(cmd);
    }

    private void reopenSelf() {
        Ref<EntityStore> pRef = playerRef.getReference();
        if (pRef == null) return;
        Store<EntityStore> pStore = pRef.getStore();
        Player p = pStore.getComponent(pRef, Player.getComponentType());
        if (p == null) return;

        ShopCategoryListPage page = new ShopCategoryListPage(
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
        public String category;

        public static final BuilderCodec<PageEventData> CODEC =
            BuilderCodec.builder(PageEventData.class, PageEventData::new)
                .append(new KeyedCodec<>("Action", Codec.STRING),
                    (d, v) -> d.action = v, d -> d.action).add()
                .append(new KeyedCodec<>("Category", Codec.STRING),
                    (d, v) -> d.category = v, d -> d.category).add()
                .build();
    }
}
