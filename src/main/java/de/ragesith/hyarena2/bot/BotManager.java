package de.ragesith.hyarena2.bot;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.asset.type.attitude.Attitude;
import com.hypixel.hytale.server.core.entity.InteractionChain;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.ProjectileComponent;
import com.hypixel.hytale.server.core.entity.movement.MovementStatesComponent;
import com.hypixel.hytale.server.core.modules.entity.component.Intangible;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.tracker.NetworkId;
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
import com.hypixel.hytale.server.npc.role.support.CombatSupport;
import com.hypixel.hytale.server.npc.role.support.MarkedEntitySupport;
import com.hypixel.hytale.server.npc.role.support.WorldSupport;
import de.ragesith.hyarena2.arena.Arena;
import de.ragesith.hyarena2.arena.ArenaConfig;
import de.ragesith.hyarena2.arena.Match;
import de.ragesith.hyarena2.arena.MatchState;
import de.ragesith.hyarena2.config.Position;
import de.ragesith.hyarena2.event.EventBus;
import de.ragesith.hyarena2.gamemode.GameMode;
import de.ragesith.hyarena2.participant.Participant;
import de.ragesith.hyarena2.participant.ParticipantType;

import java.lang.reflect.Field;
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

    // Tracks previous tick's attacking state per bot for edge detection (fire damage on false→true)
    private final Map<UUID, Boolean> wasAttacking = new ConcurrentHashMap<>();

    // One invisible marker entity per bot (for objective navigation with individual offsets)
    private final Map<UUID, Ref<EntityStore>> objectiveMarkers = new ConcurrentHashMap<>();

    // Random XZ offset per bot within zone bounds (so bots don't stack on the same point)
    private final Map<UUID, double[]> botZoneOffsets = new ConcurrentHashMap<>();
    private final Random objectiveRandom = new Random();

    // Cached reflection field for reading CombatSupport.activeAttack (protected)
    private static Field activeAttackField;
    static {
        try {
            activeAttackField = CombatSupport.class.getDeclaredField("activeAttack");
            activeAttackField.setAccessible(true);
        } catch (Exception e) {
            System.err.println("[BotManager] Could not access CombatSupport.activeAttack field: " + e.getMessage());
            activeAttackField = null;
        }
    }

    public BotManager(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    /**
     * Spawns a bot in a match.
     * MUST be called on the arena world thread (e.g. from Match.tick()).
     *
     * @param match the match to spawn the bot in
     * @param spawnPosition the position to spawn at
     * @param kitId the kit to apply (can be null)
     * @param difficulty the bot difficulty
     * @return the created BotParticipant
     */
    public BotParticipant spawnBot(Match match, Position spawnPosition, String kitId, BotDifficulty difficulty) {
        System.out.println("[DEBUG BotManager] spawnBot() called" +
            ", thread: " + Thread.currentThread().getName() +
            ", arena: " + match.getArena().getId() +
            ", world: " + match.getArena().getWorld().getName() +
            ", pos: " + String.format("%.1f,%.1f,%.1f", spawnPosition.getX(), spawnPosition.getY(), spawnPosition.getZ()));

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

        // Spawn NPC entity in the arena world (already on world thread)
        spawnBotEntity(bot, match.getArena(), spawnPosition);

        System.out.println("[BotManager] Spawned bot " + botName + " (difficulty: " + difficulty +
            ") in match " + match.getMatchId());

        return bot;
    }

    // Role name for objective-aware bots (Generic role with Follow + Idle instructions)
    private static final String OBJECTIVE_BOT_ROLE = "HyArena_Bot_Objective";

    /**
     * Spawns the visual NPC entity for a bot.
     * MUST be called on the arena world thread.
     */
    private void spawnBotEntity(BotParticipant bot, Arena arena, Position spawn) {
        World world = arena.getWorld();
        System.out.println("[DEBUG BotManager] spawnBotEntity() - bot: " + bot.getName() +
            ", thread: " + Thread.currentThread().getName() +
            ", world: " + (world != null ? world.getName() : "NULL"));

        if (world == null) {
            System.err.println("[BotManager] Cannot spawn bot entity - arena world is null");
            return;
        }

        // Determine which role to use:
        // - Objective modes (KOTH): use HyArena_Bot_Objective (has Follow + Idle)
        // - Non-objective modes (Duel, DM, LMS): use arena's botModelId (Template_Intelligent combat AI)
        String botModel;
        Match match = botMatches.get(bot.getUniqueId());
        boolean useObjectiveRole = false;
        if (match != null) {
            BotObjective objective = match.getGameMode().getBotObjective(arena.getConfig());
            if (objective != null) {
                botModel = OBJECTIVE_BOT_ROLE;
                useObjectiveRole = true;
            } else {
                botModel = arena.getConfig().getBotModelId();
                if (botModel == null || botModel.isEmpty()) {
                    botModel = DEFAULT_BOT_MODEL;
                }
            }
        } else {
            botModel = arena.getConfig().getBotModelId();
            if (botModel == null || botModel.isEmpty()) {
                botModel = DEFAULT_BOT_MODEL;
            }
        }
        System.out.println("[DEBUG BotManager] Using role: " + botModel + " (objectiveRole=" + useObjectiveRole + ")");

        try {
            Store<EntityStore> store = world.getEntityStore().getStore();
            System.out.println("[DEBUG BotManager] Got store from world: " + (store != null ? "OK" : "NULL"));

            // Set position and rotation
            Vector3d position = new Vector3d(spawn.getX(), spawn.getY(), spawn.getZ());
            Vector3f rotation = new Vector3f(spawn.getPitch(), spawn.getYaw(), 0);

            // Spawn NPC using NPCPlugin
            NPCPlugin npcPlugin = NPCPlugin.get();
            System.out.println("[DEBUG BotManager] Calling NPCPlugin.spawnNPC()...");
            var result = npcPlugin.spawnNPC(store, botModel, null, position, rotation);
            System.out.println("[DEBUG BotManager] spawnNPC result: " + (result != null ? "OK" : "NULL"));

            if (result != null) {
                Ref<EntityStore> entityRef = result.first();
                INonPlayerCharacter npc = result.second();
                System.out.println("[DEBUG BotManager] entityRef: " + (entityRef != null ? "valid=" + entityRef.isValid() : "NULL") +
                    ", npc: " + (npc != null ? npc.getClass().getSimpleName() : "NULL"));

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

                // Freeze bot at spawn if match is in WAITING/STARTING state
                Match botMatch = botMatches.get(bot.getUniqueId());
                if (botMatch != null) {
                    MatchState matchState = botMatch.getState();
                    if (matchState == MatchState.WAITING || matchState == MatchState.STARTING) {
                        freezeBotAtSpawn(bot, store);
                    }
                }

                System.out.println("[BotManager] Spawned NPC entity for bot " + bot.getName() +
                    " with model " + botModel);

            } else {
                System.err.println("[BotManager] NPCPlugin.spawnNPC returned null for model: " + botModel);
            }

        } catch (Exception e) {
            System.err.println("[BotManager] Failed to spawn bot entity: " + e.getMessage());
            e.printStackTrace();
        }
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
     * Neutralizes a dead bot — clears its NPC target so it stops chasing.
     * The entity is kept alive to prevent interaction chain crashes when players leave.
     * Actual entity removal happens later via despawnAllBotsInMatch().
     */
    public void neutralizeBot(BotParticipant bot) {
        if (bot == null) return;

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

        System.out.println("[BotManager] Neutralized dead bot " + bot.getName());
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

        // Clean up objective marker for this bot
        Ref<EntityStore> marker = objectiveMarkers.remove(botId);
        if (marker != null && marker.isValid()) {
            try {
                marker.getStore().removeEntity(marker, RemoveReason.REMOVE);
            } catch (Exception e) {
                // Ignore
            }
        }
        botZoneOffsets.remove(botId);

        // Remove from tracking
        activeBots.remove(botId);
        botMatches.remove(botId);
        wasAttacking.remove(botId);
        objectiveLogCounter.remove(botId);

        System.out.println("[BotManager] Despawned bot " + bot.getName());
    }

    /**
     * Removes the visual NPC entity.
     * MUST be called on the arena world thread.
     */
    private void despawnBotEntity(BotParticipant bot) {
        Ref<EntityStore> entityRef = bot.getEntityRef();
        System.out.println("[DEBUG BotManager] despawnBotEntity() - bot: " + bot.getName() +
            ", thread: " + Thread.currentThread().getName() +
            ", entityRef: " + (entityRef != null ? "valid=" + entityRef.isValid() : "NULL"));

        if (entityRef == null || !entityRef.isValid()) {
            System.out.println("[DEBUG BotManager] Skipping despawn - entityRef is null or invalid");
            return;
        }

        try {
            Store<EntityStore> store = entityRef.getStore();
            System.out.println("[DEBUG BotManager] entityRef.getStore(): " + (store != null ? "OK" : "NULL"));
            if (store == null) {
                System.err.println("[DEBUG BotManager] Cannot despawn - store is null!");
                return;
            }
            store.removeEntity(entityRef, RemoveReason.REMOVE);
            bot.setEntityRef(null);
            System.out.println("[DEBUG BotManager] Successfully despawned bot entity: " + bot.getName());
        } catch (Exception e) {
            System.err.println("[BotManager] Failed to despawn bot entity: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Despawns all bots in a match.
     */
    public void despawnAllBotsInMatch(Match match) {
        if (match == null) {
            return;
        }

        UUID matchId = match.getMatchId();
        System.out.println("[DEBUG BotManager] despawnAllBotsInMatch() - matchId: " + matchId +
            ", thread: " + Thread.currentThread().getName() +
            ", activeBots: " + activeBots.size() + ", botMatches: " + botMatches.size());

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
        // despawnBot() handles per-bot marker cleanup
        toRemove.forEach(this::despawnBot);
    }

    /**
     * Ticks all bots belonging to a specific match.
     * MUST be called on the arena world thread (from Match.tick()).
     */
    // Counts ticks for periodic debug logging
    private int tickDebugCounter = 0;

    public void tickBotsForMatch(Match match) {
        tickDebugCounter++;
        boolean shouldLog = (tickDebugCounter % 20 == 1); // Log once per second (first tick, then every 20)

        int botCount = 0;
        int skippedNull = 0;
        int skippedDead = 0;

        for (Participant p : match.getParticipants()) {
            if (p.getType() != ParticipantType.BOT) continue;
            BotParticipant bot = activeBots.get(p.getUniqueId());
            if (bot == null) { skippedNull++; continue; }
            if (!bot.isAlive()) { skippedDead++; continue; }
            botCount++;
            try {
                tickBot(bot);
            } catch (Exception e) {
                System.err.println("[BotManager] Error ticking bot " + bot.getName() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }

        if (shouldLog && (botCount > 0 || skippedNull > 0 || skippedDead > 0)) {
            System.out.println("[DEBUG BotManager] tickBotsForMatch() - ticked: " + botCount +
                ", skippedNull: " + skippedNull + ", skippedDead: " + skippedDead +
                ", matchState: " + match.getState() +
                ", thread: " + Thread.currentThread().getName());
        }
    }

    /**
     * Ticks a single bot.
     */
    private int tickBotDebugCounter = 0;

    private void tickBot(BotParticipant bot) {
        tickBotDebugCounter++;
        boolean shouldLog = (tickBotDebugCounter % 100 == 1); // Log every 5 seconds per bot

        Match match = botMatches.get(bot.getUniqueId());
        if (match == null || !bot.isAlive()) {
            if (shouldLog) System.out.println("[DEBUG BotManager] tickBot() skip - " + bot.getName() +
                ", match: " + (match != null ? "OK" : "NULL") + ", alive: " + bot.isAlive());
            return;
        }

        Ref<EntityStore> entityRef = bot.getEntityRef();
        if (entityRef == null || !entityRef.isValid()) {
            if (shouldLog) System.out.println("[DEBUG BotManager] tickBot() skip - " + bot.getName() +
                ", entityRef: " + (entityRef != null ? "valid=" + entityRef.isValid() : "NULL"));
            return;
        }

        Store<EntityStore> store = entityRef.getStore();
        if (store == null) {
            if (shouldLog) System.out.println("[DEBUG BotManager] tickBot() skip - " + bot.getName() +
                ", entityRef.getStore() returned NULL");
            return;
        }

        if (shouldLog) {
            System.out.println("[DEBUG BotManager] tickBot() OK - " + bot.getName() +
                ", state: " + match.getState() +
                ", health: " + String.format("%.1f/%.1f", bot.getHealth(), bot.getMaxHealth()) +
                ", pos: " + (bot.getCurrentPosition() != null ?
                    String.format("%.1f,%.1f,%.1f", bot.getCurrentPosition().getX(), bot.getCurrentPosition().getY(), bot.getCurrentPosition().getZ()) : "null") +
                ", thread: " + Thread.currentThread().getName());
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

        // Manual bot-on-bot damage (synced with NPC facing direction)
        applyBotCombatDamage(bot, match);
    }

    /**
     * Freezes a bot at its spawn position during countdown.
     * Teleports bot back to spawn if it moves and clears its target.
     */
    private int freezeDebugCounter = 0;

    private void freezeBotAtSpawn(BotParticipant bot, Store<EntityStore> store) {
        freezeDebugCounter++;
        boolean shouldLog = (freezeDebugCounter % 100 == 1); // Log every 5s

        Ref<EntityStore> entityRef = bot.getEntityRef();
        Position spawn = bot.getSpawnPosition();
        if (entityRef == null || !entityRef.isValid() || spawn == null) {
            if (shouldLog) System.out.println("[DEBUG BotManager] freezeBotAtSpawn() skip - " + bot.getName() +
                ", entityRef: " + (entityRef != null ? "valid=" + entityRef.isValid() : "NULL") +
                ", spawn: " + (spawn != null ? "OK" : "NULL"));
            return;
        }

        try {
            TransformComponent transform = store.getComponent(entityRef, TransformComponent.getComponentType());
            if (transform != null) {
                Vector3d spawnVec = new Vector3d(spawn.getX(), spawn.getY(), spawn.getZ());
                Vector3d currentPos = transform.getPosition();

                // Teleport back if bot moved too far from spawn
                double distance = currentPos.distanceTo(spawnVec);
                if (shouldLog) System.out.println("[DEBUG BotManager] freezeBotAtSpawn() " + bot.getName() +
                    " distance from spawn: " + String.format("%.2f", distance));
                if (distance > 0.5) {
                    transform.setPosition(spawnVec);
                    bot.setCurrentPosition(spawn.copy());
                }
            } else {
                if (shouldLog) System.out.println("[DEBUG BotManager] freezeBotAtSpawn() " + bot.getName() +
                    " - transform component is NULL");
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
            System.err.println("[DEBUG BotManager] freezeBotAtSpawn() error for " + bot.getName() + ": " + e.getMessage());
            e.printStackTrace();
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

    // Throttle objective logging (once per second per bot)
    private final Map<UUID, Integer> objectiveLogCounter = new ConcurrentHashMap<>();

    /**
     * Updates the bot's NPC target with objective awareness.
     * In non-objective modes: finds nearest enemy (existing behavior).
     * In objective modes (KOTH): always walks to/stays at objective zone.
     * TODO: Add zone-based fighting once movement is verified working.
     */
    private void updateBotTarget(BotParticipant bot, Match match, Store<EntityStore> store) {
        NPCEntity npcEntity = bot.getNpcEntity();
        if (npcEntity == null) return;

        Role role = npcEntity.getRole();
        if (role == null) return;

        Position botPos = bot.getCurrentPosition();
        if (botPos == null) return;

        ArenaConfig config = match.getArena().getConfig();
        BotObjective objective = match.getGameMode().getBotObjective(config);

        if (objective == null) {
            // Non-objective mode: chase nearest enemy (existing behavior)
            NearestTarget nearest = findNearestTarget(bot, match, store);
            applyEnemyTarget(bot, role, nearest, store);
            return;
        }

        // Logging throttle: once per second
        int logCount = objectiveLogCounter.merge(bot.getUniqueId(), 1, Integer::sum);
        boolean shouldLog = (logCount % 20 == 1);

        // Objective mode (TEST): always walk to zone, no fighting
        boolean botInZone = objective.isInsideZone(botPos);
        double distToCenter = botPos.distanceTo(objective.position());

        if (shouldLog) {
            System.out.println("[BotObjective] " + bot.getName() +
                " | pos: " + String.format("%.1f,%.1f,%.1f", botPos.getX(), botPos.getY(), botPos.getZ()) +
                " | zone center: " + String.format("%.1f,%.1f,%.1f",
                    objective.position().getX(), objective.position().getY(), objective.position().getZ()) +
                " | dist: " + String.format("%.1f", distToCenter) +
                " | inZone: " + botInZone +
                " | zoneBounds: " + String.format("%.0f,%.0f,%.0f -> %.0f,%.0f,%.0f",
                    objective.minX(), objective.minY(), objective.minZ(),
                    objective.maxX(), objective.maxY(), objective.maxZ()));
        }

        // Clear BotAI combat target so it doesn't interfere
        BotAI ai = bot.getAI();
        if (ai != null) {
            ai.clearTarget();
        }

        // Compute this bot's individual target position (zone center + random offset)
        Position botTarget = getBotZoneTarget(bot.getUniqueId(), objective);

        if (botInZone) {
            // At zone — hold position, clear NPC target
            clearNpcTarget(role, bot.getEntityRef(), store);
            if (shouldLog) {
                System.out.println("[BotObjective] " + bot.getName() + " → HOLDING at zone");
            }
        } else {
            // Not at zone — walk to it via invisible marker (per-bot, with offset)
            Ref<EntityStore> marker = getOrCreateObjectiveMarker(bot.getUniqueId(), botTarget, store);
            if (marker != null && marker.isValid()) {
                setNpcObjectiveTarget(role, marker, bot.getEntityRef(), store);
                if (shouldLog) {
                    try {
                        TransformComponent markerTransform = store.getComponent(marker, TransformComponent.getComponentType());
                        if (markerTransform != null) {
                            Vector3d markerPos = markerTransform.getPosition();
                            System.out.println("[BotObjective] " + bot.getName() +
                                " → WALKING to zone | target at: " +
                                String.format("%.1f,%.1f,%.1f", markerPos.getX(), markerPos.getY(), markerPos.getZ()) +
                                " | marker valid: " + marker.isValid());
                        }
                    } catch (Exception e) {
                        System.out.println("[BotObjective] " + bot.getName() +
                            " → WALKING to zone | marker read error: " + e.getMessage());
                    }
                }
            } else {
                if (shouldLog) {
                    System.out.println("[BotObjective] " + bot.getName() +
                        " → WALKING to zone | marker FAILED (null or invalid)");
                }
            }
        }
    }

    /**
     * Finds the nearest alive enemy participant relative to this bot.
     */
    private NearestTarget findNearestTarget(BotParticipant bot, Match match, Store<EntityStore> store) {
        Position botPos = bot.getCurrentPosition();
        if (botPos == null) return null;

        Participant nearestParticipant = null;
        Ref<EntityStore> nearestRef = null;
        double nearestDistance = Double.MAX_VALUE;

        for (Participant participant : match.getParticipants()) {
            if (participant.getUniqueId().equals(bot.getUniqueId())) continue;
            if (!participant.isAlive()) continue;

            Position targetPos = null;
            Ref<EntityStore> targetRef = null;

            if (participant.getType() == ParticipantType.PLAYER) {
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
                    nearestParticipant = participant;
                    nearestRef = targetRef;
                }
            }
        }

        if (nearestParticipant == null) return null;
        return new NearestTarget(nearestParticipant, nearestRef, nearestDistance);
    }

    /**
     * Applies an enemy as the bot's NPC target (combat mode).
     * Sets LockedTarget + HOSTILE attitude + updates BotAI.
     */
    private void applyEnemyTarget(BotParticipant bot, Role role, NearestTarget nearest, Store<EntityStore> store) {
        // Update BotAI target
        BotAI ai = bot.getAI();
        if (ai != null && nearest != null) {
            Position targetPos = null;
            if (nearest.participant.getType() == ParticipantType.BOT) {
                BotParticipant targetBot = activeBots.get(nearest.participant.getUniqueId());
                if (targetBot != null) {
                    targetPos = targetBot.getCurrentPosition();
                }
            }
            ai.setTarget(nearest.participant.getUniqueId(), targetPos);
        }

        // Set NPC target for movement
        if (nearest != null && nearest.entityRef != null && nearest.entityRef.isValid()) {
            try {
                MarkedEntitySupport markedSupport = role.getMarkedEntitySupport();
                if (markedSupport != null) {
                    markedSupport.setMarkedEntity(MarkedEntitySupport.DEFAULT_TARGET_SLOT, nearest.entityRef);
                }

                WorldSupport worldSupport = role.getWorldSupport();
                if (worldSupport != null) {
                    worldSupport.overrideAttitude(nearest.entityRef, Attitude.HOSTILE, 60.0);
                }
            } catch (Exception e) {
                // Ignore targeting errors
            }
        }
    }

    // Log available states once per NPC model to avoid spam
    private final Set<String> loggedRoleStates = ConcurrentHashMap.newKeySet();

    /**
     * Sets the invisible objective marker as the NPC's target and activates Follow state.
     * Requires the HyArena_Bot_Objective role which defines Follow + Idle instructions.
     * Follow uses: Target sensor (checks LockedTarget) + State sensor (checks Follow state)
     * → Seek body motion (pathfinds toward the marker entity).
     */
    private void setNpcObjectiveTarget(Role role, Ref<EntityStore> markerRef, Ref<EntityStore> botEntityRef, Store<EntityStore> store) {
        try {
            // Set marker as LockedTarget — provides the "LiveEntity" feature for Seek body motion
            MarkedEntitySupport markedSupport = role.getMarkedEntitySupport();
            if (markedSupport != null) {
                markedSupport.setMarkedEntity(MarkedEntitySupport.DEFAULT_TARGET_SLOT, markerRef);
            }

            // Log available states once per role for debugging
            var stateHelper = role.getStateSupport().getStateHelper();
            String roleName = "unknown";
            try {
                NPCEntity npcEntity = store.getComponent(botEntityRef, NPCEntity.getComponentType());
                if (npcEntity != null) roleName = npcEntity.getRoleName();
            } catch (Exception e) { /* ignore */ }
            if (loggedRoleStates.add(roleName)) {
                String[] statesToCheck = {"Follow", "Chase", "Combat", "Attack", "Idle", "Patrol", "Wander"};
                StringBuilder sb = new StringBuilder("[BotObjective] Available states for role '" + roleName + "': ");
                for (String s : statesToCheck) {
                    sb.append(s).append("=").append(stateHelper.getStateIndex(s) >= 0 ? "YES" : "no").append(", ");
                }
                System.out.println(sb.toString());
            }

            // Set Follow state — our custom role defines FollowBehavior instruction
            // that fires when: Target sensor (LockedTarget exists) AND State sensor (in Follow state)
            if (stateHelper.getStateIndex("Follow") >= 0) {
                role.getStateSupport().setState(botEntityRef, "Follow", "Default", store);
            } else {
                System.err.println("[BotObjective] WARNING: Follow state not available for role '" + roleName +
                    "' — bot won't pathfind to objective. Is HyArena_Bot_Objective role loaded?");
            }
        } catch (Exception e) {
            System.err.println("[BotObjective] setNpcObjectiveTarget error: " + e.getMessage());
        }
    }

    /**
     * Clears the NPC target so it holds position at the objective.
     * Sets Idle state explicitly — our custom role's IdleBehavior instruction
     * fires (BodyMotion: Nothing) so the bot stands still.
     */
    private void clearNpcTarget(Role role, Ref<EntityStore> botEntityRef, Store<EntityStore> store) {
        try {
            MarkedEntitySupport markedSupport = role.getMarkedEntitySupport();
            if (markedSupport != null) {
                markedSupport.setMarkedEntity(MarkedEntitySupport.DEFAULT_TARGET_SLOT, null);
            }

            // Set Idle state so the NPC holds position
            var stateHelper = role.getStateSupport().getStateHelper();
            if (stateHelper.getStateIndex("Idle") >= 0) {
                role.getStateSupport().setState(botEntityRef, "Idle", "Default", store);
            }
        } catch (Exception e) {
            // Ignore
        }
    }

    /**
     * Computes an individual target position for a bot within the zone.
     * Each bot gets a persistent random XZ offset so they spread out naturally.
     * Offset is regenerated if zone changes (handled by clamping to new bounds).
     */
    private Position getBotZoneTarget(UUID botId, BotObjective objective) {
        double[] offset = botZoneOffsets.get(botId);
        if (offset == null) {
            // Generate random offset within ±40% of zone half-width/half-depth
            double halfX = (objective.maxX() - objective.minX()) / 2.0;
            double halfZ = (objective.maxZ() - objective.minZ()) / 2.0;
            double offX = (objectiveRandom.nextDouble() * 2 - 1) * halfX * 0.4;
            double offZ = (objectiveRandom.nextDouble() * 2 - 1) * halfZ * 0.4;
            offset = new double[]{offX, offZ};
            botZoneOffsets.put(botId, offset);
        }

        // Apply offset to zone center, clamped within bounds (with 0.5 margin)
        double margin = 0.5;
        double tx = Math.max(objective.minX() + margin,
                   Math.min(objective.maxX() - margin, objective.position().getX() + offset[0]));
        double tz = Math.max(objective.minZ() + margin,
                   Math.min(objective.maxZ() - margin, objective.position().getZ() + offset[1]));
        return new Position(tx, objective.position().getY(), tz);
    }

    /**
     * Gets or creates an invisible marker entity for a specific bot.
     * One marker per bot (not per match) so each bot walks to its own offset position.
     * Position is updated each tick to handle zone rotation.
     */
    private Ref<EntityStore> getOrCreateObjectiveMarker(UUID botId, Position target, Store<EntityStore> store) {
        Ref<EntityStore> existing = objectiveMarkers.get(botId);

        if (existing != null && existing.isValid()) {
            // Update position (handles zone rotation)
            try {
                TransformComponent t = store.getComponent(existing, TransformComponent.getComponentType());
                if (t != null) {
                    t.setPosition(new Vector3d(target.getX(), target.getY(), target.getZ()));
                }
            } catch (Exception e) {
                // Ignore
            }
            return existing;
        }

        // Spawn new invisible marker
        try {
            Holder<EntityStore> holder = EntityStore.REGISTRY.newHolder();
            ProjectileComponent proj = new ProjectileComponent("Projectile");
            holder.putComponent(ProjectileComponent.getComponentType(), proj);
            holder.putComponent(TransformComponent.getComponentType(),
                new TransformComponent(
                    new Vector3d(target.getX(), target.getY(), target.getZ()),
                    new Vector3f(0, 0, 0)));
            holder.ensureComponent(UUIDComponent.getComponentType());
            holder.ensureComponent(Intangible.getComponentType());
            holder.ensureComponent(MovementStatesComponent.getComponentType());
            holder.addComponent(NetworkId.getComponentType(),
                new NetworkId(store.getExternalData().takeNextNetworkId()));
            proj.initialize();

            Ref<EntityStore> ref = store.addEntity(holder, AddReason.SPAWN);
            if (ref != null) {
                objectiveMarkers.put(botId, ref);
                System.out.println("[BotManager] Spawned objective marker for bot " + botId);
            }
            return ref;
        } catch (Exception e) {
            System.err.println("[BotManager] Failed to spawn objective marker: " + e.getMessage());
            return null;
        }
    }

    /**
     * Holds info about the nearest enemy target found during scanning.
     */
    private static class NearestTarget {
        final Participant participant;
        final Ref<EntityStore> entityRef;
        final double distance;

        NearestTarget(Participant participant, Ref<EntityStore> entityRef, double distance) {
            this.participant = participant;
            this.entityRef = entityRef;
            this.distance = distance;
        }
    }

    /**
     * Checks if the NPC is executing an actual attack interaction (not blocking).
     * Uses reflection to read the activeAttack InteractionChain from CombatSupport,
     * then checks initialRootInteraction.getId() for "Attack" substring.
     * Both attacks and blocks use InteractionType.Primary for NPCs, so we must
     * inspect the interaction name to distinguish them.
     */
    private boolean isActuallyAttacking(CombatSupport combatSupport) {
        if (!combatSupport.isExecutingAttack()) return false;
        if (activeAttackField == null) return true; // Fallback: trust isExecutingAttack

        try {
            InteractionChain chain = (InteractionChain) activeAttackField.get(combatSupport);
            if (chain == null) return false;

            var initialRoot = chain.getInitialRootInteraction();
            if (initialRoot == null) return false;

            String id = initialRoot.getId();
            if (id == null) return false;

            // Attack interactions contain "Attack" in the ID (e.g. "Root_NPC_Skeleton_Sand_Guard_Attack")
            // Block interactions contain "Block" (e.g. "Shield_Block")
            return id.contains("Attack");
        } catch (Exception e) {
            return combatSupport.isExecutingAttack(); // Fallback
        }
    }

    /**
     * Checks if the NPC is currently blocking (Shield_Block interaction).
     */
    private boolean isBlocking(BotParticipant bot) {
        NPCEntity npcEntity = bot.getNpcEntity();
        if (npcEntity == null) return false;

        Role role = npcEntity.getRole();
        if (role == null) return false;

        CombatSupport combatSupport = role.getCombatSupport();
        if (combatSupport == null || !combatSupport.isExecutingAttack()) return false;
        if (activeAttackField == null) return false;

        try {
            InteractionChain chain = (InteractionChain) activeAttackField.get(combatSupport);
            if (chain == null) return false;

            var initialRoot = chain.getInitialRootInteraction();
            if (initialRoot == null) return false;

            String id = initialRoot.getId();
            return id != null && id.contains("Block");
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Applies bot-on-bot damage using edge detection on attack animations.
     * Fires damage once per swing (on false→true transition of isActuallyAttacking).
     * Damage is negated if the victim is currently blocking.
     */
    private void applyBotCombatDamage(BotParticipant attacker, Match match) {
        if (!attacker.isAlive()) return;

        NPCEntity npcEntity = attacker.getNpcEntity();
        if (npcEntity == null) return;

        Role role = npcEntity.getRole();
        if (role == null) return;

        CombatSupport combatSupport = role.getCombatSupport();
        MarkedEntitySupport markedSupport = role.getMarkedEntitySupport();
        if (combatSupport == null || markedSupport == null) return;

        UUID attackerId = attacker.getUniqueId();
        boolean attacking = isActuallyAttacking(combatSupport);
        boolean wasAttackingPrev = wasAttacking.getOrDefault(attackerId, false);
        wasAttacking.put(attackerId, attacking);

        // Edge detection: only fire damage on the false→true transition (start of swing)
        if (!attacking || wasAttackingPrev) return;

        // Read the engine's current target
        Ref<EntityStore> targetRef = markedSupport.getMarkedEntityRef(MarkedEntitySupport.DEFAULT_TARGET_SLOT);
        if (targetRef == null || !targetRef.isValid()) return;

        Store<EntityStore> store = targetRef.getStore();
        if (store == null) return;

        UUIDComponent targetUuidComp = store.getComponent(targetRef, UUIDComponent.getComponentType());
        if (targetUuidComp == null) return;

        BotParticipant victimBot = getBotByEntityUuid(targetUuidComp.getUuid());
        if (victimBot == null || !victimBot.isAlive() || victimBot.isImmune()) return;

        // Victim is blocking → damage negated
        if (isBlocking(victimBot)) return;

        // Apply damage scaled by attacker difficulty
        double damage = attacker.getDifficulty().getBaseDamage();

        boolean died = victimBot.takeDamage(damage);
        attacker.addDamageDealt(damage);

        // Update NPC entity health bar
        applyDamageToNpcEntity(victimBot, damage);

        if (died) {
            match.recordKill(victimBot.getUniqueId(), attacker.getUniqueId());
        } else {
            match.recordDamage(victimBot.getUniqueId(), attacker.getUniqueId(), damage);
        }
    }

    /**
     * Handles damage dealt by a bot (callback from BotAI).
     * Only processes bot-to-player damage; bot-on-bot is handled by applyBotCombatDamage().
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

        // Bot-on-bot is handled by applyBotCombatDamage() during tick (synced with NPC facing)
        if (victim.getType() == ParticipantType.BOT) {
            return;
        }

        // Bot-to-player: apply damage via Hytale's damage system
        if (victim.getType() == ParticipantType.PLAYER) {
            // Bot attacking player - apply damage via Hytale's damage system
            // This will be handled by KillDetectionSystem
            applyDamageToPlayer(victimId, damage);
            attacker.addDamageDealt(damage);
        }
    }

    /**
     * Applies damage to a player entity.
     * MUST be called on the arena world thread.
     */
    private void applyDamageToPlayer(UUID playerUuid, double damage) {
        PlayerRef playerRef = Universe.get().getPlayer(playerUuid);
        if (playerRef == null) {
            return;
        }

        try {
            Ref<EntityStore> ref = playerRef.getReference();
            if (ref == null || !ref.isValid()) {
                return;
            }

            Store<EntityStore> store = ref.getStore();
            if (store == null) {
                return;
            }
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
     * MUST be called on the arena world thread.
     */
    public void applyDamageToNpcEntity(BotParticipant bot, double damage) {
        Ref<EntityStore> entityRef = bot.getEntityRef();
        System.out.println("[DEBUG BotManager] applyDamageToNpcEntity() - bot: " + bot.getName() +
            ", damage: " + damage +
            ", thread: " + Thread.currentThread().getName() +
            ", entityRef: " + (entityRef != null ? "valid=" + entityRef.isValid() : "NULL"));

        if (entityRef == null || !entityRef.isValid()) {
            System.out.println("[DEBUG BotManager] Skipping NPC damage - entityRef null/invalid");
            return;
        }

        try {
            Store<EntityStore> store = entityRef.getStore();
            System.out.println("[DEBUG BotManager] NPC damage store: " + (store != null ? "OK" : "NULL"));
            if (store == null) {
                return;
            }
            EntityStatMap stats = store.getComponent(entityRef,
                EntityStatsModule.get().getEntityStatMapComponentType());
            System.out.println("[DEBUG BotManager] NPC stats: " + (stats != null ? "OK" : "NULL"));

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

                    System.out.println("[DEBUG BotManager] NPC health: " + currentNpcHealth + " -> " + newHealth +
                        " (npcMax=" + npcMaxHealth + ", internalMax=" + internalMax + ", scaledDmg=" + String.format("%.1f", scaledDamage) + ")");
                    stats.setStatValue(healthIndex, newHealth);
                } else {
                    System.out.println("[DEBUG BotManager] NPC healthStat is NULL");
                }
            }
        } catch (Exception e) {
            System.err.println("[DEBUG BotManager] NPC damage error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
