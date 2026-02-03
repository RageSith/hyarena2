package de.ragesith.hyarena2.ui.hud;

import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import de.ragesith.hyarena2.arena.Match;
import de.ragesith.hyarena2.participant.Participant;

import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * HUD overlay shown during ENDING state displaying victory/defeat info.
 * Shows winner name, final stats, and countdown to hub return.
 */
public class VictoryHud extends CustomUIHud {

    private final UUID playerUuid;
    private final Match match;
    private final boolean isWinner;
    private final String winnerName;
    private final ScheduledExecutorService sharedScheduler;
    private final Consumer<Runnable> worldThreadExecutor;
    private ScheduledFuture<?> refreshTask;

    // Countdown state
    private int countdownSeconds;
    private static final int COUNTDOWN_DURATION = 3;

    // Flag to prevent updates after shutdown
    private volatile boolean active = true;

    public VictoryHud(PlayerRef playerRef, UUID playerUuid, Match match, boolean isWinner, String winnerName,
                      ScheduledExecutorService scheduler, Consumer<Runnable> worldThreadExecutor) {
        super(playerRef);
        this.playerUuid = playerUuid;
        this.match = match;
        this.isWinner = isWinner;
        this.winnerName = winnerName;
        this.sharedScheduler = scheduler;
        this.worldThreadExecutor = worldThreadExecutor;
        this.countdownSeconds = COUNTDOWN_DURATION;
    }

    @Override
    public void build(UICommandBuilder cmd) {
        // Load UI file
        cmd.append("Huds/VictoryHud.ui");

        // Set initial content
        updateContent(cmd);

        // Start countdown refresh
        startAutoRefresh();
    }

    /**
     * Updates the HUD content.
     */
    private void updateContent(UICommandBuilder cmd) {
        // Victory/Defeat title
        if (isWinner) {
            cmd.set("#ResultTitle.Text", "VICTORY!");
            cmd.set("#ResultTitle.Style.TextColor", "#f1c40f"); // Gold
        } else {
            cmd.set("#ResultTitle.Text", "DEFEAT");
            cmd.set("#ResultTitle.Style.TextColor", "#e74c3c"); // Red
        }

        // Winner name
        if (winnerName != null && !winnerName.isEmpty()) {
            cmd.set("#WinnerName.Text", winnerName);
            cmd.set("#WinnerName.Style.TextColor", isWinner ? "#2ecc71" : "#b7cedd");
        } else {
            cmd.set("#WinnerName.Text", "Draw");
            cmd.set("#WinnerName.Style.TextColor", "#96a9be");
        }

        // Player stats
        Participant participant = match.getParticipant(playerUuid);
        if (participant != null) {
            cmd.set("#KillsValue.Text", String.valueOf(participant.getKills()));
            cmd.set("#DeathsValue.Text", String.valueOf(participant.getDeaths()));
            cmd.set("#DamageValue.Text", String.valueOf((int) participant.getDamageDealt()));
        } else {
            cmd.set("#KillsValue.Text", "0");
            cmd.set("#DeathsValue.Text", "0");
            cmd.set("#DamageValue.Text", "0");
        }

        // Countdown
        updateCountdown(cmd);
    }

    /**
     * Updates the countdown display.
     */
    private void updateCountdown(UICommandBuilder cmd) {
        if (countdownSeconds > 0) {
            cmd.set("#Countdown.Text", "Returning to hub in " + countdownSeconds + "...");
        } else {
            cmd.set("#Countdown.Text", "Returning to hub...");
        }
    }

    /**
     * Starts the countdown refresh task.
     */
    private void startAutoRefresh() {
        if (refreshTask != null || sharedScheduler == null) {
            return;
        }

        // Refresh every 1 second for countdown
        refreshTask = sharedScheduler.scheduleAtFixedRate(() -> {
            if (!active) {
                return;
            }

            try {
                // Decrement countdown
                if (countdownSeconds > 0) {
                    countdownSeconds--;
                }

                // Update on world thread
                if (worldThreadExecutor != null) {
                    worldThreadExecutor.accept(() -> {
                        if (!active) {
                            return;
                        }
                        try {
                            UICommandBuilder cmd = new UICommandBuilder();
                            updateCountdown(cmd);
                            update(false, cmd);
                        } catch (Exception e) {
                            // UI might not be ready, skip
                        }
                    });
                }
            } catch (Exception e) {
                // HUD might be closed
            }
        }, 1, 1, TimeUnit.SECONDS);
    }

    /**
     * Stops the refresh task.
     */
    private void stopAutoRefresh() {
        if (refreshTask != null) {
            refreshTask.cancel(true);
            refreshTask = null;
        }
    }

    /**
     * Shuts down this HUD.
     */
    public void shutdown() {
        active = false;
        stopAutoRefresh();
    }
}
