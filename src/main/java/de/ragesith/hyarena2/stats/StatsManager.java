package de.ragesith.hyarena2.stats;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import de.ragesith.hyarena2.api.ApiClient;
import de.ragesith.hyarena2.arena.Arena;
import de.ragesith.hyarena2.arena.ArenaConfig;
import de.ragesith.hyarena2.arena.MatchManager;
import de.ragesith.hyarena2.bot.BotDifficulty;
import de.ragesith.hyarena2.bot.BotParticipant;
import de.ragesith.hyarena2.event.EventBus;
import de.ragesith.hyarena2.event.match.MatchCreatedEvent;
import de.ragesith.hyarena2.event.match.MatchEndedEvent;
import de.ragesith.hyarena2.event.match.MatchFinishedEvent;
import de.ragesith.hyarena2.event.match.MatchStartedEvent;
import de.ragesith.hyarena2.event.participant.ParticipantDamagedEvent;
import de.ragesith.hyarena2.event.participant.ParticipantJoinedEvent;
import de.ragesith.hyarena2.event.participant.ParticipantKilledEvent;
import de.ragesith.hyarena2.kit.KitConfig;
import de.ragesith.hyarena2.kit.KitManager;
import de.ragesith.hyarena2.participant.Participant;
import de.ragesith.hyarena2.participant.ParticipantType;

import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Orchestrates match stats recording and web API submission.
 * Subscribes to EventBus events and builds MatchRecords independently of Match.participants,
 * so eliminated players' stats are preserved.
 */
public class StatsManager {
    private final StatsConfig config;
    private final ApiClient apiClient;
    private final EventBus eventBus;
    private final MatchManager matchManager;
    private final KitManager kitManager;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private final Map<UUID, MatchRecord> activeRecords = new ConcurrentHashMap<>();

    public StatsManager(StatsConfig config, ApiClient apiClient, EventBus eventBus,
                        MatchManager matchManager, KitManager kitManager) {
        this.config = config;
        this.apiClient = apiClient;
        this.eventBus = eventBus;
        this.matchManager = matchManager;
        this.kitManager = kitManager;
    }

    /**
     * Subscribes to all match/participant events for stats tracking.
     * Always subscribes (even when API is disabled) so records are built for logging.
     */
    public void subscribeToEvents() {
        eventBus.subscribe(MatchCreatedEvent.class, this::onMatchCreated);
        eventBus.subscribe(MatchStartedEvent.class, this::onMatchStarted);
        eventBus.subscribe(ParticipantJoinedEvent.class, this::onParticipantJoined);
        eventBus.subscribe(ParticipantKilledEvent.class, this::onParticipantKilled);
        eventBus.subscribe(ParticipantDamagedEvent.class, this::onParticipantDamaged);
        eventBus.subscribe(MatchEndedEvent.class, this::onMatchEnded);
        eventBus.subscribe(MatchFinishedEvent.class, this::onMatchFinished);
        System.out.println("[StatsManager] Subscribed to match/participant events");
    }

    // ========== Event Handlers ==========

    private void onMatchCreated(MatchCreatedEvent event) {
        MatchRecord record = new MatchRecord(event.getMatchId(), event.getArenaId(), event.getGameMode());
        activeRecords.put(event.getMatchId(), record);
    }

    private void onMatchStarted(MatchStartedEvent event) {
        MatchRecord record = activeRecords.get(event.getMatchId());
        if (record != null) {
            record.setStartedAt(Instant.now());
        }
    }

    private void onParticipantJoined(ParticipantJoinedEvent event) {
        MatchRecord record = activeRecords.get(event.getMatchId());
        if (record == null) return;

        Participant p = event.getParticipant();
        boolean isBot = p.getType() == ParticipantType.BOT;

        // For bots, uuid in the JSON payload is null; for players, it's their real UUID
        UUID realUuid = isBot ? null : p.getUniqueId();
        BotDifficulty difficulty = null;
        if (isBot && p instanceof BotParticipant bot) {
            difficulty = bot.getDifficulty();
        }

        ParticipantRecord pr = new ParticipantRecord(
            realUuid, p.getName(), isBot, difficulty, p.getSelectedKitId()
        );

        // Key by participant uniqueId (bots have generated UUIDs too)
        record.addParticipant(p.getUniqueId(), pr);
    }

    private void onParticipantKilled(ParticipantKilledEvent event) {
        MatchRecord record = activeRecords.get(event.getMatchId());
        if (record == null) return;

        Participant victim = event.getVictim();
        Participant killer = event.getKiller();
        ParticipantRecord victimRec = record.getParticipant(victim.getUniqueId());

        if (killer == null) {
            // Environmental death — count as PvE death
            if (victimRec != null) {
                victimRec.recordPveDeath();
            }
            return;
        }

        ParticipantRecord killerRec = record.getParticipant(killer.getUniqueId());
        boolean killerIsPlayer = killer.getType() == ParticipantType.PLAYER;
        boolean victimIsPlayer = victim.getType() == ParticipantType.PLAYER;

        if (killerIsPlayer && victimIsPlayer) {
            // PvP: both sides
            if (killerRec != null) killerRec.recordPvpKill();
            if (victimRec != null) victimRec.recordPvpDeath();
        } else {
            // Any involvement of a bot → PvE for both sides
            if (killerRec != null) killerRec.recordPveKill();
            if (victimRec != null) victimRec.recordPveDeath();
        }
    }

    private void onParticipantDamaged(ParticipantDamagedEvent event) {
        MatchRecord record = activeRecords.get(event.getMatchId());
        if (record == null) return;

        double damage = event.getDamage();

        ParticipantRecord victimRec = record.getParticipant(event.getVictim().getUniqueId());
        if (victimRec != null) {
            victimRec.addDamageTaken(damage);
        }

        if (event.getAttacker() != null) {
            ParticipantRecord attackerRec = record.getParticipant(event.getAttacker().getUniqueId());
            if (attackerRec != null) {
                attackerRec.addDamageDealt(damage);
            }
        }
    }

    private void onMatchEnded(MatchEndedEvent event) {
        MatchRecord record = activeRecords.get(event.getMatchId());
        if (record == null) return;

        record.setEnded(true);
        record.setWinners(event.getWinners());

        // Mark winners
        if (event.getWinners() != null) {
            for (UUID winnerId : event.getWinners()) {
                ParticipantRecord pr = record.getParticipant(winnerId);
                if (pr != null) {
                    pr.setWinner(true);
                }
            }
        }
    }

    private void onMatchFinished(MatchFinishedEvent event) {
        MatchRecord record = activeRecords.remove(event.getMatchId());
        if (record == null) return;

        record.setEndedAt(Instant.now());

        if (record.isCancelled()) {
            System.out.println("[StatsManager] Match " + event.getMatchId() + " was cancelled, skipping submission");
            return;
        }

        System.out.println("[StatsManager] Match " + event.getMatchId() + " finished (" +
            record.getParticipants().size() + " participants, " +
            record.getDurationSeconds() + "s)");

        if (config.isEnabled()) {
            submitMatchRecord(record);
        }
    }

    // ========== API Submission ==========

    private void submitMatchRecord(MatchRecord record) {
        String json = gson.toJson(record.toJsonObject());

        apiClient.postAsync("/api/match/submit", json)
            .thenAccept(response -> {
                if (response == null) return; // error already logged by ApiClient
                if (response.statusCode() == 200) {
                    System.out.println("[StatsManager] Match " + record.getMatchId() + " submitted successfully");
                } else {
                    System.err.println("[StatsManager] Match submission returned HTTP " +
                        response.statusCode() + ": " + response.body());
                }
            });
    }

    /**
     * Syncs all arena and kit configs to the web backend.
     * Called on startup if enabled + syncOnStartup is true.
     */
    public void syncConfigsToWeb() {
        if (!config.isEnabled()) {
            System.out.println("[StatsManager] Stats API disabled, skipping config sync");
            return;
        }

        JsonObject payload = new JsonObject();

        // Arenas
        JsonArray arenasArray = new JsonArray();
        for (Arena arena : matchManager.getArenas()) {
            ArenaConfig ac = arena.getConfig();
            JsonObject a = new JsonObject();
            a.addProperty("id", ac.getId());
            a.addProperty("display_name", ac.getDisplayName());
            a.addProperty("description", ""); // ArenaConfig has no description field
            a.addProperty("game_mode", ac.getGameMode());
            a.addProperty("world_name", ac.getWorldName());
            a.addProperty("min_players", ac.getMinPlayers());
            a.addProperty("max_players", ac.getMaxPlayers());
            arenasArray.add(a);
        }
        payload.add("arenas", arenasArray);

        // Kits
        JsonArray kitsArray = new JsonArray();
        for (KitConfig kit : kitManager.getAllKits()) {
            JsonObject k = new JsonObject();
            k.addProperty("id", kit.getId());
            k.addProperty("display_name", kit.getDisplayName());
            k.addProperty("description", kit.getDescription());
            kitsArray.add(k);
        }
        payload.add("kits", kitsArray);

        String json = gson.toJson(payload);
        System.out.println("[StatsManager] Syncing " + arenasArray.size() + " arenas and " +
            kitsArray.size() + " kits to web API...");

        apiClient.postAsync("/api/sync", json)
            .thenAccept(response -> {
                if (response == null) return;
                if (response.statusCode() == 200) {
                    System.out.println("[StatsManager] Config sync successful: " + response.body());
                } else {
                    System.err.println("[StatsManager] Config sync returned HTTP " +
                        response.statusCode() + ": " + response.body());
                }
            });
    }

    public StatsConfig getConfig() {
        return config;
    }
}
