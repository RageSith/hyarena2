package de.ragesith.hyarena2.ui.hud;

import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import de.ragesith.hyarena2.arena.ArenaConfig;
import de.ragesith.hyarena2.arena.Match;
import de.ragesith.hyarena2.arena.MatchState;
import de.ragesith.hyarena2.gamemode.SpeedRunGameMode;
import de.ragesith.hyarena2.gamemode.SpeedRunPB;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * In-match HUD for speed run showing live timer, checkpoint splits, and lives.
 * Refreshes every 200ms for smooth timer display.
 */
public class SpeedRunHud extends CustomUIHud {

    private final UUID playerUuid;
    private final Match match;
    private final SpeedRunGameMode gameMode;
    private final ScheduledExecutorService sharedScheduler;
    private final Consumer<Runnable> worldThreadExecutor;
    private ScheduledFuture<?> refreshTask;

    private volatile boolean active = true;
    private static final int MAX_CHECKPOINTS = 5;

    public SpeedRunHud(PlayerRef playerRef, UUID playerUuid, Match match,
                       Consumer<Runnable> worldThreadExecutor, SpeedRunGameMode gameMode,
                       ScheduledExecutorService scheduler) {
        super(playerRef);
        this.playerUuid = playerUuid;
        this.match = match;
        this.gameMode = gameMode;
        this.sharedScheduler = scheduler;
        this.worldThreadExecutor = worldThreadExecutor;
    }

    @Override
    public void build(UICommandBuilder cmd) {
        cmd.append("Huds/SpeedRunHud.ui");

        // Populate static content
        ArenaConfig config = match.getArena().getConfig();
        cmd.set("#ArenaName.Text", match.getArena().getDisplayName());

        SpeedRunGameMode.SpeedRunState state = gameMode.getSpeedRunState(match.getMatchId());

        // PB reference
        if (state != null && state.personalBest != null) {
            cmd.set("#PBTime.Text", "PB: " + state.personalBest.getFormattedTime());
        } else {
            cmd.set("#PBTime.Text", "PB: None");
        }

        // Set up checkpoint rows
        List<ArenaConfig.CaptureZone> checkpoints = config.getCheckpoints();
        int cpCount = checkpoints != null ? Math.min(checkpoints.size(), MAX_CHECKPOINTS) : 0;
        for (int i = 0; i < cpCount; i++) {
            cmd.set("#CP" + i + ".Visible", true);
            String name = checkpoints.get(i).getDisplayName();
            if (name == null || name.isEmpty()) name = "CP " + (i + 1);
            cmd.set("#CPName" + i + ".Text", name);
        }

        startAutoRefresh();
    }

    private void updateContent(UICommandBuilder cmd) {
        SpeedRunGameMode.SpeedRunState state = gameMode.getSpeedRunState(match.getMatchId());
        if (state == null) return;

        ArenaConfig config = match.getArena().getConfig();

        // Timer
        long elapsedNanos = gameMode.getElapsedNanos(match.getMatchId());
        String timerText = SpeedRunPB.formatTime(elapsedNanos);
        cmd.set("#Timer.Text", timerText);

        // Timer color: green when paused (in zone), yellow when running
        if (state.inZone) {
            cmd.set("#Timer.Style.TextColor", "#2ecc71");
        } else {
            cmd.set("#Timer.Style.TextColor", "#f1c40f");
        }

        // Lives
        cmd.set("#Lives.Text", String.valueOf(state.livesRemaining));
        if (state.livesRemaining <= 1) {
            cmd.set("#Lives.Style.TextColor", "#e74c3c");
        } else {
            cmd.set("#Lives.Style.TextColor", "#f39c12");
        }

        // Update checkpoint splits
        List<ArenaConfig.CaptureZone> checkpoints = config.getCheckpoints();
        int cpCount = checkpoints != null ? Math.min(checkpoints.size(), MAX_CHECKPOINTS) : 0;
        for (int i = 0; i < cpCount; i++) {
            if (state.triggeredCheckpoints.contains(i)) {
                String splitTime = SpeedRunPB.formatTime(state.checkpointSplitNanos[i]);
                cmd.set("#CPSplit" + i + ".Text", splitTime);
                cmd.set("#CPName" + i + ".Style.TextColor", "#2ecc71"); // Green = reached

                // Delta vs PB
                if (state.personalBest != null && state.personalBest.getCheckpointSplitNanos() != null
                    && i < state.personalBest.getCheckpointSplitNanos().length) {
                    long pbSplit = state.personalBest.getCheckpointSplitNanos()[i];
                    long delta = state.checkpointSplitNanos[i] - pbSplit;
                    String deltaStr = SpeedRunPB.formatDelta(delta);
                    cmd.set("#CPDelta" + i + ".Text", deltaStr);
                    cmd.set("#CPDelta" + i + ".Style.TextColor", delta <= 0 ? "#2ecc71" : "#e74c3c");
                }
            }
        }

        // Finish row
        if (state.finished) {
            cmd.set("#FinishRow.Visible", true);
            cmd.set("#FinishTime.Text", SpeedRunPB.formatTime(state.finishTimeNanos));

            if (state.personalBest != null) {
                long delta = state.finishTimeNanos - state.personalBest.getTotalTimeNanos();
                cmd.set("#FinishDelta.Text", SpeedRunPB.formatDelta(delta));
                cmd.set("#FinishDelta.Style.TextColor", delta <= 0 ? "#2ecc71" : "#e74c3c");
            }
        }
    }

    private void startAutoRefresh() {
        if (refreshTask != null || sharedScheduler == null) return;

        // 200ms refresh for smooth timer
        refreshTask = sharedScheduler.scheduleAtFixedRate(() -> {
            if (!active) return;

            try {
                MatchState matchState = match.getState();
                if (matchState != MatchState.IN_PROGRESS) {
                    stopAutoRefresh();
                    return;
                }

                if (worldThreadExecutor != null) {
                    worldThreadExecutor.accept(() -> {
                        if (!active) return;
                        try {
                            UICommandBuilder cmd = new UICommandBuilder();
                            updateContent(cmd);
                            update(false, cmd);
                        } catch (Exception e) {
                            // UI might not be ready, skip
                        }
                    });
                }
            } catch (Exception e) {
                // Ignore
            }
        }, 1, 200, TimeUnit.MILLISECONDS);
    }

    private void stopAutoRefresh() {
        if (refreshTask != null) {
            refreshTask.cancel(true);
            refreshTask = null;
        }
    }

    public void shutdown() {
        active = false;
        stopAutoRefresh();
    }
}
