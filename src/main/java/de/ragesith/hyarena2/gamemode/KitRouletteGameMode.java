package de.ragesith.hyarena2.gamemode;

import de.ragesith.hyarena2.arena.ArenaConfig;
import de.ragesith.hyarena2.arena.Match;
import de.ragesith.hyarena2.participant.Participant;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Kit Roulette game mode — every time a player dies and respawns they get a random kit
 * from the arena's randomKitPool. Win condition: kill target OR time limit (same as Deathmatch).
 */
public class KitRouletteGameMode implements GameMode {
    private static final String ID = "kit_roulette";
    private static final String DISPLAY_NAME = "Kit Roulette";

    private final Random random = new Random();

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
        return "Deathmatch with a twist — every time you respawn, you get a random kit. Adapt your playstyle on the fly to come out on top.";
    }

    @Override
    public String getDescription() {
        return "Group { LayoutMode: Top;"
            + " Label { Text: \"Kit Roulette\"; Anchor: (Height: 28); Style: (FontSize: 18, TextColor: #e8c872, RenderBold: true); }"
            + " Label { Text: \"Chaotic combat where your kit changes every life! Adapt to whatever loadout fate gives you.\"; Anchor: (Height: 40, Top: 4); Style: (FontSize: 13, TextColor: #b7cedd, Wrap: true); }"
            + " Label { Text: \"Rules\"; Anchor: (Height: 24, Top: 12); Style: (FontSize: 15, TextColor: #e8c872, RenderBold: true); }"
            + " Label { Text: \"\u2022 You get a random kit every time you spawn\"; Anchor: (Height: 18, Top: 4); Style: (FontSize: 12, TextColor: #96a9be); }"
            + " Label { Text: \"\u2022 Players respawn instantly after death\"; Anchor: (Height: 18, Top: 2); Style: (FontSize: 12, TextColor: #96a9be); }"
            + " Label { Text: \"\u2022 First player to reach the kill target wins\"; Anchor: (Height: 18, Top: 2); Style: (FontSize: 12, TextColor: #96a9be); }"
            + " Label { Text: \"\u2022 If time runs out, the player with the most kills wins\"; Anchor: (Height: 18, Top: 2); Style: (FontSize: 12, TextColor: #96a9be); }"
            + " Label { Text: \"Tips\"; Anchor: (Height: 24, Top: 12); Style: (FontSize: 15, TextColor: #e8c872, RenderBold: true); }"
            + " Label { Text: \"\u2022 Master every kit \u2014 you never know what you'll get next\"; Anchor: (Height: 18, Top: 4); Style: (FontSize: 12, TextColor: #96a9be); }"
            + " Label { Text: \"\u2022 Play aggressively, deaths cost nothing but a kit change\"; Anchor: (Height: 18, Top: 2); Style: (FontSize: 12, TextColor: #96a9be); }"
            + " Label { Text: \"\u2022 Adapt your playstyle to your current loadout\"; Anchor: (Height: 18, Top: 2); Style: (FontSize: 12, TextColor: #96a9be); }"
            + " }";
    }

    @Override
    public String getNextKitId(ArenaConfig config, Participant participant) {
        List<String> pool = config.getRandomKitPool();
        if (pool == null || pool.isEmpty()) {
            return null;
        }
        return pool.get(random.nextInt(pool.size()));
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
                p.sendMessage("<color:#f1c40f>First to " + config.getKillTarget() + " kills wins! Your kit changes every life!</color>");
            } else {
                p.sendMessage("<color:#f1c40f>Your kit changes every life! Most kills wins!</color>");
            }
        }
    }

    @Override
    public void onTick(Match match, ArenaConfig config, List<Participant> participants, int tickCount) {
        // Time limit handled by Match via matchDurationSeconds
    }

    @Override
    public boolean onParticipantKilled(ArenaConfig config, Participant victim, Participant killer, List<Participant> participants) {
        victim.setAlive(false);
        victim.addDeath();

        if (killer != null) {
            killer.addKill();

            // Broadcast kill with score
            String scoreText = config.getKillTarget() > 0
                ? " <color:#7f8c8d>(" + killer.getKills() + "/" + config.getKillTarget() + ")</color>"
                : " <color:#7f8c8d>(" + killer.getKills() + " kills)</color>";

            for (Participant p : participants) {
                p.sendMessage("<color:#e74c3c>" + victim.getName() + "</color> <color:#7f8c8d>was killed by</color> <color:#2ecc71>" + killer.getName() + "</color>" + scoreText);
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
        return "<gradient:#f1c40f:#f39c12><b>" + winner.getName() + "</b></gradient> <color:#f1c40f>wins with " + winner.getKills() + " kills!</color>";
    }
}
