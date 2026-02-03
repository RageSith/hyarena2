package de.ragesith.hyarena2.queue;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import de.ragesith.hyarena2.arena.Arena;
import de.ragesith.hyarena2.arena.Match;
import de.ragesith.hyarena2.arena.MatchManager;
import de.ragesith.hyarena2.event.EventBus;
import de.ragesith.hyarena2.event.queue.QueueMatchFoundEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles matchmaking logic - checks queues and triggers match creation.
 * Runs on the hub world thread periodically (every 1 second).
 */
public class Matchmaker {

    private final QueueManager queueManager;
    private final MatchManager matchManager;
    private final EventBus eventBus;

    // Track when each arena started waiting for additional players
    // Key: arenaId, Value: timestamp when minPlayers was first reached
    private final Map<String, Long> waitingStartTimes = new ConcurrentHashMap<>();

    public Matchmaker(QueueManager queueManager, MatchManager matchManager, EventBus eventBus) {
        this.queueManager = queueManager;
        this.matchManager = matchManager;
        this.eventBus = eventBus;
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
     * Implements wait-for-players logic: waits for additional players after
     * minPlayers is reached, up to waitTimeSeconds or until maxPlayers.
     */
    private void checkAndCreateMatch(Arena arena) {
        String arenaId = arena.getId();
        int queueSize = queueManager.getQueueSize(arenaId);
        int minPlayers = arena.getConfig().getMinPlayers();
        int maxPlayers = arena.getConfig().getMaxPlayers();
        int waitDelaySeconds = arena.getConfig().getWaitTimeSeconds();

        // Not enough players - clear waiting state
        if (queueSize < minPlayers) {
            waitingStartTimes.remove(arenaId);
            return;
        }

        // Check if arena is available (not currently in a match)
        if (matchManager.isArenaInUse(arenaId)) {
            // Arena busy - clear waiting state since we can't start anyway
            waitingStartTimes.remove(arenaId);
            return;
        }

        boolean shouldStartMatch = false;

        // Check if max players reached - start immediately
        if (queueSize >= maxPlayers) {
            shouldStartMatch = true;
            waitingStartTimes.remove(arenaId);
        } else {
            // We have minPlayers but not maxPlayers - check wait timer
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
                }
            }
        }

        if (!shouldStartMatch) {
            return;
        }

        // Determine how many players to include in this match
        int matchSize = Math.min(queueSize, maxPlayers);

        // Pull players from queue
        List<QueueEntry> entries = queueManager.pollEntries(arenaId, matchSize);
        if (entries.isEmpty()) {
            return;
        }

        System.out.println("[Matchmaker] Creating match for arena " + arenaId + " with " + entries.size() + " players");

        // Create match
        Match match = matchManager.createMatch(arenaId);
        if (match == null) {
            System.err.println("[Matchmaker] Failed to create match for arena " + arenaId);
            // TODO: Could return entries to queue
            return;
        }

        // Add players to match
        World arenaWorld = arena.getWorld();
        List<UUID> playerUuids = new ArrayList<>();

        for (QueueEntry entry : entries) {
            UUID playerUuid = entry.getPlayerUuid();
            playerUuids.add(playerUuid);

            // Get player reference from Universe
            PlayerRef playerRef = Universe.get().getPlayer(playerUuid);
            if (playerRef == null) {
                System.err.println("[Matchmaker] Player " + entry.getPlayerName() + " not found (disconnected?)");
                continue;
            }

            // Get Player entity - need to find them in their current world
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

            // Add player to match via MatchManager
            if (matchManager.addPlayerToMatch(match.getMatchId(), player)) {
                System.out.println("[Matchmaker] Added " + entry.getPlayerName() + " to match " + match.getMatchId());
            } else {
                System.err.println("[Matchmaker] Failed to add " + entry.getPlayerName() + " to match");
            }
        }

        // Publish event
        eventBus.publish(new QueueMatchFoundEvent(match.getMatchId(), arenaId, playerUuids));
    }

    /**
     * Gets matchmaking info for an arena.
     */
    public MatchmakingInfo getMatchmakingInfo(String arenaId) {
        Arena arena = matchManager.getArena(arenaId);
        if (arena == null) {
            return null;
        }

        int queueSize = queueManager.getQueueSize(arenaId);
        int minPlayers = arena.getConfig().getMinPlayers();
        int maxPlayers = arena.getConfig().getMaxPlayers();
        int playersNeeded = Math.max(0, minPlayers - queueSize);
        String avgTime = queueManager.getAverageQueueTimeFormatted(arenaId);

        // Calculate remaining wait time if we're in the waiting phase
        int remainingWaitSeconds = -1;
        Long waitStartTime = waitingStartTimes.get(arenaId);
        if (waitStartTime != null && queueSize >= minPlayers && queueSize < maxPlayers) {
            long elapsedMs = System.currentTimeMillis() - waitStartTime;
            int waitDelaySeconds = arena.getConfig().getWaitTimeSeconds();
            long remainingMs = (waitDelaySeconds * 1000L) - elapsedMs;
            remainingWaitSeconds = Math.max(0, (int) (remainingMs / 1000));
        }

        return new MatchmakingInfo(queueSize, minPlayers, maxPlayers, playersNeeded, avgTime, remainingWaitSeconds);
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

        public MatchmakingInfo(int queueSize, int minPlayers, int maxPlayers, int playersNeeded,
                               String averageQueueTime, int remainingWaitSeconds) {
            this.queueSize = queueSize;
            this.minPlayers = minPlayers;
            this.maxPlayers = maxPlayers;
            this.playersNeeded = playersNeeded;
            this.averageQueueTime = averageQueueTime;
            this.remainingWaitSeconds = remainingWaitSeconds;
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

        public boolean isWaitingForMorePlayers() {
            return remainingWaitSeconds >= 0;
        }

        public boolean canStartMatch() {
            return playersNeeded <= 0;
        }

        public String getStatusMessage() {
            if (isWaitingForMorePlayers()) {
                return "Starting in " + remainingWaitSeconds + "s";
            }
            if (canStartMatch()) {
                return "Match starting soon!";
            }
            return "Waiting for " + playersNeeded + " more player" + (playersNeeded == 1 ? "" : "s");
        }
    }
}
