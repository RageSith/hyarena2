package de.ragesith.hyarena2.stats;

import java.util.Collections;
import java.util.List;

/**
 * Holds a page of leaderboard data returned from the web API.
 */
public class LeaderboardResult {
    private final List<LeaderboardEntry> entries;
    private final int total;
    private final int page;
    private final int perPage;
    private final int totalPages;
    private final String gameMode;
    private final boolean error;

    public LeaderboardResult(List<LeaderboardEntry> entries, int total, int page,
                             int perPage, int totalPages, String gameMode) {
        this.entries = entries;
        this.total = total;
        this.page = page;
        this.perPage = perPage;
        this.totalPages = totalPages;
        this.gameMode = gameMode;
        this.error = false;
    }

    private LeaderboardResult(boolean error) {
        this.entries = Collections.emptyList();
        this.total = 0;
        this.page = 1;
        this.perPage = 10;
        this.totalPages = 0;
        this.gameMode = null;
        this.error = error;
    }

    /** Sentinel for API errors — triggers retry on next request. */
    public static final LeaderboardResult ERROR = new LeaderboardResult(true);

    /** Sentinel for empty data sets. */
    public static final LeaderboardResult EMPTY = new LeaderboardResult(false);

    public List<LeaderboardEntry> getEntries() { return entries; }
    public int getTotal() { return total; }
    public int getPage() { return page; }
    public int getPerPage() { return perPage; }
    public int getTotalPages() { return totalPages; }
    public String getGameMode() { return gameMode; }
    public boolean isError() { return error; }
    public boolean isEmpty() { return !error && entries.isEmpty(); }
}
