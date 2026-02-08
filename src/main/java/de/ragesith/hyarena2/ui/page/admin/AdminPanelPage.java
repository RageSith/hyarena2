package de.ragesith.hyarena2.ui.page.admin;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.server.core.Message;
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
import de.ragesith.hyarena2.kit.KitManager;
import de.ragesith.hyarena2.ui.hud.HudManager;
import de.ragesith.hyarena2.ui.page.CloseablePage;

import javax.annotation.Nonnull;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Admin panel navigation page. Provides access to:
 * - Arena Management
 * - Kit Management
 * - Hub Settings
 * - Reload All Configs
 */
public class AdminPanelPage extends InteractiveCustomUIPage<AdminPanelPage.PageEventData> implements CloseablePage {

    private final PlayerRef playerRef;
    private final UUID playerUuid;
    private final MatchManager matchManager;
    private final KitManager kitManager;
    private final HubManager hubManager;
    private final ConfigManager configManager;
    private final HudManager hudManager;
    private final ScheduledExecutorService scheduler;

    private volatile boolean active = true;

    public AdminPanelPage(PlayerRef playerRef, UUID playerUuid,
                          MatchManager matchManager, KitManager kitManager,
                          HubManager hubManager, ConfigManager configManager,
                          HudManager hudManager, ScheduledExecutorService scheduler) {
        super(playerRef, CustomPageLifetime.CantClose, PageEventData.CODEC);
        this.playerRef = playerRef;
        this.playerUuid = playerUuid;
        this.matchManager = matchManager;
        this.kitManager = kitManager;
        this.hubManager = hubManager;
        this.configManager = configManager;
        this.hudManager = hudManager;
        this.scheduler = scheduler;
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder cmd,
                      @Nonnull UIEventBuilder events, @Nonnull Store<EntityStore> store) {
        cmd.append("Pages/AdminPanelPage.ui");

        // Bind navigation buttons
        events.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#ArenaManagementBtn",
            EventData.of("Action", "arenas"),
            false
        );

        events.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#KitManagementBtn",
            EventData.of("Action", "kits"),
            false
        );

        events.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#HubSettingsBtn",
            EventData.of("Action", "hub"),
            false
        );

        events.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#HologramBtn",
            EventData.of("Action", "holograms"),
            false
        );

        events.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#ReloadBtn",
            EventData.of("Action", "reload"),
            false
        );

        events.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#CloseButton",
            EventData.of("Action", "close"),
            false
        );

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

                case "arenas":
                    openArenaList(ref, store, player);
                    break;

                case "kits":
                    openKitList(ref, store, player);
                    break;

                case "hub":
                    openHubSettings(ref, store, player);
                    break;

                case "holograms":
                    openHologramList(ref, store, player);
                    break;

                case "reload":
                    handleReload(player);
                    break;
            }
        } catch (Exception e) {
            System.err.println("[AdminPanelPage] Error handling event: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void openArenaList(Ref<EntityStore> ref, Store<EntityStore> store, Player player) {
        shutdown();
        ArenaListPage page = new ArenaListPage(
            playerRef, playerUuid, matchManager, kitManager, hubManager,
            configManager, hudManager, scheduler,
            this::openSelf
        );
        player.getPageManager().openCustomPage(ref, store, page);
    }

    private void openKitList(Ref<EntityStore> ref, Store<EntityStore> store, Player player) {
        shutdown();
        KitListPage page = new KitListPage(
            playerRef, playerUuid, kitManager, matchManager,
            hubManager, configManager, hudManager, scheduler,
            this::openSelf
        );
        player.getPageManager().openCustomPage(ref, store, page);
    }

    private void openHologramList(Ref<EntityStore> ref, Store<EntityStore> store, Player player) {
        shutdown();
        HologramListPage page = new HologramListPage(
            playerRef, playerUuid, hubManager, configManager,
            hudManager, scheduler,
            this::openSelf
        );
        player.getPageManager().openCustomPage(ref, store, page);
    }

    private void openHubSettings(Ref<EntityStore> ref, Store<EntityStore> store, Player player) {
        shutdown();
        HubSettingsPage page = new HubSettingsPage(
            playerRef, playerUuid, hubManager, configManager,
            hudManager, scheduler,
            this::openSelf
        );
        player.getPageManager().openCustomPage(ref, store, page);
    }

    /**
     * Callback to reopen the admin panel from child pages.
     */
    private void openSelf() {
        Ref<EntityStore> pRef = playerRef.getReference();
        if (pRef == null) return;
        Store<EntityStore> pStore = pRef.getStore();
        Player p = pStore.getComponent(pRef, Player.getComponentType());
        if (p == null) return;

        AdminPanelPage page = new AdminPanelPage(
            playerRef, playerUuid, matchManager, kitManager,
            hubManager, configManager, hudManager, scheduler
        );
        p.getPageManager().openCustomPage(pRef, pStore, page);
    }

    private void handleReload(Player player) {
        try {
            configManager.reload();
            matchManager.reloadArenas();
            kitManager.reloadKits();
            hubManager.updateConfig(configManager.getHubConfig());

            // Show success
            UICommandBuilder cmd = new UICommandBuilder();
            cmd.set("#StatusMessage.Text", "All configs reloaded successfully!");
            cmd.set("#StatusMessage.Visible", true);
            cmd.set("#StatusMessage.Style.TextColor", "#2ecc71");
            safeSendUpdate(cmd);
        } catch (Exception e) {
            UICommandBuilder cmd = new UICommandBuilder();
            cmd.set("#StatusMessage.Text", "Reload failed: " + e.getMessage());
            cmd.set("#StatusMessage.Visible", true);
            cmd.set("#StatusMessage.Style.TextColor", "#e74c3c");
            safeSendUpdate(cmd);
        }
    }

    private void showStatus(String message, String color) {
        UICommandBuilder cmd = new UICommandBuilder();
        cmd.set("#StatusMessage.Text", message);
        cmd.set("#StatusMessage.Visible", true);
        cmd.set("#StatusMessage.Style.TextColor", color);
        safeSendUpdate(cmd);
    }

    private void safeSendUpdate(UICommandBuilder cmd) {
        if (!active) return;
        try {
            sendUpdate(cmd, false);
        } catch (Exception e) {
            active = false;
        }
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

        public static final BuilderCodec<PageEventData> CODEC =
            BuilderCodec.builder(PageEventData.class, PageEventData::new)
                .append(new KeyedCodec<>("Action", Codec.STRING),
                    (d, v) -> d.action = v, d -> d.action).add()
                .build();
    }
}
