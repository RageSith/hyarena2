package de.ragesith.hyarena2.gamemode;

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
    public void onMatchStart(List<Participant> participants) {
        // Ensure all participants start alive
        for (Participant p : participants) {
            p.setAlive(true);
        }
    }

    @Override
    public void onGameplayBegin(List<Participant> participants) {
        // Send fight message
        for (Participant p : participants) {
            p.sendMessage("<gradient:#2ecc71:#27ae60><b>FIGHT!</b></gradient>");
        }
    }

    @Override
    public void onTick(List<Participant> participants, int tickCount) {
        // No periodic behavior needed for duel mode
    }

    @Override
    public boolean onParticipantKilled(Participant victim, Participant killer, List<Participant> participants) {
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
    public void onParticipantDamaged(Participant victim, Participant attacker, double damage) {
        victim.addDamageTaken(damage);
        if (attacker != null) {
            attacker.addDamageDealt(damage);
        }
    }

    @Override
    public boolean shouldMatchEnd(List<Participant> participants) {
        // Match ends if 0 or 1 players are alive
        long aliveCount = participants.stream().filter(Participant::isAlive).count();
        return aliveCount <= 1;
    }

    @Override
    public List<UUID> getWinners(List<Participant> participants) {
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
    public String getVictoryMessage(List<Participant> winners) {
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
    public boolean shouldRespawn(Participant participant) {
        // No respawns in duel mode - last standing wins
        return false;
    }

    @Override
    public int getRespawnDelayTicks() {
        return 0; // Not used since shouldRespawn returns false
    }
}
