package de.ragesith.hyarena2.ui.hud;

import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import de.ragesith.hyarena2.arena.Arena;
import de.ragesith.hyarena2.arena.MatchManager;
import de.ragesith.hyarena2.queue.Matchmaker;
import de.ragesith.hyarena2.queue.QueueEntry;
import de.ragesith.hyarena2.queue.QueueManager;

import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * HUD overlay showing queue status in the top-right corner.
 * Displayed when a player is in queue, hidden when they leave or match starts.
 */
public class QueueHud extends CustomUIHud {

    private final UUID playerUuid;
    private final QueueManager queueManager;
    private final Matchmaker matchmaker;
    private final MatchManager matchManager;

    // Shared scheduler for auto-refresh
    private final ScheduledExecutorService sharedScheduler;
    private final Consumer<Runnable> worldThreadExecutor;
    private ScheduledFuture<?> refreshTask;

    // Flag to prevent updates after shutdown
    private volatile boolean active = true;

    public QueueHud(PlayerRef playerRef, UUID playerUuid, QueueManager queueManager,
                    Matchmaker matchmaker, MatchManager matchManager,
                    ScheduledExecutorService sharedScheduler,
                    Consumer<Runnable> worldThreadExecutor) {
        super(playerRef);
        this.playerUuid = playerUuid;
        this.queueManager = queueManager;
        this.matchmaker = matchmaker;
        this.matchManager = matchManager;
        this.sharedScheduler = sharedScheduler;
        this.worldThreadExecutor = worldThreadExecutor;
    }

    @Override
    public void build(UICommandBuilder cmd) {
        // Load UI file only - content will be populated by auto-refresh
        cmd.append("Huds/QueueHud.ui");

        // Start auto-refresh with delay to let UI load
        startAutoRefresh();
    }

    /**
     * Updates the HUD content with current queue information.
     */
    private void updateContent(UICommandBuilder cmd) {
        QueueEntry entry = queueManager.getQueueEntry(playerUuid);
        if (entry == null) {
            return;
        }

        String arenaId = entry.getArenaId();

        // Arena name
        Arena arena = matchManager.getArena(arenaId);
        String arenaName = arena != null ? arena.getDisplayName() : arenaId;
        cmd.set("#ArenaName.Text", arenaName);

        // Queue position
        int position = queueManager.getQueuePosition(playerUuid);
        cmd.set("#QueuePosition.Text", "#" + position);

        // Players in queue and status
        Matchmaker.MatchmakingInfo info = matchmaker.getMatchmakingInfo(arenaId);
        if (info != null) {
            cmd.set("#QueuePlayers.Text", info.getQueueSize() + "/" + info.getMaxPlayers());
            cmd.set("#QueueTime.Text", info.getAverageQueueTime());

            // Show status message
            cmd.set("#QueueStatus.Text", info.getStatusMessage());

            // Change status color based on state
            if (info.isWaitingForMorePlayers()) {
                // Countdown active - orange
                cmd.set("#QueueStatus.Style.TextColor", "#f39c12");
            } else if (info.canStartMatch()) {
                // Ready to start - green
                cmd.set("#QueueStatus.Style.TextColor", "#2ecc71");
            } else {
                // Still waiting for minimum players - blue
                cmd.set("#QueueStatus.Style.TextColor", "#3498db");
            }
        } else {
            cmd.set("#QueuePlayers.Text", "-/-");
            cmd.set("#QueueTime.Text", "N/A");
            cmd.set("#QueueStatus.Text", "Waiting for players...");
        }

        // Elapsed time in queue
        long elapsedMs = System.currentTimeMillis() - entry.getJoinTime();
        long elapsedSeconds = elapsedMs / 1000;
        long minutes = elapsedSeconds / 60;
        long seconds = elapsedSeconds % 60;
        cmd.set("#ElapsedTime.Text", String.format("%d:%02d", minutes, seconds));
    }

    /**
     * Starts the auto-refresh task using the shared scheduler.
     */
    private void startAutoRefresh() {
        if (refreshTask != null || sharedScheduler == null) {
            return;
        }

        // Use longer initial delay (2 seconds) to let UI load on client
        refreshTask = sharedScheduler.scheduleAtFixedRate(() -> {
            try {
                // Check if HUD is still active
                if (!active) {
                    return;
                }

                // Check if still in queue - if not, stop refreshing
                if (!queueManager.isInQueue(playerUuid)) {
                    stopAutoRefresh();
                    return;
                }

                // Update content on world thread
                if (active && worldThreadExecutor != null) {
                    worldThreadExecutor.accept(() -> {
                        // Double-check active flag and queue state before update
                        if (!active) {
                            return;
                        }
                        if (!queueManager.isInQueue(playerUuid)) {
                            return;
                        }
                        try {
                            UICommandBuilder cmd = new UICommandBuilder();
                            updateContent(cmd);
                            update(false, cmd);
                        } catch (Exception e) {
                            // UI might not be ready yet, skip this update
                        }
                    });
                }
            } catch (Exception e) {
                // HUD might be closed, ignore errors
            }
        }, 2, 1, TimeUnit.SECONDS);
    }

    /**
     * Stops the auto-refresh task.
     */
    private void stopAutoRefresh() {
        if (refreshTask != null) {
            refreshTask.cancel(true);
            refreshTask = null;
        }
    }

    /**
     * Shuts down this HUD's refresh task.
     */
    public void shutdown() {
        active = false;
        stopAutoRefresh();
    }
}
