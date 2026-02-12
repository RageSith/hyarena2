package de.ragesith.hyarena2.bot;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.asset.type.attitude.Attitude;
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

import de.ragesith.hyarena2.utils.EntityInteractionHelper;
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

    // NPC role variants (different appearances, same behavior)
    private static final String[] BOT_ROLE_VARIANTS = {
        "HyArena_Bot_Objective_0",
        "HyArena_Bot_Objective_1",
        "HyArena_Bot_Objective_2",
        "HyArena_Bot_Objective_3"
    };
    private final Random roleRandom = new Random();

    // NPC entity health tracking (for proportional damage)
    private final Map<UUID, Float> npcMaxHealthMap = new ConcurrentHashMap<>();

    // Tracks previous tick's attacking state per bot for edge detection (fire damage on false→true)
    private final Map<UUID, Boolean> wasAttacking = new ConcurrentHashMap<>();

    // One invisible marker entity per bot (for objective navigation with individual offsets)
    private final Map<UUID, Ref<EntityStore>> objectiveMarkers = new ConcurrentHashMap<>();

    // Random XZ offset per bot within zone bounds (so bots don't stack on the same point)
    private final Map<UUID, double[]> botZoneOffsets = new ConcurrentHashMap<>();
    private final Random objectiveRandom = new Random();

    // Max distance at which a bot on the zone will attack without leaving (Defend state)
    private static final double DEFEND_RANGE = 3.0;

    // Max distance at which a bot on the zone will face an enemy (Watchout state — alert but not fighting)
    private static final double WATCHOUT_RANGE = 10.0;

    // Tracks the bot's current NPC state ("Combat", "Defend", "Idle", "Follow") for transition detection.
    // setState is only called on transitions to avoid interrupting NPC combat sequences.
    private final Map<UUID, String> botNpcState = new ConcurrentHashMap<>();

    // Counts ticks a bot has been stuck near the zone in Follow without entering.
    // After FOLLOW_STUCK_THRESHOLD ticks, falls back to Combat if enemies are nearby.
    private final Map<UUID, Integer> followStuckTicks = new ConcurrentHashMap<>();
    private static final int FOLLOW_STUCK_THRESHOLD = 30; // ~1.5 seconds at 20 ticks/sec
    private static final double ZONE_NEAR_DISTANCE = 3.0; // "close to zone" threshold

    // Reactive blocking: bot detects enemy starting an attack and raises shield for the attack's duration
    private final Map<UUID, Boolean> enemyWasAttacking = new ConcurrentHashMap<>();
    private final Map<UUID, String> preBlockState = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> blockActiveTicks = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> blockCooldownTicks = new ConcurrentHashMap<>();
    private final Random blockRandom = new Random();
    private static final int MAX_BLOCK_TICKS = 60;        // ~3s safety cap
    private static final int BLOCK_COOLDOWN_TICKS = 40;   // ~2s between blocks

    // Brain AI toggle — when true, uses BotBrain priority system; when false, uses legacy updateBotTarget/checkReactiveBlock
    private boolean useBrainAI = true;

    public void setUseBrainAI(boolean use) { this.useBrainAI = use; }
    public boolean isUsingBrainAI() { return useBrainAI; }

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
        if (match == null || spawnPosition == null) {
            System.err.println("[BotManager] Cannot spawn bot - match or position is null");
            return null;
        }

        // Generate unique name
        String botName = generateBotName();

        // Pick a random appearance variant, avoiding duplicates while unused variants remain
        Set<String> usedRoles = new HashSet<>();
        for (Participant p : match.getParticipants()) {
            if (p.getType() == ParticipantType.BOT && p instanceof BotParticipant bp) {
                usedRoles.add(bp.getRoleId());
            }
        }
        List<String> available = new ArrayList<>();
        for (String variant : BOT_ROLE_VARIANTS) {
            if (!usedRoles.contains(variant)) {
                available.add(variant);
            }
        }
        if (available.isEmpty()) {
            available = Arrays.asList(BOT_ROLE_VARIANTS);
        }
        String roleId = available.get(roleRandom.nextInt(available.size()));

        // Create bot participant
        BotParticipant bot = new BotParticipant(botName, difficulty, roleId);
        bot.setSelectedKitId(kitId);
        bot.setSpawnPosition(spawnPosition);
        bot.setCurrentPosition(spawnPosition.copy());

        // Create AI controller
        BotAI ai = new BotAI(bot);
        ai.setDamageCallback(this::handleBotDamage);
        bot.setAI(ai);

        // Create brain decision engine
        BotBrain brain = new BotBrain(difficulty);
        bot.setBrain(brain);

        // Track bot
        activeBots.put(bot.getUniqueId(), bot);
        botMatches.put(bot.getUniqueId(), match);

        // Spawn NPC entity in the arena world (already on world thread)
        spawnBotEntity(bot, match.getArena(), spawnPosition);

        System.out.println("[BotManager] Spawned bot " + botName + " (difficulty: " + difficulty +
            ") in match " + match.getMatchId());

        return bot;
    }

    /**
     * Spawns the visual NPC entity for a bot.
     * Objective modes use OBJECTIVE_BOT_ROLE (custom role with Follow/Idle/Combat states).
     * Non-objective modes use the arena's configured botModelId.
     * MUST be called on the arena world thread.
     */
    private void spawnBotEntity(BotParticipant bot, Arena arena, Position spawn) {
        World world = arena.getWorld();

        if (world == null) {
            System.err.println("[BotManager] Cannot spawn bot entity - arena world is null");
            return;
        }

        // Use the bot's assigned role variant (set once at creation, persists through respawns)
        String botModel = bot.getRoleId();

        try {
            Store<EntityStore> store = world.getEntityStore().getStore();

            // Set position and rotation
            Vector3d position = new Vector3d(spawn.getX(), spawn.getY(), spawn.getZ());
            Vector3f rotation = new Vector3f(spawn.getPitch(), spawn.getYaw(), 0);

            // Spawn NPC using NPCPlugin
            NPCPlugin npcPlugin = NPCPlugin.get();
            var result = npcPlugin.spawnNPC(store, botModel, null, position, rotation);

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
        if (bot.getBrain() != null) {
            bot.getBrain().reset();
        }

        // Clear tracked NPC state so updateBotTarget() re-evaluates from scratch
        UUID respawnId = bot.getUniqueId();
        botNpcState.remove(respawnId);
        enemyWasAttacking.remove(respawnId);
        preBlockState.remove(respawnId);
        blockActiveTicks.remove(respawnId);
        blockCooldownTicks.remove(respawnId);

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
        botNpcState.remove(botId);
        followStuckTicks.remove(botId);
        enemyWasAttacking.remove(botId);
        preBlockState.remove(botId);
        blockActiveTicks.remove(botId);
        blockCooldownTicks.remove(botId);
        System.out.println("[BotManager] Despawned bot " + bot.getName());
    }

    /**
     * Removes the visual NPC entity.
     * MUST be called on the arena world thread.
     */
    private void despawnBotEntity(BotParticipant bot) {
        Ref<EntityStore> entityRef = bot.getEntityRef();

        if (entityRef == null || !entityRef.isValid()) {
            return;
        }

        try {
            Store<EntityStore> store = entityRef.getStore();
            if (store == null) {
                return;
            }
            store.removeEntity(entityRef, RemoveReason.REMOVE);
            bot.setEntityRef(null);
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
    public void tickBotsForMatch(Match match) {
        for (Participant p : match.getParticipants()) {
            if (p.getType() != ParticipantType.BOT) continue;
            BotParticipant bot = activeBots.get(p.getUniqueId());
            if (bot == null) continue;
            if (!bot.isAlive()) continue;
            try {
                tickBot(bot);
            } catch (Exception e) {
                System.err.println("[BotManager] Error ticking bot " + bot.getName() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * Ticks a single bot.
     */
    private void tickBot(BotParticipant bot) {
        Match match = botMatches.get(bot.getUniqueId());
        if (match == null || !bot.isAlive()) {
            return;
        }

        Ref<EntityStore> entityRef = bot.getEntityRef();
        if (entityRef == null || !entityRef.isValid()) {
            return;
        }

        Store<EntityStore> store = entityRef.getStore();
        if (store == null) {
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

        // Update AI state (reaction time gating — shared by both paths)
        BotAI ai = bot.getAI();
        if (ai != null) {
            ai.tick();
        }

        if (useBrainAI) {
            // Brain AI path — priority-based decisions
            BotBrain brain = bot.getBrain();
            if (brain != null) {
                BrainContext ctx = buildBrainContext(bot, match, store);
                BrainDecision decision = brain.evaluate(ctx);
                applyBrainDecision(bot, decision, ctx, store);
            }
        } else {
            // Legacy path — original updateBotTarget + checkReactiveBlock
            updateBotTarget(bot, match, store);
            checkReactiveBlock(bot, match, store);
        }

        // Manual bot-on-bot damage (synced with NPC facing direction) — shared by both paths
        applyBotCombatDamage(bot, match);
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
            System.err.println("[BotManager] freezeBotAtSpawn() error for " + bot.getName() + ": " + e.getMessage());
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

    /**
     * Updates the bot's NPC target with objective awareness.
     * In non-objective modes: finds nearest enemy (existing behavior).
     * In objective modes (KOTH): zone-status-driven decision:
     *   1. Enemies in zone → fight nearest enemy on zone (stop them scoring)
     *   2. Bot in zone, no enemies → hold position (score)
     *   3. Zone empty, bot not there → walk to zone (capture)
     */
    private void updateBotTarget(BotParticipant bot, Match match, Store<EntityStore> store) {
        // Don't change target while bot is reactively blocking
        if ("Block".equals(botNpcState.get(bot.getUniqueId()))) return;

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
            // Track state as Combat so checkReactiveBlock can trigger
            if (nearest != null) {
                botNpcState.putIfAbsent(bot.getUniqueId(), "Combat");
            }
            return;
        }

        // Find enemies in the zone (all zone participants except this bot)
        List<UUID> enemiesInZone = objective.participantsInZone().stream()
            .filter(id -> !id.equals(bot.getUniqueId()))
            .toList();
        boolean botInZone = objective.isInsideZone(botPos);

        // Strategic target management — Java decides WHO/WHERE, NPC role handles HOW
        // (attack, block, strafe are all handled by the role natively)
        UUID botId = bot.getUniqueId();
        String prevState = botNpcState.get(botId);

        if (botInZone) {
            // Bot entered the zone — reset stuck counter
            followStuckTicks.remove(botId);

            // Bot is ON the zone — decide: fight, defend, or idle
            if (!enemiesInZone.isEmpty()) {
                // FIGHT: enemies also on zone — full combat to contest
                NearestTarget nearest = findNearestTargetFromSet(bot, match, store, enemiesInZone);
                if (nearest != null) {
                    applyEnemyTarget(bot, role, nearest, store);
                    if (!"Combat".equals(prevState)) {
                        role.getStateSupport().setState(bot.getEntityRef(), "Combat", "Default", store);
                        botNpcState.put(botId, "Combat");
                    }
                }
            } else {
                // No enemies in zone — check for nearby threats
                NearestTarget nearbyEnemy = findNearestTarget(bot, match, store);
                boolean enemyInDefendRange = nearbyEnemy != null && nearbyEnemy.distance <= DEFEND_RANGE;

                if (enemyInDefendRange) {
                    // DEFEND: enemy within reach — attack in place without leaving zone
                    applyEnemyTarget(bot, role, nearbyEnemy, store);
                    if (!"Defend".equals(prevState)) {
                        role.getStateSupport().setState(bot.getEntityRef(), "Defend", "Default", store);
                        botNpcState.put(botId, "Defend");
                    }
                } else if (nearbyEnemy != null && nearbyEnemy.distance <= WATCHOUT_RANGE) {
                    // WATCHOUT: enemy nearby but not in attack range — face them, stay alert
                    applyEnemyTarget(bot, role, nearbyEnemy, store);
                    if (!"Watchout".equals(prevState)) {
                        role.getStateSupport().setState(bot.getEntityRef(), "Watchout", "Default", store);
                        botNpcState.put(botId, "Watchout");
                    }
                } else {
                    // HOLD: no enemies nearby — idle and capture
                    if (!"Idle".equals(prevState)) {
                        BotAI ai = bot.getAI();
                        if (ai != null) ai.clearTarget();
                        clearNpcTarget(role, bot.getEntityRef(), store);
                        botNpcState.put(botId, "Idle");
                    }
                }
            }
        } else {
            // WALK: not in zone — go there first, fight only if stuck
            double distToZoneCenter = botPos.distanceTo(objective.position());

            // Track stuck ticks when close to zone but not inside
            if (distToZoneCenter <= ZONE_NEAR_DISTANCE) {
                int stuck = followStuckTicks.merge(botId, 1, Integer::sum);

                // Stuck too long near zone — fall back to fighting nearest enemy
                if (stuck >= FOLLOW_STUCK_THRESHOLD) {
                    NearestTarget nearest = findNearestTarget(bot, match, store);
                    if (nearest != null) {
                        applyEnemyTarget(bot, role, nearest, store);
                        if (!"Combat".equals(prevState)) {
                            role.getStateSupport().setState(bot.getEntityRef(), "Combat", "Default", store);
                            botNpcState.put(botId, "Combat");
                        }
                        return;
                    }
                }
            } else {
                followStuckTicks.remove(botId);
            }

            // Normal follow to zone
            Position botTarget = getBotZoneTarget(botId, objective);
            if (!"Follow".equals(prevState)) {
                BotAI ai = bot.getAI();
                if (ai != null) ai.clearTarget();
                Ref<EntityStore> marker = getOrCreateObjectiveMarker(botId, botTarget, store);
                if (marker != null && marker.isValid()) {
                    setNpcObjectiveTarget(role, marker, bot.getEntityRef(), store);
                    botNpcState.put(botId, "Follow");
                }
            } else {
                // Already following — update marker position and re-assign as target
                Ref<EntityStore> marker = getOrCreateObjectiveMarker(botId, botTarget, store);
                if (marker != null && marker.isValid()) {
                    MarkedEntitySupport markedSupport = role.getMarkedEntitySupport();
                    if (markedSupport != null) {
                        markedSupport.setMarkedEntity(MarkedEntitySupport.DEFAULT_TARGET_SLOT, marker);
                    }
                }
            }
        }
    }

    /**
     * Reactive blocking: detects enemy starting an attack and raises shield for the attack's duration.
     * Enter Block when enemy swing starts (edge: false→true), hold while enemy is attacking,
     * exit when enemy stops attacking. Cooldown prevents block spam.
     */
    private void checkReactiveBlock(BotParticipant bot, Match match, Store<EntityStore> store) {
        UUID botId = bot.getUniqueId();

        // Get bot's NPC role (needed for state changes and target lookup)
        NPCEntity npcEntity = bot.getNpcEntity();
        if (npcEntity == null) return;
        Role role = npcEntity.getRole();
        if (role == null) return;

        // Check if the bot's current target enemy is attacking
        boolean enemyAttacking = isEnemyAttacking(role, store);
        boolean wasEnemyAtk = enemyWasAttacking.getOrDefault(botId, false);
        enemyWasAttacking.put(botId, enemyAttacking);

        // Currently in Block state — hold until enemy stops attacking or safety cap
        if ("Block".equals(botNpcState.get(botId))) {
            int ticks = blockActiveTicks.merge(botId, 1, Integer::sum);

            if (!enemyAttacking || ticks >= MAX_BLOCK_TICKS) {
                // Exit block — restore previous state
                String restore = preBlockState.remove(botId);
                if (restore == null) restore = "Combat";
                role.getStateSupport().setState(bot.getEntityRef(), restore, "Default", store);
                botNpcState.put(botId, restore);
                blockCooldownTicks.put(botId, BLOCK_COOLDOWN_TICKS);
                blockActiveTicks.remove(botId);
            }
            return;
        }

        // Decrement cooldown if active
        Integer cooldown = blockCooldownTicks.get(botId);
        if (cooldown != null) {
            cooldown--;
            if (cooldown <= 0) {
                blockCooldownTicks.remove(botId);
            } else {
                blockCooldownTicks.put(botId, cooldown);
                return;
            }
        }

        // Edge detect: enemy just started attacking (false→true)
        if (!enemyAttacking || wasEnemyAtk) return;

        // Only block from combat-related states (not Follow, Idle)
        String currentState = botNpcState.get(botId);
        if (currentState == null || (!currentState.equals("Combat") && !currentState.equals("Defend") && !currentState.equals("Watchout"))) {
            return;
        }

        // Can't raise shield while in own attack frames
        if (isActuallyAttacking(bot)) return;

        // Probability roll based on difficulty
        if (blockRandom.nextDouble() >= bot.getDifficulty().getBlockProbability()) return;

        // Enter Block state — shield stays up until enemy finishes attacking
        preBlockState.put(botId, currentState);
        blockActiveTicks.put(botId, 0);
        botNpcState.put(botId, "Block");
        role.getStateSupport().setState(bot.getEntityRef(), "Block", "Default", store);
    }

    /**
     * Checks if the bot's current NPC target (locked enemy) is executing an attack.
     * Works for both bot and player targets via EntityInteractionHelper.
     */
    private boolean isEnemyAttacking(Role role, Store<EntityStore> store) {
        MarkedEntitySupport markedSupport = role.getMarkedEntitySupport();
        if (markedSupport == null) return false;

        Ref<EntityStore> targetRef = markedSupport.getMarkedEntityRef(MarkedEntitySupport.DEFAULT_TARGET_SLOT);
        if (targetRef == null || !targetRef.isValid()) return false;

        try {
            Store<EntityStore> targetStore = targetRef.getStore();
            if (targetStore == null) return false;
            String interaction = EntityInteractionHelper.getPrimaryInteraction(targetRef, targetStore);
            return EntityInteractionHelper.classifyInteraction(interaction) == EntityInteractionHelper.InteractionKind.ATTACK;
        } catch (Exception e) {
            return false;
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
        Position nearestPos = null;
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
                    nearestPos = targetPos;
                }
            }
        }

        if (nearestParticipant == null) return null;
        return new NearestTarget(nearestParticipant, nearestRef, nearestDistance, nearestPos);
    }

    /**
     * Finds the nearest alive enemy participant from a specific set of UUIDs.
     * Used for zone-based targeting (only fight enemies inside the capture zone).
     */
    private NearestTarget findNearestTargetFromSet(BotParticipant bot, Match match, Store<EntityStore> store, List<UUID> targetIds) {
        Position botPos = bot.getCurrentPosition();
        if (botPos == null) return null;

        Participant nearestParticipant = null;
        Ref<EntityStore> nearestRef = null;
        Position nearestPos = null;
        double nearestDistance = Double.MAX_VALUE;

        for (Participant participant : match.getParticipants()) {
            if (!targetIds.contains(participant.getUniqueId())) continue;
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
                    nearestPos = targetPos;
                }
            }
        }

        if (nearestParticipant == null) return null;
        return new NearestTarget(nearestParticipant, nearestRef, nearestDistance, nearestPos);
    }

    /**
     * Applies an enemy as the bot's NPC target (combat mode).
     * Sets LockedTarget + HOSTILE attitude + updates BotAI.
     */
    private void applyEnemyTarget(BotParticipant bot, Role role, NearestTarget nearest, Store<EntityStore> store) {
        // Update BotAI target (position for all types so BotAI can calculate distance)
        BotAI ai = bot.getAI();
        if (ai != null && nearest != null) {
            ai.setTarget(nearest.participant.getUniqueId(), nearest.position);
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

    /**
     * Clears the NPC's LockedTarget and sets Idle state.
     * The NPC can still react to attacks via damage sensors in the role JSON.
     */
    private void clearNpcTarget(Role role, Ref<EntityStore> botEntityRef, Store<EntityStore> store) {
        try {
            MarkedEntitySupport markedSupport = role.getMarkedEntitySupport();
            if (markedSupport != null) {
                markedSupport.setMarkedEntity(MarkedEntitySupport.DEFAULT_TARGET_SLOT, null);
            }
            role.getStateSupport().setState(botEntityRef, "Idle", "Default", store);
        } catch (Exception e) {
            // Ignore
        }
    }

    /**
     * Sets a marker entity as the NPC's target for objective navigation.
     * Sets Follow state so the NPC pathfinds to the marker.
     */
    private void setNpcObjectiveTarget(Role role, Ref<EntityStore> markerRef, Ref<EntityStore> botEntityRef, Store<EntityStore> store) {
        try {
            MarkedEntitySupport markedSupport = role.getMarkedEntitySupport();
            if (markedSupport != null) {
                markedSupport.setMarkedEntity(MarkedEntitySupport.DEFAULT_TARGET_SLOT, markerRef);
            }
            role.getStateSupport().setState(botEntityRef, "Follow", "Default", store);
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
            // Generate random offset within ±20% of zone half-width/half-depth
            double halfX = (objective.maxX() - objective.minX()) / 2.0;
            double halfZ = (objective.maxZ() - objective.minZ()) / 2.0;
            double offX = (objectiveRandom.nextDouble() * 2 - 1) * halfX * 0.2;
            double offZ = (objectiveRandom.nextDouble() * 2 - 1) * halfZ * 0.2;
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

    // ========== Brain AI Methods ==========

    /**
     * Builds a BrainContext from existing match/bot data for the brain to evaluate.
     */
    private BrainContext buildBrainContext(BotParticipant bot, Match match, Store<EntityStore> store) {
        Position botPos = bot.getCurrentPosition();
        ArenaConfig config = match.getArena().getConfig();
        BotObjective objective = match.getGameMode().getBotObjective(config);

        // Find nearest enemy
        NearestEnemy nearest = findNearestEnemy(bot, match, store);

        // Objective-mode data
        List<UUID> enemiesInZone = List.of();
        boolean botInZone = false;
        if (objective != null && botPos != null) {
            enemiesInZone = objective.participantsInZone().stream()
                .filter(id -> !id.equals(bot.getUniqueId()))
                .toList();
            botInZone = objective.isInsideZone(botPos);
        }

        // Attack state detection
        boolean enemyAttacking = false;
        boolean botAttacking = isActuallyAttacking(bot);

        // First try the bot's current marked target
        NPCEntity npcEntity = bot.getNpcEntity();
        if (npcEntity != null) {
            Role role = npcEntity.getRole();
            if (role != null) {
                enemyAttacking = isEnemyAttacking(role, store);
            }
        }

        // If marked target isn't attacking (e.g. bot is following a marker entity),
        // check the nearest enemy's attack state directly
        if (!enemyAttacking && nearest != null && nearest.entityRef != null && nearest.entityRef.isValid()) {
            try {
                Store<EntityStore> enemyStore = nearest.entityRef.getStore();
                if (enemyStore != null) {
                    String interaction = EntityInteractionHelper.getPrimaryInteraction(nearest.entityRef, enemyStore);
                    enemyAttacking = EntityInteractionHelper.classifyInteraction(interaction) == EntityInteractionHelper.InteractionKind.ATTACK;
                }
            } catch (Exception e) {
                // Ignore
            }
        }

        // Resolve threat target — lowest-HP active threat from the brain's threat map
        NearestEnemy threatTarget = resolveThreatTarget(bot, match, store);

        return new BrainContext(bot, match, store, botPos, objective, nearest,
            enemiesInZone, botInZone, enemyAttacking, botAttacking, config.getBounds(), threatTarget);
    }

    /**
     * Resolves the lowest-HP active threat from the bot's brain threat map.
     * Returns a NearestEnemy for the best threat target, or null if no active threats.
     */
    private NearestEnemy resolveThreatTarget(BotParticipant bot, Match match, Store<EntityStore> store) {
        BotBrain brain = bot.getBrain();
        if (brain == null || !brain.hasActiveThreats()) return null;

        Position botPos = bot.getCurrentPosition();
        if (botPos == null) return null;

        Participant bestParticipant = null;
        Ref<EntityStore> bestRef = null;
        Position bestPos = null;
        double bestHealth = Double.MAX_VALUE;
        double bestDistance = 0;

        for (UUID threatUuid : brain.getThreats().keySet()) {
            Participant participant = match.getParticipant(threatUuid);
            if (participant == null || !participant.isAlive()) continue;

            Position targetPos = null;
            Ref<EntityStore> targetRef = null;
            double health = Double.MAX_VALUE;

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
                            // Player health from entity stats
                            EntityStatMap stats = store.getComponent(targetRef,
                                EntityStatsModule.get().getEntityStatMapComponentType());
                            if (stats != null) {
                                int healthIndex = EntityStatType.getAssetMap().getIndex("health");
                                EntityStatValue healthStat = stats.get(healthIndex);
                                if (healthStat != null) {
                                    health = healthStat.get();
                                }
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
                    health = otherBot.getHealth();
                }
            }

            if (targetPos == null) continue;

            double distance = botPos.distanceTo(targetPos);

            // Pick lowest HP threat
            if (health < bestHealth) {
                bestHealth = health;
                bestParticipant = participant;
                bestRef = targetRef;
                bestPos = targetPos;
                bestDistance = distance;
            }
        }

        if (bestParticipant == null) return null;
        return new NearestEnemy(bestParticipant, bestRef, bestDistance, bestPos);
    }

    /**
     * Translates a BrainDecision into NPC role state and target changes.
     */
    private void applyBrainDecision(BotParticipant bot, BrainDecision decision, BrainContext ctx, Store<EntityStore> store) {
        NPCEntity npcEntity = bot.getNpcEntity();
        if (npcEntity == null) return;

        Role role = npcEntity.getRole();
        if (role == null) return;

        UUID botId = bot.getUniqueId();
        String prevState = botNpcState.get(botId);

        switch (decision) {
            case BLOCK -> {
                // Ensure enemy is set as target so the NPC faces them while blocking
                if (ctx.nearestEnemy != null) {
                    applyEnemyTargetFromNearestEnemy(bot, role, ctx.nearestEnemy, store);
                }
                if (!"Block".equals(prevState)) {
                    role.getStateSupport().setState(bot.getEntityRef(), "Block", "Default", store);
                    botNpcState.put(botId, "Block");
                }
            }

            case COMBAT -> {
                // Choose target based on context:
                // 1. Off-zone with active threats → fight the threat target (lowest HP)
                // 2. On-zone with enemies in zone → fight zone enemies
                // 3. Default → fight nearest enemy
                NearestEnemy target = ctx.nearestEnemy;
                if (ctx.objective != null && !ctx.botInZone && ctx.threatTarget != null) {
                    target = ctx.threatTarget;
                } else if (ctx.objective != null && ctx.botInZone && !ctx.enemiesInZone.isEmpty()) {
                    NearestEnemy zoneEnemy = findNearestEnemyFromSet(bot, ctx.match, store, ctx.enemiesInZone);
                    if (zoneEnemy != null) {
                        target = zoneEnemy;
                    }
                }
                if (target != null) {
                    applyEnemyTargetFromNearestEnemy(bot, role, target, store);
                    if (!"Combat".equals(prevState)) {
                        role.getStateSupport().setState(bot.getEntityRef(), "Combat", "Default", store);
                        botNpcState.put(botId, "Combat");
                    }
                }
            }

            case DEFEND_ZONE -> {
                if (ctx.nearestEnemy != null) {
                    applyEnemyTargetFromNearestEnemy(bot, role, ctx.nearestEnemy, store);
                    // Use Defend if in attack range, Watchout if farther
                    String targetState = ctx.nearestEnemy.distance <= DEFEND_RANGE ? "Defend" : "Watchout";
                    if (!targetState.equals(prevState)) {
                        role.getStateSupport().setState(bot.getEntityRef(), targetState, "Default", store);
                        botNpcState.put(botId, targetState);
                    }
                }
            }

            case OBJECTIVE -> {
                Position botTarget = getBotZoneTarget(botId, ctx.objective);
                if (!"Follow".equals(prevState)) {
                    BotAI ai = bot.getAI();
                    if (ai != null) ai.clearTarget();
                    Ref<EntityStore> marker = getOrCreateObjectiveMarker(botId, botTarget, store);
                    if (marker != null && marker.isValid()) {
                        setNpcObjectiveTarget(role, marker, bot.getEntityRef(), store);
                        botNpcState.put(botId, "Follow");
                    }
                } else {
                    // Already following — update marker position
                    Ref<EntityStore> marker = getOrCreateObjectiveMarker(botId, botTarget, store);
                    if (marker != null && marker.isValid()) {
                        MarkedEntitySupport markedSupport = role.getMarkedEntitySupport();
                        if (markedSupport != null) {
                            markedSupport.setMarkedEntity(MarkedEntitySupport.DEFAULT_TARGET_SLOT, marker);
                        }
                    }
                }
            }

            case ROAM -> {
                BotBrain brain = bot.getBrain();
                Position waypoint = brain != null ? brain.getRoamWaypoint() : null;
                if (waypoint != null) {
                    if (!"Follow".equals(prevState)) {
                        BotAI ai = bot.getAI();
                        if (ai != null) ai.clearTarget();
                        Ref<EntityStore> marker = getOrCreateObjectiveMarker(botId, waypoint, store);
                        if (marker != null && marker.isValid()) {
                            setNpcObjectiveTarget(role, marker, bot.getEntityRef(), store);
                            botNpcState.put(botId, "Follow");
                        }
                    } else {
                        // Already following — update marker to waypoint position
                        Ref<EntityStore> marker = getOrCreateObjectiveMarker(botId, waypoint, store);
                        if (marker != null && marker.isValid()) {
                            MarkedEntitySupport markedSupport = role.getMarkedEntitySupport();
                            if (markedSupport != null) {
                                markedSupport.setMarkedEntity(MarkedEntitySupport.DEFAULT_TARGET_SLOT, marker);
                            }
                        }
                    }
                }
            }

            case IDLE -> {
                if (!"Idle".equals(prevState)) {
                    BotAI ai = bot.getAI();
                    if (ai != null) ai.clearTarget();
                    clearNpcTarget(role, bot.getEntityRef(), store);
                    botNpcState.put(botId, "Idle");
                }
            }
        }
    }

    /**
     * Applies an enemy as the bot's NPC target using NearestEnemy (brain path).
     * Parallel to applyEnemyTarget() which uses NearestTarget (legacy path).
     */
    private void applyEnemyTargetFromNearestEnemy(BotParticipant bot, Role role, NearestEnemy nearest, Store<EntityStore> store) {
        BotAI ai = bot.getAI();
        if (ai != null && nearest != null) {
            ai.setTarget(nearest.participant.getUniqueId(), nearest.position);
        }

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

    /**
     * Finds the nearest alive enemy participant relative to this bot (brain path).
     * Returns NearestEnemy (top-level class) instead of NearestTarget (inner class).
     */
    private NearestEnemy findNearestEnemy(BotParticipant bot, Match match, Store<EntityStore> store) {
        Position botPos = bot.getCurrentPosition();
        if (botPos == null) return null;

        Participant nearestParticipant = null;
        Ref<EntityStore> nearestRef = null;
        Position nearestPos = null;
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
                    nearestPos = targetPos;
                }
            }
        }

        if (nearestParticipant == null) return null;
        return new NearestEnemy(nearestParticipant, nearestRef, nearestDistance, nearestPos);
    }

    /**
     * Finds the nearest alive enemy from a specific set of UUIDs (brain path).
     * Used for zone-based targeting.
     */
    private NearestEnemy findNearestEnemyFromSet(BotParticipant bot, Match match, Store<EntityStore> store, List<UUID> targetIds) {
        Position botPos = bot.getCurrentPosition();
        if (botPos == null) return null;

        Participant nearestParticipant = null;
        Ref<EntityStore> nearestRef = null;
        Position nearestPos = null;
        double nearestDistance = Double.MAX_VALUE;

        for (Participant participant : match.getParticipants()) {
            if (!targetIds.contains(participant.getUniqueId())) continue;
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
                    nearestPos = targetPos;
                }
            }
        }

        if (nearestParticipant == null) return null;
        return new NearestEnemy(nearestParticipant, nearestRef, nearestDistance, nearestPos);
    }

    /**
     * Holds info about the nearest enemy target found during scanning.
     */
    private static class NearestTarget {
        final Participant participant;
        final Ref<EntityStore> entityRef;
        final double distance;
        final Position position;

        NearestTarget(Participant participant, Ref<EntityStore> entityRef, double distance, Position position) {
            this.participant = participant;
            this.entityRef = entityRef;
            this.distance = distance;
            this.position = position;
        }
    }

    /**
     * Checks if a bot is currently executing an attack interaction (Primary).
     */
    private boolean isActuallyAttacking(BotParticipant bot) {
        Ref<EntityStore> ref = bot.getEntityRef();
        if (ref == null || !ref.isValid()) return false;
        Store<EntityStore> store = ref.getStore();
        if (store == null) return false;
        String interaction = EntityInteractionHelper.getPrimaryInteraction(ref, store);
        return EntityInteractionHelper.classifyInteraction(interaction) == EntityInteractionHelper.InteractionKind.ATTACK;
    }

    /**
     * Checks if a bot is currently blocking (Secondary guard interaction).
     */
    private boolean isBlocking(BotParticipant bot) {
        Ref<EntityStore> ref = bot.getEntityRef();
        if (ref == null || !ref.isValid()) return false;
        Store<EntityStore> store = ref.getStore();
        if (store == null) return false;
        String interaction = EntityInteractionHelper.getSecondaryInteraction(ref, store);
        return EntityInteractionHelper.classifyInteraction(interaction) == EntityInteractionHelper.InteractionKind.BLOCK;
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

        MarkedEntitySupport markedSupport = role.getMarkedEntitySupport();
        if (markedSupport == null) return;

        UUID attackerId = attacker.getUniqueId();
        boolean attacking = isActuallyAttacking(attacker);
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

        // Register attacker as a threat on the victim's brain
        BotBrain victimBrain = victimBot.getBrain();
        if (victimBrain != null) {
            victimBrain.registerThreat(attacker.getUniqueId());
        }

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
     * Bot-on-bot damage is handled by applyBotCombatDamage() (synced with NPC facing).
     * Bot-to-player damage is handled entirely by the NPC's native combat (InteractionVars)
     * which goes through Hytale's damage system → KillDetectionSystem. That system
     * prevents fatal damage, records kills, and triggers respawns. We must NOT manually
     * apply damage here via setStatValue() as it bypasses KillDetectionSystem.
     */
    private void handleBotDamage(BotParticipant attacker, UUID victimId, double damage) {
        // No-op: both damage paths are handled elsewhere:
        // - Bot-to-player: NPC native combat → KillDetectionSystem
        // - Bot-on-bot: applyBotCombatDamage() in tickBot()
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

        if (entityRef == null || !entityRef.isValid()) {
            return;
        }

        try {
            Store<EntityStore> store = entityRef.getStore();
            if (store == null) {
                return;
            }
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
            System.err.println("[BotManager] NPC damage error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
