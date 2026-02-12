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
import de.ragesith.hyarena2.kit.KitManager;
import de.ragesith.hyarena2.participant.Participant;
import de.ragesith.hyarena2.participant.ParticipantType;
import de.ragesith.hyarena2.participant.PlayerParticipant;
import de.ragesith.hyarena2.bot.BotDifficulty;
import de.ragesith.hyarena2.bot.BotManager;
import de.ragesith.hyarena2.bot.BotParticipant;

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
    private final KitManager kitManager;
    private BotManager botManager;
    private de.ragesith.hyarena2.ui.hud.HudManager hudManager;

    private final Map<UUID, Participant> participants;
    private final Set<UUID> arrivedPlayers; // Players who have completed teleport to arena
    private final Map<UUID, Integer> respawnTimers; // Ticks remaining per dead player awaiting respawn
    private MatchState state;
    private int tickCount;
    private int waitingTicks; // Ticks spent in WAITING state
    private int countdownTicks;
    private int victoryDelayTicks;
    private List<UUID> winners;
    private final Set<Integer> sentTimeWarnings = new HashSet<>(); // Track which warnings have been sent
    private int nextSpawnIndex = 0; // Dedicated counter for spawn point assignment

    // Pending bot queue — bots queued by Matchmaker, drained on arena world thread in tickWaiting()
    private final List<PendingBot> pendingBots = new ArrayList<>();

    // TPS tracking — samples collected every 5s during IN_PROGRESS
    private final List<Double> tpsSamples = new ArrayList<>();
    private long tpsSampleStartTime;
    private int tpsSampleStartTick;
    private static final int TPS_SAMPLE_INTERVAL_TICKS = 100; // 5 seconds at 20 TPS

    private static final int TICKS_PER_SECOND = 20;
    private static final int VICTORY_DELAY_SECONDS = 3;
    private static final int SPAWN_IMMUNITY_MS = 3000; // 3 seconds immunity after spawn
    private static final int[] TIME_WARNINGS = {60, 30, 10, 5, 4, 3, 2, 1}; // Seconds remaining for warnings
    private static final int WAITING_TIMEOUT_SECONDS = 30; // Cancel match if stuck in WAITING for too long

    public Match(Arena arena, GameMode gameMode, EventBus eventBus, HubManager hubManager, KitManager kitManager) {
        this.matchId = UUID.randomUUID();
        this.arena = arena;
        this.gameMode = gameMode;
        this.eventBus = eventBus;
        this.hubManager = hubManager;
        this.kitManager = kitManager;
        this.participants = new ConcurrentHashMap<>();
        this.arrivedPlayers = ConcurrentHashMap.newKeySet();
        this.respawnTimers = new ConcurrentHashMap<>();
        this.state = MatchState.WAITING;
        this.tickCount = 0;
        this.waitingTicks = 0;
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
     * Adds a player to the match without kit selection.
     * @return true if successfully added
     */
    public synchronized boolean addPlayer(Player player) {
        return addPlayer(player, null);
    }

    /**
     * Adds a player to the match with kit selection and teleports them to a spawn point.
     * @param player the player to add
     * @param kitId the kit to apply, or null for no kit
     * @return true if successfully added
     */
    public synchronized boolean addPlayer(Player player, String kitId) {
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

        // Create participant and set kit
        Participant participant = new PlayerParticipant(playerUuid, player.getDisplayName());
        if (kitId != null) {
            participant.setSelectedKitId(kitId);
        }
        participants.put(playerUuid, participant);

        // Get spawn point (use dedicated counter to avoid index collisions)
        int spawnIndex = nextSpawnIndex % arena.getSpawnPoints().size();
        nextSpawnIndex++;
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
            // Wait for player to fully load, then freeze, apply kit, and mark as arrived
            CompletableFuture.delayedExecutor(500, TimeUnit.MILLISECONDS).execute(() -> {
                arenaWorld.execute(() -> {
                    PlayerRef pRef = Universe.get().getPlayer(playerUuid);
                    if (pRef != null) {
                        PlayerMovementControl.disableMovementForPlayer(pRef, arenaWorld);
                        System.out.println("[Match] Froze player after teleport: " + participant.getName());
                    }

                    // Check if game mode overrides the kit (e.g. Kit Roulette)
                    String gameModeKit = gameMode.getNextKitId(arena.getConfig(), participant);
                    if (gameModeKit != null) {
                        participant.setSelectedKitId(gameModeKit);
                        System.out.println("[Match] Game mode assigned kit: " + gameModeKit);
                    }

                    // Apply kit if selected, otherwise fallback to first allowed kit
                    String selectedKit = participant.getSelectedKitId();

                    // Fallback to first allowed kit if none selected
                    if (selectedKit == null && arena.getConfig().getAllowedKits() != null
                            && !arena.getConfig().getAllowedKits().isEmpty()) {
                        selectedKit = arena.getConfig().getAllowedKits().get(0);
                        participant.setSelectedKitId(selectedKit);
                        System.out.println("[Match] No kit selected, using default: " + selectedKit);
                    }

                    if (selectedKit != null && kitManager != null) {
                        Player p = getPlayerFromUuid(playerUuid);
                        if (p != null) {
                            kitManager.applyKit(p, selectedKit);
                            System.out.println("[Match] Applied kit '" + selectedKit + "' to " + participant.getName());
                        }
                    }

                    // Heal to full health after a short delay (armor stats need time to apply)
                    // Using 200ms delay to ensure all armor stats are computed
                    CompletableFuture.delayedExecutor(200, TimeUnit.MILLISECONDS).execute(() -> {
                        arenaWorld.execute(() -> healPlayer(playerUuid, arenaWorld));
                    });

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
     * Adds a bot to the match.
     * Bot is already spawned by BotManager, this just registers it as a participant.
     *
     * @param bot the bot participant to add
     * @return true if successfully added
     */
    public synchronized boolean addBot(BotParticipant bot) {
        if (state != MatchState.WAITING && state != MatchState.STARTING && state != MatchState.IN_PROGRESS) {
            return false;
        }

        if (participants.containsKey(bot.getUniqueId())) {
            return false;
        }

        // Check capacity
        if (participants.size() >= arena.getMaxPlayers()) {
            return false;
        }

        // Add bot to participants
        participants.put(bot.getUniqueId(), bot);

        // Bots are considered "arrived" immediately since they're spawned in place
        arrivedPlayers.add(bot.getUniqueId());

        // Grant spawn immunity
        bot.grantImmunity(SPAWN_IMMUNITY_MS);

        System.out.println("[Match] Added bot " + bot.getName() + " to match " + matchId);

        // Fire event
        eventBus.publish(new ParticipantJoinedEvent(matchId, bot));

        // If match is in WAITING state, check if we can start
        if (state == MatchState.WAITING) {
            checkAndStartIfReady();
        }

        return true;
    }

    /**
     * Queues a bot to be spawned on the arena world thread during the next tick.
     * Called by Matchmaker from the hub world thread — actual spawn happens in drainPendingBots().
     */
    public synchronized void queueBot(Position spawnPosition, String kitId, BotDifficulty difficulty) {
        pendingBots.add(new PendingBot(spawnPosition, kitId, difficulty));
    }

    /**
     * Drains the pending bot queue by spawning bots on the arena world thread.
     * Called from tickWaiting() which runs on the correct world thread.
     */
    private void drainPendingBots() {
        if (pendingBots.isEmpty()) return;
        if (botManager == null) {
            System.err.println("[Match] drainPendingBots() - botManager is null, cannot spawn " + pendingBots.size() + " bots");
            return;
        }

        // Wait for at least one real player to arrive before spawning bots.
        // Players loading into the world trigger chunk loading — without loaded chunks,
        // NPC entities spawn into unloaded chunks and get garbage-collected by the engine.
        boolean hasArrivedPlayer = false;
        for (Participant p : getParticipants()) {
            if (p.getType() == ParticipantType.PLAYER && arrivedPlayers.contains(p.getUniqueId())) {
                hasArrivedPlayer = true;
                break;
            }
        }
        if (!hasArrivedPlayer) {
            return;
        }

        // Copy and clear pending list BEFORE spawning, so that addBot() → checkAndStartIfReady()
        // sees an empty pendingBots list and can actually start the match.
        List<PendingBot> toSpawn = new ArrayList<>(pendingBots);
        pendingBots.clear();

        // Pre-load chunks at each bot spawn position to prevent spawning into unloaded chunks
        World arenaWorld = arena.getWorld();
        for (PendingBot pending : toSpawn) {
            try {
                int bx = (int) pending.spawnPosition.getX();
                int by = (int) pending.spawnPosition.getY();
                int bz = (int) pending.spawnPosition.getZ();
                arenaWorld.getBlockType(bx, by, bz);
            } catch (Exception e) {
                // Ignore chunk pre-load errors
            }
        }

        for (PendingBot pending : toSpawn) {
            BotParticipant bot = botManager.spawnBot(this, pending.spawnPosition, pending.kitId, pending.difficulty);
            if (bot != null) {
                addBot(bot);
            }
        }
    }

    /**
     * Sets the bot manager for bot cleanup.
     */
    public void setBotManager(BotManager botManager) {
        this.botManager = botManager;
    }

    /**
     * Gets the bot manager.
     */
    public BotManager getBotManager() {
        return botManager;
    }

    /**
     * Sets the HUD manager for showing LobbyHud on hub return.
     */
    public void setHudManager(de.ragesith.hyarena2.ui.hud.HudManager hudManager) {
        this.hudManager = hudManager;
    }

    /**
     * Checks if all participants have arrived and starts the match if ready.
     */
    private void checkAndStartIfReady() {
        if (state != MatchState.WAITING) {
            return;
        }

        // Wait for queued bots to spawn first
        if (!pendingBots.isEmpty()) {
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
                // Show LobbyHud (handles same-world arenas where no world change event fires)
                if (hudManager != null) {
                    hudManager.showLobbyHud(uuid);
                }
            });
        }

        // Handle state-specific logic
        if (state == MatchState.IN_PROGRESS) {
            // Treat as death
            participant.setAlive(false);
            participant.addDeath();

            // Check if match should end
            if (gameMode.shouldMatchEnd(arena.getConfig(), getParticipants())) {
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
        gameMode.onMatchStart(arena.getConfig(), getParticipants());

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
        tpsSamples.clear();
        tpsSampleStartTime = System.currentTimeMillis();
        tpsSampleStartTick = 0;

        // Grant spawn immunity to all participants
        for (Participant participant : getParticipants()) {
            participant.grantImmunity(SPAWN_IMMUNITY_MS);
        }

        // Unfreeze all players - fight begins!
        unfreezeAllParticipants();

        // Show MatchHud to all player participants
        showMatchHudToAllPlayers();

        // Notify game mode
        gameMode.onGameplayBegin(arena.getConfig(), getParticipants());
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
        winners = gameMode.getWinners(arena.getConfig(), getParticipants());

        // Get victory message
        List<Participant> winnerParticipants = winners.stream()
                .map(participants::get)
                .filter(Objects::nonNull)
                .toList();
        String victoryMessage = gameMode.getVictoryMessage(arena.getConfig(), winnerParticipants);

        // Get winner name for VictoryHud
        String winnerName = winnerParticipants.isEmpty() ? null : winnerParticipants.get(0).getName();

        // Show VictoryHud to all player participants (replaces MatchHud)
        showVictoryHudToAllPlayers(winnerName);

        // Broadcast victory
        broadcast(victoryMessage);

        // Log TPS stats for the match
        logTpsStats();

        // Fire event
        eventBus.publish(new MatchEndedEvent(matchId, winners, victoryMessage));
    }

    /**
     * Ends the match due to timeout - delegates winner determination to game mode.
     */
    private synchronized void endByTimeout() {
        if (state != MatchState.IN_PROGRESS) {
            return;
        }

        state = MatchState.ENDING;
        victoryDelayTicks = VICTORY_DELAY_SECONDS * TICKS_PER_SECOND;

        // Freeze all players
        freezeAllParticipantsNoHud();

        // Heal all alive players
        healAllAliveParticipants();

        // Delegate winner determination to game mode
        winners = gameMode.getWinners(arena.getConfig(), getParticipants());

        // Get victory message
        List<Participant> winnerParticipants = winners.stream()
                .map(participants::get)
                .filter(Objects::nonNull)
                .toList();
        String victoryMessage = "<color:#f39c12>Time's up!</color> " +
            gameMode.getVictoryMessage(arena.getConfig(), winnerParticipants);

        String winnerName = winnerParticipants.isEmpty() ? null : winnerParticipants.get(0).getName();

        // Show VictoryHud to all player participants
        showVictoryHudToAllPlayers(winnerName);

        // Broadcast victory
        broadcast(victoryMessage);

        // Log TPS stats for the match
        logTpsStats();

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

        // Let game mode clean up (e.g., clear debug shapes)
        gameMode.onMatchFinished(getParticipants());

        // VictoryHud pages are NOT hidden here — they persist after teleport
        // and the player dismisses them manually (close button or ESC).

        // Teleport all player participants back to hub FIRST.
        // Bot entities must still exist when players leave to prevent
        // interaction chain crashes from stale entity references.
        for (Participant participant : getParticipants()) {
            if (participant.getType() == ParticipantType.BOT) {
                continue; // Bots despawned after players leave
            }
            if (participant.isValid()) {
                UUID participantUuid = participant.getUniqueId();
                Player player = getPlayerFromUuid(participantUuid);
                if (player != null) {
                    // Clear kit before teleporting back to hub
                    if (kitManager != null) {
                        kitManager.clearKit(player);
                    }
                    hubManager.teleportToHub(player, () -> {
                        // Unfreeze after teleport to hub (runs on hub world thread via HubManager)
                        PlayerRef playerRef = Universe.get().getPlayer(participantUuid);
                        if (playerRef != null) {
                            World hubWorld = hubManager.getHubWorld();
                            PlayerMovementControl.enableMovementForPlayer(playerRef, hubWorld);
                        }
                        // LobbyHud is shown when player dismisses VictoryHud page
                        participant.sendMessage("<color:#2ecc71>Thanks for playing!</color>");
                    });
                }
            }
        }

        // Despawn bots after a delay to ensure players have fully left the arena world.
        // Cross-world teleports take ~1.5s; 2s delay provides safety margin.
        if (botManager != null) {
            World arenaWorld = arena.getWorld();
            CompletableFuture.delayedExecutor(2000, TimeUnit.MILLISECONDS).execute(() -> {
                arenaWorld.execute(() -> {
                    botManager.despawnAllBotsInMatch(this);
                });
            });
        }

        // Fire event
        eventBus.publish(new MatchFinishedEvent(matchId));
    }

    /**
     * Cancels the match (teleports players back to hub without completion).
     */
    public synchronized void cancel(String reason) {
        broadcast("<color:#e74c3c>Match cancelled: " + reason + "</color>");

        // Let game mode clean up (e.g., clear debug shapes)
        gameMode.onMatchFinished(getParticipants());

        // Teleport all player participants back to hub FIRST.
        // Bot entities must still exist when players leave to prevent
        // interaction chain crashes from stale entity references.
        for (Participant participant : getParticipants()) {
            if (participant.getType() == ParticipantType.BOT) {
                continue; // Bots despawned after players leave
            }
            if (participant.isValid()) {
                UUID participantUuid = participant.getUniqueId();
                Player player = getPlayerFromUuid(participantUuid);
                if (player != null) {
                    // Clear kit before teleporting back to hub
                    if (kitManager != null) {
                        kitManager.clearKit(player);
                    }
                    hubManager.teleportToHub(player, () -> {
                        // Unfreeze after teleport to hub (runs on hub world thread via HubManager)
                        PlayerRef playerRef = Universe.get().getPlayer(participantUuid);
                        if (playerRef != null) {
                            World hubWorld = hubManager.getHubWorld();
                            PlayerMovementControl.enableMovementForPlayer(playerRef, hubWorld);
                        }
                        // Show LobbyHud (handles same-world arenas where no world change event fires)
                        if (hudManager != null) {
                            hudManager.showLobbyHud(participantUuid);
                        }
                    });
                }
            }
        }

        // Despawn bots after a delay to ensure players have fully left the arena world.
        if (botManager != null) {
            World arenaWorld = arena.getWorld();
            CompletableFuture.delayedExecutor(2000, TimeUnit.MILLISECONDS).execute(() -> {
                arenaWorld.execute(() -> {
                    botManager.despawnAllBotsInMatch(this);
                });
            });
        }

        // Mark as finished for cleanup
        state = MatchState.FINISHED;
        eventBus.publish(new MatchFinishedEvent(matchId));
    }

    /**
     * Immediate cleanup for server shutdown. Despawns bots and holograms
     * without delays or player teleports (server is stopping anyway).
     */
    public synchronized void forceCleanup() {
        // Count entities before cleanup for logging
        int hologramCount = gameMode.getSpawnedEntityCount();
        long botCount = participants.values().stream()
            .filter(p -> p.getType() == ParticipantType.BOT).count();

        World arenaWorld = arena.getWorld();
        if (arenaWorld != null) {
            arenaWorld.execute(() -> {
                gameMode.onMatchFinished(getParticipants());

                if (botManager != null) {
                    botManager.despawnAllBotsInMatch(this);
                }
            });
        }

        state = MatchState.FINISHED;

        System.out.println("[Match] Force-cleaned match " + matchId
            + " on arena " + arena.getId()
            + " (despawned " + botCount + " bot(s), " + hologramCount + " hologram(s))");
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

        // Track last attacker for kill attribution on environmental deaths
        if (attackerUuid != null) {
            victim.setLastAttacker(attackerUuid);
        }

        // Notify game mode
        gameMode.onParticipantDamaged(arena.getConfig(), victim, attacker, damage);

        // Fire damage event
        eventBus.publish(new ParticipantDamagedEvent(matchId, victim, attacker, damage));

        // Send damage notifications
        int dmg = (int) Math.round(damage);
        if (attacker != null) {
            // Notify attacker they dealt damage
            sendNotificationToPlayer(attacker.getUniqueId(),
                "-" + dmg,
                victim.getName(),
                NotificationStyle.Success);

            // Notify victim they took damage
            sendNotificationToPlayer(victimUuid,
                "-" + dmg,
                attacker.getName(),
                NotificationStyle.Warning);
        } else {
            // Environmental damage - notify victim only
            sendNotificationToPlayer(victimUuid,
                "-" + dmg,
                "Damage taken",
                NotificationStyle.Warning);
        }

        return false;
    }

    private static final long LAST_HIT_ATTRIBUTION_WINDOW_MS = 10_000; // 10 seconds

    /**
     * Records a kill.
     * @return true if the match should end
     */
    public boolean recordKill(UUID victimUuid, UUID killerUuid) {
        Participant victim = participants.get(victimUuid);
        if (victim == null) {
            return false;
        }

        // If no direct killer, check for last hit attribution (environmental death within 10s of last damage)
        if (killerUuid == null) {
            UUID lastAttacker = victim.getLastAttackerUuid();
            long lastDamageTime = victim.getLastDamageTimestamp();
            if (lastAttacker != null && (System.currentTimeMillis() - lastDamageTime) < LAST_HIT_ATTRIBUTION_WINDOW_MS) {
                killerUuid = lastAttacker;
                System.out.println("[Match] Kill attributed to last attacker: " + lastAttacker + " (environmental death)");
            }
        }

        Participant killer = killerUuid != null ? participants.get(killerUuid) : null;

        // Notify game mode and check if match ends
        boolean shouldEnd = gameMode.onParticipantKilled(arena.getConfig(), victim, killer, getParticipants());

        // Fire kill event
        eventBus.publish(new ParticipantKilledEvent(matchId, victim, killer));

        // Broadcast kill message
        if (killer != null) {
            broadcast("<color:#e74c3c>" + victim.getName() + "</color> <color:#7f8c8d>was killed by</color> <color:#2ecc71>" + killer.getName() + "</color>");
        } else {
            broadcast("<color:#e74c3c>" + victim.getName() + "</color> <color:#7f8c8d>died</color>");
        }

        // Handle respawning
        if (gameMode.shouldRespawn(arena.getConfig(), victim)) {
            // Instant respawn (delay 0 = next tick). Timer infrastructure exists for future delay support.
            respawnTimers.put(victim.getUniqueId(), 0);
            if (victim.getType() == ParticipantType.PLAYER) {
                victim.sendMessage("<color:#f39c12>Respawning...</color>");
            }
        } else if (victim.getType() == ParticipantType.BOT) {
            // No respawn — neutralize the bot but don't remove the entity yet.
            // Entity removal is deferred to finish()/cancel() after players leave,
            // to prevent interaction chain crashes from stale entity references.
            if (botManager != null) {
                BotParticipant bot = botManager.getBot(victimUuid);
                if (bot != null) {
                    System.out.println("[Match] Bot killed (entity removal deferred): " + victim.getName());
                    botManager.neutralizeBot(bot);
                }
            }
        }

        return shouldEnd;
    }

    /**
     * Called every tick by MatchManager.
     */
    public void tick() {
        switch (state) {
            case WAITING:
                tickWaiting();
                break;
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

    private void tickWaiting() {
        // Drain pending bots (spawns them on this world thread)
        drainPendingBots();

        // Tick bots for freeze enforcement
        if (botManager != null) {
            botManager.tickBotsForMatch(this);
        }

        waitingTicks++;

        // Cancel if waiting too long (e.g., player disconnect during teleport)
        if (waitingTicks >= WAITING_TIMEOUT_SECONDS * TICKS_PER_SECOND) {
            System.out.println("[Match] Waiting timeout - cancelling match " + matchId);
            cancel("Timed out waiting for players");
        }
    }

    private void tickStarting() {
        // Tick bots for freeze enforcement
        if (botManager != null) {
            botManager.tickBotsForMatch(this);
        }

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

        // Sample TPS every 5 seconds
        int ticksSinceLastSample = tickCount - tpsSampleStartTick;
        if (ticksSinceLastSample >= TPS_SAMPLE_INTERVAL_TICKS) {
            long now = System.currentTimeMillis();
            long elapsedMs = now - tpsSampleStartTime;
            if (elapsedMs > 0) {
                double tps = ticksSinceLastSample / (elapsedMs / 1000.0);
                tpsSamples.add(tps);
            }
            tpsSampleStartTime = now;
            tpsSampleStartTick = tickCount;
        }

        // Tick bots (AI, position sync, targeting)
        if (botManager != null) {
            botManager.tickBotsForMatch(this);
        }

        // Let game mode tick
        gameMode.onTick(arena.getConfig(), getParticipants(), tickCount);

        // Process respawn timers
        processRespawnTimers();

        // Check if match should end (normal game mode condition)
        if (gameMode.shouldMatchEnd(arena.getConfig(), getParticipants())) {
            end();
            return;
        }

        // Check match duration timeout
        int matchDurationTicks = arena.getConfig().getMatchDurationSeconds() * TICKS_PER_SECOND;
        int remainingTicks = matchDurationTicks - tickCount;
        int remainingSeconds = remainingTicks / TICKS_PER_SECOND;

        // Send time warnings
        for (int warningTime : TIME_WARNINGS) {
            if (remainingSeconds == warningTime && !sentTimeWarnings.contains(warningTime)) {
                sentTimeWarnings.add(warningTime);
                String timeText = warningTime >= 60 ? (warningTime / 60) + " minute" + (warningTime >= 120 ? "s" : "")
                                                    : warningTime + " second" + (warningTime > 1 ? "s" : "");
                broadcast("<color:#f39c12>" + timeText + " remaining!</color>");
            }
        }

        // End match if time has run out
        if (remainingTicks <= 0) {
            endByTimeout();
        }
    }

    private void tickEnding() {
        victoryDelayTicks--;

        // Finish when delay expires
        if (victoryDelayTicks <= 0) {
            finish();
        }
    }

    private void logTpsStats() {
        // Capture any remaining partial window
        int ticksSinceLastSample = tickCount - tpsSampleStartTick;
        if (ticksSinceLastSample > 0) {
            long elapsedMs = System.currentTimeMillis() - tpsSampleStartTime;
            if (elapsedMs > 0) {
                tpsSamples.add(ticksSinceLastSample / (elapsedMs / 1000.0));
            }
        }

        if (tpsSamples.isEmpty()) {
            System.out.println("[Match " + matchId.toString().substring(0, 8) + "] No TPS data (match too short)");
            return;
        }

        double sum = 0;
        for (double s : tpsSamples) sum += s;
        double avgTps = sum / tpsSamples.size();

        double diffSum = 0;
        for (double s : tpsSamples) diffSum += Math.abs(s - 20.0);
        double avgDiff = diffSum / tpsSamples.size();

        System.out.println("[Match " + matchId.toString().substring(0, 8) + "] TPS Report — "
            + "samples: " + tpsSamples.size()
            + ", avg TPS: " + String.format("%.2f", avgTps)
            + ", avg deviation from 20.0: " + String.format("%.2f", avgDiff));
    }

    /**
     * Processes respawn timers: ticks down each entry and respawns participants when timer reaches 0.
     */
    private void processRespawnTimers() {
        if (respawnTimers.isEmpty()) {
            return;
        }

        Iterator<Map.Entry<UUID, Integer>> it = respawnTimers.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Integer> entry = it.next();
            UUID uuid = entry.getKey();
            int remaining = entry.getValue() - 1;

            if (remaining > 0) {
                entry.setValue(remaining);
                continue;
            }

            // Timer expired — respawn this participant
            it.remove();

            Participant participant = participants.get(uuid);
            if (participant == null) {
                continue;
            }

            // Check if game mode wants to override the kit
            String nextKit = gameMode.getNextKitId(arena.getConfig(), participant);
            if (nextKit != null) {
                participant.setSelectedKitId(nextKit);
            }

            // Pick a random spawn point
            List<ArenaConfig.SpawnPoint> spawnPoints = arena.getSpawnPoints();
            ArenaConfig.SpawnPoint spawn = spawnPoints.get(new Random().nextInt(spawnPoints.size()));
            Position spawnPos = new Position(
                spawn.getX(), spawn.getY(), spawn.getZ(),
                spawn.getYaw(), spawn.getPitch()
            );

            if (participant.getType() == ParticipantType.BOT) {
                respawnBot(uuid, participant, spawnPos);
            } else {
                respawnPlayer(uuid, participant, spawnPos);
            }
        }
    }

    private void respawnPlayer(UUID playerUuid, Participant participant, Position spawnPos) {
        Player player = getPlayerFromUuid(playerUuid);
        if (player == null) {
            return;
        }

        World arenaWorld = arena.getWorld();
        hubManager.teleportPlayerToWorld(player, spawnPos, arenaWorld, () -> {
            CompletableFuture.delayedExecutor(300, TimeUnit.MILLISECONDS).execute(() -> {
                arenaWorld.execute(() -> {
                    String kitId = participant.getSelectedKitId();
                    if (kitId != null && kitManager != null) {
                        Player p = getPlayerFromUuid(playerUuid);
                        if (p != null) {
                            kitManager.applyKit(p, kitId);
                        }
                    }

                    CompletableFuture.delayedExecutor(200, TimeUnit.MILLISECONDS).execute(() -> {
                        arenaWorld.execute(() -> {
                            healPlayer(playerUuid, arenaWorld);
                            participant.grantImmunity(SPAWN_IMMUNITY_MS);
                            participant.setAlive(true);
                            System.out.println("[Match] Respawned " + participant.getName());
                        });
                    });
                });
            });
        });
    }

    private void respawnBot(UUID botUuid, Participant participant, Position spawnPos) {
        if (botManager == null) return;

        BotParticipant bot = botManager.getBot(botUuid);
        if (bot == null) return;

        botManager.respawnBot(bot, arena, spawnPos);
        System.out.println("[Match] Respawned bot " + participant.getName());
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
     * Sends a notification to a specific player (no event title, just notification popup).
     */
    private void sendNotificationToPlayer(UUID playerUuid, String title, String message, NotificationStyle style) {
        PlayerRef playerRef = Universe.get().getPlayer(playerUuid);
        if (playerRef != null) {
            try {
                NotificationUtil.sendNotification(
                    playerRef.getPacketHandler(),
                    Message.raw(title),
                    Message.raw(message),
                    style
                );
            } catch (Exception e) {
                // Ignore notification errors for damage
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
     * Already on arena world thread via Match.tick().
     */
    private void freezeAllParticipants() {
        System.out.println("[Match] Freezing " + participants.size() + " participants");
        World world = arena.getWorld();
        for (Participant participant : getParticipants()) {
            if (participant.getType() == ParticipantType.PLAYER) {
                PlayerRef playerRef = Universe.get().getPlayer(participant.getUniqueId());
                if (playerRef != null) {
                    PlayerMovementControl.disableMovementForPlayer(playerRef, world);
                    System.out.println("[Match] Froze player: " + participant.getName());
                }
            } else if (participant.getType() == ParticipantType.BOT && botManager != null) {
                BotParticipant bot = botManager.getBot(participant.getUniqueId());
                if (bot != null && bot.getEntityRef() != null && bot.getEntityRef().isValid()) {
                    Ref<EntityStore> entityRef = bot.getEntityRef();
                    Store<EntityStore> store = entityRef.getStore();
                    if (store != null) {
                        PlayerMovementControl.disableMovementForEntity(entityRef, store);
                        System.out.println("[Match] Froze bot: " + participant.getName());
                    }
                }
            }
        }
    }

    /**
     * Freezes all participants without HUD (for match ended).
     * Already on arena world thread via Match.tick().
     */
    private void freezeAllParticipantsNoHud() {
        System.out.println("[Match] Freezing " + participants.size() + " participants (no HUD)");
        World world = arena.getWorld();
        for (Participant participant : getParticipants()) {
            if (participant.getType() == ParticipantType.PLAYER) {
                PlayerRef playerRef = Universe.get().getPlayer(participant.getUniqueId());
                if (playerRef != null) {
                    PlayerMovementControl.disableMovementForPlayerNoHud(playerRef, world);
                    System.out.println("[Match] Froze player (no HUD): " + participant.getName());
                }
            } else if (participant.getType() == ParticipantType.BOT && botManager != null) {
                BotParticipant bot = botManager.getBot(participant.getUniqueId());
                if (bot != null && bot.getEntityRef() != null && bot.getEntityRef().isValid()) {
                    Ref<EntityStore> entityRef = bot.getEntityRef();
                    Store<EntityStore> store = entityRef.getStore();
                    if (store != null) {
                        PlayerMovementControl.disableMovementForEntity(entityRef, store);
                        System.out.println("[Match] Froze bot (no HUD): " + participant.getName());
                    }
                }
            }
        }
    }

    /**
     * Unfreezes all participants (enables movement).
     * Already on arena world thread via Match.tick().
     */
    private void unfreezeAllParticipants() {
        System.out.println("[Match] Unfreezing " + participants.size() + " participants");
        World world = arena.getWorld();
        for (Participant participant : getParticipants()) {
            if (participant.getType() == ParticipantType.PLAYER) {
                PlayerRef playerRef = Universe.get().getPlayer(participant.getUniqueId());
                if (playerRef != null) {
                    PlayerMovementControl.enableMovementForPlayer(playerRef, world);
                    System.out.println("[Match] Unfroze player: " + participant.getName());
                }
            } else if (participant.getType() == ParticipantType.BOT && botManager != null) {
                BotParticipant bot = botManager.getBot(participant.getUniqueId());
                if (bot != null && bot.getEntityRef() != null && bot.getEntityRef().isValid()) {
                    Ref<EntityStore> entityRef = bot.getEntityRef();
                    Store<EntityStore> store = entityRef.getStore();
                    if (store != null) {
                        PlayerMovementControl.enableMovementForEntity(entityRef, store);
                        System.out.println("[Match] Unfroze bot: " + participant.getName());
                    }
                }
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

    /**
     * Shows MatchHud to all player participants.
     */
    private void showMatchHudToAllPlayers() {
        if (hudManager == null) {
            return;
        }

        World world = arena.getWorld();
        java.util.function.Consumer<Runnable> worldExecutor = world::execute;

        for (Participant participant : getParticipants()) {
            if (participant.getType() == ParticipantType.PLAYER) {
                hudManager.showMatchHud(participant.getUniqueId(), this, worldExecutor);
            }
        }
    }

    /**
     * Shows VictoryHud to all player participants.
     * @param winnerName the name of the winner, or null for draw
     */
    private void showVictoryHudToAllPlayers(String winnerName) {
        if (hudManager == null) {
            return;
        }

        for (Participant participant : getParticipants()) {
            if (participant.getType() == ParticipantType.PLAYER) {
                boolean isWinner = winners.contains(participant.getUniqueId());
                hudManager.showVictoryHud(participant.getUniqueId(), this, isWinner, winnerName);
            }
        }
    }

    /**
     * Gets the tick count since IN_PROGRESS started.
     * Used by MatchHud for elapsed time display.
     */
    public int getTickCount() {
        return tickCount;
    }

    /**
     * Gets the remaining match time in seconds.
     * Used by MatchHud for countdown display.
     */
    public int getRemainingSeconds() {
        int matchDurationTicks = arena.getConfig().getMatchDurationSeconds() * TICKS_PER_SECOND;
        int remainingTicks = Math.max(0, matchDurationTicks - tickCount);
        return remainingTicks / TICKS_PER_SECOND;
    }

    /**
     * Gets the match duration in seconds from config.
     */
    public int getMatchDurationSeconds() {
        return arena.getConfig().getMatchDurationSeconds();
    }

    /**
     * Data class for bots queued by Matchmaker (from hub thread) awaiting spawn on arena world thread.
     */
    private static class PendingBot {
        final Position spawnPosition;
        final String kitId;
        final BotDifficulty difficulty;

        PendingBot(Position spawnPosition, String kitId, BotDifficulty difficulty) {
            this.spawnPosition = spawnPosition;
            this.kitId = kitId;
            this.difficulty = difficulty;
        }
    }
}
