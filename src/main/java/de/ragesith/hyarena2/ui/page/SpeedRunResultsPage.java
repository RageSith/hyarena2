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
import de.ragesith.hyarena2.arena.ArenaConfig;
import de.ragesith.hyarena2.arena.Match;
import de.ragesith.hyarena2.gamemode.SpeedRunGameMode;
import de.ragesith.hyarena2.gamemode.SpeedRunPB;
import de.ragesith.hyarena2.ui.hud.HudManager;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.UUID;

/**
 * Results overlay for speed run matches.
 * Shows finish time or DNF, per-checkpoint splits with PB deltas, and lives used.
 * Persists after teleport — player dismisses manually.
 */
public class SpeedRunResultsPage extends InteractiveCustomUIPage<SpeedRunResultsPage.PageEventData> implements CloseablePage {

    private final UUID playerUuid;
    private final Match match;
    private final SpeedRunGameMode gameMode;
    private final HudManager hudManager;

    public SpeedRunResultsPage(PlayerRef playerRef, UUID playerUuid, Match match,
                               SpeedRunGameMode gameMode, HudManager hudManager) {
        super(playerRef, CustomPageLifetime.CanDismiss, PageEventData.CODEC);
        this.playerUuid = playerUuid;
        this.match = match;
        this.gameMode = gameMode;
        this.hudManager = hudManager;
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder cmd,
                      @Nonnull UIEventBuilder events, @Nonnull Store<EntityStore> store) {
        cmd.append("Pages/SpeedRunResultsPage.ui");

        SpeedRunGameMode.SpeedRunState state = gameMode.getSpeedRunState(match.getMatchId());
        ArenaConfig config = match.getArena().getConfig();

        if (state == null) {
            cmd.set("#ResultTitle.Text", "ERROR");
            return;
        }

        // Title
        if (state.isDNF) {
            cmd.set("#ResultTitle.Text", "DID NOT FINISH");
            cmd.set("#ResultTitle.Style.TextColor", "#e74c3c");
        } else {
            cmd.set("#ResultTitle.Text", "FINISHED!");
            cmd.set("#ResultTitle.Style.TextColor", "#f1c40f");
        }

        // New PB banner
        if (state.isNewPB && !state.isDNF) {
            cmd.set("#NewPBBanner.Visible", true);
        }

        // Final time
        if (state.finishTimeNanos > 0) {
            cmd.set("#FinalTime.Text", SpeedRunPB.formatTime(state.finishTimeNanos));
        } else {
            long accumulated = gameMode.getElapsedNanos(match.getMatchId());
            cmd.set("#FinalTime.Text", SpeedRunPB.formatTime(accumulated));
            cmd.set("#FinalTime.Style.TextColor", "#e74c3c");
        }

        // PB delta
        if (state.personalBest != null && state.finishTimeNanos > 0) {
            long delta = state.finishTimeNanos - state.personalBest.getTotalTimeNanos();
            String deltaStr = SpeedRunPB.formatDelta(delta);
            String color = delta <= 0 ? "#2ecc71" : "#e74c3c";
            cmd.set("#PBDelta.Text", "vs PB: " + deltaStr);
            cmd.set("#PBDelta.Style.TextColor", color);
        } else if (state.personalBest == null && !state.isDNF) {
            cmd.set("#PBDelta.Text", "First completion!");
            cmd.set("#PBDelta.Style.TextColor", "#f1c40f");
        }

        // Lives used
        int livesUsed = config.getMaxRespawns() - state.livesRemaining;
        cmd.set("#LivesUsedValue.Text", String.valueOf(livesUsed));

        // Checkpoints
        int totalCheckpoints = config.getCheckpoints() != null ? config.getCheckpoints().size() : 0;
        int reached = state.lastCheckpointReached + 1;
        cmd.set("#CheckpointsValue.Text", reached + "/" + totalCheckpoints);

        // Splits
        List<ArenaConfig.CaptureZone> checkpoints = config.getCheckpoints();
        int cpCount = checkpoints != null ? Math.min(checkpoints.size(), 5) : 0;
        int splitIndex = 0;

        for (int i = 0; i < cpCount; i++) {
            cmd.set("#Split" + splitIndex + ".Visible", true);
            String name = checkpoints.get(i).getDisplayName();
            if (name == null || name.isEmpty()) name = "CP " + (i + 1);
            cmd.set("#SplitName" + splitIndex + ".Text", name);

            if (state.triggeredCheckpoints.contains(i)) {
                cmd.set("#SplitTime" + splitIndex + ".Text", SpeedRunPB.formatTime(state.checkpointSplitNanos[i]));
                cmd.set("#SplitName" + splitIndex + ".Style.TextColor", "#2ecc71");

                // PB delta
                if (state.personalBest != null && state.personalBest.getCheckpointSplitNanos() != null
                    && i < state.personalBest.getCheckpointSplitNanos().length) {
                    long pbSplit = state.personalBest.getCheckpointSplitNanos()[i];
                    long delta = state.checkpointSplitNanos[i] - pbSplit;
                    cmd.set("#SplitDelta" + splitIndex + ".Text", SpeedRunPB.formatDelta(delta));
                    cmd.set("#SplitDelta" + splitIndex + ".Style.TextColor", delta <= 0 ? "#2ecc71" : "#e74c3c");
                }
            } else {
                cmd.set("#SplitTime" + splitIndex + ".Text", "---");
                cmd.set("#SplitName" + splitIndex + ".Style.TextColor", "#666666");
            }
            splitIndex++;
        }

        // Finish split
        if (state.finished) {
            cmd.set("#Split" + splitIndex + ".Visible", true);
            cmd.set("#SplitName" + splitIndex + ".Text", "Finish");
            cmd.set("#SplitName" + splitIndex + ".Style.TextColor", "#f1c40f");
            cmd.set("#SplitTime" + splitIndex + ".Text", SpeedRunPB.formatTime(state.finishTimeNanos));
            cmd.set("#SplitTime" + splitIndex + ".Style.TextColor", "#f1c40f");

            if (state.personalBest != null) {
                long delta = state.finishTimeNanos - state.personalBest.getTotalTimeNanos();
                cmd.set("#SplitDelta" + splitIndex + ".Text", SpeedRunPB.formatDelta(delta));
                cmd.set("#SplitDelta" + splitIndex + ".Style.TextColor", delta <= 0 ? "#2ecc71" : "#e74c3c");
            }
        }

        // Close button
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
        if (data == null || data.action == null) return;

        if ("close".equals(data.action)) {
            Player player = store.getComponent(ref, Player.getComponentType());
            if (player != null) {
                player.getPageManager().setPage(ref, store, Page.None);
            }
        }
    }

    @Override
    public void onDismiss(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        hudManager.unregisterPage(playerUuid, this);
        hudManager.showLobbyHud(playerUuid);
    }

    @Override
    public void shutdown() {
        hudManager.unregisterPage(playerUuid, this);
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
