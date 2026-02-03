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
import de.ragesith.hyarena2.arena.Arena;
import de.ragesith.hyarena2.arena.Match;
import de.ragesith.hyarena2.arena.MatchManager;
import de.ragesith.hyarena2.kit.KitManager;
import de.ragesith.hyarena2.queue.QueueManager;
import de.ragesith.hyarena2.ui.hud.HudManager;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Arena selection page showing all available arenas.
 * Players can click an arena to view details and join the queue.
 */
public class ArenaMenuPage extends InteractiveCustomUIPage<ArenaMenuPage.PageEventData> implements CloseablePage {

    private static final int MAX_ARENAS = 6;

    private final PlayerRef playerRef;
    private final UUID playerUuid;
    private final MatchManager matchManager;
    private final QueueManager queueManager;
    private final KitManager kitManager;
    private final HudManager hudManager;
    private final ScheduledExecutorService scheduler;

    // Stored for page transitions
    private Ref<EntityStore> storedRef;
    private Store<EntityStore> storedStore;

    // Arena list cached for event handling
    private List<Arena> arenaList;

    // Auto-refresh task
    private ScheduledFuture<?> refreshTask;
    private volatile boolean active = true;

    public ArenaMenuPage(PlayerRef playerRef, UUID playerUuid,
                         MatchManager matchManager, QueueManager queueManager,
                         KitManager kitManager, HudManager hudManager,
                         ScheduledExecutorService scheduler) {
        super(playerRef, CustomPageLifetime.CanDismiss, PageEventData.CODEC);
        this.playerRef = playerRef;
        this.playerUuid = playerUuid;
        this.matchManager = matchManager;
        this.queueManager = queueManager;
        this.kitManager = kitManager;
        this.hudManager = hudManager;
        this.scheduler = scheduler;
        this.arenaList = new ArrayList<>(matchManager.getArenas());
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder cmd,
                      @Nonnull UIEventBuilder events, @Nonnull Store<EntityStore> store) {
        this.storedRef = ref;
        this.storedStore = store;

        cmd.append("Pages/ArenaMenuPage.ui");

        // Populate arena list
        populateArenas(cmd);

        // Bind arena button events
        for (int i = 0; i < MAX_ARENAS && i < arenaList.size(); i++) {
            Arena arena = arenaList.get(i);
            events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#ArenaButton" + i,
                EventData.of("Arena", arena.getId()),
                false
            );
        }

        // Register with HudManager for proper cleanup
        hudManager.registerPage(playerUuid, this);

        // Start auto-refresh for queue counts
        startAutoRefresh();
    }

    /**
     * Populates the arena buttons with current data.
     */
    private void populateArenas(UICommandBuilder cmd) {
        // Show/hide no arenas notice
        if (arenaList.isEmpty()) {
            cmd.set("#NoArenasNotice.Visible", true);
        } else {
            cmd.set("#NoArenasNotice.Visible", false);
        }

        // Populate arena buttons
        for (int i = 0; i < MAX_ARENAS; i++) {
            if (i < arenaList.size()) {
                Arena arena = arenaList.get(i);
                cmd.set("#ArenaButton" + i + ".Visible", true);

                // Arena name
                cmd.set("#ArenaName" + i + ".Text", arena.getDisplayName());

                // Game mode and player count
                String gameModeInfo = String.format("%s | %d-%d Players",
                    arena.getGameMode(),
                    arena.getMinPlayers(),
                    arena.getMaxPlayers());
                cmd.set("#GameMode" + i + ".Text", gameModeInfo);

                // Status tag (Available / In Use)
                boolean inUse = matchManager.isArenaInUse(arena.getId());
                if (inUse) {
                    cmd.set("#StatusTag" + i + ".Text", "IN USE");
                    cmd.set("#StatusTag" + i + ".Style.TextColor", "#e74c3c");
                } else {
                    cmd.set("#StatusTag" + i + ".Text", "AVAILABLE");
                    cmd.set("#StatusTag" + i + ".Style.TextColor", "#2ecc71");
                }

                // Queue count
                int queueCount = queueManager.getQueueSize(arena.getId());
                cmd.set("#InQueue" + i + ".Text", "In Queue: " + queueCount);

                // In-game count
                int inGameCount = getPlayersInArenaMatch(arena.getId());
                cmd.set("#InGame" + i + ".Text", "In Game: " + inGameCount);
            } else {
                cmd.set("#ArenaButton" + i + ".Visible", false);
            }
        }
    }

    /**
     * Gets the number of players in active matches for an arena.
     */
    private int getPlayersInArenaMatch(String arenaId) {
        int count = 0;
        for (Match match : matchManager.getActiveMatches()) {
            if (match.getArena().getId().equals(arenaId) && !match.isFinished()) {
                count += match.getParticipants().size();
            }
        }
        return count;
    }

    /**
     * Starts auto-refresh for queue/game counts.
     */
    private void startAutoRefresh() {
        if (refreshTask != null || scheduler == null) {
            return;
        }

        refreshTask = scheduler.scheduleAtFixedRate(() -> {
            if (!active) {
                return;
            }

            try {
                UICommandBuilder cmd = new UICommandBuilder();

                // Update queue and in-game counts
                for (int i = 0; i < MAX_ARENAS && i < arenaList.size(); i++) {
                    Arena arena = arenaList.get(i);

                    // Status
                    boolean inUse = matchManager.isArenaInUse(arena.getId());
                    if (inUse) {
                        cmd.set("#StatusTag" + i + ".Text", "IN USE");
                        cmd.set("#StatusTag" + i + ".Style.TextColor", "#e74c3c");
                    } else {
                        cmd.set("#StatusTag" + i + ".Text", "AVAILABLE");
                        cmd.set("#StatusTag" + i + ".Style.TextColor", "#2ecc71");
                    }

                    int queueCount = queueManager.getQueueSize(arena.getId());
                    cmd.set("#InQueue" + i + ".Text", "In Queue: " + queueCount);

                    int inGameCount = getPlayersInArenaMatch(arena.getId());
                    cmd.set("#InGame" + i + ".Text", "In Game: " + inGameCount);
                }

                safeSendUpdate(cmd);
            } catch (Exception e) {
                // Page might be closed
            }
        }, 2, 2, TimeUnit.SECONDS);
    }

    /**
     * Safely sends a UI update, checking active flag first.
     * Prevents "CustomUI command not found" errors when page is closed.
     */
    private void safeSendUpdate(UICommandBuilder cmd) {
        if (!active) {
            return;
        }
        try {
            sendUpdate(cmd);
        } catch (Exception e) {
            // Page might be closed, stop further updates
            active = false;
        }
    }

    /**
     * Stops auto-refresh.
     */
    private void stopAutoRefresh() {
        active = false;
        if (refreshTask != null) {
            refreshTask.cancel(false);
            refreshTask = null;
        }
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, PageEventData data) {
        if (data == null) {
            return;
        }

        // Handle arena selection
        if (data.arena != null) {
            stopAutoRefresh();

            Arena arena = matchManager.getArena(data.arena);
            if (arena == null) {
                return;
            }

            // Open arena detail page
            Player player = store.getComponent(ref, Player.getComponentType());
            if (player != null) {
                ArenaDetailPage detailPage = new ArenaDetailPage(
                    playerRef, playerUuid, arena,
                    matchManager, queueManager, kitManager, hudManager, scheduler,
                    this::onBackFromDetail
                );
                player.getPageManager().openCustomPage(ref, store, detailPage);
            }
        }
    }

    /**
     * Callback when returning from detail page.
     */
    private void onBackFromDetail(Ref<EntityStore> ref, Store<EntityStore> store) {
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player != null) {
            // Refresh arena list and reopen this page
            ArenaMenuPage newPage = new ArenaMenuPage(
                playerRef, playerUuid, matchManager, queueManager, kitManager, hudManager, scheduler
            );
            player.getPageManager().openCustomPage(ref, store, newPage);
        }
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

    public static class PageEventData {
        public String arena;

        public static final BuilderCodec<PageEventData> CODEC =
            BuilderCodec.builder(PageEventData.class, PageEventData::new)
                .append(new KeyedCodec<>("Arena", Codec.STRING),
                    (d, v) -> d.arena = v, d -> d.arena).add()
                .build();
    }
}
