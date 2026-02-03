package de.ragesith.hyarena2.ui.hud;

import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import de.ragesith.hyarena2.arena.MatchManager;
import de.ragesith.hyarena2.queue.QueueManager;

import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * HUD showing server stats at the top center of the screen.
 * Displays: players online, players in queue, players in game, avg queue time.
 */
public class LobbyHud extends CustomUIHud {

    private final UUID playerUuid;
    private final QueueManager queueManager;
    private final MatchManager matchManager;
    private final Supplier<Integer> onlinePlayerCountSupplier;

    // Shared scheduler for auto-refresh
    private final ScheduledExecutorService sharedScheduler;
    private ScheduledFuture<?> refreshTask;

    // Flag to prevent updates after shutdown
    private volatile boolean active = true;

    public LobbyHud(PlayerRef playerRef, UUID playerUuid, QueueManager queueManager,
                    MatchManager matchManager, Supplier<Integer> onlinePlayerCountSupplier,
                    ScheduledExecutorService scheduler) {
        super(playerRef);
        this.playerUuid = playerUuid;
        this.queueManager = queueManager;
        this.matchManager = matchManager;
        this.onlinePlayerCountSupplier = onlinePlayerCountSupplier;
        this.sharedScheduler = scheduler;
    }

    @Override
    public void build(UICommandBuilder cmd) {
        // Load UI file only - content will be populated by auto-refresh
        cmd.append("Huds/LobbyHud.ui");

        // Start auto-refresh with delay to let UI load
        startAutoRefresh();
    }

    /**
     * Updates the HUD content with current stats.
     */
    private void updateContent(UICommandBuilder cmd) {
        // Players online
        int online = onlinePlayerCountSupplier != null ? onlinePlayerCountSupplier.get() : 0;
        cmd.set("#OnlineCount.Text", String.valueOf(online));

        // Players in queue (across all arenas)
        int inQueue = queueManager.getTotalQueuedPlayers();
        cmd.set("#QueueCount.Text", String.valueOf(inQueue));

        // Color queue count based on activity
        if (inQueue > 0) {
            cmd.set("#QueueCount.Style.TextColor", "#f39c12"); // Orange when players queuing
        } else {
            cmd.set("#QueueCount.Style.TextColor", "#7f8c8d"); // Gray when empty
        }

        // Players in game
        int inGame = getTotalPlayersInMatches();
        cmd.set("#InGameCount.Text", String.valueOf(inGame));

        // Color in-game count
        if (inGame > 0) {
            cmd.set("#InGameCount.Style.TextColor", "#e74c3c"); // Red when active games
        } else {
            cmd.set("#InGameCount.Style.TextColor", "#7f8c8d"); // Gray when no games
        }

        // Average queue time
        String avgTime = getOverallAverageQueueTime();
        cmd.set("#AvgTime.Text", avgTime);
    }

    /**
     * Gets the total players across all matches.
     */
    private int getTotalPlayersInMatches() {
        int total = 0;
        for (var match : matchManager.getActiveMatches()) {
            total += match.getParticipants().size();
        }
        return total;
    }

    /**
     * Gets the overall average queue time across all arenas.
     */
    private String getOverallAverageQueueTime() {
        var arenaIds = queueManager.getActiveArenaIds();
        if (arenaIds.isEmpty()) {
            return "N/A";
        }

        long totalMs = 0;
        int count = 0;
        for (String arenaId : arenaIds) {
            long avgMs = queueManager.getAverageQueueTime(arenaId);
            if (avgMs > 0) {
                totalMs += avgMs;
                count++;
            }
        }

        if (count == 0) {
            return "N/A";
        }

        long avgMs = totalMs / count;
        return formatTime(avgMs);
    }

    /**
     * Formats milliseconds to a readable string.
     */
    private String formatTime(long ms) {
        long seconds = ms / 1000;
        if (seconds < 60) {
            return seconds + "s";
        }
        long minutes = seconds / 60;
        seconds = seconds % 60;
        if (seconds == 0) {
            return minutes + "m";
        }
        return minutes + "m " + seconds + "s";
    }

    /**
     * Starts the auto-refresh task.
     * TEST: Running directly on scheduler thread without world.execute()
     */
    private void startAutoRefresh() {
        if (refreshTask != null || sharedScheduler == null) {
            return;
        }

        // Use longer initial delay (2 seconds) to let UI load on client
        refreshTask = sharedScheduler.scheduleAtFixedRate(() -> {
            // TEST: Direct update without world thread
            if (!active) {
                return;
            }

            // Stop if player is in a match (they'll have a different HUD)
            if (matchManager.isPlayerInMatch(playerUuid)) {
                stopAutoRefresh();
                return;
            }

            try {
                UICommandBuilder cmd = new UICommandBuilder();
                updateContent(cmd);
                update(false, cmd);
            } catch (Exception e) {
                // UI might not be ready yet, skip this update
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
