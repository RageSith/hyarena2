package de.ragesith.hyarena2.ui.hud;

import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import de.ragesith.hyarena2.HyArena2;
import de.ragesith.hyarena2.arena.Arena;
import de.ragesith.hyarena2.arena.Match;
import de.ragesith.hyarena2.arena.MatchManager;
import de.ragesith.hyarena2.economy.EconomyManager;
import de.ragesith.hyarena2.economy.HonorManager;
import de.ragesith.hyarena2.queue.Matchmaker;
import de.ragesith.hyarena2.queue.QueueEntry;
import de.ragesith.hyarena2.queue.QueueManager;

import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Combined HUD showing server stats and queue info.
 * Top bar: players online, players in queue, players in game, avg queue time.
 * Bottom bar (shown when in queue): arena name, position, players, elapsed time, status.
 */
public class LobbyHud extends CustomUIHud {

    private final UUID playerUuid;
    private final QueueManager queueManager;
    private final Matchmaker matchmaker;
    private final MatchManager matchManager;
    private final Supplier<Integer> onlinePlayerCountSupplier;

    // Shared scheduler for auto-refresh
    private final ScheduledExecutorService sharedScheduler;
    private ScheduledFuture<?> refreshTask;

    // Flag to prevent updates after shutdown
    private volatile boolean active = true;

    // Track if queue panel is currently visible
    private boolean queuePanelVisible = false;

    public LobbyHud(PlayerRef playerRef, UUID playerUuid, QueueManager queueManager,
                    Matchmaker matchmaker, MatchManager matchManager,
                    Supplier<Integer> onlinePlayerCountSupplier,
                    ScheduledExecutorService scheduler) {
        super(playerRef);
        this.playerUuid = playerUuid;
        this.queueManager = queueManager;
        this.matchmaker = matchmaker;
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
        // === Top bar: Global stats ===

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

        // Active matches count (excludes finished matches)
        int matchCount = getActiveMatchCount();
        cmd.set("#MatchCount.Text", String.valueOf(matchCount));

        // Color match count
        if (matchCount > 0) {
            cmd.set("#MatchCount.Style.TextColor", "#9b59b6"); // Purple when active matches
        } else {
            cmd.set("#MatchCount.Style.TextColor", "#7f8c8d"); // Gray when no matches
        }

        // Average queue time
        String avgTime = getOverallAverageQueueTime();
        cmd.set("#AvgTime.Text", avgTime);

        // === Rank & AP ===
        HyArena2 plugin = HyArena2.getInstance();
        if (plugin != null) {
            EconomyManager economyManager = plugin.getEconomyManager();
            HonorManager honorManager = plugin.getHonorManager();
            if (economyManager != null && honorManager != null) {
                cmd.set("#RankName.Text", honorManager.getRankDisplayName(playerUuid));
                cmd.set("#RankName.Style.TextColor", honorManager.getRankColor(playerUuid));
                cmd.set("#APCount.Text", String.valueOf(economyManager.getArenaPoints(playerUuid)));
            }
        }

        // === Bottom bar: Queue info (only when in queue) ===

        boolean isInQueue = queueManager.isInQueue(playerUuid);

        if (isInQueue) {
            // Show queue panel
            if (!queuePanelVisible) {
                cmd.set("#QueuePanel.Visible", true);
                queuePanelVisible = true;
            }

            // Update queue info
            updateQueuePanel(cmd);
        } else {
            // Hide queue panel
            if (queuePanelVisible) {
                cmd.set("#QueuePanel.Visible", false);
                queuePanelVisible = false;
            }
        }
    }

    /**
     * Updates the queue panel with current queue information.
     */
    private void updateQueuePanel(UICommandBuilder cmd) {
        QueueEntry entry = queueManager.getQueueEntry(playerUuid);
        if (entry == null) {
            return;
        }

        String arenaId = entry.getArenaId();

        // Arena name
        Arena arena = matchManager.getArena(arenaId);
        String arenaName = arena != null ? arena.getDisplayName() : arenaId;
        cmd.set("#QueueArenaName.Text", arenaName);

        // Queue position
        int position = queueManager.getQueuePosition(playerUuid);
        cmd.set("#QueuePosition.Text", "#" + position);

        // Players in queue and status
        Matchmaker.MatchmakingInfo info = matchmaker.getMatchmakingInfo(arenaId);
        if (info != null) {
            cmd.set("#QueuePlayers.Text", info.getQueueSize() + "/" + info.getMaxPlayers());

            // Status message
            cmd.set("#QueueStatus.Text", info.getStatusMessage());

            // Status color based on state
            if (info.isWaitingForMorePlayers()) {
                cmd.set("#QueueStatus.Style.TextColor", "#f39c12"); // Orange - countdown
            } else if (info.canStartMatch()) {
                cmd.set("#QueueStatus.Style.TextColor", "#2ecc71"); // Green - ready
            } else {
                cmd.set("#QueueStatus.Style.TextColor", "#3498db"); // Blue - waiting
            }
        } else {
            cmd.set("#QueuePlayers.Text", "-/-");
            cmd.set("#QueueStatus.Text", "Waiting...");
            cmd.set("#QueueStatus.Style.TextColor", "#3498db");
        }

        // Elapsed time in queue
        long elapsedMs = System.currentTimeMillis() - entry.getJoinTime();
        long elapsedSeconds = elapsedMs / 1000;
        long minutes = elapsedSeconds / 60;
        long seconds = elapsedSeconds % 60;
        cmd.set("#QueueElapsed.Text", String.format("%d:%02d", minutes, seconds));
    }

    /**
     * Gets the total players across all active (non-finished) matches.
     */
    private int getTotalPlayersInMatches() {
        int total = 0;
        for (Match match : matchManager.getActiveMatches()) {
            if (!match.isFinished()) {
                total += match.getParticipants().size();
            }
        }
        return total;
    }

    /**
     * Gets the count of active (non-finished) matches.
     */
    private int getActiveMatchCount() {
        int count = 0;
        for (Match match : matchManager.getActiveMatches()) {
            if (!match.isFinished()) {
                count++;
            }
        }
        return count;
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
     */
    private void startAutoRefresh() {
        if (refreshTask != null || sharedScheduler == null) {
            return;
        }

        // Short initial delay, then refresh every second
        refreshTask = sharedScheduler.scheduleAtFixedRate(() -> {
            if (!active) {
                return;
            }

            try {
                UICommandBuilder cmd = new UICommandBuilder();
                updateContent(cmd);
                update(false, cmd);
            } catch (Exception e) {
                // UI might not be ready yet, skip this update
            }

            // Stop refreshing if player joined a match (they'll be teleported to arena)
            if (matchManager.isPlayerInMatch(playerUuid)) {
                stopAutoRefresh();
            }
        }, 500, 1000, TimeUnit.MILLISECONDS);
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
