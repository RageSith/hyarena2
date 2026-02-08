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
import de.ragesith.hyarena2.arena.MatchManager;
import de.ragesith.hyarena2.config.ConfigManager;
import de.ragesith.hyarena2.hub.HubManager;
import de.ragesith.hyarena2.kit.KitConfig;
import de.ragesith.hyarena2.kit.KitManager;
import de.ragesith.hyarena2.ui.hud.HudManager;
import de.ragesith.hyarena2.ui.page.CloseablePage;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Admin page listing all kits with Edit/Delete/Create buttons.
 */
public class KitListPage extends InteractiveCustomUIPage<KitListPage.PageEventData> implements CloseablePage {

    private final PlayerRef playerRef;
    private final UUID playerUuid;
    private final KitManager kitManager;
    private final MatchManager matchManager;
    private final HubManager hubManager;
    private final ConfigManager configManager;
    private final HudManager hudManager;
    private final ScheduledExecutorService scheduler;
    private final Runnable onBack;

    private volatile boolean active = true;
    private List<KitConfig> kitList;
    private String pendingDeleteId;

    public KitListPage(PlayerRef playerRef, UUID playerUuid,
                       KitManager kitManager, MatchManager matchManager,
                       HubManager hubManager, ConfigManager configManager,
                       HudManager hudManager, ScheduledExecutorService scheduler,
                       Runnable onBack) {
        super(playerRef, CustomPageLifetime.CantClose, PageEventData.CODEC);
        this.playerRef = playerRef;
        this.playerUuid = playerUuid;
        this.kitManager = kitManager;
        this.matchManager = matchManager;
        this.hubManager = hubManager;
        this.configManager = configManager;
        this.hudManager = hudManager;
        this.scheduler = scheduler;
        this.onBack = onBack;
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder cmd,
                      @Nonnull UIEventBuilder events, @Nonnull Store<EntityStore> store) {
        cmd.append("Pages/KitListPage.ui");

        kitList = kitManager.getAllKits();
        kitList.sort((a, b) -> a.getDisplayName().compareToIgnoreCase(b.getDisplayName()));

        cmd.set("#KitCountLabel.Text", kitList.size() + " kits loaded");

        for (int i = 0; i < kitList.size(); i++) {
            KitConfig kit = kitList.get(i);

            cmd.append("#KitList", "Pages/AdminKitRow.ui");
            String row = "#KitList[" + i + "]";

            cmd.set(row + " #RowKitName.Text", kit.getDisplayName());
            String permInfo = kit.requiresPermission() ? kit.getPermission() : "No permission";
            cmd.set(row + " #RowKitInfo.Text", kit.getId() + " | " + permInfo);

            events.addEventBinding(CustomUIEventBindingType.Activating, row + " #RowEditBtn",
                EventData.of("Action", "edit").append("Kit", kit.getId()), false);

            if (kit.getId().equals(pendingDeleteId)) {
                cmd.set(row + " #RowDeleteBtn.Text", "Confirm?");
            }

            events.addEventBinding(CustomUIEventBindingType.Activating, row + " #RowDeleteBtn",
                EventData.of("Action", "delete").append("Kit", kit.getId()), false);
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
                    if (data.kit != null) {
                        KitConfig kit = kitManager.getKit(data.kit);
                        if (kit != null) {
                            openEditor(ref, store, player, kit);
                        } else {
                            showStatus("Kit not found: " + data.kit, "#e74c3c");
                        }
                    }
                    break;

                case "delete":
                    handleDelete(data.kit);
                    break;
            }
        } catch (Exception e) {
            System.err.println("[KitListPage] Error handling event: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void openEditor(Ref<EntityStore> ref, Store<EntityStore> store, Player player, KitConfig kit) {
        shutdown();
        KitEditorPage editorPage = new KitEditorPage(
            playerRef, playerUuid, kit, kitManager, matchManager,
            hubManager, configManager, hudManager, scheduler,
            this::reopenSelf
        );
        player.getPageManager().openCustomPage(ref, store, editorPage);
    }

    private void handleDelete(String kitId) {
        if (kitId == null) return;

        if (!kitId.equals(pendingDeleteId)) {
            // First click — show confirmation on the button
            pendingDeleteId = kitId;
            for (int i = 0; i < kitList.size(); i++) {
                if (kitList.get(i).getId().equals(kitId)) {
                    UICommandBuilder cmd = new UICommandBuilder();
                    cmd.set("#KitList[" + i + "] #RowDeleteBtn.Text", "Confirm?");
                    safeSendUpdate(cmd);
                    break;
                }
            }
            return;
        }

        // Second click — actually delete
        boolean deleted = kitManager.deleteKit(kitId);
        pendingDeleteId = null;

        if (deleted) {
            reopenSelf();
        } else {
            showStatus("Failed to delete kit", "#e74c3c");
        }
    }

    private void showStatus(String message, String color) {
        UICommandBuilder cmd = new UICommandBuilder();
        cmd.set("#KitCountLabel.Text", message);
        cmd.set("#KitCountLabel.Style.TextColor", color);
        safeSendUpdate(cmd);
    }

    private void reopenSelf() {
        Ref<EntityStore> pRef = playerRef.getReference();
        if (pRef == null) return;
        Store<EntityStore> pStore = pRef.getStore();
        Player p = pStore.getComponent(pRef, Player.getComponentType());
        if (p == null) return;

        KitListPage page = new KitListPage(
            playerRef, playerUuid, kitManager, matchManager,
            hubManager, configManager, hudManager, scheduler, onBack
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
        public String kit;

        public static final BuilderCodec<PageEventData> CODEC =
            BuilderCodec.builder(PageEventData.class, PageEventData::new)
                .append(new KeyedCodec<>("Action", Codec.STRING),
                    (d, v) -> d.action = v, d -> d.action).add()
                .append(new KeyedCodec<>("Kit", Codec.STRING),
                    (d, v) -> d.kit = v, d -> d.kit).add()
                .build();
    }
}
