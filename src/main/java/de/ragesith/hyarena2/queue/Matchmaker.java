package de.ragesith.hyarena2.queue;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import de.ragesith.hyarena2.arena.Arena;
import de.ragesith.hyarena2.arena.ArenaConfig;
import de.ragesith.hyarena2.arena.Match;
import de.ragesith.hyarena2.arena.MatchManager;
import de.ragesith.hyarena2.bot.BotDifficulty;
import de.ragesith.hyarena2.bot.BotManager;
import de.ragesith.hyarena2.bot.BotParticipant;
import de.ragesith.hyarena2.config.Position;
import de.ragesith.hyarena2.event.EventBus;
import de.ragesith.hyarena2.event.queue.QueueMatchFoundEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles matchmaking logic - checks queues and triggers match creation.
 * Supports auto-fill with bots when enabled per arena.
 * Runs on the hub world thread periodically (every 1 second).
 */
public class Matchmaker {

    private final QueueManager queueManager;
    private final MatchManager matchManager;
    private final EventBus eventBus;
    private BotManager botManager;

    // Track when each arena started waiting for additional players (minPlayers reached)
    // Key: arenaId, Value: timestamp when minPlayers was first reached
    private final Map<String, Long> waitingStartTimes = new ConcurrentHashMap<>();

    // Track when first player joined each arena's queue (for auto-fill timer)
    // Key: arenaId, Value: timestamp when first player queued
    private final Map<String, Long> autoFillStartTimes = new ConcurrentHashMap<>();

    public Matchmaker(QueueManager queueManager, MatchManager matchManager, EventBus eventBus) {
        this.queueManager = queueManager;
        this.matchManager = matchManager;
        this.eventBus = eventBus;
    }

    /**
     * Sets the bot manager for auto-fill functionality.
     */
    public void setBotManager(BotManager botManager) {
        this.botManager = botManager;
    }

    /**
     * Checks all queues and triggers match creation when enough players are available.
     * Should be called periodically (every 1 second) on the hub world thread.
     */
    public void tick() {
        for (Arena arena : matchManager.getArenas()) {
            checkAndCreateMatch(arena);
        }
    }

    /**
     * Checks a specific arena and creates a match if possible.
     * Implements wait-for-players logic and auto-fill with bots.
     */
    private void checkAndCreateMatch(Arena arena) {
        String arenaId = arena.getId();
        ArenaConfig config = arena.getConfig();
        int queueSize = queueManager.getQueueSize(arenaId);
        int minPlayers = config.getMinPlayers();
        int maxPlayers = config.getMaxPlayers();
        int waitDelaySeconds = config.getWaitTimeSeconds();

        // Check if arena is available (not currently in a match)
        if (matchManager.isArenaInUse(arenaId)) {
            // Arena busy - clear waiting states since we can't start anyway
            waitingStartTimes.remove(arenaId);
            autoFillStartTimes.remove(arenaId);
            return;
        }

        // No players in queue - clear all timers
        if (queueSize == 0) {
            waitingStartTimes.remove(arenaId);
            autoFillStartTimes.remove(arenaId);
            return;
        }

        // Track auto-fill timer (starts when first player joins queue)
        if (config.isAutoFillEnabled() && queueSize > 0) {
            autoFillStartTimes.putIfAbsent(arenaId, System.currentTimeMillis());
        }

        boolean shouldStartMatch = false;
        boolean useAutoFill = false;

        // Check if max players reached - start immediately
        if (queueSize >= maxPlayers) {
            shouldStartMatch = true;
            waitingStartTimes.remove(arenaId);
            autoFillStartTimes.remove(arenaId);
        }
        // Check if min players reached (normal queue behavior)
        else if (queueSize >= minPlayers) {
            Long waitStartTime = waitingStartTimes.get(arenaId);

            if (waitStartTime == null) {
                // First time reaching minPlayers - start the wait timer
                waitingStartTimes.put(arenaId, System.currentTimeMillis());
                System.out.println("[Matchmaker] Arena " + arenaId + ": countdown started (" + waitDelaySeconds + "s)");
            } else {
                // Check if wait time has elapsed
                long elapsedMs = System.currentTimeMillis() - waitStartTime;
                if (elapsedMs >= waitDelaySeconds * 1000L) {
                    // Wait time expired - start the match with current players
                    shouldStartMatch = true;
                    waitingStartTimes.remove(arenaId);
                    autoFillStartTimes.remove(arenaId);
                }
            }
        }
        // Not enough players - check auto-fill
        else if (config.isAutoFillEnabled() && queueSize >= config.getMinRealPlayers()) {
            // Clear normal wait timer (we don't have minPlayers)
            waitingStartTimes.remove(arenaId);

            Long autoFillStart = autoFillStartTimes.get(arenaId);
            if (autoFillStart != null) {
                long elapsedMs = System.currentTimeMillis() - autoFillStart;
                int autoFillDelaySeconds = config.getAutoFillDelaySeconds();

                if (elapsedMs >= autoFillDelaySeconds * 1000L) {
                    // Auto-fill timer expired - start match with bots
                    shouldStartMatch = true;
                    useAutoFill = true;
                    autoFillStartTimes.remove(arenaId);
                    System.out.println("[Matchmaker] Arena " + arenaId + ": auto-fill triggered after " + autoFillDelaySeconds + "s");
                }
            }
        } else {
            // Not enough players and auto-fill not applicable
            waitingStartTimes.remove(arenaId);
            if (!config.isAutoFillEnabled()) {
                autoFillStartTimes.remove(arenaId);
            }
        }

        if (!shouldStartMatch) {
            return;
        }

        // Create the match
        createMatchWithPlayers(arena, useAutoFill);
    }

    /**
     * Creates a match with queued players, optionally filling with bots.
     */
    private void createMatchWithPlayers(Arena arena, boolean fillWithBots) {
        String arenaId = arena.getId();
        ArenaConfig config = arena.getConfig();
        int maxPlayers = config.getMaxPlayers();
        int queueSize = queueManager.getQueueSize(arenaId);

        // Determine how many players to include
        int playersToTake = Math.min(queueSize, maxPlayers);

        // Pull players from queue
        List<QueueEntry> entries = queueManager.pollEntries(arenaId, playersToTake);
        if (entries.isEmpty()) {
            return;
        }

        System.out.println("[Matchmaker] Creating match for arena " + arenaId + " with " + entries.size() + " players" +
            (fillWithBots ? " (auto-fill enabled)" : ""));

        // Create match
        Match match = matchManager.createMatch(arenaId);
        if (match == null) {
            System.err.println("[Matchmaker] Failed to create match for arena " + arenaId);
            return;
        }

        // Add players to match
        List<UUID> playerUuids = new ArrayList<>();
        int playersAdded = 0;

        for (QueueEntry entry : entries) {
            UUID playerUuid = entry.getPlayerUuid();
            playerUuids.add(playerUuid);

            // Get player reference from Universe
            PlayerRef playerRef = Universe.get().getPlayer(playerUuid);
            if (playerRef == null) {
                System.err.println("[Matchmaker] Player " + entry.getPlayerName() + " not found (disconnected?)");
                continue;
            }

            // Get Player entity
            Ref<EntityStore> entityRef = playerRef.getReference();
            if (entityRef == null) {
                System.err.println("[Matchmaker] Player " + entry.getPlayerName() + " has no entity ref");
                continue;
            }

            Store<EntityStore> store = entityRef.getStore();
            if (store == null) {
                System.err.println("[Matchmaker] Player " + entry.getPlayerName() + " has no store");
                continue;
            }

            Player player = store.getComponent(entityRef, Player.getComponentType());
            if (player == null) {
                System.err.println("[Matchmaker] Player " + entry.getPlayerName() + " component not found");
                continue;
            }

            // Add player to match via MatchManager (with kit from queue entry)
            String kitId = entry.getSelectedKitId();
            if (matchManager.addPlayerToMatch(match.getMatchId(), player, kitId)) {
                playersAdded++;
                System.out.println("[Matchmaker] Added " + entry.getPlayerName() + " to match " + match.getMatchId() +
                    (kitId != null ? " with kit: " + kitId : ""));
            } else {
                System.err.println("[Matchmaker] Failed to add " + entry.getPlayerName() + " to match");
            }
        }

        // Fill remaining slots with bots if enabled
        if (fillWithBots && botManager != null) {
            int botsNeeded = maxPlayers - playersAdded;
            if (botsNeeded > 0) {
                fillWithBots(match, arena, playersAdded, botsNeeded);
            }
        }

        // Publish event
        eventBus.publish(new QueueMatchFoundEvent(match.getMatchId(), arenaId, playerUuids));
    }

    /**
     * Fills remaining match slots with bots.
     */
    private void fillWithBots(Match match, Arena arena, int startIndex, int botCount) {
        ArenaConfig config = arena.getConfig();
        BotDifficulty difficulty = BotDifficulty.fromString(config.getBotDifficulty());
        List<ArenaConfig.SpawnPoint> spawnPoints = arena.getSpawnPoints();

        System.out.println("[Matchmaker] Spawning " + botCount + " bots for match " + match.getMatchId());

        for (int i = 0; i < botCount; i++) {
            int spawnIndex = startIndex + i;
            if (spawnIndex >= spawnPoints.size()) {
                System.err.println("[Matchmaker] No spawn point available for bot " + i);
                break;
            }

            ArenaConfig.SpawnPoint sp = spawnPoints.get(spawnIndex);
            Position spawnPos = new Position(sp.getX(), sp.getY(), sp.getZ(), sp.getYaw(), sp.getPitch());

            // Use first allowed kit or null
            String kitId = null;
            List<String> allowedKits = config.getAllowedKits();
            if (allowedKits != null && !allowedKits.isEmpty()) {
                kitId = allowedKits.get(0);
            }

            // Spawn bot
            BotParticipant bot = botManager.spawnBot(match, spawnPos, kitId, difficulty);
            if (bot != null) {
                if (match.addBot(bot)) {
                    System.out.println("[Matchmaker] Added bot " + bot.getName() + " to match");
                } else {
                    botManager.despawnBot(bot);
                    System.err.println("[Matchmaker] Failed to add bot to match");
                }
            } else {
                System.err.println("[Matchmaker] Failed to spawn bot");
            }
        }
    }

    /**
     * Gets matchmaking info for an arena.
     */
    public MatchmakingInfo getMatchmakingInfo(String arenaId) {
        Arena arena = matchManager.getArena(arenaId);
        if (arena == null) {
            return null;
        }

        ArenaConfig config = arena.getConfig();
        int queueSize = queueManager.getQueueSize(arenaId);
        int minPlayers = config.getMinPlayers();
        int maxPlayers = config.getMaxPlayers();
        int playersNeeded = Math.max(0, minPlayers - queueSize);
        String avgTime = queueManager.getAverageQueueTimeFormatted(arenaId);

        // Calculate remaining wait time if we're in the waiting phase
        int remainingWaitSeconds = -1;
        Long waitStartTime = waitingStartTimes.get(arenaId);
        if (waitStartTime != null && queueSize >= minPlayers && queueSize < maxPlayers) {
            long elapsedMs = System.currentTimeMillis() - waitStartTime;
            int waitDelaySeconds = config.getWaitTimeSeconds();
            long remainingMs = (waitDelaySeconds * 1000L) - elapsedMs;
            remainingWaitSeconds = Math.max(0, (int) (remainingMs / 1000));
        }

        // Calculate auto-fill remaining time
        int autoFillRemainingSeconds = -1;
        if (config.isAutoFillEnabled() && queueSize >= config.getMinRealPlayers() && queueSize < minPlayers) {
            Long autoFillStart = autoFillStartTimes.get(arenaId);
            if (autoFillStart != null) {
                long elapsedMs = System.currentTimeMillis() - autoFillStart;
                int autoFillDelaySeconds = config.getAutoFillDelaySeconds();
                long remainingMs = (autoFillDelaySeconds * 1000L) - elapsedMs;
                autoFillRemainingSeconds = Math.max(0, (int) (remainingMs / 1000));
            }
        }

        return new MatchmakingInfo(queueSize, minPlayers, maxPlayers, playersNeeded, avgTime,
            remainingWaitSeconds, autoFillRemainingSeconds, config.isAutoFillEnabled());
    }

    /**
     * Information about the current matchmaking state for an arena.
     */
    public static class MatchmakingInfo {
        private final int queueSize;
        private final int minPlayers;
        private final int maxPlayers;
        private final int playersNeeded;
        private final String averageQueueTime;
        private final int remainingWaitSeconds; // -1 if not in waiting phase
        private final int autoFillRemainingSeconds; // -1 if not in auto-fill countdown
        private final boolean autoFillEnabled;

        public MatchmakingInfo(int queueSize, int minPlayers, int maxPlayers, int playersNeeded,
                               String averageQueueTime, int remainingWaitSeconds,
                               int autoFillRemainingSeconds, boolean autoFillEnabled) {
            this.queueSize = queueSize;
            this.minPlayers = minPlayers;
            this.maxPlayers = maxPlayers;
            this.playersNeeded = playersNeeded;
            this.averageQueueTime = averageQueueTime;
            this.remainingWaitSeconds = remainingWaitSeconds;
            this.autoFillRemainingSeconds = autoFillRemainingSeconds;
            this.autoFillEnabled = autoFillEnabled;
        }

        public int getQueueSize() {
            return queueSize;
        }

        public int getMinPlayers() {
            return minPlayers;
        }

        public int getMaxPlayers() {
            return maxPlayers;
        }

        public int getPlayersNeeded() {
            return playersNeeded;
        }

        public String getAverageQueueTime() {
            return averageQueueTime;
        }

        public int getRemainingWaitSeconds() {
            return remainingWaitSeconds;
        }

        public int getAutoFillRemainingSeconds() {
            return autoFillRemainingSeconds;
        }

        public boolean isAutoFillEnabled() {
            return autoFillEnabled;
        }

        public boolean isWaitingForMorePlayers() {
            return remainingWaitSeconds >= 0;
        }

        public boolean isAutoFillCountdown() {
            return autoFillRemainingSeconds >= 0;
        }

        public boolean canStartMatch() {
            return playersNeeded <= 0;
        }

        public String getStatusMessage() {
            if (isWaitingForMorePlayers()) {
                return "Starting in " + remainingWaitSeconds + "s";
            }
            if (isAutoFillCountdown()) {
                return "Bot fill in " + autoFillRemainingSeconds + "s";
            }
            if (canStartMatch()) {
                return "Match starting soon!";
            }
            if (autoFillEnabled && playersNeeded > 0) {
                return "Waiting... (bots will fill)";
            }
            return "Waiting for " + playersNeeded + " more player" + (playersNeeded == 1 ? "" : "s");
        }
    }
}
