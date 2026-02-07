package de.ragesith.hyarena2.gamemode;

import de.ragesith.hyarena2.arena.ArenaConfig;
import de.ragesith.hyarena2.participant.Participant;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Deathmatch game mode with respawns.
 * First player to reach the kill target wins.
 */
public class DeathmatchGameMode implements GameMode {
    private static final String ID = "deathmatch";
    private static final String DISPLAY_NAME = "Deathmatch";

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
            + " Label { Text: \"Deathmatch\"; Anchor: (Height: 28); Style: (FontSize: 18, TextColor: #e8c872, RenderBold: true); }"
            + " Label { Text: \"Fast-paced combat with respawns. Race to reach the kill target before your opponents.\"; Anchor: (Height: 40, Top: 4); Style: (FontSize: 13, TextColor: #b7cedd, Wrap: true); }"
            + " Label { Text: \"Rules\"; Anchor: (Height: 24, Top: 12); Style: (FontSize: 15, TextColor: #e8c872, RenderBold: true); }"
            + " Label { Text: \"\u2022 Players respawn after a short delay when killed\"; Anchor: (Height: 18, Top: 4); Style: (FontSize: 12, TextColor: #96a9be); }"
            + " Label { Text: \"\u2022 First player to reach the kill target wins\"; Anchor: (Height: 18, Top: 2); Style: (FontSize: 12, TextColor: #96a9be); }"
            + " Label { Text: \"\u2022 If time runs out, the player with the most kills wins\"; Anchor: (Height: 18, Top: 2); Style: (FontSize: 12, TextColor: #96a9be); }"
            + " Label { Text: \"\u2022 Kills and deaths are tracked on the scoreboard\"; Anchor: (Height: 18, Top: 2); Style: (FontSize: 12, TextColor: #96a9be); }"
            + " Label { Text: \"Tips\"; Anchor: (Height: 24, Top: 12); Style: (FontSize: 15, TextColor: #e8c872, RenderBold: true); }"
            + " Label { Text: \"\u2022 Stay aggressive \u2014 every kill counts toward the target\"; Anchor: (Height: 18, Top: 4); Style: (FontSize: 12, TextColor: #96a9be); }"
            + " Label { Text: \"\u2022 Use the respawn delay to reposition strategically\"; Anchor: (Height: 18, Top: 2); Style: (FontSize: 12, TextColor: #96a9be); }"
            + " Label { Text: \"\u2022 Watch the scoreboard to know how close opponents are to winning\"; Anchor: (Height: 18, Top: 2); Style: (FontSize: 12, TextColor: #96a9be); }"
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
        for (Participant p : participants) {
            p.sendMessage("<gradient:#2ecc71:#27ae60><b>FIGHT!</b></gradient>");
            if (config.getKillTarget() > 0) {
                p.sendMessage("<color:#f1c40f>First to " + config.getKillTarget() + " kills wins!</color>");
            }
        }
    }

    @Override
    public void onTick(ArenaConfig config, List<Participant> participants, int tickCount) {
        // Time limit handled by Match via matchDurationSeconds
    }

    @Override
    public boolean onParticipantKilled(ArenaConfig config, Participant victim, Participant killer, List<Participant> participants) {
        victim.setAlive(false);
        victim.addDeath();

        if (killer != null) {
            killer.addKill();

            // Broadcast kill
            for (Participant p : participants) {
                p.sendMessage("<color:#e74c3c>" + victim.getName() + "</color> <color:#7f8c8d>was killed by</color> <color:#2ecc71>" + killer.getName() + "</color> <color:#7f8c8d>(" + killer.getKills() + "/" + config.getKillTarget() + ")</color>");
            }

            // Check if killer hit the kill target
            if (config.getKillTarget() > 0 && killer.getKills() >= config.getKillTarget()) {
                return true;
            }
        } else {
            for (Participant p : participants) {
                p.sendMessage("<color:#e74c3c>" + victim.getName() + "</color> <color:#7f8c8d>died</color>");
            }
        }

        return false;
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
        if (config.getKillTarget() <= 0) {
            return false; // No kill target — match ends by time only
        }
        return participants.stream().anyMatch(p -> p.getKills() >= config.getKillTarget());
    }

    @Override
    public boolean shouldRespawn(ArenaConfig config, Participant participant) {
        return true;
    }

    @Override
    public int getRespawnDelayTicks(ArenaConfig config) {
        return config.getRespawnDelaySeconds() * 20;
    }

    @Override
    public List<UUID> getWinners(ArenaConfig config, List<Participant> participants) {
        if (participants.isEmpty()) {
            return new ArrayList<>();
        }

        int maxKills = participants.stream()
                .mapToInt(Participant::getKills)
                .max()
                .orElse(0);

        if (maxKills == 0) {
            return new ArrayList<>();
        }

        List<Participant> topKillers = participants.stream()
                .filter(p -> p.getKills() == maxKills)
                .collect(Collectors.toList());

        if (topKillers.size() == 1) {
            return List.of(topKillers.get(0).getUniqueId());
        }

        // Tie — no winner
        return new ArrayList<>();
    }

    @Override
    public String getVictoryMessage(ArenaConfig config, List<Participant> winners) {
        if (winners.isEmpty()) {
            return "<color:#f39c12>It's a draw!</color>";
        }

        Participant winner = winners.get(0);
        return "<gradient:#f1c40f:#f39c12><b>" + winner.getName() + "</b></gradient> <color:#f1c40f>dominates with " + winner.getKills() + " kills!</color>";
    }
}
