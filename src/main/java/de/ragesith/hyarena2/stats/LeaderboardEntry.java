package de.ragesith.hyarena2.stats;

/**
 * A single row in a leaderboard result.
 */
public class LeaderboardEntry {
    private final int rank;
    private final String username;
    private final String playerUuid;
    private final int pvpKills;
    private final int pvpDeaths;
    private final double kdRatio;
    private final int matchesWon;
    private final double winRate;
    private final int pveKills;
    private final int pveDeaths;
    private final int bestWavesSurvived;
    private final int matchesPlayed;

    public LeaderboardEntry(int rank, String username, String playerUuid,
                            int pvpKills, int pvpDeaths, double kdRatio,
                            int matchesWon, double winRate,
                            int pveKills, int pveDeaths,
                            int bestWavesSurvived, int matchesPlayed) {
        this.rank = rank;
        this.username = username;
        this.playerUuid = playerUuid;
        this.pvpKills = pvpKills;
        this.pvpDeaths = pvpDeaths;
        this.kdRatio = kdRatio;
        this.matchesWon = matchesWon;
        this.winRate = winRate;
        this.pveKills = pveKills;
        this.pveDeaths = pveDeaths;
        this.bestWavesSurvived = bestWavesSurvived;
        this.matchesPlayed = matchesPlayed;
    }

    public int getRank() { return rank; }
    public String getUsername() { return username; }
    public String getPlayerUuid() { return playerUuid; }
    public int getPvpKills() { return pvpKills; }
    public int getPvpDeaths() { return pvpDeaths; }
    public double getKdRatio() { return kdRatio; }
    public int getMatchesWon() { return matchesWon; }
    public double getWinRate() { return winRate; }
    public int getPveKills() { return pveKills; }
    public int getPveDeaths() { return pveDeaths; }
    public int getBestWavesSurvived() { return bestWavesSurvived; }
    public int getMatchesPlayed() { return matchesPlayed; }
}
