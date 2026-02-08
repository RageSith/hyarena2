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
import de.ragesith.hyarena2.arena.Arena;
import de.ragesith.hyarena2.arena.MatchManager;
import de.ragesith.hyarena2.config.ConfigManager;
import de.ragesith.hyarena2.hub.HubManager;
import de.ragesith.hyarena2.kit.KitManager;
import de.ragesith.hyarena2.ui.hud.HudManager;
import de.ragesith.hyarena2.ui.page.CloseablePage;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Admin page listing all arenas with Edit/Delete/Create buttons.
 */
public class ArenaListPage extends InteractiveCustomUIPage<ArenaListPage.PageEventData> implements CloseablePage {

    private final PlayerRef playerRef;
    private final UUID playerUuid;
    private final MatchManager matchManager;
    private final KitManager kitManager;
    private final HubManager hubManager;
    private final ConfigManager configManager;
    private final HudManager hudManager;
    private final ScheduledExecutorService scheduler;
    private final Runnable onBack;

    private volatile boolean active = true;
    private List<Arena> arenaList;

    // Track which arena is pending delete confirmation (null = none)
    private String pendingDeleteId;

    public ArenaListPage(PlayerRef playerRef, UUID playerUuid,
                         MatchManager matchManager, KitManager kitManager,
                         HubManager hubManager, ConfigManager configManager,
                         HudManager hudManager, ScheduledExecutorService scheduler,
                         Runnable onBack) {
        super(playerRef, CustomPageLifetime.CantClose, PageEventData.CODEC);
        this.playerRef = playerRef;
        this.playerUuid = playerUuid;
        this.matchManager = matchManager;
        this.kitManager = kitManager;
        this.hubManager = hubManager;
        this.configManager = configManager;
        this.hudManager = hudManager;
        this.scheduler = scheduler;
        this.onBack = onBack;
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder cmd,
                      @Nonnull UIEventBuilder events, @Nonnull Store<EntityStore> store) {
        cmd.append("Pages/ArenaListPage.ui");

        arenaList = new ArrayList<>(matchManager.getArenas());
        arenaList.sort((a, b) -> a.getDisplayName().compareToIgnoreCase(b.getDisplayName()));

        cmd.set("#ArenaCountLabel.Text", arenaList.size() + " arenas loaded");

        for (int i = 0; i < arenaList.size(); i++) {
            Arena arena = arenaList.get(i);

            cmd.append("#ArenaList", "Pages/AdminArenaRow.ui");
            String row = "#ArenaList[" + i + "]";

            cmd.set(row + " #RowArenaName.Text", arena.getDisplayName());
            String info = matchManager.getGameModeDisplayName(arena) + " | " + MatchManager.formatPlayerCount(arena);
            cmd.set(row + " #RowArenaInfo.Text", info);

            // Edit button
            events.addEventBinding(
                CustomUIEventBindingType.Activating,
                row + " #RowEditBtn",
                EventData.of("Action", "edit").append("Arena", arena.getId()),
                false
            );

            // Delete button — set text directly on the button element
            if (arena.getId().equals(pendingDeleteId)) {
                cmd.set(row + " #RowDeleteBtn.Text", "Confirm?");
            }

            events.addEventBinding(
                CustomUIEventBindingType.Activating,
                row + " #RowDeleteBtn",
                EventData.of("Action", "delete").append("Arena", arena.getId()),
                false
            );
        }

        // Bind buttons
        events.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#CreateNewBtn",
            EventData.of("Action", "create"),
            false
        );

        events.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#BackBtn",
            EventData.of("Action", "back"),
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

                case "back":
                    shutdown();
                    if (onBack != null) onBack.run();
                    break;

                case "create":
                    openEditor(ref, store, player, null);
                    break;

                case "edit":
                    if (data.arena != null) {
                        Arena arena = matchManager.getArena(data.arena);
                        if (arena != null) {
                            openEditor(ref, store, player, arena);
                        } else {
                            showStatus("Arena not found: " + data.arena, "#e74c3c");
                        }
                    }
                    break;

                case "delete":
                    handleDelete(data.arena);
                    break;
            }
        } catch (Exception e) {
            System.err.println("[ArenaListPage] Error handling event: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void openEditor(Ref<EntityStore> ref, Store<EntityStore> store, Player player, Arena arena) {
        shutdown();
        ArenaEditorPage editorPage = new ArenaEditorPage(
            playerRef, playerUuid,
            arena != null ? arena.getConfig() : null,
            matchManager, kitManager, hubManager, configManager,
            hudManager, scheduler,
            this::reopenSelf
        );
        player.getPageManager().openCustomPage(ref, store, editorPage);
    }

    private void handleDelete(String arenaId) {
        if (arenaId == null) return;

        if (!arenaId.equals(pendingDeleteId)) {
            // First click — show confirmation on the button
            pendingDeleteId = arenaId;
            for (int i = 0; i < arenaList.size(); i++) {
                if (arenaList.get(i).getId().equals(arenaId)) {
                    UICommandBuilder cmd = new UICommandBuilder();
                    cmd.set("#ArenaList[" + i + "] #RowDeleteBtn.Text", "Confirm?");
                    safeSendUpdate(cmd);
                    break;
                }
            }
            return;
        }

        // Second click — actually delete
        if (matchManager.isArenaInUse(arenaId)) {
            showStatus("Cannot delete: arena is in use", "#e74c3c");
            pendingDeleteId = null;
            return;
        }

        boolean deleted = matchManager.deleteArena(arenaId);
        pendingDeleteId = null;

        if (deleted) {
            reopenSelf();
        } else {
            showStatus("Failed to delete arena", "#e74c3c");
        }
    }

    private void showStatus(String message, String color) {
        UICommandBuilder cmd = new UICommandBuilder();
        cmd.set("#ArenaCountLabel.Text", message);
        cmd.set("#ArenaCountLabel.Style.TextColor", color);
        safeSendUpdate(cmd);
    }

    private void reopenSelf() {
        Ref<EntityStore> pRef = playerRef.getReference();
        if (pRef == null) return;
        Store<EntityStore> pStore = pRef.getStore();
        Player p = pStore.getComponent(pRef, Player.getComponentType());
        if (p == null) return;

        ArenaListPage page = new ArenaListPage(
            playerRef, playerUuid, matchManager, kitManager,
            hubManager, configManager, hudManager, scheduler, onBack
        );
        p.getPageManager().openCustomPage(pRef, pStore, page);
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
        public String arena;

        public static final BuilderCodec<PageEventData> CODEC =
            BuilderCodec.builder(PageEventData.class, PageEventData::new)
                .append(new KeyedCodec<>("Action", Codec.STRING),
                    (d, v) -> d.action = v, d -> d.action).add()
                .append(new KeyedCodec<>("Arena", Codec.STRING),
                    (d, v) -> d.arena = v, d -> d.arena).add()
                .build();
    }
}
