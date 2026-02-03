package de.ragesith.hyarena2.ui.hud;

import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import de.ragesith.hyarena2.arena.Match;
import de.ragesith.hyarena2.arena.MatchState;
import de.ragesith.hyarena2.participant.Participant;
import de.ragesith.hyarena2.participant.ParticipantType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * HUD overlay showing match status in the top-right corner.
 * Displays arena name, game mode, elapsed time, and player list with K/D.
 */
public class MatchHud extends CustomUIHud {

    private final UUID playerUuid;
    private final Match match;
    private final ScheduledExecutorService sharedScheduler;
    private final Consumer<Runnable> worldThreadExecutor;
    private ScheduledFuture<?> refreshTask;

    // Flag to prevent updates after shutdown
    private volatile boolean active = true;

    private static final int MAX_PLAYER_ROWS = 10;

    public MatchHud(PlayerRef playerRef, UUID playerUuid, Match match,
                    ScheduledExecutorService scheduler, Consumer<Runnable> worldThreadExecutor) {
        super(playerRef);
        this.playerUuid = playerUuid;
        this.match = match;
        this.sharedScheduler = scheduler;
        this.worldThreadExecutor = worldThreadExecutor;
    }

    @Override
    public void build(UICommandBuilder cmd) {
        // Load UI file only - content will be populated by auto-refresh
        cmd.append("Huds/MatchHud.ui");

        // Start auto-refresh with delay to let UI load
        startAutoRefresh();
    }

    /**
     * Updates the HUD content with current match information.
     */
    private void updateContent(UICommandBuilder cmd) {
        // Arena name
        String arenaName = match.getArena().getDisplayName();
        cmd.set("#ArenaName.Text", arenaName);

        // Game mode
        String gameMode = match.getGameMode().getDisplayName();
        cmd.set("#GameMode.Text", gameMode);

        // Match time (countdown)
        int remainingSeconds = match.getRemainingSeconds();
        int minutes = remainingSeconds / 60;
        int seconds = remainingSeconds % 60;
        String timeDisplay = String.format("%d:%02d", minutes, seconds);
        cmd.set("#MatchTime.Text", timeDisplay);

        // Color timer based on remaining time (red when low)
        if (remainingSeconds <= 10) {
            cmd.set("#MatchTime.Style.TextColor", "#e74c3c"); // Red - critical
        } else if (remainingSeconds <= 30) {
            cmd.set("#MatchTime.Style.TextColor", "#f39c12"); // Orange - warning
        } else {
            cmd.set("#MatchTime.Style.TextColor", "#f1c40f"); // Yellow - normal
        }

        // Build participant list sorted by kills descending
        List<ParticipantInfo> participantList = new ArrayList<>();
        for (Participant p : match.getParticipants()) {
            participantList.add(new ParticipantInfo(
                p.getName(),
                p.getKills(),
                p.getDeaths(),
                p.isAlive(),
                p.getType() == ParticipantType.BOT
            ));
        }

        // Sort by kills descending
        participantList.sort((a, b) -> Integer.compare(b.kills, a.kills));

        // Populate player rows
        int rowIndex = 0;
        for (ParticipantInfo p : participantList) {
            if (rowIndex >= MAX_PLAYER_ROWS) {
                break;
            }

            // Show row
            cmd.set("#PlayerRow" + rowIndex + ".Visible", true);

            // Name (with [X] prefix if dead, [BOT] suffix for bots)
            String displayName = p.name;
            if (p.isBot) {
                displayName += " [BOT]";
            }
            if (!p.alive) {
                displayName = "[X] " + displayName;
            }
            cmd.set("#PlayerName" + rowIndex + ".Text", displayName);

            // Color: gray for dead, greenish for bots, white for alive players
            if (!p.alive) {
                cmd.set("#PlayerName" + rowIndex + ".Style.TextColor", "#666666");
            } else if (p.isBot) {
                cmd.set("#PlayerName" + rowIndex + ".Style.TextColor", "#a0c0a0");
            } else {
                cmd.set("#PlayerName" + rowIndex + ".Style.TextColor", "#b7cedd");
            }

            // Kills and deaths
            cmd.set("#PlayerKills" + rowIndex + ".Text", String.valueOf(p.kills));
            cmd.set("#PlayerDeaths" + rowIndex + ".Text", String.valueOf(p.deaths));

            rowIndex++;
        }

        // Hide unused rows
        for (int i = rowIndex; i < MAX_PLAYER_ROWS; i++) {
            cmd.set("#PlayerRow" + i + ".Visible", false);
        }
    }

    /**
     * Helper class to hold participant info for display.
     */
    private static class ParticipantInfo {
        final String name;
        final int kills;
        final int deaths;
        final boolean alive;
        final boolean isBot;

        ParticipantInfo(String name, int kills, int deaths, boolean alive, boolean isBot) {
            this.name = name;
            this.kills = kills;
            this.deaths = deaths;
            this.alive = alive;
            this.isBot = isBot;
        }
    }

    /**
     * Starts the auto-refresh task.
     */
    private void startAutoRefresh() {
        if (refreshTask != null || sharedScheduler == null) {
            return;
        }

        // Initial delay of 2s to let UI load, then refresh every 1s
        refreshTask = sharedScheduler.scheduleAtFixedRate(() -> {
            if (!active) {
                return;
            }

            try {
                // Check if match is still in progress
                MatchState state = match.getState();
                if (state != MatchState.IN_PROGRESS) {
                    stopAutoRefresh();
                    return;
                }

                // Update content on world thread for thread safety
                if (worldThreadExecutor != null) {
                    worldThreadExecutor.accept(() -> {
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
