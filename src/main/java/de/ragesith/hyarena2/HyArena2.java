package de.ragesith.hyarena2;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.player.PlayerChatEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import de.ragesith.hyarena2.arena.Arena;
import de.ragesith.hyarena2.arena.ArenaConfig;
import de.ragesith.hyarena2.arena.KillDetectionSystem;
import de.ragesith.hyarena2.arena.MatchManager;
import de.ragesith.hyarena2.economy.EconomyConfig;
import de.ragesith.hyarena2.economy.EconomyManager;
import de.ragesith.hyarena2.economy.HonorManager;
import de.ragesith.hyarena2.economy.PlayerDataManager;
import de.ragesith.hyarena2.boundary.BoundaryManager;
import de.ragesith.hyarena2.command.AdminCommand;
import de.ragesith.hyarena2.command.AdminPlayCommand;
import de.ragesith.hyarena2.command.ArenaCommand;
import de.ragesith.hyarena2.command.DebugCommand;
import de.ragesith.hyarena2.command.BuildCommand;
import de.ragesith.hyarena2.command.BugCommand;
import de.ragesith.hyarena2.command.LinkCommand;
import de.ragesith.hyarena2.command.WelcomeCommand;
import de.ragesith.hyarena2.debug.DebugViewManager;
import de.ragesith.hyarena2.command.testing.TestMatchArenasCommand;
import de.ragesith.hyarena2.command.testing.TestMatchCreateCommand;
import de.ragesith.hyarena2.command.testing.TestMatchJoinCommand;
import de.ragesith.hyarena2.command.testing.TestMatchListCommand;
import de.ragesith.hyarena2.command.testing.TestMatchLeaveCommand;
import de.ragesith.hyarena2.command.testing.TestMatchCancelCommand;
import de.ragesith.hyarena2.command.testing.TestMatchStartCommand;
import de.ragesith.hyarena2.command.testing.TestQueueJoinCommand;
import de.ragesith.hyarena2.command.testing.TestQueueJoinKitCommand;
import de.ragesith.hyarena2.command.testing.TestQueueLeaveCommand;
import de.ragesith.hyarena2.command.testing.TestKitListCommand;
import de.ragesith.hyarena2.command.testing.TestKitApplyCommand;
import de.ragesith.hyarena2.command.testing.TestKitClearCommand;
import de.ragesith.hyarena2.command.testing.TestBotSpawnCommand;
import de.ragesith.hyarena2.command.testing.TestBotListCommand;
import de.ragesith.hyarena2.command.testing.TestBotRemoveCommand;
import de.ragesith.hyarena2.command.testing.TestBotAIToggleCommand;
import de.ragesith.hyarena2.command.testing.TestEconomyInfoCommand;
import de.ragesith.hyarena2.command.testing.TestGiveAPCommand;
import de.ragesith.hyarena2.command.testing.TestGiveHonorCommand;
import de.ragesith.hyarena2.command.testing.TestResetEconomyCommand;
import de.ragesith.hyarena2.command.testing.TestShopListCommand;
import de.ragesith.hyarena2.command.testing.TestShopBuyCommand;
import de.ragesith.hyarena2.command.testing.TestHyMLCommand;
import de.ragesith.hyarena2.config.ConfigManager;
import de.ragesith.hyarena2.config.GlobalConfig;
import de.ragesith.hyarena2.config.HubConfig;
import de.ragesith.hyarena2.event.EventBus;
import de.ragesith.hyarena2.event.PlayerJoinedHubEvent;
import de.ragesith.hyarena2.event.queue.QueueMatchFoundEvent;
import de.ragesith.hyarena2.generated.BuildInfo;
import de.ragesith.hyarena2.hub.HubManager;
import de.ragesith.hyarena2.interaction.OpenLeaderboardInteraction;
import de.ragesith.hyarena2.interaction.OpenMatchmakingInteraction;
import de.ragesith.hyarena2.interaction.OpenShopInteraction;
import de.ragesith.hyarena2.kit.KitManager;
import de.ragesith.hyarena2.queue.Matchmaker;
import de.ragesith.hyarena2.queue.QueueManager;
import de.ragesith.hyarena2.shop.ShopConfig;
import de.ragesith.hyarena2.shop.ShopManager;
import de.ragesith.hyarena2.stats.StatsConfig;
import de.ragesith.hyarena2.stats.StatsManager;
import de.ragesith.hyarena2.api.ApiClient;
import de.ragesith.hyarena2.ui.hud.HudManager;
import de.ragesith.hyarena2.utils.ArenaCleanupUtil;
import de.ragesith.hyarena2.utils.PlayerMovementControl;
import de.ragesith.hyarena2.bot.BotManager;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * HyArena2 - PvP Arena Plugin
 * Main plugin entry point.
 */
public class HyArena2 extends JavaPlugin {

    private static HyArena2 instance;
    private ConfigManager configManager;
    private EventBus eventBus;
    private HubManager hubManager;
    private BoundaryManager boundaryManager;
    private MatchManager matchManager;
    private KillDetectionSystem killDetectionSystem;
    private KitManager kitManager;
    private BotManager botManager;
    private QueueManager queueManager;
    private Matchmaker matchmaker;
    private HudManager hudManager;
    private PlayerDataManager playerDataManager;
    private EconomyManager economyManager;
    private HonorManager honorManager;
    private ShopManager shopManager;
    private StatsManager statsManager;
    private ApiClient apiClient;
    private de.ragesith.hyarena2.chat.ChatManager chatManager;
    private DebugViewManager debugViewManager;

    // Track known players (to detect world changes vs fresh joins)
    private final Map<UUID, String> knownPlayers = new ConcurrentHashMap<>();

    // World reference for thread operations
    private volatile World world;

    // Scheduler for periodic tasks
    private ScheduledExecutorService scheduler;

    public HyArena2(@NonNullDecl JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        super.setup();
        instance = this;

        System.out.println("[HyArena2] ========================================");
        System.out.println("[HyArena2] Initializing HyArena2 plugin...");

        // Load configuration
        this.configManager = new ConfigManager(this.getDataDirectory()).load();

        // Initialize event bus
        this.eventBus = new EventBus();

        // Initialize hub manager
        this.hubManager = new HubManager(configManager.getHubConfig());

        // Initialize boundary manager
        this.boundaryManager = new BoundaryManager(
            configManager.getHubConfig(),
            configManager.getGlobalConfig(),
            hubManager
        );

        // Initialize match manager
        this.matchManager = new MatchManager(configManager, eventBus, hubManager);
        this.matchManager.initialize();

        // Wire boundary manager to match manager for state-driven boundary checks
        this.boundaryManager.setMatchManager(matchManager);
        this.matchManager.setBoundaryManager(boundaryManager);

        // Initialize kit manager
        this.kitManager = new KitManager(configManager.getConfigRoot(), eventBus);
        this.kitManager.loadKits();
        this.kitManager.setMatchManager(matchManager);
        this.matchManager.setKitManager(kitManager);

        // Initialize bot manager
        this.botManager = new BotManager(eventBus);
        this.matchManager.setBotManager(botManager);
        System.out.println("[HyArena2] BotManager initialized");

        // Initialize SpeedRun PB manager
        de.ragesith.hyarena2.gamemode.SpeedRunPBManager pbManager =
            new de.ragesith.hyarena2.gamemode.SpeedRunPBManager(configManager.getConfigRoot());
        this.matchManager.setSpeedRunPBManager(pbManager);

        // Initialize kill detection system
        // Registered via EntityStoreRegistry which should be global
        // We also track worlds and log when they're first seen for debugging
        this.killDetectionSystem = new KillDetectionSystem(matchManager);
        this.killDetectionSystem.setBotManager(botManager);
        this.getEntityStoreRegistry().registerSystem(killDetectionSystem);

        System.out.println("[HyArena2] KillDetectionSystem registered with EntityStoreRegistry");

        // Register block protection systems
        de.ragesith.hyarena2.protection.BlockBreakProtectionSystem.setMatchManager(matchManager);
        this.getEntityStoreRegistry().registerSystem(new de.ragesith.hyarena2.protection.BlockBreakProtectionSystem());
        this.getEntityStoreRegistry().registerSystem(new de.ragesith.hyarena2.protection.BlockPlaceProtectionSystem());
        System.out.println("[HyArena2] Block protection systems registered");

        // Create scheduler early so it's available for queue system
        scheduler = Executors.newScheduledThreadPool(2);

        // Initialize queue system
        String hubWorldName = configManager.getHubConfig().getEffectiveWorldName();
        this.queueManager = new QueueManager(eventBus, hubManager, hubWorldName);
        this.queueManager.setMatchChecker(matchManager::isPlayerInMatch);
        this.queueManager.setHubWorldChecker(this::isPlayerInHubWorld);
        this.queueManager.setKitManager(kitManager);

        this.matchmaker = new Matchmaker(queueManager, matchManager, eventBus);
        this.matchmaker.setBotManager(botManager);

        this.hudManager = new HudManager(
            queueManager, matchmaker, matchManager,
            scheduler, this::getOnlinePlayerCount
        );
        this.matchManager.setHudManager(hudManager);
        this.matchmaker.setHudManager(hudManager);

        // Set up HyML directory for markup-based pages
        java.nio.file.Path hymlDir = configManager.getConfigRoot().resolve("hyml");
        try {
            java.nio.file.Files.createDirectories(hymlDir);
        } catch (java.io.IOException e) {
            System.err.println("[HyArena2] Failed to create hyml directory: " + e.getMessage());
        }
        this.hudManager.setHymlDir(hymlDir);

        // Subscribe to queue events for HUD management
        subscribeToQueueEvents();

        System.out.println("[HyArena2] Queue system initialized");

        // Initialize economy system
        EconomyConfig economyConfig = configManager.loadConfig("economy.json", EconomyConfig.class);
        if (economyConfig == null) {
            economyConfig = new EconomyConfig();
            configManager.saveConfig("economy.json", economyConfig);
            System.out.println("[HyArena2] Created default economy.json");
        }

        this.playerDataManager = new PlayerDataManager(configManager.getConfigRoot());
        this.economyManager = new EconomyManager(economyConfig, playerDataManager, eventBus);
        this.honorManager = new HonorManager(economyConfig, playerDataManager, eventBus);
        this.economyManager.setHonorManager(honorManager);
        this.economyManager.subscribeToEvents();
        this.matchManager.setEconomyManagerForModes(economyManager);

        // Initialize shop
        ShopConfig shopConfig = configManager.loadConfig("shop.json", ShopConfig.class);
        if (shopConfig == null) {
            shopConfig = new ShopConfig();
            configManager.saveConfig("shop.json", shopConfig);
            System.out.println("[HyArena2] Created default shop.json");
        }
        this.shopManager = new ShopManager(shopConfig, configManager, economyManager, playerDataManager, eventBus);

        // Initialize stats tracking & web API
        StatsConfig statsConfig = configManager.loadConfig("stats.json", StatsConfig.class);
        if (statsConfig == null) {
            statsConfig = new StatsConfig();
            configManager.saveConfig("stats.json", statsConfig);
            System.out.println("[HyArena2] Created default stats.json");
        }
        this.apiClient = new ApiClient(statsConfig.getBaseUrl(), statsConfig.getApiKey());
        this.statsManager = new StatsManager(statsConfig, apiClient, eventBus, matchManager, kitManager, economyManager, honorManager);
        this.statsManager.subscribeToEvents();
        this.statsManager.initSyncScheduler(scheduler);

        // Initialize chat formatting
        this.chatManager = new de.ragesith.hyarena2.chat.ChatManager(honorManager);

        System.out.println("[HyArena2] Economy, shop & stats system initialized");

        // Initialize debug view manager
        this.debugViewManager = new DebugViewManager(configManager, matchManager);
        System.out.println("[HyArena2] DebugViewManager initialized");

        // Register commands
        this.getCommandRegistry().registerCommand(new ArenaCommand(this));
        this.getCommandRegistry().registerCommand(new AdminCommand(this));
        this.getCommandRegistry().registerCommand(new LinkCommand(this));
        this.getCommandRegistry().registerCommand(new DebugCommand(debugViewManager));
        this.getCommandRegistry().registerCommand(new WelcomeCommand(this));
        this.getCommandRegistry().registerCommand(new AdminPlayCommand(scheduler));
        this.getCommandRegistry().registerCommand(new BugCommand(this));
        this.getCommandRegistry().registerCommand(new BuildCommand(hubManager));

        // Test match commands (Phase 2 testing)
        this.getCommandRegistry().registerCommand(new TestMatchArenasCommand(matchManager));
        this.getCommandRegistry().registerCommand(new TestMatchCreateCommand(matchManager));
        this.getCommandRegistry().registerCommand(new TestMatchJoinCommand(matchManager));
        this.getCommandRegistry().registerCommand(new TestMatchListCommand(matchManager));
        this.getCommandRegistry().registerCommand(new TestMatchLeaveCommand(matchManager));
        this.getCommandRegistry().registerCommand(new TestMatchCancelCommand(matchManager));
        this.getCommandRegistry().registerCommand(new TestMatchStartCommand(matchManager));

        // Test queue commands (Phase 3 testing)
        this.getCommandRegistry().registerCommand(new TestQueueJoinCommand(queueManager, matchManager));
        this.getCommandRegistry().registerCommand(new TestQueueJoinKitCommand(queueManager, matchManager, kitManager));
        this.getCommandRegistry().registerCommand(new TestQueueLeaveCommand(queueManager));

        // Test kit commands (Phase 4 testing)
        this.getCommandRegistry().registerCommand(new TestKitListCommand(kitManager));
        this.getCommandRegistry().registerCommand(new TestKitApplyCommand(kitManager));
        this.getCommandRegistry().registerCommand(new TestKitClearCommand(kitManager));

        // Test bot commands (Phase 5 testing)
        this.getCommandRegistry().registerCommand(new TestBotSpawnCommand(matchManager, botManager));
        this.getCommandRegistry().registerCommand(new TestBotListCommand(botManager));
        this.getCommandRegistry().registerCommand(new TestBotRemoveCommand(botManager));
        this.getCommandRegistry().registerCommand(new TestBotAIToggleCommand(botManager));

        // Test economy commands (Phase 7 testing)
        this.getCommandRegistry().registerCommand(new TestEconomyInfoCommand(economyManager, honorManager));
        this.getCommandRegistry().registerCommand(new TestGiveAPCommand(economyManager));
        this.getCommandRegistry().registerCommand(new TestGiveHonorCommand(economyManager, honorManager));
        this.getCommandRegistry().registerCommand(new TestResetEconomyCommand(economyManager, honorManager));
        this.getCommandRegistry().registerCommand(new TestShopListCommand(shopManager, economyManager));
        this.getCommandRegistry().registerCommand(new TestShopBuyCommand(shopManager, economyManager));

        // Test HyML command
        this.getCommandRegistry().registerCommand(new TestHyMLCommand(hudManager, economyManager, honorManager));

        // Register custom interactions for NPC statues
        OpenMatchmakingInteraction.setPluginInstance(this);
        OpenLeaderboardInteraction.setPluginInstance(this);
        OpenShopInteraction.setPluginInstance(this);
        this.getCodecRegistry(Interaction.CODEC)
            .register("Hyarena_Open_Matchmaking", OpenMatchmakingInteraction.class, OpenMatchmakingInteraction.CODEC);
        this.getCodecRegistry(Interaction.CODEC)
            .register("Hyarena_Open_Leaderboard", OpenLeaderboardInteraction.class, OpenLeaderboardInteraction.CODEC);
        this.getCodecRegistry(Interaction.CODEC)
                .register("Hyarena_Open_Shop", OpenShopInteraction.class, OpenShopInteraction.CODEC);

        System.out.println("[HyArena2] Custom interactions registered");

        // Register Hytale events
        this.getEventRegistry().registerGlobal(PlayerReadyEvent.class, this::onPlayerReady);
        this.getEventRegistry().registerGlobal(PlayerDisconnectEvent.class, this::onPlayerDisconnect);
        this.getEventRegistry().registerAsyncGlobal(PlayerChatEvent.class, future ->
            future.thenApply(event -> {
                chatManager.onChat(event);
                return event;
            })
        );

        // Start scheduled tasks
        startScheduledTasks();

        // Register shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(this::cleanup));

        // Log startup info
        System.out.println("[HyArena2] ----------------------------------------");
        System.out.println("[HyArena2] HyArena2 v" + BuildInfo.VERSION + " (Build #" + BuildInfo.BUILD_NUMBER + ")");
        System.out.println("[HyArena2] Commit: " + BuildInfo.GIT_COMMIT + " (" + BuildInfo.GIT_BRANCH + ")");
        System.out.println("[HyArena2] Built: " + BuildInfo.BUILD_TIME);
        System.out.println("[HyArena2] ----------------------------------------");
        System.out.println("[HyArena2] Hub world: " + configManager.getHubConfig().getEffectiveWorldName());
        System.out.println("[HyArena2] Debug logging: " + configManager.getGlobalConfig().isDebugLogging());
        System.out.println("[HyArena2] Plugin initialized successfully!");
        System.out.println("[HyArena2] ========================================");
    }

    /**
     * Starts scheduled tasks for boundary checking, matchmaking, etc.
     * Note: Scheduler is created early in setup() for HUD support.
     */
    private void startScheduledTasks() {
        // Boundary check task - hub + arena at same interval, dispatches to world threads internally
        int boundaryCheckIntervalMs = configManager.getGlobalConfig().getBoundaryCheckIntervalMs();
        scheduler.scheduleAtFixedRate(() -> {
            try {
                boundaryManager.tickHub();
                boundaryManager.tickArena();
            } catch (Exception e) {
                System.err.println("[HyArena2] Error in boundary tick: " + e.getMessage());
            }
        }, boundaryCheckIntervalMs, boundaryCheckIntervalMs, TimeUnit.MILLISECONDS);

        // Matchmaker task - runs every 1 second on hub world thread
        scheduler.scheduleAtFixedRate(() -> {
            World hubWorld = hubManager.getHubWorld();
            if (hubWorld != null) {
                hubWorld.execute(() -> {
                    try {
                        matchmaker.tick();
                    } catch (Exception e) {
                        System.err.println("[HyArena2] Error in matchmaker tick: " + e.getMessage());
                        e.printStackTrace();
                    }
                });
            }
        }, 1000, 1000, TimeUnit.MILLISECONDS);

        // Bot ticking is now driven by Match.tick() on the arena world thread — no separate scheduler needed.

        // Honor decay tick every 60 seconds
        if (honorManager != null) {
            scheduler.scheduleAtFixedRate(() -> {
                try {
                    honorManager.tickDecay();
                } catch (Exception e) {
                    System.err.println("[HyArena2] Error in honor decay tick: " + e.getMessage());
                }
            }, 60, 60, TimeUnit.SECONDS);
        }

        // Stats config sync on startup (delayed to let arenas/kits fully load)
        if (statsManager != null && statsManager.getConfig().isEnabled() && statsManager.getConfig().isSyncOnStartup()) {
            scheduler.schedule(() -> {
                try {
                    statsManager.syncConfigsToWeb();
                } catch (Exception e) {
                    System.err.println("[HyArena2] Error in stats config sync: " + e.getMessage());
                }
            }, 5, TimeUnit.SECONDS);
        }

        // Economy auto-save every 5 minutes
        if (economyManager != null) {
            scheduler.scheduleAtFixedRate(() -> {
                try {
                    economyManager.saveAll();
                } catch (Exception e) {
                    System.err.println("[HyArena2] Error in economy auto-save: " + e.getMessage());
                }
            }, 5, 5, TimeUnit.MINUTES);
        }

        // Debug view tick every 1.5 seconds
        if (debugViewManager != null) {
            scheduler.scheduleAtFixedRate(() -> {
                try {
                    debugViewManager.tick();
                } catch (Exception e) {
                    System.err.println("[HyArena2] Error in debug view tick: " + e.getMessage());
                }
            }, 1500, 1500, TimeUnit.MILLISECONDS);
        }

        System.out.println("[HyArena2] Scheduled tasks started (boundary check every " + boundaryCheckIntervalMs + "ms, matchmaker every 1s, honor decay every 60s, auto-save every 5min)");
    }

    /**
     * Cleans up resources on shutdown.
     */
    private void cleanup() {
        System.out.println("[HyArena2] Shutting down...");

        // Despawn hub holograms so they don't persist as orphans across restarts
        if (hubManager != null) {
            hubManager.despawnHubHolograms();
        }

        // Flush remaining dirty economy data to web API before saving locally
        if (statsManager != null) {
            statsManager.shutdown();
        }

        if (economyManager != null) {
            economyManager.saveAll();
        }

        if (hudManager != null) {
            hudManager.shutdown();
        }

        if (matchManager != null) {
            matchManager.shutdown();
        }

        if (scheduler != null) {
            scheduler.shutdown();
        }

        if (eventBus != null) {
            eventBus.shutdown();
        }

        System.out.println("[HyArena2] Cleanup complete.");
    }

    /**
     * Runs ArenaCleanupUtil on all arena worlds at startup to remove
     * stale NPCs/holograms left behind by crashes or forced reboots.
     */
    private void runStartupArenaCleanup() {
        String hubWorldName = configManager.getHubConfig().getEffectiveWorldName();
        Map<String, List<ArenaConfig.Bounds>> worldBounds = new HashMap<>();

        for (Arena arena : matchManager.getArenas()) {
            String worldName = arena.getConfig().getWorldName();
            if (worldName == null || worldName.equals(hubWorldName)) continue;
            ArenaConfig.Bounds bounds = arena.getConfig().getBounds();
            if (bounds == null) continue;
            worldBounds.computeIfAbsent(worldName, k -> new ArrayList<>()).add(bounds);
        }

        if (worldBounds.isEmpty()) return;

        int delay = 5000; // 5s delay to let worlds fully load entities
        for (Map.Entry<String, List<ArenaConfig.Bounds>> entry : worldBounds.entrySet()) {
            String worldName = entry.getKey();
            List<ArenaConfig.Bounds> boundsList = entry.getValue();

            scheduler.schedule(() -> {
                World w = com.hypixel.hytale.server.core.universe.Universe.get().getWorld(worldName);
                if (w == null) {
                    System.out.println("[HyArena2] Startup cleanup skipped for world '" + worldName + "' (not loaded)");
                    return;
                }
                w.execute(() -> {
                    for (ArenaConfig.Bounds bounds : boundsList) {
                        ArenaCleanupUtil.cleanupArena(w, bounds);
                    }
                });
            }, delay, java.util.concurrent.TimeUnit.MILLISECONDS);
            delay += 500;
        }

        System.out.println("[HyArena2] Scheduled startup cleanup for " + worldBounds.size() + " arena world(s)");
    }

    /**
     * Subscribes to queue events for HUD management.
     */
    private void subscribeToQueueEvents() {
        // Hide LobbyHud when match is found (players will be teleported to arena)
        // Note: Queue panel visibility is handled automatically by LobbyHud's refresh
        eventBus.subscribe(QueueMatchFoundEvent.class, event -> {
            World hubWorld = hubManager.getHubWorld();
            if (hubWorld != null) {
                hubWorld.execute(() -> {
                    for (UUID playerUuid : event.getPlayerUuids()) {
                        hudManager.hideLobbyHud(playerUuid);
                    }
                });
            }
        });
    }

    /**
     * Checks if a player is in the hub world.
     */
    private boolean isPlayerInHubWorld(UUID playerUuid) {
        String hubWorldName = configManager.getHubConfig().getEffectiveWorldName();
        String currentWorld = knownPlayers.get(playerUuid);
        // If we don't have tracking info, assume they're in hub (initial state)
        if (currentWorld == null) {
            return true;
        }
        // Actually need to check their current world - use boundary manager's player tracking
        // Since we don't store world name in knownPlayers (it stores player name), let's use a different approach
        // We'll check via the player's world directly
        return boundaryManager.isPlayerInWorld(playerUuid, hubWorldName);
    }

    /**
     * Gets the number of online players.
     */
    private int getOnlinePlayerCount() {
        return knownPlayers.size();
    }

    // ========== Event Handlers ==========

    /**
     * Called when a player is ready (joins or changes worlds).
     */
    private void onPlayerReady(PlayerReadyEvent event) {
        Player player = event.getPlayer();
        World playerWorld = player.getWorld();

        // PlayerReadyEvent can fire on the Scheduler thread — dispatch to the player's world thread
        playerWorld.execute(() -> {
            Ref<EntityStore> entityRef = event.getPlayerRef();
            Store<EntityStore> store = entityRef.getStore();

            PlayerRef playerRef = store.getComponent(entityRef, PlayerRef.getComponentType());
            UUID playerId = playerRef.getUuid();
            String playerName = player.getDisplayName();

        // Capture world reference on first player join
        if (this.world == null) {
            this.world = player.getWorld();
            // Hub world is now guaranteed loaded — spawn hub holograms
            hubManager.spawnHubHolograms();

            // Clean up stale entities from previous crashes in all arena worlds
            runStartupArenaCleanup();
        }

        // Check if this is a world change vs fresh join
        boolean isWorldChange = knownPlayers.containsKey(playerId);

        if (isWorldChange) {
            // World change - update boundary tracking
            boundaryManager.registerPlayer(playerId, player);
            System.out.println("[HyArena2] Player " + playerName + " changed worlds to " + player.getWorld().getName());

            // Refresh chat name color on world change (picks up permission changes)
            chatManager.cachePlayerNameColor(playerId, player);

            String hubWorldName = configManager.getHubConfig().getEffectiveWorldName();
            boolean enteringHub = player.getWorld().getName().equals(hubWorldName);

            if (enteringHub) {
                // Entering hub world - clear any arena effects (freeze, inventory, etc.)
                PlayerMovementControl.enableMovementForPlayer(playerRef, player.getWorld());
                kitManager.clearKit(player);
                System.out.println("[HyArena2] Cleared arena effects for " + playerName + " entering hub");

                // Show lobby HUD when entering hub
                hudManager.showLobbyHud(playerId);
            } else {
                // Leaving hub world (entering arena) - hide lobby HUD
                hudManager.hideLobbyHud(playerId);
            }
            return;
        }

        // Fresh join - do full initialization
        System.out.println("[HyArena2] Player " + playerName + " joined the server");

        // Track the player
        knownPlayers.put(playerId, playerName);

        // Register for boundary checking
        boundaryManager.registerPlayer(playerId, player);

        // Load economy data
        if (economyManager != null) {
            economyManager.loadPlayer(playerId, playerName);
        }
        if (honorManager != null) {
            honorManager.decayHonor(playerId);
            honorManager.updatePlayerRank(playerId);
        }

        // Cache name color for chat formatting (must be on world thread)
        chatManager.cachePlayerNameColor(playerId, player);

        // Check if player is already in hub world (same-world teleport won't trigger world change event)
        String hubWorldName = configManager.getHubConfig().getEffectiveWorldName();
        boolean alreadyInHub = player.getWorld().getName().equals(hubWorldName);

        // Skip hub teleport if player is in the build world (preserve their position)
        if (player.getWorld().getName().equals(BuildCommand.BUILD_WORLD_NAME)) {
            System.out.println("[HyArena2] Player " + playerName + " is in build world, skipping hub teleport");
            return;
        }

        // Clear any leftover inventory from previous session
        kitManager.clearKit(player);

        // Teleport to hub
        hubManager.teleportToHub(player, () -> {
            // Send welcome message after teleport
            player.sendMessage(Message.raw("Welcome to HyArena2!"));
            player.sendMessage(Message.raw("Use /arena to open the menu."));

            // If player was already in hub, show LobbyHud now (no world change event will fire)
            if (alreadyInHub) {
                hudManager.showLobbyHud(playerId);
            }
            // Otherwise, LobbyHud will be shown when world change event fires

            // Show welcome page (unless player opted out)
            if (!playerDataManager.getData(playerId).isHideWelcome()) {
                showWelcomePage(playerId, playerName);
            }
        });
        boundaryManager.grantTeleportGrace(playerId);

        // Publish event
        eventBus.publish(new PlayerJoinedHubEvent(playerId, playerName, true));
        }); // end world.execute()
    }

    /**
     * Shows the welcome HyML page with the "Don't show again" checkbox listener.
     */
    public void showWelcomePage(UUID playerId, String playerName) {
        Map<String, String> vars = new java.util.HashMap<>();
        vars.put("player_name", playerName);
        vars.put("ap", String.valueOf(economyManager.getArenaPoints(playerId)));
        vars.put("rank", honorManager.getRankDisplayName(playerId));
        boolean currentHideWelcome = playerDataManager.getData(playerId).isHideWelcome();
        hudManager.showHyMLPage(playerId, "welcome.hyml", vars, "hide_welcome", currentHideWelcome, (uuid, cbId, value) -> {
            playerDataManager.getData(uuid).setHideWelcome(value);
            playerDataManager.save(uuid);
        });
    }

    /**
     * Called when a player disconnects.
     */
    private void onPlayerDisconnect(PlayerDisconnectEvent event) {
        PlayerRef playerRef = event.getPlayerRef();
        UUID playerId = playerRef.getUuid();

        // Get player name before cleanup
        String playerName = knownPlayers.getOrDefault(playerId, "Unknown");
        System.out.println("[HyArena2] Player " + playerName + " disconnected");

        // Remove from queue if in one
        queueManager.handlePlayerDisconnect(playerId);

        // Clean up HUDs
        hudManager.handlePlayerDisconnect(playerId);

        // Remove from match if in one
        matchManager.removePlayerFromMatch(playerId, "Disconnected");

        // Flush economy to web API before saving/unloading
        if (statsManager != null) {
            statsManager.flushPlayerEconomy(playerId);
        }

        // Save and unload economy data
        if (economyManager != null) {
            economyManager.savePlayer(playerId);
            economyManager.unloadPlayer(playerId);
        }

        // Unregister from boundary checking
        boundaryManager.unregisterPlayer(playerId);

        // Clean up chat cache
        chatManager.removePlayer(playerId);

        // Clean up debug view state
        if (debugViewManager != null) {
            debugViewManager.handlePlayerDisconnect(playerId);
        }

        // Remove from tracking
        knownPlayers.remove(playerId);
    }

    /**
     * Sets the world reference for thread operations.
     */
    public void setWorld(World world) {
        if (this.world == null && world != null) {
            this.world = world;
        }
    }

    /**
     * Gets the world reference.
     */
    public World getWorld() {
        return world;
    }

    // ========== Getters ==========

    public static HyArena2 getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public GlobalConfig getGlobalConfig() {
        return configManager.getGlobalConfig();
    }

    public HubConfig getHubConfig() {
        return configManager.getHubConfig();
    }

    public EventBus getEventBus() {
        return eventBus;
    }

    public HubManager getHubManager() {
        return hubManager;
    }

    public BoundaryManager getBoundaryManager() {
        return boundaryManager;
    }

    public MatchManager getMatchManager() {
        return matchManager;
    }

    public KitManager getKitManager() {
        return kitManager;
    }

    public BotManager getBotManager() {
        return botManager;
    }

    public QueueManager getQueueManager() {
        return queueManager;
    }

    public Matchmaker getMatchmaker() {
        return matchmaker;
    }

    public HudManager getHudManager() {
        return hudManager;
    }

    public ScheduledExecutorService getScheduler() {
        return scheduler;
    }

    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }

    public EconomyManager getEconomyManager() {
        return economyManager;
    }

    public HonorManager getHonorManager() {
        return honorManager;
    }

    public ShopManager getShopManager() {
        return shopManager;
    }

    public ApiClient getApiClient() {
        return apiClient;
    }

    public StatsManager getStatsManager() {
        return statsManager;
    }

    public de.ragesith.hyarena2.chat.ChatManager getChatManager() {
        return chatManager;
    }

    /**
     * Triggers an asynchronous web sync of all arena, kit, and game mode configs.
     * Called after admin saves (arena editor, kit editor) so the website stays up-to-date.
     */
    public void triggerWebSync() {
        if (statsManager != null && statsManager.getConfig().isEnabled()) {
            scheduler.execute(() -> {
                try {
                    statsManager.syncConfigsToWeb();
                } catch (Exception e) {
                    System.err.println("[HyArena2] Web sync error: " + e.getMessage());
                }
            });
        }
    }
}
