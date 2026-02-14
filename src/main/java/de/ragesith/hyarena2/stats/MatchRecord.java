package de.ragesith.hyarena2.stats;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-match aggregate that lives independently of Match.participants.
 * Built incrementally from EventBus events by StatsManager.
 */
public class MatchRecord {
    private static final DateTimeFormatter ISO_FORMAT = DateTimeFormatter.ISO_INSTANT;

    private final UUID matchId;
    private final String arenaId;
    private final String gameMode;
    private Instant startedAt;
    private Instant endedAt;
    private final Map<UUID, ParticipantRecord> participants = new ConcurrentHashMap<>();
    private List<UUID> winners;
    private boolean cancelled;
    private boolean ended; // true if MatchEndedEvent was received

    public MatchRecord(UUID matchId, String arenaId, String gameMode) {
        this.matchId = matchId;
        this.arenaId = arenaId;
        this.gameMode = gameMode;
    }

    public void addParticipant(UUID uniqueId, ParticipantRecord record) {
        participants.put(uniqueId, record);
    }

    public ParticipantRecord getParticipant(UUID uniqueId) {
        return participants.get(uniqueId);
    }

    public long getDurationSeconds() {
        if (startedAt == null || endedAt == null) return 0;
        return endedAt.getEpochSecond() - startedAt.getEpochSecond();
    }

    /**
     * Builds the JSON payload matching /api/match/submit schema.
     */
    public JsonObject toJsonObject() {
        JsonObject json = new JsonObject();
        json.addProperty("arena_id", arenaId);
        json.addProperty("game_mode", gameMode);
        json.addProperty("duration_seconds", getDurationSeconds());

        if (startedAt != null) {
            json.addProperty("started_at", ISO_FORMAT.format(startedAt));
        }
        if (endedAt != null) {
            json.addProperty("ended_at", ISO_FORMAT.format(endedAt));
        }

        // winner_uuid — first non-bot winner, or null
        String winnerUuid = null;
        if (winners != null) {
            for (UUID winId : winners) {
                ParticipantRecord rec = participants.get(winId);
                if (rec != null && !rec.isBot()) {
                    winnerUuid = rec.getUuid() != null ? rec.getUuid().toString() : null;
                    break;
                }
            }
        }
        if (winnerUuid != null) {
            json.addProperty("winner_uuid", winnerUuid);
        } else {
            json.add("winner_uuid", null);
        }

        // participants array
        JsonArray participantsArray = new JsonArray();
        for (ParticipantRecord rec : participants.values()) {
            JsonObject p = new JsonObject();
            if (rec.getUuid() != null && !rec.isBot()) {
                p.addProperty("uuid", rec.getUuid().toString());
            } else {
                p.add("uuid", null);
            }
            p.addProperty("username", rec.getUsername());
            p.addProperty("is_bot", rec.isBot());
            if (rec.isBot()) {
                p.addProperty("bot_name", rec.getUsername());
                if (rec.getBotDifficulty() != null) {
                    p.addProperty("bot_difficulty", rec.getBotDifficulty().name());
                }
            }
            p.addProperty("kit_id", rec.getKitId());
            p.addProperty("pvp_kills", rec.getPvpKills());
            p.addProperty("pvp_deaths", rec.getPvpDeaths());
            p.addProperty("pve_kills", rec.getPveKills());
            p.addProperty("pve_deaths", rec.getPveDeaths());
            p.addProperty("damage_dealt", rec.getDamageDealt());
            p.addProperty("damage_taken", rec.getDamageTaken());
            p.addProperty("is_winner", rec.isWinner());

            // Economy snapshot for non-bot participants
            if (!rec.isBot()) {
                p.addProperty("arena_points", rec.getArenaPoints());
                p.addProperty("honor", (int) rec.getHonor());
                p.addProperty("honor_rank", rec.getHonorRank());
            }

            // Wave defense: last wave fully cleared while alive
            if (rec.getWavesSurvived() >= 0) {
                p.addProperty("waves_survived", rec.getWavesSurvived());
            }

            participantsArray.add(p);
        }
        json.add("participants", participantsArray);

        return json;
    }

    // Getters and setters
    public UUID getMatchId() { return matchId; }
    public String getArenaId() { return arenaId; }
    public String getGameMode() { return gameMode; }
    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }
    public Instant getEndedAt() { return endedAt; }
    public void setEndedAt(Instant endedAt) { this.endedAt = endedAt; }
    public Map<UUID, ParticipantRecord> getParticipants() { return participants; }
    public List<UUID> getWinners() { return winners; }
    public void setWinners(List<UUID> winners) { this.winners = winners; }
    public boolean isCancelled() { return !ended; }
    public void setEnded(boolean ended) { this.ended = ended; }
}
