package de.ragesith.hyarena2;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import de.ragesith.hyarena2.arena.KillDetectionSystem;
import de.ragesith.hyarena2.arena.MatchManager;
import de.ragesith.hyarena2.boundary.BoundaryManager;
import de.ragesith.hyarena2.command.ArenaCommand;
import de.ragesith.hyarena2.command.TestMatchArenasCommand;
import de.ragesith.hyarena2.command.TestMatchCreateCommand;
import de.ragesith.hyarena2.command.TestMatchJoinCommand;
import de.ragesith.hyarena2.command.TestMatchListCommand;
import de.ragesith.hyarena2.command.TestMatchLeaveCommand;
import de.ragesith.hyarena2.command.TestMatchCancelCommand;
import de.ragesith.hyarena2.command.TestMatchStartCommand;
import de.ragesith.hyarena2.config.ConfigManager;
import de.ragesith.hyarena2.config.GlobalConfig;
import de.ragesith.hyarena2.config.HubConfig;
import de.ragesith.hyarena2.event.EventBus;
import de.ragesith.hyarena2.event.PlayerJoinedHubEvent;
import de.ragesith.hyarena2.generated.BuildInfo;
import de.ragesith.hyarena2.hub.HubManager;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

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

        // Initialize kill detection system
        this.killDetectionSystem = new KillDetectionSystem(matchManager);
        this.getEntityStoreRegistry().registerSystem(killDetectionSystem);

        // Register commands
        this.getCommandRegistry().registerCommand(new ArenaCommand(this));

        // Test match commands (Phase 2 testing)
        this.getCommandRegistry().registerCommand(new TestMatchArenasCommand(matchManager));
        this.getCommandRegistry().registerCommand(new TestMatchCreateCommand(matchManager));
        this.getCommandRegistry().registerCommand(new TestMatchJoinCommand(matchManager));
        this.getCommandRegistry().registerCommand(new TestMatchListCommand(matchManager));
        this.getCommandRegistry().registerCommand(new TestMatchLeaveCommand(matchManager));
        this.getCommandRegistry().registerCommand(new TestMatchCancelCommand(matchManager));
        this.getCommandRegistry().registerCommand(new TestMatchStartCommand(matchManager));

        // Register Hytale events
        this.getEventRegistry().registerGlobal(PlayerReadyEvent.class, this::onPlayerReady);
        this.getEventRegistry().registerGlobal(PlayerDisconnectEvent.class, this::onPlayerDisconnect);

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
     * Starts scheduled tasks for boundary checking etc.
     */
    private void startScheduledTasks() {
        scheduler = Executors.newScheduledThreadPool(1);

        // Boundary check task - runs on scheduler, executes on world thread
        int checkIntervalMs = configManager.getGlobalConfig().getBoundaryCheckIntervalMs();
        scheduler.scheduleAtFixedRate(() -> {
            if (world != null) {
                world.execute(() -> {
                    try {
                        boundaryManager.tick();
                    } catch (Exception e) {
                        System.err.println("[HyArena2] Error in boundary tick: " + e.getMessage());
                    }
                });
            }
        }, checkIntervalMs, checkIntervalMs, TimeUnit.MILLISECONDS);

        System.out.println("[HyArena2] Scheduled tasks started (boundary check every " + checkIntervalMs + "ms)");
    }

    /**
     * Cleans up resources on shutdown.
     */
    private void cleanup() {
        System.out.println("[HyArena2] Shutting down...");

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

    // ========== Event Handlers ==========

    /**
     * Called when a player is ready (joins or changes worlds).
     */
    private void onPlayerReady(PlayerReadyEvent event) {
        Player player = event.getPlayer();
        Ref<EntityStore> entityRef = event.getPlayerRef();
        Store<EntityStore> store = entityRef.getStore();

        PlayerRef playerRef = store.getComponent(entityRef, PlayerRef.getComponentType());
        UUID playerId = playerRef.getUuid();
        String playerName = player.getDisplayName();

        // Capture world reference on first player join
        if (this.world == null) {
            this.world = player.getWorld();
        }

        // Check if this is a world change vs fresh join
        boolean isWorldChange = knownPlayers.containsKey(playerId);

        if (isWorldChange) {
            // World change - just update boundary tracking
            boundaryManager.registerPlayer(playerId, player);
            System.out.println("[HyArena2] Player " + playerName + " changed worlds to " + player.getWorld().getName());
            return;
        }

        // Fresh join - do full initialization
        System.out.println("[HyArena2] Player " + playerName + " joined the server");

        // Track the player
        knownPlayers.put(playerId, playerName);

        // Register for boundary checking
        boundaryManager.registerPlayer(playerId, player);

        // Teleport to hub
        hubManager.teleportToHub(player, () -> {
            // Send welcome message after teleport
            player.sendMessage(Message.raw("Welcome to HyArena2!"));
            player.sendMessage(Message.raw("Use /arena to open the menu."));
        });
        boundaryManager.grantTeleportGrace(playerId);

        // Publish event
        eventBus.publish(new PlayerJoinedHubEvent(playerId, playerName, true));
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

        // Remove from match if in one
        matchManager.removePlayerFromMatch(playerId, "Disconnected");

        // Unregister from boundary checking
        boundaryManager.unregisterPlayer(playerId);

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
}
