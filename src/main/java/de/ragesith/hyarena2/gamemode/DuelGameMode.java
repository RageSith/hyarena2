package de.ragesith.hyarena2.gamemode;

import de.ragesith.hyarena2.arena.ArenaConfig;
import de.ragesith.hyarena2.arena.Match;
import de.ragesith.hyarena2.participant.Participant;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 1v1 last standing game mode.
 * Winner is the last player alive.
 */
public class DuelGameMode implements GameMode {
    private static final String ID = "duel";
    private static final String DISPLAY_NAME = "Duel";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getDisplayName() {
        return DISPLAY_NAME;
    }

    @Override
    public String getWebDescription() {
        return "Classic 1v1 combat. Two players enter, one leaves. No respawns — last fighter standing wins.";
    }

    @Override
    public String getDescription() {
        return "Group { LayoutMode: Top;"
            + " Label { Text: \"Duel\"; Anchor: (Height: 28); Style: (FontSize: 18, TextColor: #e8c872, RenderBold: true); }"
            + " Label { Text: \"A classic 1v1 battle. Two players enter, one leaves victorious.\"; Anchor: (Height: 40, Top: 4); Style: (FontSize: 13, TextColor: #b7cedd, Wrap: true); }"
            + " Label { Text: \"Rules\"; Anchor: (Height: 24, Top: 12); Style: (FontSize: 15, TextColor: #e8c872, RenderBold: true); }"
            + " Label { Text: \"\u2022 Two players fight head-to-head\"; Anchor: (Height: 18, Top: 4); Style: (FontSize: 12, TextColor: #96a9be); }"
            + " Label { Text: \"\u2022 No respawns \u2014 one life only\"; Anchor: (Height: 18, Top: 2); Style: (FontSize: 12, TextColor: #96a9be); }"
            + " Label { Text: \"\u2022 Last player standing wins\"; Anchor: (Height: 18, Top: 2); Style: (FontSize: 12, TextColor: #96a9be); }"
            + " Label { Text: \"\u2022 If time runs out, the player with more kills wins\"; Anchor: (Height: 18, Top: 2); Style: (FontSize: 12, TextColor: #96a9be); }"
            + " Label { Text: \"Tips\"; Anchor: (Height: 24, Top: 12); Style: (FontSize: 15, TextColor: #e8c872, RenderBold: true); }"
            + " Label { Text: \"\u2022 Choose your kit wisely \u2014 you only get one shot\"; Anchor: (Height: 18, Top: 4); Style: (FontSize: 12, TextColor: #96a9be); }"
            + " Label { Text: \"\u2022 Use the 3-second spawn immunity to position yourself\"; Anchor: (Height: 18, Top: 2); Style: (FontSize: 12, TextColor: #96a9be); }"
            + " Label { Text: \"\u2022 Watch the timer; play aggressively if time is running out\"; Anchor: (Height: 18, Top: 2); Style: (FontSize: 12, TextColor: #96a9be); }"
            + " }";
    }

    @Override
    public void onMatchStart(ArenaConfig config, List<Participant> participants) {
        // Ensure all participants start alive
        for (Participant p : participants) {
            p.setAlive(true);
        }
    }

    @Override
    public void onGameplayBegin(ArenaConfig config, List<Participant> participants) {
        // Send fight message
        for (Participant p : participants) {
            p.sendMessage("<gradient:#2ecc71:#27ae60><b>FIGHT!</b></gradient>");
        }
    }

    @Override
    public void onTick(Match match, ArenaConfig config, List<Participant> participants, int tickCount) {
        // No periodic behavior needed for duel mode
    }

    @Override
    public boolean onParticipantKilled(ArenaConfig config, Participant victim, Participant killer, List<Participant> participants) {
        victim.setAlive(false);
        victim.addDeath();

        if (killer != null) {
            killer.addKill();
        }

        // Duel ends when only one player remains alive
        long aliveCount = participants.stream().filter(Participant::isAlive).count();
        return aliveCount <= 1;
    }

    @Override
    public void onParticipantDamaged(ArenaConfig config, Participant victim, Participant attacker, double damage) {
        victim.addDamageTaken(damage);
        if (attacker != null) {
            attacker.addDamageDealt(damage);
        }
    }

    @Override
    public boolean shouldMatchEnd(ArenaConfig config, List<Participant> participants) {
        // Match ends if 0 or 1 players are alive
        long aliveCount = participants.stream().filter(Participant::isAlive).count();
        return aliveCount <= 1;
    }

    @Override
    public List<UUID> getWinners(ArenaConfig config, List<Participant> participants) {
        // Winner is the last alive participant
        List<Participant> alive = participants.stream()
                .filter(Participant::isAlive)
                .collect(Collectors.toList());

        if (alive.size() == 1) {
            return List.of(alive.get(0).getUniqueId());
        }

        // No winners if all dead (tie/draw)
        return new ArrayList<>();
    }

    @Override
    public String getVictoryMessage(ArenaConfig config, List<Participant> winners) {
        if (winners.isEmpty()) {
            return "<color:#f39c12>Match ended in a draw!</color>";
        }

        // Get winner name (assumes single winner in duel)
        Participant winner = winners.stream()
                .findFirst()
                .orElse(null);

        if (winner != null) {
            return "<gradient:#f1c40f:#f39c12><b>" + winner.getName() + "</b></gradient> <color:#f1c40f>wins the duel!</color>";
        }

        return "<color:#f39c12>Match ended!</color>";
    }

    @Override
    public boolean shouldRespawn(ArenaConfig config, Participant participant) {
        // No respawns in duel mode - last standing wins
        return false;
    }

    @Override
    public int getRespawnDelayTicks(ArenaConfig config) {
        return 0; // Not used since shouldRespawn returns false
    }
}
