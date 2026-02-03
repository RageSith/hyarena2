package de.ragesith.hyarena2.event.queue;

import de.ragesith.hyarena2.event.Event;

import java.util.List;
import java.util.UUID;

/**
 * Fired when a match is found from the queue
 */
public class QueueMatchFoundEvent implements Event {
    private final UUID matchId;
    private final String arenaId;
    private final List<UUID> playerUuids;

    public QueueMatchFoundEvent(UUID matchId, String arenaId, List<UUID> playerUuids) {
        this.matchId = matchId;
        this.arenaId = arenaId;
        this.playerUuids = playerUuids;
    }

    public UUID getMatchId() {
        return matchId;
    }

    public String getArenaId() {
        return arenaId;
    }

    public List<UUID> getPlayerUuids() {
        return playerUuids;
    }
}
