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
import de.ragesith.hyarena2.arena.MatchManager;
import de.ragesith.hyarena2.kit.KitConfig;
import de.ragesith.hyarena2.kit.KitManager;
import de.ragesith.hyarena2.queue.QueueEntry;
import de.ragesith.hyarena2.queue.QueueManager;
import de.ragesith.hyarena2.queue.Matchmaker;
import de.ragesith.hyarena2.ui.hud.HudManager;

import javax.annotation.Nonnull;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Page showing queue status when player is already in queue.
 * Displays queue info and provides option to leave queue.
 */
public class QueueStatusPage extends InteractiveCustomUIPage<QueueStatusPage.PageEventData> implements CloseablePage {

    private final PlayerRef playerRef;
    private final UUID playerUuid;
    private final QueueManager queueManager;
    private final Matchmaker matchmaker;
    private final MatchManager matchManager;
    private final KitManager kitManager;
    private final HudManager hudManager;
    private final ScheduledExecutorService scheduler;

    // Auto-refresh
    private ScheduledFuture<?> refreshTask;
    private volatile boolean active = true;

    public QueueStatusPage(PlayerRef playerRef, UUID playerUuid,
                           QueueManager queueManager, Matchmaker matchmaker,
                           MatchManager matchManager, KitManager kitManager,
                           HudManager hudManager, ScheduledExecutorService scheduler) {
        super(playerRef, CustomPageLifetime.CanDismiss, PageEventData.CODEC);
        this.playerRef = playerRef;
        this.playerUuid = playerUuid;
        this.queueManager = queueManager;
        this.matchmaker = matchmaker;
        this.matchManager = matchManager;
        this.kitManager = kitManager;
        this.hudManager = hudManager;
        this.scheduler = scheduler;
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder cmd,
                      @Nonnull UIEventBuilder events, @Nonnull Store<EntityStore> store) {
        cmd.append("Pages/QueueStatusPage.ui");

        // Register with HudManager for proper cleanup
        hudManager.registerPage(playerUuid, this);

        // Populate initial content
        updateContent(cmd);

        // Bind leave queue button
        events.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#LeaveQueueButton",
            EventData.of("Action", "leave"),
            false
        );

        // Start auto-refresh
        startAutoRefresh();
    }

    /**
     * Updates the page content with current queue info.
     */
    private void updateContent(UICommandBuilder cmd) {
        QueueEntry entry = queueManager.getQueueEntry(playerUuid);

        if (entry == null) {
            // Player is no longer in queue - close page
            active = false;
            return;
        }

        String arenaId = entry.getArenaId();
        Arena arena = matchManager.getArena(arenaId);

        // Arena name
        if (arena != null) {
            cmd.set("#ArenaName.Text", arena.getDisplayName());
        } else {
            cmd.set("#ArenaName.Text", arenaId);
        }

        // Get matchmaking info for status
        Matchmaker.MatchmakingInfo info = matchmaker.getMatchmakingInfo(arenaId);

        // Queue count
        int queueCount = info != null ? info.getQueueSize() : queueManager.getQueueSize(arenaId);
        cmd.set("#QueueCount.Text", String.valueOf(queueCount));

        // Needed count
        int minPlayers = info != null ? info.getMinPlayers() : (arena != null ? arena.getMinPlayers() : 2);
        cmd.set("#NeededCount.Text", String.valueOf(minPlayers));

        // Color queue count based on status
        if (queueCount >= minPlayers) {
            cmd.set("#QueueCount.Style.TextColor", "#2ecc71"); // Green - ready
        } else {
            cmd.set("#QueueCount.Style.TextColor", "#3498db"); // Blue - waiting
        }

        // Status message - use MatchmakingInfo's built-in status message
        String statusMessage = info != null ? info.getStatusMessage() : "Waiting for players...";
        cmd.set("#StatusMessage.Text", statusMessage);

        // Kit info
        String kitId = entry.getSelectedKitId();
        if (kitId != null) {
            KitConfig kit = kitManager.getKit(kitId);
            if (kit != null) {
                cmd.set("#KitInfo.Text", "Kit: " + kit.getDisplayName());
            } else {
                cmd.set("#KitInfo.Text", "Kit: " + kitId);
            }
        } else {
            cmd.set("#KitInfo.Text", "No kit selected");
        }
    }

    /**
     * Starts auto-refresh for queue status.
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
                // Check if still in queue
                if (!queueManager.isInQueue(playerUuid)) {
                    // Player left queue or match started - close page
                    active = false;
                    closePageOnMainThread();
                    return;
                }

                UICommandBuilder cmd = new UICommandBuilder();
                updateContent(cmd);
                safeSendUpdate(cmd);
            } catch (Exception e) {
                // Page might be closed
            }
        }, 1, 1, TimeUnit.SECONDS);
    }

    /**
     * Closes the page on the main thread.
     */
    private void closePageOnMainThread() {
        // The page will be closed when the player's match starts
        // or they can manually close it
    }

    /**
     * Safely sends a UI update, checking active flag first.
     */
    private void safeSendUpdate(UICommandBuilder cmd) {
        if (!active) {
            return;
        }
        try {
            sendUpdate(cmd);
        } catch (Exception e) {
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
        if (data == null || !active) {
            return;
        }

        if ("leave".equals(data.action)) {
            // Leave the queue
            queueManager.leaveQueue(playerUuid, "Player left via UI");

            // Close the page
            shutdown();

            Player player = store.getComponent(ref, Player.getComponentType());
            if (player != null) {
                player.getPageManager().setPage(ref, store, Page.None);
            }
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
        public String action;

        public static final BuilderCodec<PageEventData> CODEC =
            BuilderCodec.builder(PageEventData.class, PageEventData::new)
                .append(new KeyedCodec<>("Action", Codec.STRING),
                    (d, v) -> d.action = v, d -> d.action).add()
                .build();
    }
}
