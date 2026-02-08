package de.ragesith.hyarena2.ui.hud;

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
import de.ragesith.hyarena2.arena.Match;
import de.ragesith.hyarena2.gamemode.GameMode;
import de.ragesith.hyarena2.participant.Participant;
import de.ragesith.hyarena2.ui.page.CloseablePage;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * Victory/defeat overlay shown as an interactive page during ENDING state.
 * Displays winner name, final stats, and a close button.
 * Persists after teleport to hub — player dismisses manually.
 */
public class VictoryHud extends InteractiveCustomUIPage<VictoryHud.PageEventData> implements CloseablePage {

    private final UUID playerUuid;
    private final Match match;
    private final boolean isWinner;
    private final String winnerName;
    private final HudManager hudManager;

    public VictoryHud(PlayerRef playerRef, UUID playerUuid, Match match, boolean isWinner, String winnerName,
                      HudManager hudManager) {
        super(playerRef, CustomPageLifetime.CanDismiss, PageEventData.CODEC);
        this.playerUuid = playerUuid;
        this.match = match;
        this.isWinner = isWinner;
        this.winnerName = winnerName;
        this.hudManager = hudManager;
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder cmd,
                      @Nonnull UIEventBuilder events, @Nonnull Store<EntityStore> store) {
        // Load UI file
        cmd.append("Huds/VictoryHud.ui");

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

        // Game-mode-specific score (KOTH etc.)
        GameMode gm = match.getGameMode();
        String scoreLabel = gm.getScoreLabel();
        if (scoreLabel != null) {
            int score = gm.getParticipantScore(playerUuid);
            int target = gm.getScoreTarget(match.getArena().getConfig());
            cmd.set("#ScoreRow.Visible", true);
            cmd.set("#ScoreLabel.Text", scoreLabel);
            cmd.set("#ScoreValue.Text", target > 0 ? score + "/" + target : String.valueOf(score));
        }

        // Close button event binding
        events.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#CloseButton",
            EventData.of("Action", "close"),
            false
        );

        // Register with HudManager for cleanup
        hudManager.registerPage(playerUuid, this);
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, PageEventData data) {
        if (data == null || data.action == null) {
            return;
        }

        if ("close".equals(data.action)) {
            Player player = store.getComponent(ref, Player.getComponentType());
            if (player != null) {
                player.getPageManager().setPage(ref, store, Page.None);
            }
        }
    }

    @Override
    public void onDismiss(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        // Player dismissed the page (ESC or close button) — show LobbyHud
        hudManager.unregisterPage(playerUuid, this);
        hudManager.showLobbyHud(playerUuid);
    }

    @Override
    public void shutdown() {
        // Called from HudManager cleanup (disconnect, page replacement) — no LobbyHud
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
