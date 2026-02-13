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
        "Chompers", "Bonkus", "Sir Lags", "Waffles", "Noodle",
        "Stabitha", "Clanky", "Spud", "Grug", "Fumbles",
        "Biscuit", "Yeetimus", "Wobble", "Turnip", "Clonk",
        "Pickles", "Dingus", "Snoot", "Bumble", "Thwack"
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

    // Tracks the bot's current NPC state ("Combat", "Defend", "Idle", "Follow") for transition detection.
    private final Map<UUID, String> botNpcState = new ConcurrentHashMap<>();

    // Strafe evasion: alternating direction and tick counter per bot
    private final Map<UUID, int[]> strafeState = new ConcurrentHashMap<>(); // [tickCounter, direction(1/-1)]
    private final Random strafeRandom = new Random();

    // Brain AI toggle — when true, uses BotBrain utility system; when false, uses legacy updateBotTarget/checkReactiveBlock
    private boolean useBrainAI = true;

    // Legacy path fields (only used when useBrainAI = false)
    private final Map<UUID, Boolean> enemyWasAttacking = new ConcurrentHashMap<>();
    private final Map<UUID, String> preBlockState = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> blockActiveTicks = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> blockCooldownTicks = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> followStuckTicks = new ConcurrentHashMap<>();
    private final Random blockRandom = new Random();
    private static final int MAX_BLOCK_TICKS = 60;
    private static final int BLOCK_COOLDOWN_TICKS = 40;
    private static final int FOLLOW_STUCK_THRESHOLD = 30;
    private static final double ZONE_NEAR_DISTANCE = 3.0;
    private static final double DEFEND_RANGE = 3.0;
    private static final double WATCHOUT_RANGE = 10.0;

    public void setUseBrainAI(boolean use) { this.useBrainAI = use; }
    public boolean isUsingBrainAI() { return useBrainAI; }

    public BotManager(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    /**
     * Spawns a bot in a match.
     * MUST be called on the arena world thread (e.g. from Match.tick()).
     */
    public BotParticipant spawnBot(Match match, Position spawnPosition, String kitId, BotDifficulty difficulty) {
        if (match == null || spawnPosition == null) {
            System.err.println("[BotManager] Cannot spawn bot - match or position is null");
            return null;
        }

        String botName = generateBotName();

        // Pick a random appearance variant, avoiding duplicates
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

        BotParticipant bot = new BotParticipant(botName, difficulty, roleId);
        bot.setSelectedKitId(kitId);
        bot.setSpawnPosition(spawnPosition);
        bot.setCurrentPosition(spawnPosition.copy());

        BotAI ai = new BotAI(bot);
        ai.setDamageCallback(this::handleBotDamage);
        bot.setAI(ai);

        BotBrain brain = new BotBrain(difficulty);
        bot.setBrain(brain);

        activeBots.put(bot.getUniqueId(), bot);
        botMatches.put(bot.getUniqueId(), match);

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

        String botModel = bot.getRoleId();

        try {
            Store<EntityStore> store = world.getEntityStore().getStore();

            Vector3d position = new Vector3d(spawn.getX(), spawn.getY(), spawn.getZ());
            Vector3f rotation = new Vector3f(spawn.getPitch(), spawn.getYaw(), 0);

            NPCPlugin npcPlugin = NPCPlugin.get();
            var result = npcPlugin.spawnNPC(store, botModel, null, position, rotation);

            if (result != null) {
                Ref<EntityStore> entityRef = result.first();
                INonPlayerCharacter npc = result.second();

                bot.setEntityRef(entityRef);
                bot.setNpc(npc);

                if (npc instanceof NPCEntity npcEntity) {
                    bot.setNpcEntity(npcEntity);
                }

                UUIDComponent uuidComponent = store.getComponent(entityRef, UUIDComponent.getComponentType());
                if (uuidComponent != null) {
                    UUID entityUuid = uuidComponent.getUuid();
                    bot.setEntityUuid(entityUuid);
                    entityUuidToBotMap.put(entityUuid, bot);
                }

                captureNpcMaxHealth(bot, store, entityRef);

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

        UUID oldEntityUuid = bot.getEntityUuid();
        if (oldEntityUuid != null) {
            entityUuidToBotMap.remove(oldEntityUuid);
            npcMaxHealthMap.remove(oldEntityUuid);
        }

        despawnBotEntity(bot);

        bot.resetHealth();
        bot.setSpawnPosition(spawnPosition);
        bot.setCurrentPosition(spawnPosition.copy());
        if (bot.getAI() != null) {
            bot.getAI().reset();
        }
        if (bot.getBrain() != null) {
            bot.getBrain().reset();
        }

        UUID respawnId = bot.getUniqueId();
        botNpcState.remove(respawnId);
        strafeState.remove(respawnId);

        // Legacy path cleanup
        enemyWasAttacking.remove(respawnId);
        preBlockState.remove(respawnId);
        blockActiveTicks.remove(respawnId);
        blockCooldownTicks.remove(respawnId);

        spawnBotEntity(bot, arena, spawnPosition);
        bot.grantImmunity(3000);

        System.out.println("[BotManager] Respawned bot " + bot.getName() + " at " +
            String.format("%.1f, %.1f, %.1f", spawnPosition.getX(), spawnPosition.getY(), spawnPosition.getZ()));
    }

    /**
     * Neutralizes a dead bot — clears its NPC target so it stops chasing.
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

        UUID entityUuid = bot.getEntityUuid();
        if (entityUuid != null) {
            entityUuidToBotMap.remove(entityUuid);
            npcMaxHealthMap.remove(entityUuid);
        }

        despawnBotEntity(bot);

        Ref<EntityStore> marker = objectiveMarkers.remove(botId);
        if (marker != null && marker.isValid()) {
            try {
                marker.getStore().removeEntity(marker, RemoveReason.REMOVE);
            } catch (Exception e) {
                // Ignore
            }
        }
        botZoneOffsets.remove(botId);

        activeBots.remove(botId);
        botMatches.remove(botId);
        wasAttacking.remove(botId);
        botNpcState.remove(botId);
        strafeState.remove(botId);

        // Legacy path cleanup
        followStuckTicks.remove(botId);
        enemyWasAttacking.remove(botId);
        preBlockState.remove(botId);
        blockActiveTicks.remove(botId);
        blockCooldownTicks.remove(botId);

        System.out.println("[BotManager] Despawned bot " + bot.getName());
    }

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

        syncBotPosition(bot, store);

        MatchState state = match.getState();
        if (state == MatchState.WAITING || state == MatchState.STARTING) {
            freezeBotAtSpawn(bot, store);
            return;
        }

        BotAI ai = bot.getAI();
        if (ai != null) {
            ai.tick();
        }

        if (useBrainAI) {
            // Utility AI path — scores all actions, picks highest
            BotBrain brain = bot.getBrain();
            if (brain != null) {
                BrainContext ctx = buildBrainContext(bot, match, store);
                ScoredAction action = brain.evaluate(ctx);
                applyBrainDecision(bot, action, ctx, store);
            }
        } else {
            // Legacy path
            updateBotTarget(bot, match, store);
            checkReactiveBlock(bot, match, store);
        }

        applyBotCombatDamage(bot, match);
    }

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

                double distance = currentPos.distanceTo(spawnVec);
                if (distance > 0.5) {
                    transform.setPosition(spawnVec);
                    bot.setCurrentPosition(spawn.copy());
                }
            }

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

    // ========== Brain AI Methods ==========

    /**
     * Builds a BrainContext with full enemy list for utility scoring.
     */
    private BrainContext buildBrainContext(BotParticipant bot, Match match, Store<EntityStore> store) {
        Position botPos = bot.getCurrentPosition();
        ArenaConfig config = match.getArena().getConfig();
        BotObjective objective = match.getGameMode().getBotObjective(config);

        boolean botInZone = false;
        if (objective != null && botPos != null) {
            botInZone = objective.isInsideZone(botPos);
        }

        boolean botAttacking = isActuallyAttacking(bot);

        // Build full enemy list
        List<EnemyInfo> enemies = new ArrayList<>();
        BotBrain brain = bot.getBrain();
        Map<UUID, ThreatEntry> threatMap = (brain != null) ? brain.getThreats() : Map.of();

        for (Participant participant : match.getParticipants()) {
            if (participant.getUniqueId().equals(bot.getUniqueId())) continue;
            if (!participant.isAlive()) continue;

            Position targetPos = null;
            Ref<EntityStore> targetRef = null;
            double healthPercent = 1.0;

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
                                if (healthStat != null && healthStat.getMax() > 0) {
                                    healthPercent = healthStat.get() / healthStat.getMax();
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
                    healthPercent = otherBot.getHealthPercentage();
                }
            }

            if (targetPos == null || botPos == null) continue;

            double distance = botPos.distanceTo(targetPos);

            // Detect attack state via EntityInteractionHelper
            boolean isAttacking = false;
            boolean isRangedAttacking = false;
            if (targetRef != null && targetRef.isValid()) {
                try {
                    Store<EntityStore> targetStore = targetRef.getStore();
                    if (targetStore != null) {
                        String interaction = EntityInteractionHelper.getPrimaryInteraction(targetRef, targetStore);
                        EntityInteractionHelper.InteractionKind kind = EntityInteractionHelper.classifyInteraction(interaction);
                        if (kind == EntityInteractionHelper.InteractionKind.RANGED_ATTACK) {
                            isAttacking = true;
                            isRangedAttacking = true;
                        } else if (kind == EntityInteractionHelper.InteractionKind.ATTACK) {
                            isAttacking = true;
                        }
                    }
                } catch (Exception e) {
                    // Ignore
                }
            }

            // Zone check
            boolean isInZone = false;
            if (objective != null) {
                isInZone = objective.isInsideZone(targetPos);
            }

            // Threat data from brain's threat map
            boolean isThreat = false;
            long lastHitTick = 0;
            ThreatType threatType = ThreatType.NONE;
            double threatDamage = 0;
            ThreatEntry threat = threatMap.get(participant.getUniqueId());
            if (threat != null) {
                isThreat = true;
                lastHitTick = threat.getLastHitTick();
                threatType = threat.getLastHitType();
                threatDamage = threat.getTotalDamage();
            }

            enemies.add(new EnemyInfo(participant, targetRef, targetPos, distance, healthPercent,
                isAttacking, isRangedAttacking, isInZone, isThreat, lastHitTick, threatType, threatDamage));
        }

        double botHealthPercent = bot.getHealthPercentage();
        double botBlockEnergy = (brain != null) ? brain.getBlockEnergy() : 0;

        return new BrainContext(bot, match, store, botPos, objective, botInZone, botAttacking,
            config.getBounds(), enemies, botHealthPercent, botBlockEnergy);
    }

    /**
     * Translates a ScoredAction into NPC role state and target changes.
     */
    private void applyBrainDecision(BotParticipant bot, ScoredAction action, BrainContext ctx, Store<EntityStore> store) {
        NPCEntity npcEntity = bot.getNpcEntity();
        if (npcEntity == null) return;

        Role role = npcEntity.getRole();
        if (role == null) return;

        UUID botId = bot.getUniqueId();
        String prevState = botNpcState.get(botId);

        // After taking damage, force re-apply
        if (bot.consumeStateRefresh()) {
            prevState = null;
        }

        switch (action.decision()) {
            case BLOCK -> {
                // Set nearest attacking enemy as target so bot faces them
                EnemyInfo target = action.target();
                if (target == null) target = ctx.nearestAttackingEnemy;
                if (target == null) target = ctx.nearestEnemy;
                if (target != null) {
                    applyEnemyTarget(bot, role, target, store);
                }
                if (!"Block".equals(prevState)) {
                    role.getStateSupport().setState(bot.getEntityRef(), "Block", "Default", store);
                    botNpcState.put(botId, "Block");
                }
            }

            case COMBAT -> {
                EnemyInfo target = action.target();
                if (target != null) {
                    applyEnemyTarget(bot, role, target, store);
                    if (!"Combat".equals(prevState)) {
                        role.getStateSupport().setState(bot.getEntityRef(), "Combat", "Default", store);
                        botNpcState.put(botId, "Combat");
                    }
                }
            }

            case DEFEND_ZONE -> {
                EnemyInfo target = action.target();
                if (target == null) target = ctx.nearestEnemy;
                if (target != null) {
                    applyEnemyTarget(bot, role, target, store);
                    String targetState = target.distance <= DEFEND_RANGE ? "Defend" : "Watchout";
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
                    Ref<EntityStore> marker = getOrCreateObjectiveMarker(botId, botTarget, store);
                    if (marker != null && marker.isValid()) {
                        MarkedEntitySupport markedSupport = role.getMarkedEntitySupport();
                        if (markedSupport != null) {
                            markedSupport.setMarkedEntity(MarkedEntitySupport.DEFAULT_TARGET_SLOT, marker);
                        }
                    }
                }
            }

            case STRAFE_EVADE -> {
                // Zig-zag toward goal (objective or nearest enemy) with lateral offset
                Position goal = null;
                if (ctx.objective != null) {
                    goal = getBotZoneTarget(botId, ctx.objective);
                } else if (ctx.nearestEnemy != null) {
                    goal = ctx.nearestEnemy.position;
                }

                if (goal != null && ctx.botPos != null) {
                    Position strafeTarget = computeStrafeTarget(botId, ctx.botPos, goal);
                    if (!"Follow".equals(prevState)) {
                        BotAI ai = bot.getAI();
                        if (ai != null) ai.clearTarget();
                        Ref<EntityStore> marker = getOrCreateObjectiveMarker(botId, strafeTarget, store);
                        if (marker != null && marker.isValid()) {
                            setNpcObjectiveTarget(role, marker, bot.getEntityRef(), store);
                            botNpcState.put(botId, "Follow");
                        }
                    } else {
                        Ref<EntityStore> marker = getOrCreateObjectiveMarker(botId, strafeTarget, store);
                        if (marker != null && marker.isValid()) {
                            MarkedEntitySupport markedSupport = role.getMarkedEntitySupport();
                            if (markedSupport != null) {
                                markedSupport.setMarkedEntity(MarkedEntitySupport.DEFAULT_TARGET_SLOT, marker);
                            }
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
     * Computes a strafe target: goal position + perpendicular lateral offset.
     * Alternates left/right every 8-12 ticks for zig-zag movement.
     */
    private Position computeStrafeTarget(UUID botId, Position botPos, Position goal) {
        int[] state = strafeState.computeIfAbsent(botId, k -> new int[]{0, 1});
        state[0]++;

        // Alternate direction every 8-12 ticks (randomized per switch)
        int switchInterval = 8 + strafeRandom.nextInt(5);
        if (state[0] >= switchInterval) {
            state[0] = 0;
            state[1] = -state[1]; // flip direction
        }

        // Direction vector from bot to goal
        double dx = goal.getX() - botPos.getX();
        double dz = goal.getZ() - botPos.getZ();
        double len = Math.sqrt(dx * dx + dz * dz);
        if (len < 0.01) return goal;

        // Normalize
        double ndx = dx / len;
        double ndz = dz / len;

        // Perpendicular (rotate 90 degrees)
        double perpX = -ndz;
        double perpZ = ndx;

        // Lateral offset: 3-5 blocks
        double lateralDist = 3.0 + strafeRandom.nextDouble() * 2.0;
        double offsetX = perpX * lateralDist * state[1];
        double offsetZ = perpZ * lateralDist * state[1];

        // Target: midpoint toward goal + lateral offset
        double advanceDist = Math.min(len, 5.0); // Advance at most 5 blocks toward goal
        return new Position(
            botPos.getX() + ndx * advanceDist + offsetX,
            goal.getY(),
            botPos.getZ() + ndz * advanceDist + offsetZ
        );
    }

    /**
     * Applies an enemy as the bot's NPC target using EnemyInfo.
     */
    private void applyEnemyTarget(BotParticipant bot, Role role, EnemyInfo enemy, Store<EntityStore> store) {
        BotAI ai = bot.getAI();
        if (ai != null && enemy != null) {
            ai.setTarget(enemy.participant.getUniqueId(), enemy.position);
        }

        if (enemy != null && enemy.entityRef != null && enemy.entityRef.isValid()) {
            try {
                MarkedEntitySupport markedSupport = role.getMarkedEntitySupport();
                if (markedSupport != null) {
                    markedSupport.setMarkedEntity(MarkedEntitySupport.DEFAULT_TARGET_SLOT, enemy.entityRef);
                }

                WorldSupport worldSupport = role.getWorldSupport();
                if (worldSupport != null) {
                    worldSupport.overrideAttitude(enemy.entityRef, Attitude.HOSTILE, 60.0);
                }
            } catch (Exception e) {
                // Ignore targeting errors
            }
        }
    }

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

    private Position getBotZoneTarget(UUID botId, BotObjective objective) {
        double[] offset = botZoneOffsets.get(botId);
        if (offset == null) {
            double halfX = (objective.maxX() - objective.minX()) / 2.0;
            double halfZ = (objective.maxZ() - objective.minZ()) / 2.0;
            double offX = (objectiveRandom.nextDouble() * 2 - 1) * halfX * 0.2;
            double offZ = (objectiveRandom.nextDouble() * 2 - 1) * halfZ * 0.2;
            offset = new double[]{offX, offZ};
            botZoneOffsets.put(botId, offset);
        }

        double margin = 0.5;
        double tx = Math.max(objective.minX() + margin,
                   Math.min(objective.maxX() - margin, objective.position().getX() + offset[0]));
        double tz = Math.max(objective.minZ() + margin,
                   Math.min(objective.maxZ() - margin, objective.position().getZ() + offset[1]));
        // Use vertical midpoint of zone box so markers aren't at floor level
        double ty = (objective.minY() + objective.maxY()) / 2.0;
        return new Position(tx, ty, tz);
    }

    private Ref<EntityStore> getOrCreateObjectiveMarker(UUID botId, Position target, Store<EntityStore> store) {
        Ref<EntityStore> existing = objectiveMarkers.get(botId);

        if (existing != null && existing.isValid()) {
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

    // ========== Combat Damage ==========

    private boolean isActuallyAttacking(BotParticipant bot) {
        Ref<EntityStore> ref = bot.getEntityRef();
        if (ref == null || !ref.isValid()) return false;
        Store<EntityStore> store = ref.getStore();
        if (store == null) return false;
        String interaction = EntityInteractionHelper.getPrimaryInteraction(ref, store);
        return EntityInteractionHelper.classifyInteraction(interaction) == EntityInteractionHelper.InteractionKind.ATTACK;
    }

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
     * Classifies the attack type and passes ThreatType to registerThreat.
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

        if (!attacking || wasAttackingPrev) return;

        Ref<EntityStore> targetRef = markedSupport.getMarkedEntityRef(MarkedEntitySupport.DEFAULT_TARGET_SLOT);
        if (targetRef == null || !targetRef.isValid()) return;

        Store<EntityStore> store = targetRef.getStore();
        if (store == null) return;

        UUIDComponent targetUuidComp = store.getComponent(targetRef, UUIDComponent.getComponentType());
        if (targetUuidComp == null) return;

        BotParticipant victimBot = getBotByEntityUuid(targetUuidComp.getUuid());
        if (victimBot == null || !victimBot.isAlive() || victimBot.isImmune()) return;

        // Block only negates damage from the front — attacks from behind go through
        if (isBlocking(victimBot)) {
            Position victimPos = victimBot.getCurrentPosition();
            Position attackerPos = attacker.getCurrentPosition();
            if (victimPos != null && attackerPos != null && BotBrain.isInFront(victimPos, attackerPos)) {
                return;
            }
        }

        double damage = attacker.getDifficulty().getBaseDamage();

        // Classify attacker's interaction type for threat registration
        ThreatType attackType = ThreatType.MELEE;
        try {
            Ref<EntityStore> attackerRef = attacker.getEntityRef();
            if (attackerRef != null && attackerRef.isValid()) {
                Store<EntityStore> attackerStore = attackerRef.getStore();
                if (attackerStore != null) {
                    String interaction = EntityInteractionHelper.getPrimaryInteraction(attackerRef, attackerStore);
                    EntityInteractionHelper.InteractionKind kind = EntityInteractionHelper.classifyInteraction(interaction);
                    if (kind == EntityInteractionHelper.InteractionKind.RANGED_ATTACK) {
                        attackType = ThreatType.RANGED;
                    }
                }
            }
        } catch (Exception e) {
            // Default to MELEE
        }

        boolean died = victimBot.takeDamage(damage);
        attacker.addDamageDealt(damage);

        BotBrain victimBrain = victimBot.getBrain();
        if (victimBrain != null) {
            victimBrain.registerThreat(attacker.getUniqueId(), attackType, damage);
        }

        applyDamageToNpcEntity(victimBot, damage);

        if (died) {
            match.recordKill(victimBot.getUniqueId(), attacker.getUniqueId());
        } else {
            match.recordDamage(victimBot.getUniqueId(), attacker.getUniqueId(), damage);
        }
    }

    /**
     * Handles damage dealt by a bot (callback from BotAI).
     * No-op: both damage paths are handled elsewhere.
     */
    private void handleBotDamage(BotParticipant attacker, UUID victimId, double damage) {
        // Bot-to-player: NPC native combat → KillDetectionSystem
        // Bot-on-bot: applyBotCombatDamage() in tickBot()
    }

    // ========== Legacy Path Methods (useBrainAI = false) ==========

    private void updateBotTarget(BotParticipant bot, Match match, Store<EntityStore> store) {
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
            NearestTarget nearest = findNearestTarget(bot, match, store);
            applyLegacyEnemyTarget(bot, role, nearest, store);
            if (nearest != null) {
                botNpcState.putIfAbsent(bot.getUniqueId(), "Combat");
            }
            return;
        }

        List<UUID> enemiesInZone = objective.participantsInZone().stream()
            .filter(id -> !id.equals(bot.getUniqueId()))
            .toList();
        boolean botInZone = objective.isInsideZone(botPos);

        UUID botId = bot.getUniqueId();
        String prevState = botNpcState.get(botId);

        if (botInZone) {
            followStuckTicks.remove(botId);

            if (!enemiesInZone.isEmpty()) {
                NearestTarget nearest = findNearestTargetFromSet(bot, match, store, enemiesInZone);
                if (nearest != null) {
                    applyLegacyEnemyTarget(bot, role, nearest, store);
                    if (!"Combat".equals(prevState)) {
                        role.getStateSupport().setState(bot.getEntityRef(), "Combat", "Default", store);
                        botNpcState.put(botId, "Combat");
                    }
                }
            } else {
                NearestTarget nearbyEnemy = findNearestTarget(bot, match, store);
                boolean enemyInDefendRange = nearbyEnemy != null && nearbyEnemy.distance <= DEFEND_RANGE;

                if (enemyInDefendRange) {
                    applyLegacyEnemyTarget(bot, role, nearbyEnemy, store);
                    if (!"Defend".equals(prevState)) {
                        role.getStateSupport().setState(bot.getEntityRef(), "Defend", "Default", store);
                        botNpcState.put(botId, "Defend");
                    }
                } else if (nearbyEnemy != null && nearbyEnemy.distance <= WATCHOUT_RANGE) {
                    applyLegacyEnemyTarget(bot, role, nearbyEnemy, store);
                    if (!"Watchout".equals(prevState)) {
                        role.getStateSupport().setState(bot.getEntityRef(), "Watchout", "Default", store);
                        botNpcState.put(botId, "Watchout");
                    }
                } else {
                    if (!"Idle".equals(prevState)) {
                        BotAI ai = bot.getAI();
                        if (ai != null) ai.clearTarget();
                        clearNpcTarget(role, bot.getEntityRef(), store);
                        botNpcState.put(botId, "Idle");
                    }
                }
            }
        } else {
            double distToZoneCenter = botPos.distanceTo(objective.position());

            if (distToZoneCenter <= ZONE_NEAR_DISTANCE) {
                int stuck = followStuckTicks.merge(botId, 1, Integer::sum);

                if (stuck >= FOLLOW_STUCK_THRESHOLD) {
                    NearestTarget nearest = findNearestTarget(bot, match, store);
                    if (nearest != null) {
                        applyLegacyEnemyTarget(bot, role, nearest, store);
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

    private void checkReactiveBlock(BotParticipant bot, Match match, Store<EntityStore> store) {
        UUID botId = bot.getUniqueId();

        NPCEntity npcEntity = bot.getNpcEntity();
        if (npcEntity == null) return;
        Role role = npcEntity.getRole();
        if (role == null) return;

        boolean enemyAttacking = isEnemyAttacking(role, store);
        boolean wasEnemyAtk = enemyWasAttacking.getOrDefault(botId, false);
        enemyWasAttacking.put(botId, enemyAttacking);

        if ("Block".equals(botNpcState.get(botId))) {
            int ticks = blockActiveTicks.merge(botId, 1, Integer::sum);

            if (!enemyAttacking || ticks >= MAX_BLOCK_TICKS) {
                String restore = preBlockState.remove(botId);
                if (restore == null) restore = "Combat";
                role.getStateSupport().setState(bot.getEntityRef(), restore, "Default", store);
                botNpcState.put(botId, restore);
                blockCooldownTicks.put(botId, BLOCK_COOLDOWN_TICKS);
                blockActiveTicks.remove(botId);
            }
            return;
        }

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

        if (!enemyAttacking || wasEnemyAtk) return;

        String currentState = botNpcState.get(botId);
        if (currentState == null || (!currentState.equals("Combat") && !currentState.equals("Defend") && !currentState.equals("Watchout"))) {
            return;
        }

        if (isActuallyAttacking(bot)) return;

        if (blockRandom.nextDouble() >= bot.getDifficulty().getBlockProbability()) return;

        preBlockState.put(botId, currentState);
        blockActiveTicks.put(botId, 0);
        botNpcState.put(botId, "Block");
        role.getStateSupport().setState(bot.getEntityRef(), "Block", "Default", store);
    }

    private boolean isEnemyAttacking(Role role, Store<EntityStore> store) {
        MarkedEntitySupport markedSupport = role.getMarkedEntitySupport();
        if (markedSupport == null) return false;

        Ref<EntityStore> targetRef = markedSupport.getMarkedEntityRef(MarkedEntitySupport.DEFAULT_TARGET_SLOT);
        if (targetRef == null || !targetRef.isValid()) return false;

        try {
            Store<EntityStore> targetStore = targetRef.getStore();
            if (targetStore == null) return false;
            String interaction = EntityInteractionHelper.getPrimaryInteraction(targetRef, targetStore);
            EntityInteractionHelper.InteractionKind kind = EntityInteractionHelper.classifyInteraction(interaction);
            return kind == EntityInteractionHelper.InteractionKind.ATTACK || kind == EntityInteractionHelper.InteractionKind.RANGED_ATTACK;
        } catch (Exception e) {
            return false;
        }
    }

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

    private void applyLegacyEnemyTarget(BotParticipant bot, Role role, NearestTarget nearest, Store<EntityStore> store) {
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

    // ========== Public Accessors ==========

    public BotParticipant getBot(UUID botId) {
        return activeBots.get(botId);
    }

    public BotParticipant getBotByEntityUuid(UUID entityUuid) {
        return entityUuidToBotMap.get(entityUuid);
    }

    public Match getBotMatch(UUID botId) {
        return botMatches.get(botId);
    }

    public boolean isBot(UUID participantId) {
        return activeBots.containsKey(participantId);
    }

    public Collection<BotParticipant> getAllBots() {
        return Collections.unmodifiableCollection(activeBots.values());
    }

    public int getActiveBotCount() {
        return activeBots.size();
    }

    private String generateBotName() {
        String baseName = BOT_NAMES.get(nameCounter % BOT_NAMES.size());
        int suffix = nameCounter / BOT_NAMES.size();
        nameCounter++;
        return suffix > 0 ? baseName + " " + (suffix + 1) : baseName;
    }

    /**
     * Applies proportional damage to a bot's NPC entity (for health bar visuals).
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
