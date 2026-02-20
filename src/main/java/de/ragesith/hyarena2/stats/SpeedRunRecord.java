package de.ragesith.hyarena2.stats;

/**
 * A per-arena speed run record: the best player and their time.
 */
public class SpeedRunRecord {
    private final String arenaId;
    private final String arenaName;
    private final String playerUuid;
    private final String username;
    private final int bestTimeMs;

    public SpeedRunRecord(String arenaId, String arenaName, String playerUuid, String username, int bestTimeMs) {
        this.arenaId = arenaId;
        this.arenaName = arenaName;
        this.playerUuid = playerUuid;
        this.username = username;
        this.bestTimeMs = bestTimeMs;
    }

    public String getArenaId() { return arenaId; }
    public String getArenaName() { return arenaName; }
    public String getPlayerUuid() { return playerUuid; }
    public String getUsername() { return username; }
    public int getBestTimeMs() { return bestTimeMs; }
}
