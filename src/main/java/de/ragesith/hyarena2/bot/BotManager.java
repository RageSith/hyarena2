package de.ragesith.hyarena2.bot;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.asset.type.attitude.Attitude;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatsModule;
import com.hypixel.hytale.server.core.modules.entitystats.asset.EntityStatType;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.npc.INonPlayerCharacter;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.NPCPlugin;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.role.Role;
import com.hypixel.hytale.server.npc.role.support.MarkedEntitySupport;
import com.hypixel.hytale.server.npc.role.support.WorldSupport;
import de.ragesith.hyarena2.arena.Arena;
import de.ragesith.hyarena2.arena.Match;
import de.ragesith.hyarena2.arena.MatchState;
import de.ragesith.hyarena2.config.Position;
import de.ragesith.hyarena2.event.EventBus;
import de.ragesith.hyarena2.participant.Participant;
import de.ragesith.hyarena2.participant.ParticipantType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central manager for bot lifecycle - spawning, tracking, despawning, and AI ticking.
 */
public class BotManager {
    private final EventBus eventBus;

    // Maps bot UUID -> BotParticipant
    private final Map<UUID, BotParticipant> activeBots = new ConcurrentHashMap<>();

    // Maps entity UUID (from Hytale) -> BotParticipant for damage detection
    private final Map<UUID, BotParticipant> entityUuidToBotMap = new ConcurrentHashMap<>();

    // Maps bot UUID -> Match
    private final Map<UUID, Match> botMatches = new ConcurrentHashMap<>();

    // Bot name pool
    private static final List<String> BOT_NAMES = Arrays.asList(
        "Alpha", "Bravo", "Charlie", "Delta", "Echo", "Foxtrot",
        "Golf", "Hotel", "India", "Juliet", "Kilo", "Lima"
    );
    private int nameCounter = 0;

    // Default bot model (fallback)
    private static final String DEFAULT_BOT_MODEL = "Blook_Skeleton_Pirate_Gunner_Blunderbuss";

    // NPC entity health tracking (for proportional damage)
    private final Map<UUID, Float> npcMaxHealthMap = new ConcurrentHashMap<>();

    public BotManager(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    /**
     * Spawns a bot in a match.
     *
     * @param match the match to spawn the bot in
     * @param spawnPosition the position to spawn at
     * @param kitId the kit to apply (can be null)
     * @param difficulty the bot difficulty
     * @return the created BotParticipant
     */
    public BotParticipant spawnBot(Match match, Position spawnPosition, String kitId, BotDifficulty difficulty) {
        if (match == null || spawnPosition == null) {
            System.err.println("[BotManager] Cannot spawn bot - match or position is null");
            return null;
        }

        // Generate unique name
        String botName = generateBotName();

        // Create bot participant
        BotParticipant bot = new BotParticipant(botName, difficulty);
        bot.setSelectedKitId(kitId);
        bot.setSpawnPosition(spawnPosition);
        bot.setCurrentPosition(spawnPosition.copy());

        // Create AI controller
        BotAI ai = new BotAI(bot);
        ai.setDamageCallback(this::handleBotDamage);
        bot.setAI(ai);

        // Track bot
        activeBots.put(bot.getUniqueId(), bot);
        botMatches.put(bot.getUniqueId(), match);

        // Spawn NPC entity in the arena world
        spawnBotEntity(bot, match.getArena(), spawnPosition);

        System.out.println("[BotManager] Spawned bot " + botName + " (difficulty: " + difficulty +
            ") in match " + match.getMatchId());

        return bot;
    }

    /**
     * Spawns the visual NPC entity for a bot.
     */
    private void spawnBotEntity(BotParticipant bot, Arena arena, Position spawn) {
        World world = arena.getWorld();
        if (world == null) {
            System.err.println("[BotManager] Cannot spawn bot entity - arena world is null");
            return;
        }

        // Get bot model from arena config or use default
        String botModel = arena.getConfig().getBotModelId();
        if (botModel == null || botModel.isEmpty()) {
            botModel = DEFAULT_BOT_MODEL;
        }
        final String modelId = botModel;

        world.execute(() -> {
            try {
                Store<EntityStore> store = world.getEntityStore().getStore();

                // Set position and rotation
                Vector3d position = new Vector3d(spawn.getX(), spawn.getY(), spawn.getZ());
                Vector3f rotation = new Vector3f(spawn.getPitch(), spawn.getYaw(), 0);

                // Spawn NPC using NPCPlugin
                NPCPlugin npcPlugin = NPCPlugin.get();
                var result = npcPlugin.spawnNPC(store, modelId, null, position, rotation);

                if (result != null) {
                    Ref<EntityStore> entityRef = result.first();
                    INonPlayerCharacter npc = result.second();

                    bot.setEntityRef(entityRef);
                    bot.setNpc(npc);

                    // Store NPCEntity for role access
                    if (npc instanceof NPCEntity npcEntity) {
                        bot.setNpcEntity(npcEntity);
                    }

                    // Capture entity UUID for damage detection
                    UUIDComponent uuidComponent = store.getComponent(entityRef, UUIDComponent.getComponentType());
                    if (uuidComponent != null) {
                        UUID entityUuid = uuidComponent.getUuid();
                        bot.setEntityUuid(entityUuid);
                        entityUuidToBotMap.put(entityUuid, bot);
                    }

                    // Capture NPC's max health
                    captureNpcMaxHealth(bot, store, entityRef);

                    System.out.println("[BotManager] Spawned NPC entity for bot " + bot.getName() +
                        " with model " + modelId);

                } else {
                    System.err.println("[BotManager] NPCPlugin.spawnNPC returned null for model: " + modelId);
                }

            } catch (Exception e) {
                System.err.println("[BotManager] Failed to spawn bot entity: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    /**
     * Captures the NPC entity's max health for proportional damage calculation.
     */
    private void captureNpcMaxHealth(BotParticipant bot, Store<EntityStore> store, Ref<EntityStore> entityRef) {
        try {
            EntityStatMap stats = store.getComponent(entityRef,
                EntityStatsModule.get().getEntityStatMapComponentType());

            if (stats != null) {
                int healthIndex = EntityStatType.getAssetMap().getIndex("health");
                EntityStatValue healthStat = stats.get(healthIndex);
                if (healthStat != null) {
                    float npcMaxHealth = healthStat.getMax();
                    if (bot.getEntityUuid() != null) {
                        npcMaxHealthMap.put(bot.getEntityUuid(), npcMaxHealth);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[BotManager] Failed to capture NPC max health: " + e.getMessage());
        }
    }

    /**
     * Respawns a dead bot: despawns old entity, resets state, spawns new entity at given position.
     */
    public void respawnBot(BotParticipant bot, Arena arena, Position spawnPosition) {
        if (bot == null || arena == null || spawnPosition == null) {
            return;
        }

        // Remove old entity UUID mapping
        UUID oldEntityUuid = bot.getEntityUuid();
        if (oldEntityUuid != null) {
            entityUuidToBotMap.remove(oldEntityUuid);
            npcMaxHealthMap.remove(oldEntityUuid);
        }

        // Despawn old NPC entity
        despawnBotEntity(bot);

        // Reset internal state
        bot.resetHealth();
        bot.setSpawnPosition(spawnPosition);
        bot.setCurrentPosition(spawnPosition.copy());
        if (bot.getAI() != null) {
            bot.getAI().reset();
        }

        // Spawn new NPC entity at new position
        spawnBotEntity(bot, arena, spawnPosition);

        // Grant immunity
        bot.grantImmunity(3000);

        System.out.println("[BotManager] Respawned bot " + bot.getName() + " at " +
            String.format("%.1f, %.1f, %.1f", spawnPosition.getX(), spawnPosition.getY(), spawnPosition.getZ()));
    }

    /**
     * Despawns a bot.
     */
    public void despawnBot(BotParticipant bot) {
        if (bot == null) {
            return;
        }

        UUID botId = bot.getUniqueId();

        // Remove from entity UUID map
        UUID entityUuid = bot.getEntityUuid();
        if (entityUuid != null) {
            entityUuidToBotMap.remove(entityUuid);
            npcMaxHealthMap.remove(entityUuid);
        }

        // Remove visual entity
        despawnBotEntity(bot);

        // Remove from tracking
        activeBots.remove(botId);
        botMatches.remove(botId);

        System.out.println("[BotManager] Despawned bot " + bot.getName());
    }

    /**
     * Removes the visual NPC entity.
     */
    private void despawnBotEntity(BotParticipant bot) {
        Ref<EntityStore> entityRef = bot.getEntityRef();
        if (entityRef == null || !entityRef.isValid()) {
            return;
        }

        Match match = botMatches.get(bot.getUniqueId());
        if (match == null) {
            return;
        }

        World world = match.getArena().getWorld();
        if (world == null) {
            return;
        }

        world.execute(() -> {
            try {
                Store<EntityStore> store = world.getEntityStore().getStore();
                store.removeEntity(entityRef, RemoveReason.REMOVE);
                bot.setEntityRef(null);
            } catch (Exception e) {
                System.err.println("[BotManager] Failed to despawn bot entity: " + e.getMessage());
            }
        });
    }

    /**
     * Despawns all bots in a match.
     */
    public void despawnAllBotsInMatch(Match match) {
        if (match == null) {
            return;
        }

        UUID matchId = match.getMatchId();
        List<BotParticipant> toRemove = new ArrayList<>();

        for (Map.Entry<UUID, Match> entry : botMatches.entrySet()) {
            if (entry.getValue().getMatchId().equals(matchId)) {
                BotParticipant bot = activeBots.get(entry.getKey());
                if (bot != null) {
                    toRemove.add(bot);
                }
            }
        }

        System.out.println("[BotManager] Despawning " + toRemove.size() + " bots from match " + matchId);
        toRemove.forEach(this::despawnBot);
    }

    /**
     * Ticks all active bots (AI updates, position sync, targeting).
     */
    public void tickAllBots() {
        // Group bots by match/world for efficient execution
        Map<World, List<BotParticipant>> botsByWorld = new HashMap<>();

        for (BotParticipant bot : activeBots.values()) {
            if (!bot.isAlive()) {
                continue;
            }

            Match match = botMatches.get(bot.getUniqueId());
            if (match == null) {
                continue;
            }

            World world = match.getArena().getWorld();
            if (world == null) {
                continue;
            }

            botsByWorld.computeIfAbsent(world, k -> new ArrayList<>()).add(bot);
        }

        // Execute ticks on each world's thread
        for (Map.Entry<World, List<BotParticipant>> entry : botsByWorld.entrySet()) {
            World world = entry.getKey();
            List<BotParticipant> bots = entry.getValue();

            world.execute(() -> {
                Store<EntityStore> store = world.getEntityStore().getStore();

                for (BotParticipant bot : bots) {
                    try {
                        tickBot(bot, store);
                    } catch (Exception e) {
                        System.err.println("[BotManager] Error ticking bot " + bot.getName() + ": " + e.getMessage());
                    }
                }
            });
        }
    }

    /**
     * Ticks a single bot.
     */
    private void tickBot(BotParticipant bot, Store<EntityStore> store) {
        Match match = botMatches.get(bot.getUniqueId());
        if (match == null || !bot.isAlive()) {
            return;
        }

        // Sync position from entity
        syncBotPosition(bot, store);

        // During WAITING or STARTING phases, freeze bot at spawn (like players)
        MatchState state = match.getState();
        if (state == MatchState.WAITING || state == MatchState.STARTING) {
            freezeBotAtSpawn(bot, store);
            return;
        }

        // Update AI state
        BotAI ai = bot.getAI();
        if (ai != null) {
            ai.tick();
        }

        // Update NPC targeting
        updateBotTarget(bot, match, store);
    }

    /**
     * Freezes a bot at its spawn position during countdown.
     * Teleports bot back to spawn if it moves and clears its target.
     */
    private void freezeBotAtSpawn(BotParticipant bot, Store<EntityStore> store) {
        Ref<EntityStore> entityRef = bot.getEntityRef();
        Position spawn = bot.getSpawnPosition();
        if (entityRef == null || !entityRef.isValid() || spawn == null) {
            return;
        }

        try {
            TransformComponent transform = store.getComponent(entityRef, TransformComponent.getComponentType());
            if (transform != null) {
                Vector3d spawnVec = new Vector3d(spawn.getX(), spawn.getY(), spawn.getZ());
                Vector3d currentPos = transform.getPosition();

                // Teleport back if bot moved too far from spawn
                double distance = currentPos.distanceTo(spawnVec);
                if (distance > 0.5) {
                    transform.setPosition(spawnVec);
                    bot.setCurrentPosition(spawn.copy());
                }
            }

            // Clear target to prevent NPC from trying to move
            NPCEntity npcEntity = bot.getNpcEntity();
            if (npcEntity != null) {
                Role role = npcEntity.getRole();
                if (role != null) {
                    MarkedEntitySupport markedSupport = role.getMarkedEntitySupport();
                    if (markedSupport != null) {
                        markedSupport.setMarkedEntity(MarkedEntitySupport.DEFAULT_TARGET_SLOT, null);
                    }
                }
            }
        } catch (Exception e) {
            // Ignore freeze errors
        }
    }

    /**
     * Syncs the bot's position from the NPC entity.
     */
    private void syncBotPosition(BotParticipant bot, Store<EntityStore> store) {
        Ref<EntityStore> entityRef = bot.getEntityRef();
        if (entityRef == null || !entityRef.isValid()) {
            return;
        }

        try {
            TransformComponent transform = store.getComponent(entityRef, TransformComponent.getComponentType());
            if (transform != null) {
                Vector3d pos = transform.getPosition();
                float yaw = transform.getRotation().getYaw();
                float pitch = transform.getRotation().getPitch();
                bot.setCurrentPosition(new Position(pos.getX(), pos.getY(), pos.getZ(), yaw, pitch));
            }
        } catch (Exception e) {
            // Entity may have been removed
        }
    }

    /**
     * Updates the bot's NPC target to chase the nearest participant.
     */
    private void updateBotTarget(BotParticipant bot, Match match, Store<EntityStore> store) {
        NPCEntity npcEntity = bot.getNpcEntity();
        if (npcEntity == null) {
            return;
        }

        Role role = npcEntity.getRole();
        if (role == null) {
            return;
        }

        Position botPos = bot.getCurrentPosition();
        if (botPos == null) {
            return;
        }

        // Find nearest alive participant
        Participant nearestTarget = null;
        Ref<EntityStore> nearestRef = null;
        double nearestDistance = Double.MAX_VALUE;

        for (Participant participant : match.getParticipants()) {
            if (participant.getUniqueId().equals(bot.getUniqueId())) {
                continue; // Don't target self
            }
            if (!participant.isAlive()) {
                continue;
            }

            // Get position based on type
            Position targetPos = null;
            Ref<EntityStore> targetRef = null;

            if (participant.getType() == ParticipantType.PLAYER) {
                // Get player position
                PlayerRef playerRef = Universe.get().getPlayer(participant.getUniqueId());
                if (playerRef != null) {
                    targetRef = playerRef.getReference();
                    if (targetRef != null && targetRef.isValid()) {
                        try {
                            TransformComponent transform = store.getComponent(targetRef, TransformComponent.getComponentType());
                            if (transform != null) {
                                Vector3d pos = transform.getPosition();
                                targetPos = new Position(pos.getX(), pos.getY(), pos.getZ());
                            }
                        } catch (Exception e) {
                            // Ignore
                        }
                    }
                }
            } else if (participant.getType() == ParticipantType.BOT) {
                // Get bot position
                BotParticipant otherBot = activeBots.get(participant.getUniqueId());
                if (otherBot != null) {
                    targetPos = otherBot.getCurrentPosition();
                    targetRef = otherBot.getEntityRef();
                }
            }

            if (targetPos != null) {
                double distance = botPos.distanceTo(targetPos);
                if (distance < nearestDistance) {
                    nearestDistance = distance;
                    nearestTarget = participant;
                    nearestRef = targetRef;
                }
            }
        }

        // Update AI target
        BotAI ai = bot.getAI();
        if (ai != null && nearestTarget != null) {
            Position targetPos = null;
            if (nearestTarget.getType() == ParticipantType.BOT) {
                BotParticipant targetBot = activeBots.get(nearestTarget.getUniqueId());
                if (targetBot != null) {
                    targetPos = targetBot.getCurrentPosition();
                }
            }
            ai.setTarget(nearestTarget.getUniqueId(), targetPos);
        }

        // Set NPC target for movement
        if (nearestRef != null && nearestRef.isValid()) {
            try {
                MarkedEntitySupport markedSupport = role.getMarkedEntitySupport();
                if (markedSupport != null) {
                    markedSupport.setMarkedEntity(MarkedEntitySupport.DEFAULT_TARGET_SLOT, nearestRef);
                }

                // Set hostile attitude
                WorldSupport worldSupport = role.getWorldSupport();
                if (worldSupport != null) {
                    worldSupport.overrideAttitude(nearestRef, Attitude.HOSTILE, 60.0);
                }
            } catch (Exception e) {
                // Ignore targeting errors
            }
        }
    }

    /**
     * Handles damage dealt by a bot (callback from BotAI).
     */
    private void handleBotDamage(BotParticipant attacker, UUID victimId, double damage) {
        Match match = botMatches.get(attacker.getUniqueId());
        if (match == null) {
            return;
        }

        Participant victim = match.getParticipant(victimId);
        if (victim == null || !victim.isAlive()) {
            return;
        }

        // Check immunity
        if (victim.isImmune()) {
            return;
        }

        // Apply damage based on victim type
        if (victim.getType() == ParticipantType.BOT) {
            BotParticipant botVictim = activeBots.get(victimId);
            if (botVictim != null) {
                boolean died = botVictim.takeDamage(damage);
                attacker.addDamageDealt(damage);

                if (died) {
                    // Record kill
                    attacker.addKill();
                    match.recordKill(victimId, attacker.getUniqueId());
                } else {
                    match.recordDamage(victimId, attacker.getUniqueId(), damage);
                }
            }
        } else if (victim.getType() == ParticipantType.PLAYER) {
            // Bot attacking player - apply damage via Hytale's damage system
            // This will be handled by KillDetectionSystem
            applyDamageToPlayer(victimId, damage, match);
            attacker.addDamageDealt(damage);
        }
    }

    /**
     * Applies damage to a player entity.
     */
    private void applyDamageToPlayer(UUID playerUuid, double damage, Match match) {
        PlayerRef playerRef = Universe.get().getPlayer(playerUuid);
        if (playerRef == null) {
            return;
        }

        World world = match.getArena().getWorld();
        if (world == null) {
            return;
        }

        world.execute(() -> {
            try {
                Ref<EntityStore> ref = playerRef.getReference();
                if (ref == null || !ref.isValid()) {
                    return;
                }

                Store<EntityStore> store = world.getEntityStore().getStore();
                EntityStatMap stats = store.getComponent(ref,
                    EntityStatsModule.get().getEntityStatMapComponentType());

                if (stats != null) {
                    int healthIndex = EntityStatType.getAssetMap().getIndex("health");
                    EntityStatValue healthStat = stats.get(healthIndex);
                    if (healthStat != null) {
                        float currentHealth = healthStat.get();
                        float newHealth = (float) Math.max(0, currentHealth - damage);
                        stats.setStatValue(healthIndex, newHealth);
                    }
                }
            } catch (Exception e) {
                System.err.println("[BotManager] Failed to apply damage to player: " + e.getMessage());
            }
        });
    }

    /**
     * Gets a bot by UUID.
     */
    public BotParticipant getBot(UUID botId) {
        return activeBots.get(botId);
    }

    /**
     * Gets a bot by its entity UUID (for damage detection).
     */
    public BotParticipant getBotByEntityUuid(UUID entityUuid) {
        return entityUuidToBotMap.get(entityUuid);
    }

    /**
     * Gets the match a bot is in.
     */
    public Match getBotMatch(UUID botId) {
        return botMatches.get(botId);
    }

    /**
     * Checks if a UUID belongs to a bot.
     */
    public boolean isBot(UUID participantId) {
        return activeBots.containsKey(participantId);
    }

    /**
     * Gets all active bots.
     */
    public Collection<BotParticipant> getAllBots() {
        return Collections.unmodifiableCollection(activeBots.values());
    }

    /**
     * Gets the count of active bots.
     */
    public int getActiveBotCount() {
        return activeBots.size();
    }

    /**
     * Generates a unique bot name.
     */
    private String generateBotName() {
        String baseName = BOT_NAMES.get(nameCounter % BOT_NAMES.size());
        int suffix = nameCounter / BOT_NAMES.size();
        nameCounter++;
        return suffix > 0 ? "Bot_" + baseName + "_" + suffix : "Bot_" + baseName;
    }

    /**
     * Applies proportional damage to a bot's NPC entity (for health bar visuals).
     */
    public void applyDamageToNpcEntity(BotParticipant bot, double damage) {
        Ref<EntityStore> entityRef = bot.getEntityRef();
        if (entityRef == null || !entityRef.isValid()) {
            return;
        }

        Match match = botMatches.get(bot.getUniqueId());
        if (match == null) {
            return;
        }

        World world = match.getArena().getWorld();
        if (world == null) {
            return;
        }

        world.execute(() -> {
            try {
                Store<EntityStore> store = world.getEntityStore().getStore();
                EntityStatMap stats = store.getComponent(entityRef,
                    EntityStatsModule.get().getEntityStatMapComponentType());

                if (stats != null) {
                    int healthIndex = EntityStatType.getAssetMap().getIndex("health");
                    EntityStatValue healthStat = stats.get(healthIndex);
                    if (healthStat != null) {
                        float currentNpcHealth = healthStat.get();
                        float npcMaxHealth = npcMaxHealthMap.getOrDefault(bot.getEntityUuid(), healthStat.getMax());

                        // Scale damage proportionally
                        double internalMax = bot.getMaxHealth();
                        double scaledDamage = damage * (npcMaxHealth / internalMax);
                        float newHealth = (float) Math.max(1, currentNpcHealth - scaledDamage);

                        stats.setStatValue(healthIndex, newHealth);
                    }
                }
            } catch (Exception e) {
                // Ignore health bar errors
            }
        });
    }
}
