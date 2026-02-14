package de.ragesith.hyarena2.stats;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import de.ragesith.hyarena2.api.ApiClient;
import de.ragesith.hyarena2.arena.Arena;
import de.ragesith.hyarena2.arena.ArenaConfig;
import de.ragesith.hyarena2.arena.Match;
import de.ragesith.hyarena2.arena.MatchManager;
import de.ragesith.hyarena2.bot.BotDifficulty;
import de.ragesith.hyarena2.bot.BotParticipant;
import de.ragesith.hyarena2.gamemode.GameMode;
import de.ragesith.hyarena2.economy.EconomyManager;
import de.ragesith.hyarena2.economy.HonorManager;
import de.ragesith.hyarena2.event.EventBus;
import de.ragesith.hyarena2.event.economy.ArenaPointsEarnedEvent;
import de.ragesith.hyarena2.event.economy.ArenaPointsSpentEvent;
import de.ragesith.hyarena2.event.economy.HonorEarnedEvent;
import de.ragesith.hyarena2.event.economy.HonorRankChangedEvent;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Orchestrates match stats recording and web API submission.
 * Subscribes to EventBus events and builds MatchRecords independently of Match.participants,
 * so eliminated players' stats are preserved.
 */
public class StatsManager {
    private static final long FLUSH_DELAY_MS = 2000; // 2s debounce

    private final StatsConfig config;
    private final ApiClient apiClient;
    private final EventBus eventBus;
    private final MatchManager matchManager;
    private final KitManager kitManager;
    private final EconomyManager economyManager;
    private final HonorManager honorManager;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private final Map<UUID, MatchRecord> activeRecords = new ConcurrentHashMap<>();

    // Dirty-player sync for economy pushes
    private final Set<UUID> dirtyPlayers = ConcurrentHashMap.newKeySet();
    private volatile ScheduledExecutorService syncScheduler;
    private volatile ScheduledFuture<?> pendingFlush;

    public StatsManager(StatsConfig config, ApiClient apiClient, EventBus eventBus,
                        MatchManager matchManager, KitManager kitManager,
                        EconomyManager economyManager, HonorManager honorManager) {
        this.config = config;
        this.apiClient = apiClient;
        this.eventBus = eventBus;
        this.matchManager = matchManager;
        this.kitManager = kitManager;
        this.economyManager = economyManager;
        this.honorManager = honorManager;
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

        // Economy events — mark players dirty for web sync
        eventBus.subscribe(ArenaPointsEarnedEvent.class, e -> markDirty(e.getPlayerUuid()));
        eventBus.subscribe(ArenaPointsSpentEvent.class, e -> markDirty(e.getPlayerUuid()));
        eventBus.subscribe(HonorEarnedEvent.class, e -> markDirty(e.getPlayerUuid()));
        eventBus.subscribe(HonorRankChangedEvent.class, e -> markDirty(e.getPlayerUuid()));

        System.out.println("[StatsManager] Subscribed to match/participant/economy events");
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

        // Capture per-player waves survived (wave_defense)
        // Must run here (not in onMatchFinished) because WaveDefenseGameMode cleans up state in finish()
        Match match = matchManager.getMatch(event.getMatchId());
        if (match != null) {
            GameMode gameMode = match.getGameMode();
            for (Map.Entry<UUID, ParticipantRecord> entry : record.getParticipants().entrySet()) {
                int waves = gameMode.getParticipantWavesSurvived(event.getMatchId(), entry.getKey());
                if (waves >= 0) {
                    entry.getValue().setWavesSurvived(waves);
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

        // Snapshot economy data onto each non-bot participant before submission
        for (ParticipantRecord rec : record.getParticipants().values()) {
            if (!rec.isBot() && rec.getUuid() != null) {
                rec.setArenaPoints(economyManager.getArenaPoints(rec.getUuid()));
                rec.setHonor(economyManager.getHonor(rec.getUuid()));
                rec.setHonorRank(honorManager.getRankDisplayName(rec.getUuid()));
            }
        }

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

        // Game Modes
        JsonArray gameModesArray = new JsonArray();
        for (GameMode gm : matchManager.getGameModes()) {
            JsonObject g = new JsonObject();
            g.addProperty("id", gm.getId());
            g.addProperty("display_name", gm.getDisplayName());
            g.addProperty("description", gm.getWebDescription());
            gameModesArray.add(g);
        }
        payload.add("game_modes", gameModesArray);

        String json = gson.toJson(payload);
        System.out.println("[StatsManager] Syncing " + arenasArray.size() + " arenas, " +
            kitsArray.size() + " kits, and " + gameModesArray.size() + " game modes to web API...");

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

    // ========== Dirty-Player Economy Sync ==========

    /**
     * Marks a player as dirty so their economy data will be pushed to the web API.
     * Schedules a debounced flush if one isn't already pending.
     */
    private void markDirty(UUID uuid) {
        if (!config.isEnabled() || syncScheduler == null) return;
        dirtyPlayers.add(uuid);

        if (pendingFlush == null || pendingFlush.isDone()) {
            pendingFlush = syncScheduler.schedule(this::flushDirtyPlayers, FLUSH_DELAY_MS, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Flushes all dirty players' economy data to the web API.
     * Snapshots and clears the dirty set, then POSTs a batch update.
     * On failure, re-adds UUIDs for retry on the next flush.
     */
    private void flushDirtyPlayers() {
        if (dirtyPlayers.isEmpty()) return;

        // Snapshot and clear
        List<UUID> toFlush = new ArrayList<>(dirtyPlayers);
        dirtyPlayers.removeAll(toFlush);

        JsonObject payload = new JsonObject();
        JsonArray playersArray = new JsonArray();

        for (UUID uuid : toFlush) {
            var playerData = economyManager.getPlayerData(uuid);
            if (playerData == null) continue;

            JsonObject p = new JsonObject();
            p.addProperty("uuid", uuid.toString());
            p.addProperty("username", playerData.getPlayerName());
            p.addProperty("arena_points", economyManager.getArenaPoints(uuid));
            p.addProperty("honor", (int) economyManager.getHonor(uuid));
            p.addProperty("honor_rank", honorManager.getRankDisplayName(uuid));
            playersArray.add(p);
        }

        if (playersArray.isEmpty()) return;

        payload.add("players", playersArray);
        String json = gson.toJson(payload);

        System.out.println("[StatsManager] Flushing economy data for " + playersArray.size() + " player(s)");

        apiClient.postAsync("/api/player/sync", json)
            .thenAccept(response -> {
                if (response == null) {
                    // Network error — re-add for retry
                    dirtyPlayers.addAll(toFlush);
                    return;
                }
                if (response.statusCode() == 200) {
                    System.out.println("[StatsManager] Economy sync successful for " + playersArray.size() + " player(s)");
                } else {
                    System.err.println("[StatsManager] Economy sync returned HTTP " +
                        response.statusCode() + ": " + response.body());
                    dirtyPlayers.addAll(toFlush);
                }
            });
    }

    /**
     * Eagerly flushes a single player's economy data (e.g. on disconnect).
     */
    public void flushPlayerEconomy(UUID uuid) {
        if (!config.isEnabled()) return;

        // Remove from dirty set so the periodic flush won't duplicate
        dirtyPlayers.remove(uuid);

        var playerData = economyManager.getPlayerData(uuid);
        if (playerData == null) return;

        JsonObject payload = new JsonObject();
        JsonArray playersArray = new JsonArray();

        JsonObject p = new JsonObject();
        p.addProperty("uuid", uuid.toString());
        p.addProperty("username", playerData.getPlayerName());
        p.addProperty("arena_points", economyManager.getArenaPoints(uuid));
        p.addProperty("honor", (int) economyManager.getHonor(uuid));
        p.addProperty("honor_rank", honorManager.getRankDisplayName(uuid));
        playersArray.add(p);

        payload.add("players", playersArray);
        String json = gson.toJson(payload);

        apiClient.postAsync("/api/player/sync", json)
            .thenAccept(response -> {
                if (response != null && response.statusCode() == 200) {
                    System.out.println("[StatsManager] Disconnect flush for " + playerData.getPlayerName() + " successful");
                }
            });
    }

    /**
     * Initializes the sync scheduler for periodic and debounced economy flushes.
     * Must be called after subscribeToEvents().
     */
    public void initSyncScheduler(ScheduledExecutorService scheduler) {
        this.syncScheduler = scheduler;

        // Safety-net periodic flush every 5 minutes
        scheduler.scheduleAtFixedRate(() -> {
            try {
                flushDirtyPlayers();
            } catch (Exception e) {
                System.err.println("[StatsManager] Error in periodic economy flush: " + e.getMessage());
            }
        }, 5, 5, TimeUnit.MINUTES);

        System.out.println("[StatsManager] Sync scheduler initialized (5-min safety-net flush)");
    }

    /**
     * Performs a final flush of all dirty players on shutdown.
     */
    public void shutdown() {
        System.out.println("[StatsManager] Shutting down, flushing remaining dirty players...");
        flushDirtyPlayers();
    }

    public StatsConfig getConfig() {
        return config;
    }
}
