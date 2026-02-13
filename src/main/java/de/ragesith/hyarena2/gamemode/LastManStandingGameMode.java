package de.ragesith.hyarena2.gamemode;

import de.ragesith.hyarena2.arena.ArenaConfig;
import de.ragesith.hyarena2.arena.Match;
import de.ragesith.hyarena2.participant.Participant;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Free-for-all last man standing game mode.
 * No respawns — last player alive wins.
 */
public class LastManStandingGameMode implements GameMode {
    private static final String ID = "lms";
    private static final String DISPLAY_NAME = "Last Man Standing";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getDisplayName() {
        return DISPLAY_NAME;
    }

    @Override
    public String getDescription() {
        return "Group { LayoutMode: Top;"
            + " Label { Text: \"Last Man Standing\"; Anchor: (Height: 28); Style: (FontSize: 18, TextColor: #e8c872, RenderBold: true); }"
            + " Label { Text: \"A free-for-all battle royale. Multiple players, no respawns. Be the last one alive to claim victory.\"; Anchor: (Height: 40, Top: 4); Style: (FontSize: 13, TextColor: #b7cedd, Wrap: true); }"
            + " Label { Text: \"Rules\"; Anchor: (Height: 24, Top: 12); Style: (FontSize: 15, TextColor: #e8c872, RenderBold: true); }"
            + " Label { Text: \"\u2022 All players fight at once \u2014 free-for-all\"; Anchor: (Height: 18, Top: 4); Style: (FontSize: 12, TextColor: #96a9be); }"
            + " Label { Text: \"\u2022 No respawns \u2014 one life only\"; Anchor: (Height: 18, Top: 2); Style: (FontSize: 12, TextColor: #96a9be); }"
            + " Label { Text: \"\u2022 Eliminated players spectate until the match ends\"; Anchor: (Height: 18, Top: 2); Style: (FontSize: 12, TextColor: #96a9be); }"
            + " Label { Text: \"\u2022 Last player standing wins\"; Anchor: (Height: 18, Top: 2); Style: (FontSize: 12, TextColor: #96a9be); }"
            + " Label { Text: \"\u2022 If time runs out, the player with the most kills wins\"; Anchor: (Height: 18, Top: 2); Style: (FontSize: 12, TextColor: #96a9be); }"
            + " Label { Text: \"Tips\"; Anchor: (Height: 24, Top: 12); Style: (FontSize: 15, TextColor: #e8c872, RenderBold: true); }"
            + " Label { Text: \"\u2022 Let others fight first \u2014 pick off weakened enemies\"; Anchor: (Height: 18, Top: 4); Style: (FontSize: 12, TextColor: #96a9be); }"
            + " Label { Text: \"\u2022 Stay near the center to avoid being cornered\"; Anchor: (Height: 18, Top: 2); Style: (FontSize: 12, TextColor: #96a9be); }"
            + " Label { Text: \"\u2022 Keep track of how many players remain\"; Anchor: (Height: 18, Top: 2); Style: (FontSize: 12, TextColor: #96a9be); }"
            + " }";
    }

    @Override
    public void onMatchStart(ArenaConfig config, List<Participant> participants) {
        for (Participant p : participants) {
            p.setAlive(true);
        }
    }

    @Override
    public void onGameplayBegin(ArenaConfig config, List<Participant> participants) {
        long aliveCount = participants.stream().filter(Participant::isAlive).count();
        for (Participant p : participants) {
            p.sendMessage("<gradient:#2ecc71:#27ae60><b>FIGHT!</b></gradient>");
            p.sendMessage("<color:#f1c40f>" + aliveCount + " players remain</color>");
        }
    }

    @Override
    public void onTick(Match match, ArenaConfig config, List<Participant> participants, int tickCount) {
        // No periodic behavior needed
    }

    @Override
    public boolean onParticipantKilled(ArenaConfig config, Participant victim, Participant killer, List<Participant> participants) {
        victim.setAlive(false);
        victim.addDeath();

        if (killer != null) {
            killer.addKill();
        }

        long aliveCount = participants.stream().filter(Participant::isAlive).count();

        // Broadcast elimination
        String eliminationMsg;
        if (killer != null) {
            eliminationMsg = "<color:#e74c3c>" + victim.getName() + "</color> <color:#95a5a6>was eliminated by</color> <color:#e74c3c>" + killer.getName() + "</color><color:#95a5a6>!</color> <color:#f1c40f>" + aliveCount + " players remain</color>";
        } else {
            eliminationMsg = "<color:#e74c3c>" + victim.getName() + "</color> <color:#95a5a6>was eliminated!</color> <color:#f1c40f>" + aliveCount + " players remain</color>";
        }
        for (Participant p : participants) {
            p.sendMessage(eliminationMsg);
        }

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
        long aliveCount = participants.stream().filter(Participant::isAlive).count();
        return aliveCount <= 1;
    }

    @Override
    public List<UUID> getWinners(ArenaConfig config, List<Participant> participants) {
        List<Participant> alive = participants.stream()
                .filter(Participant::isAlive)
                .collect(Collectors.toList());

        if (alive.size() == 1) {
            return List.of(alive.get(0).getUniqueId());
        }

        return new ArrayList<>();
    }

    @Override
    public String getVictoryMessage(ArenaConfig config, List<Participant> winners) {
        if (winners.isEmpty()) {
            return "<color:#f39c12>No one survived!</color>";
        }

        Participant winner = winners.stream()
                .findFirst()
                .orElse(null);

        if (winner != null) {
            return "<gradient:#f1c40f:#f39c12><b>" + winner.getName() + "</b></gradient> <color:#f1c40f>is the last one standing!</color>";
        }

        return "<color:#f39c12>Match ended!</color>";
    }

    @Override
    public boolean shouldRespawn(ArenaConfig config, Participant participant) {
        return false;
    }

    @Override
    public int getRespawnDelayTicks(ArenaConfig config) {
        return 0;
    }
}
