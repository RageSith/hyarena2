package de.ragesith.hyarena2.arena;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.NotificationStyle;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatsModule;
import com.hypixel.hytale.server.core.modules.entitystats.asset.EntityStatType;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import com.hypixel.hytale.server.core.util.EventTitleUtil;
import com.hypixel.hytale.server.core.util.NotificationUtil;
import de.ragesith.hyarena2.config.Position;
import fi.sulku.hytale.TinyMsg;
import de.ragesith.hyarena2.event.EventBus;
import de.ragesith.hyarena2.event.match.MatchCreatedEvent;
import de.ragesith.hyarena2.event.match.MatchEndedEvent;
import de.ragesith.hyarena2.event.match.MatchFinishedEvent;
import de.ragesith.hyarena2.event.match.MatchStartedEvent;
import de.ragesith.hyarena2.event.participant.ParticipantDamagedEvent;
import de.ragesith.hyarena2.event.participant.ParticipantJoinedEvent;
import de.ragesith.hyarena2.event.participant.ParticipantKilledEvent;
import de.ragesith.hyarena2.event.participant.ParticipantLeftEvent;
import de.ragesith.hyarena2.gamemode.GameMode;
import de.ragesith.hyarena2.hub.HubManager;
import de.ragesith.hyarena2.participant.Participant;
import de.ragesith.hyarena2.participant.PlayerParticipant;

import de.ragesith.hyarena2.utils.PlayerMovementControl;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Core match class that manages match state, participants, and game flow.
 * Implements a state machine: WAITING → STARTING → IN_PROGRESS → ENDING → FINISHED
 */
public class Match {
    private final UUID matchId;
    private final Arena arena;
    private final GameMode gameMode;
    private final EventBus eventBus;
    private final HubManager hubManager;

    private final Map<UUID, Participant> participants;
    private final Set<UUID> arrivedPlayers; // Players who have completed teleport to arena
    private MatchState state;
    private int tickCount;
    private int countdownTicks;
    private int victoryDelayTicks;
    private List<UUID> winners;

    private static final int TICKS_PER_SECOND = 20;
    private static final int VICTORY_DELAY_SECONDS = 3;

    public Match(Arena arena, GameMode gameMode, EventBus eventBus, HubManager hubManager) {
        this.matchId = UUID.randomUUID();
        this.arena = arena;
        this.gameMode = gameMode;
        this.eventBus = eventBus;
        this.hubManager = hubManager;
        this.participants = new ConcurrentHashMap<>();
        this.arrivedPlayers = ConcurrentHashMap.newKeySet();
        this.state = MatchState.WAITING;
        this.tickCount = 0;
        this.countdownTicks = 0;
        this.victoryDelayTicks = 0;
        this.winners = new ArrayList<>();

        // Fire creation event
        eventBus.publish(new MatchCreatedEvent(matchId, arena.getId(), gameMode.getId()));
    }

    public UUID getMatchId() {
        return matchId;
    }

    public Arena getArena() {
        return arena;
    }

    public GameMode getGameMode() {
        return gameMode;
    }

    public MatchState getState() {
        return state;
    }

    public List<Participant> getParticipants() {
        return new ArrayList<>(participants.values());
    }

    public Participant getParticipant(UUID uuid) {
        return participants.get(uuid);
    }

    public boolean hasParticipant(UUID uuid) {
        return participants.containsKey(uuid);
    }

    /**
     * Adds a player to the match and teleports them to a spawn point.
     * @return true if successfully added
     */
    public synchronized boolean addPlayer(Player player) {
        if (state != MatchState.WAITING) {
            return false;
        }

        // Get PlayerRef to access UUID
        Ref<EntityStore> ref = player.getReference();
        if (ref == null) return false;
        Store<EntityStore> store = ref.getStore();
        if (store == null) return false;
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        if (playerRef == null) return false;

        UUID playerUuid = playerRef.getUuid();
        if (participants.containsKey(playerUuid)) {
            return false;
        }

        // Check capacity
        if (participants.size() >= arena.getMaxPlayers()) {
            return false;
        }

        // Create participant
        Participant participant = new PlayerParticipant(playerUuid, player.getDisplayName());
        participants.put(playerUuid, participant);

        // Get spawn point
        int spawnIndex = participants.size() - 1;
        ArenaConfig.SpawnPoint spawnConfig = arena.getSpawnPoints().get(spawnIndex);

        // Convert to Position
        Position spawnPos = new Position(
                spawnConfig.getX(), spawnConfig.getY(), spawnConfig.getZ(),
                spawnConfig.getYaw(), spawnConfig.getPitch()
        );

        // Teleport to arena, then freeze player after a short delay
        World arenaWorld = arena.getWorld();
        hubManager.teleportPlayerToWorld(player, spawnPos, arenaWorld, () -> {
            participant.sendMessage("<color:#2ecc71>You have joined the match!</color>");
            // Wait for player to fully load, then freeze and mark as arrived
            CompletableFuture.delayedExecutor(500, TimeUnit.MILLISECONDS).execute(() -> {
                arenaWorld.execute(() -> {
                    PlayerRef pRef = Universe.get().getPlayer(playerUuid);
                    if (pRef != null) {
                        PlayerMovementControl.disableMovementForPlayer(pRef, arenaWorld);
                        System.out.println("[Match] Froze player after teleport: " + participant.getName());
                    }

                    // Heal player to full health on arena entry
                    healPlayer(playerUuid, arenaWorld);

                    // Mark player as arrived in arena
                    arrivedPlayers.add(playerUuid);
                    System.out.println("[Match] Player arrived: " + participant.getName() +
                        " (" + arrivedPlayers.size() + "/" + participants.size() + ")");

                    // Check if all players have arrived and we have enough to start
                    checkAndStartIfReady();
                });
            });
        });

        // Fire event
        eventBus.publish(new ParticipantJoinedEvent(matchId, participant));

        return true;
    }

    /**
     * Checks if all participants have arrived and starts the match if ready.
     */
    private void checkAndStartIfReady() {
        if (state != MatchState.WAITING) {
            return;
        }

        // Check if we have enough players AND all have arrived
        if (participants.size() >= arena.getMinPlayers() &&
            arrivedPlayers.size() >= participants.size()) {
            System.out.println("[Match] All players arrived, starting match!");
            start();
        }
    }

    /**
     * Removes a participant from the match (disconnect or kick).
     */
    public synchronized void removeParticipant(UUID uuid, String reason) {
        Participant participant = participants.remove(uuid);
        if (participant == null) {
            return;
        }

        // Remove from arrived tracking
        arrivedPlayers.remove(uuid);

        // Fire event
        eventBus.publish(new ParticipantLeftEvent(matchId, participant, reason));

        // Teleport player back to hub and unfreeze
        Player player = getPlayerFromUuid(uuid);
        if (player != null) {
            hubManager.teleportToHub(player, () -> {
                // Unfreeze after teleport to hub (runs on hub world thread via HubManager)
                PlayerRef playerRef = Universe.get().getPlayer(uuid);
                if (playerRef != null) {
                    World hubWorld = hubManager.getHubWorld();
                    PlayerMovementControl.enableMovementForPlayer(playerRef, hubWorld);
                }
            });
        }

        // Handle state-specific logic
        if (state == MatchState.IN_PROGRESS) {
            // Treat as death
            participant.setAlive(false);
            participant.addDeath();

            // Check if match should end
            if (gameMode.shouldMatchEnd(getParticipants())) {
                end();
            }
        } else if (state == MatchState.WAITING || state == MatchState.STARTING) {
            // Cancel match if not enough players
            if (participants.size() < arena.getMinPlayers()) {
                cancel("Not enough players");
            }
        }
    }

    /**
     * Starts the match (transitions to STARTING state and begins countdown).
     */
    public synchronized void start() {
        System.out.println("[Match] start() called, current state: " + state);
        if (state != MatchState.WAITING) {
            System.out.println("[Match] Cannot start - not in WAITING state");
            return;
        }

        state = MatchState.STARTING;
        countdownTicks = arena.getWaitTimeSeconds() * TICKS_PER_SECOND;
        System.out.println("[Match] State changed to STARTING, countdown: " + countdownTicks + " ticks");

        // Freeze all players during countdown
        freezeAllParticipants();

        // Notify game mode
        gameMode.onMatchStart(getParticipants());

        // Fire event
        eventBus.publish(new MatchStartedEvent(matchId, arena.getWaitTimeSeconds()));

        // Send countdown message
        broadcast("<color:#2ecc71>Match starting in </color><color:#f1c40f>" + arena.getWaitTimeSeconds() + "</color><color:#2ecc71> seconds!</color>");
    }

    /**
     * Begins gameplay (transitions to IN_PROGRESS state).
     */
    private synchronized void beginGameplay() {
        if (state != MatchState.STARTING) {
            return;
        }

        state = MatchState.IN_PROGRESS;
        tickCount = 0;

        // Unfreeze all players - fight begins!
        unfreezeAllParticipants();

        // Notify game mode
        gameMode.onGameplayBegin(getParticipants());
    }

    /**
     * Ends the match (transitions to ENDING state).
     */
    public synchronized void end() {
        if (state != MatchState.IN_PROGRESS) {
            return;
        }

        state = MatchState.ENDING;
        victoryDelayTicks = VICTORY_DELAY_SECONDS * TICKS_PER_SECOND;

        // Freeze all players (no HUD - let them see the victory message)
        freezeAllParticipantsNoHud();

        // Heal all alive players
        healAllAliveParticipants();

        // Determine winners
        winners = gameMode.getWinners(getParticipants());

        // Get victory message
        List<Participant> winnerParticipants = winners.stream()
                .map(participants::get)
                .filter(Objects::nonNull)
                .toList();
        String victoryMessage = gameMode.getVictoryMessage(winnerParticipants);

        // Broadcast victory
        broadcast(victoryMessage);

        // Fire event
        eventBus.publish(new MatchEndedEvent(matchId, winners, victoryMessage));
    }

    /**
     * Finishes the match (transitions to FINISHED state).
     */
    private synchronized void finish() {
        if (state != MatchState.ENDING) {
            return;
        }

        state = MatchState.FINISHED;

        // Teleport all participants back to hub and unfreeze
        for (Participant participant : getParticipants()) {
            if (participant.isValid()) {
                UUID participantUuid = participant.getUniqueId();
                Player player = getPlayerFromUuid(participantUuid);
                if (player != null) {
                    hubManager.teleportToHub(player, () -> {
                        // Unfreeze after teleport to hub (runs on hub world thread via HubManager)
                        PlayerRef playerRef = Universe.get().getPlayer(participantUuid);
                        if (playerRef != null) {
                            World hubWorld = hubManager.getHubWorld();
                            PlayerMovementControl.enableMovementForPlayer(playerRef, hubWorld);
                        }
                        participant.sendMessage("<color:#2ecc71>Thanks for playing!</color>");
                    });
                }
            }
        }

        // Fire event
        eventBus.publish(new MatchFinishedEvent(matchId));
    }

    /**
     * Cancels the match (teleports players back to hub without completion).
     */
    public synchronized void cancel(String reason) {
        broadcast("<color:#e74c3c>Match cancelled: " + reason + "</color>");

        // Teleport all participants back to hub and unfreeze
        for (Participant participant : getParticipants()) {
            if (participant.isValid()) {
                UUID participantUuid = participant.getUniqueId();
                Player player = getPlayerFromUuid(participantUuid);
                if (player != null) {
                    hubManager.teleportToHub(player, () -> {
                        // Unfreeze after teleport to hub (runs on hub world thread via HubManager)
                        PlayerRef playerRef = Universe.get().getPlayer(participantUuid);
                        if (playerRef != null) {
                            World hubWorld = hubManager.getHubWorld();
                            PlayerMovementControl.enableMovementForPlayer(playerRef, hubWorld);
                        }
                    });
                }
            }
        }

        // Mark as finished for cleanup
        state = MatchState.FINISHED;
        eventBus.publish(new MatchFinishedEvent(matchId));
    }

    /**
     * Records damage dealt to a participant.
     * @return true if the damage resulted in a kill
     */
    public boolean recordDamage(UUID victimUuid, UUID attackerUuid, double damage) {
        if (state != MatchState.IN_PROGRESS) {
            return false;
        }

        Participant victim = participants.get(victimUuid);
        if (victim == null || !victim.isAlive()) {
            return false;
        }

        Participant attacker = attackerUuid != null ? participants.get(attackerUuid) : null;

        // Notify game mode
        gameMode.onParticipantDamaged(victim, attacker, damage);

        // Fire damage event
        eventBus.publish(new ParticipantDamagedEvent(matchId, victim, attacker, damage));

        return false;
    }

    /**
     * Records a kill.
     * @return true if the match should end
     */
    public boolean recordKill(UUID victimUuid, UUID killerUuid) {
        Participant victim = participants.get(victimUuid);
        if (victim == null) {
            return false;
        }

        Participant killer = killerUuid != null ? participants.get(killerUuid) : null;

        // Notify game mode and check if match ends
        boolean shouldEnd = gameMode.onParticipantKilled(victim, killer, getParticipants());

        // Fire kill event
        eventBus.publish(new ParticipantKilledEvent(matchId, victim, killer));

        // Broadcast kill message
        if (killer != null) {
            broadcast("<color:#e74c3c>" + victim.getName() + "</color> <color:#7f8c8d>was killed by</color> <color:#2ecc71>" + killer.getName() + "</color>");
        } else {
            broadcast("<color:#e74c3c>" + victim.getName() + "</color> <color:#7f8c8d>died</color>");
        }

        return shouldEnd;
    }

    /**
     * Called every tick by MatchManager.
     */
    public void tick() {
        switch (state) {
            case STARTING:
                tickStarting();
                break;
            case IN_PROGRESS:
                tickInProgress();
                break;
            case ENDING:
                tickEnding();
                break;
        }
    }

    private void tickStarting() {
        countdownTicks--;

        // Send countdown notifications at intervals
        int secondsLeft = countdownTicks / TICKS_PER_SECOND;
        if (countdownTicks % TICKS_PER_SECOND == 0) {
            if (secondsLeft > 0 && secondsLeft <= 5) {
                sendNotificationToAll(
                    "Match Starting",
                    secondsLeft + "...",
                    NotificationStyle.Warning
                );
            }
        }

        // Begin gameplay when countdown expires
        if (countdownTicks <= 0) {
            sendNotificationToAll(
                "Fight!",
                "The match has begun!",
                NotificationStyle.Success
            );
            beginGameplay();
        }
    }

    private void tickInProgress() {
        tickCount++;

        // Let game mode tick
        gameMode.onTick(getParticipants(), tickCount);

        // Check if match should end
        if (gameMode.shouldMatchEnd(getParticipants())) {
            end();
        }
    }

    private void tickEnding() {
        victoryDelayTicks--;

        // Finish when delay expires
        if (victoryDelayTicks <= 0) {
            finish();
        }
    }

    /**
     * Broadcasts a message to all participants.
     */
    private void broadcast(String message) {
        for (Participant participant : getParticipants()) {
            participant.sendMessage(message);
        }
    }

    /**
     * Sends a notification to all participants.
     */
    private void sendNotificationToAll(String title, String message, NotificationStyle style) {
        for (Participant participant : getParticipants()) {
            PlayerRef playerRef = Universe.get().getPlayer(participant.getUniqueId());
            if (playerRef != null) {
                try {
                    EventTitleUtil.showEventTitleToPlayer(playerRef,Message.raw(message),Message.raw(title),true,null,1,0,0);
                    NotificationUtil.sendNotification(
                        playerRef.getPacketHandler(),
                        Message.raw(title),
                        Message.raw(message),
                        style
                    );
                } catch (Exception e) {
                    // Fallback to chat message if notification fails
                    participant.sendMessage(title + ": " + message);
                }
            }
        }
    }

    /**
     * Checks if the match is finished and ready for cleanup.
     */
    public boolean isFinished() {
        return state == MatchState.FINISHED;
    }

    /**
     * Helper to get Player entity from UUID.
     */
    private Player getPlayerFromUuid(UUID uuid) {
        PlayerRef playerRef = Universe.get().getPlayer(uuid);
        if (playerRef == null) return null;
        Ref<EntityStore> ref = playerRef.getReference();
        if (ref == null) return null;
        Store<EntityStore> store = ref.getStore();
        if (store == null) return null;
        return store.getComponent(ref, Player.getComponentType());
    }

    /**
     * Freezes all participants (disables movement).
     */
    private void freezeAllParticipants() {
        System.out.println("[Match] Freezing " + participants.size() + " participants");
        World world = arena.getWorld();
        for (Participant participant : getParticipants()) {
            PlayerRef playerRef = Universe.get().getPlayer(participant.getUniqueId());
            if (playerRef != null) {
                PlayerMovementControl.disableMovementForPlayer(playerRef, world);
                System.out.println("[Match] Froze player: " + participant.getName());
            }
        }
    }

    /**
     * Freezes all participants without HUD (for match ended).
     */
    private void freezeAllParticipantsNoHud() {
        System.out.println("[Match] Freezing " + participants.size() + " participants (no HUD)");
        World world = arena.getWorld();
        for (Participant participant : getParticipants()) {
            PlayerRef playerRef = Universe.get().getPlayer(participant.getUniqueId());
            if (playerRef != null) {
                PlayerMovementControl.disableMovementForPlayerNoHud(playerRef, world);
                System.out.println("[Match] Froze player (no HUD): " + participant.getName());
            }
        }
    }

    /**
     * Unfreezes all participants (enables movement).
     */
    private void unfreezeAllParticipants() {
        System.out.println("[Match] Unfreezing " + participants.size() + " participants");
        World world = arena.getWorld();
        for (Participant participant : getParticipants()) {
            PlayerRef playerRef = Universe.get().getPlayer(participant.getUniqueId());
            if (playerRef != null) {
                PlayerMovementControl.enableMovementForPlayer(playerRef, world);
                System.out.println("[Match] Unfroze player: " + participant.getName());
            }
        }
    }

    /**
     * Heals a player to full health.
     * Must be called on the correct world thread.
     */
    private void healPlayer(UUID playerUuid, World world) {
        try {
            PlayerRef playerRef = Universe.get().getPlayer(playerUuid);
            if (playerRef == null) return;

            Ref<EntityStore> ref = playerRef.getReference();
            if (ref == null) return;

            Store<EntityStore> store = ref.getStore();
            if (store == null) return;

            EntityStatMap stats = store.getComponent(ref,
                EntityStatsModule.get().getEntityStatMapComponentType());
            if (stats == null) return;

            int healthIndex = EntityStatType.getAssetMap().getIndex("health");
            EntityStatValue healthStat = stats.get(healthIndex);
            if (healthStat == null) return;

            float maxHealth = healthStat.getMax();
            stats.setStatValue(healthIndex, maxHealth);
        } catch (Exception e) {
            System.err.println("[Match] Error healing player " + playerUuid + ": " + e.getMessage());
        }
    }

    /**
     * Heals all alive participants to full health.
     */
    private void healAllAliveParticipants() {
        World world = arena.getWorld();
        world.execute(() -> {
            for (Participant participant : getParticipants()) {
                if (participant.isAlive()) {
                    healPlayer(participant.getUniqueId(), world);
                }
            }
        });
    }
}
