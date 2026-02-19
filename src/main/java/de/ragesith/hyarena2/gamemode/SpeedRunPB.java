package de.ragesith.hyarena2.gamemode;

import java.util.UUID;

/**
 * Data class representing a player's personal best time for a speedrun arena.
 */
public class SpeedRunPB {
    private String arenaId;
    private String playerUuid;
    private long totalTimeNanos;
    private long[] checkpointSplitNanos;
    private long finishTimestamp;

    public SpeedRunPB() {}

    public SpeedRunPB(String arenaId, UUID playerUuid, long totalTimeNanos,
                      long[] checkpointSplitNanos, long finishTimestamp) {
        this.arenaId = arenaId;
        this.playerUuid = playerUuid.toString();
        this.totalTimeNanos = totalTimeNanos;
        this.checkpointSplitNanos = checkpointSplitNanos;
        this.finishTimestamp = finishTimestamp;
    }

    public String getArenaId() { return arenaId; }
    public String getPlayerUuid() { return playerUuid; }
    public long getTotalTimeNanos() { return totalTimeNanos; }
    public long[] getCheckpointSplitNanos() { return checkpointSplitNanos; }
    public long getFinishTimestamp() { return finishTimestamp; }

    /**
     * Formats a nanosecond time value as "M:SS.mmm".
     */
    public static String formatTime(long nanos) {
        if (nanos <= 0) return "--:--.---";
        long totalMs = nanos / 1_000_000;
        long minutes = totalMs / 60_000;
        long seconds = (totalMs % 60_000) / 1000;
        long millis = totalMs % 1000;
        return String.format("%d:%02d.%03d", minutes, seconds, millis);
    }

    public String getFormattedTime() {
        return formatTime(totalTimeNanos);
    }

    /**
     * Formats a delta (current split - PB split) with +/- prefix.
     * Positive = behind PB, negative = ahead of PB.
     */
    public static String formatDelta(long deltaNanos) {
        String prefix = deltaNanos >= 0 ? "+" : "-";
        long absMs = Math.abs(deltaNanos) / 1_000_000;
        long seconds = absMs / 1000;
        long millis = absMs % 1000;
        return prefix + seconds + "." + String.format("%03d", millis);
    }
}
