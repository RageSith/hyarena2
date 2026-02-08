package de.ragesith.hyarena2.arena;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import de.ragesith.hyarena2.config.ConfigManager;
import de.ragesith.hyarena2.event.EventBus;
import de.ragesith.hyarena2.gamemode.DeathmatchGameMode;
import de.ragesith.hyarena2.gamemode.DuelGameMode;
import de.ragesith.hyarena2.gamemode.GameMode;
import de.ragesith.hyarena2.gamemode.KingOfTheHillGameMode;
import de.ragesith.hyarena2.gamemode.KitRouletteGameMode;
import de.ragesith.hyarena2.gamemode.LastManStandingGameMode;
import de.ragesith.hyarena2.hub.HubManager;
import de.ragesith.hyarena2.kit.KitManager;
import de.ragesith.hyarena2.bot.BotManager;
import de.ragesith.hyarena2.bot.BotParticipant;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages all active matches, arena loading, and match lifecycle.
 * Ticks all matches at 20 TPS (50ms intervals).
 */
public class MatchManager {
    
    private final ConfigManager configManager;
    private final EventBus eventBus;
    private final HubManager hubManager;
    private KitManager kitManager;
    private BotManager botManager;
    private de.ragesith.hyarena2.ui.hud.HudManager hudManager;

    private final Map<String, Arena> arenas;
    private final Map<UUID, Match> activeMatches;
    private final Map<UUID, UUID> playerToMatch; // Player UUID -> Match UUID
    private final Map<String, GameMode> gameModes;

    private static final long TICK_INTERVAL_MS = 50; // 20 TPS
    private Timer tickTimer;

    public MatchManager(ConfigManager configManager, EventBus eventBus, HubManager hubManager) {
        this.configManager = configManager;
        this.eventBus = eventBus;
        this.hubManager = hubManager;
        this.arenas = new ConcurrentHashMap<>();
        this.activeMatches = new ConcurrentHashMap<>();
        this.playerToMatch = new ConcurrentHashMap<>();
        this.gameModes = new ConcurrentHashMap<>();

        // Register game modes
        registerGameMode(new DuelGameMode());
        registerGameMode(new LastManStandingGameMode());
        registerGameMode(new DeathmatchGameMode());
        registerGameMode(new KingOfTheHillGameMode());
        registerGameMode(new KitRouletteGameMode());
    }

    /**
     * Sets the kit manager for kit application in matches.
     */
    public void setKitManager(KitManager kitManager) {
        this.kitManager = kitManager;
    }

    /**
     * Sets the bot manager for bot support in matches.
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
     * Sets the HUD manager for showing LobbyHud when players return to hub.
     */
    public void setHudManager(de.ragesith.hyarena2.ui.hud.HudManager hudManager) {
        this.hudManager = hudManager;
    }

    /**
     * Initializes the match manager: loads arenas and starts ticker.
     */
    public void initialize() {
        loadArenas();
        startTicker();
        System.out.println("MatchManager initialized with " + arenas.size() + " arenas");
    }

    /**
     * Shuts down the match manager: stops ticker and cleans up matches.
     */
    public void shutdown() {
        stopTicker();

        // Cancel all active matches
        for (Match match : activeMatches.values()) {
            match.cancel("Server shutdown");
        }

        activeMatches.clear();
        playerToMatch.clear();
        System.out.println("MatchManager shut down");
    }

    /**
     * Registers a game mode.
     */
    public void registerGameMode(GameMode gameMode) {
        gameModes.put(gameMode.getId(), gameMode);
        System.out.println("Registered game mode: " + gameMode.getDisplayName());
    }

    /**
     * Gets a game mode by ID.
     */
    public GameMode getGameMode(String id) {
        return gameModes.get(id);
    }

    /**
     * Gets all registered game modes.
     */
    public Collection<GameMode> getGameModes() {
        return gameModes.values();
    }

    /**
     * Gets the display name for an arena's game mode, with fallback to the raw ID.
     */
    public String getGameModeDisplayName(Arena arena) {
        GameMode gm = gameModes.get(arena.getGameMode());
        return gm != null ? gm.getDisplayName() : arena.getGameMode();
    }

    /**
     * Gets a formatted player count string for an arena.
     * Returns "2 Players" when min==max, "2-4 Players" otherwise.
     */
    public static String formatPlayerCount(Arena arena) {
        if (arena.getMinPlayers() == arena.getMaxPlayers()) {
            return arena.getMinPlayers() + " Players";
        }
        return arena.getMinPlayers() + "-" + arena.getMaxPlayers() + " Players";
    }

    /**
     * Loads all arenas from config/arenas/ directory.
     */
    private void loadArenas() {
        // Use plugin's config root, not hardcoded path
        File arenasDir = configManager.getConfigRoot().resolve("arenas").toFile();
        System.out.println("[MatchManager] Looking for arenas in: " + arenasDir.getAbsolutePath());

        if (!arenasDir.exists()) {
            arenasDir.mkdirs();
            System.out.println("[MatchManager] Arenas directory created: " + arenasDir.getAbsolutePath());
            return;
        }

        File[] arenaFiles = arenasDir.listFiles((dir, name) -> name.endsWith(".json"));
        if (arenaFiles == null || arenaFiles.length == 0) {
            System.out.println("[MatchManager] No arena configurations found in " + arenasDir.getAbsolutePath());
            return;
        }

        System.out.println("[MatchManager] Found " + arenaFiles.length + " arena files");
        for (File file : arenaFiles) {
            try {
                String fileName = file.getName();
                String arenaId = fileName.substring(0, fileName.length() - 5); // Remove .json
                System.out.println("[MatchManager] Loading arena file: " + fileName);

                ArenaConfig config = configManager.loadConfig("arenas/" + arenaId, ArenaConfig.class);
                if (config == null) {
                    System.out.println("[MatchManager] Failed to parse arena config: " + fileName);
                    continue;
                }

                if (!config.validate()) {
                    System.out.println("[MatchManager] Arena validation failed for: " + fileName);
                    System.out.println("[MatchManager]   - id: " + config.getId());
                    System.out.println("[MatchManager]   - displayName: " + config.getDisplayName());
                    System.out.println("[MatchManager]   - worldName: " + config.getWorldName());
                    System.out.println("[MatchManager]   - gameMode: " + config.getGameMode());
                    System.out.println("[MatchManager]   - minPlayers: " + config.getMinPlayers());
                    System.out.println("[MatchManager]   - maxPlayers: " + config.getMaxPlayers());
                    System.out.println("[MatchManager]   - spawnPoints: " + (config.getSpawnPoints() != null ? config.getSpawnPoints().size() : "null"));
                    System.out.println("[MatchManager]   - bounds: " + (config.getBounds() != null ? "present" : "null"));
                    continue;
                }

                Arena arena = new Arena(config);
                arenas.put(config.getId(), arena);
                System.out.println("[MatchManager] Loaded arena: " + config.getDisplayName() + " (" + config.getId() + ")");
            } catch (Exception e) {
                System.err.println("[MatchManager] Failed to load arena from " + file.getName() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * Saves an arena config to disk and updates the runtime map.
     * Creates or updates an arena.
     *
     * @param config the arena configuration to save
     * @return true if saved successfully
     */
    public boolean saveArena(ArenaConfig config) {
        if (config == null || !config.validate()) {
            System.err.println("[MatchManager] Cannot save invalid arena config");
            return false;
        }

        // Save to disk
        configManager.saveConfig("arenas/" + config.getId(), config);

        // Update runtime map
        Arena arena = new Arena(config);
        arenas.put(config.getId(), arena);

        System.out.println("[MatchManager] Saved arena: " + config.getDisplayName() + " (" + config.getId() + ")");
        return true;
    }

    /**
     * Deletes an arena from disk and runtime.
     * Fails if the arena is currently in use.
     *
     * @param arenaId the arena ID to delete
     * @return true if deleted successfully
     */
    public boolean deleteArena(String arenaId) {
        if (isArenaInUse(arenaId)) {
            System.err.println("[MatchManager] Cannot delete arena in use: " + arenaId);
            return false;
        }

        // Delete from disk
        boolean deleted = configManager.deleteConfig("arenas/" + arenaId);

        // Remove from runtime map
        arenas.remove(arenaId);

        if (deleted) {
            System.out.println("[MatchManager] Deleted arena: " + arenaId);
        }
        return deleted;
    }

    /**
     * Reloads all arenas from disk.
     * Clears the runtime map and re-reads all arena JSON files.
     */
    public void reloadArenas() {
        arenas.clear();
        loadArenas();
        System.out.println("[MatchManager] Reloaded " + arenas.size() + " arenas");
    }

    /**
     * Gets an arena by ID.
     */
    public Arena getArena(String id) {
        return arenas.get(id);
    }

    /**
     * Gets all available arenas.
     */
    public Collection<Arena> getArenas() {
        return arenas.values();
    }

    /**
     * Creates a new match.
     */
    public Match createMatch(String arenaId) {
        Arena arena = arenas.get(arenaId);
        if (arena == null) {
            System.out.println("Cannot create match: arena not found: " + arenaId);
            return null;
        }

        // Check if world is available
        World world = arena.getWorld();
        if (world == null) {
            System.out.println("Cannot create match: world not loaded: " + arena.getConfig().getWorldName());
            return null;
        }

        // Get game mode
        GameMode gameMode = gameModes.get(arena.getGameMode());
        if (gameMode == null) {
            System.out.println("Cannot create match: game mode not found: " + arena.getGameMode());
            return null;
        }

        // Create match
        Match match = new Match(arena, gameMode, eventBus, hubManager, kitManager);
        match.setBotManager(botManager);
        match.setHudManager(hudManager);
        activeMatches.put(match.getMatchId(), match);

        System.out.println("Created match " + match.getMatchId() + " on arena " + arenaId + " (world: " + arena.getConfig().getWorldName() + ")");
        return match;
    }

    /**
     * Adds a bot to a match.
     * @param matchId the match ID
     * @param bot the bot participant
     * @return true if successfully added
     */
    public boolean addBotToMatch(UUID matchId, BotParticipant bot) {
        Match match = activeMatches.get(matchId);
        if (match == null) {
            return false;
        }
        return match.addBot(bot);
    }

    /**
     * Gets a match by ID.
     */
    public Match getMatch(UUID matchId) {
        return activeMatches.get(matchId);
    }

    /**
     * Gets all active matches.
     */
    public Collection<Match> getActiveMatches() {
        return activeMatches.values();
    }

    /**
     * Gets the first waiting match (for quick join).
     * @return A waiting match, or null if none available
     */
    public Match getFirstWaitingMatch() {
        for (Match match : activeMatches.values()) {
            if (match.getState() == MatchState.WAITING) {
                return match;
            }
        }
        return null;
    }

    /**
     * Gets the match a player is currently in.
     */
    public Match getPlayerMatch(UUID playerUuid) {
        UUID matchId = playerToMatch.get(playerUuid);
        return matchId != null ? activeMatches.get(matchId) : null;
    }

    /**
     * Adds a player to a match without kit selection.
     */
    public boolean addPlayerToMatch(UUID matchId, Player player) {
        return addPlayerToMatch(matchId, player, null);
    }

    /**
     * Adds a player to a match with kit selection.
     */
    public boolean addPlayerToMatch(UUID matchId, Player player, String kitId) {
        Match match = activeMatches.get(matchId);
        if (match == null) {
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

        // Check if player is already in a match
        if (playerToMatch.containsKey(playerUuid)) {
            return false;
        }

        // Add player to match
        if (match.addPlayer(player, kitId)) {
            playerToMatch.put(playerUuid, matchId);
            return true;
        }

        return false;
    }

    /**
     * Removes a player from their current match.
     */
    public void removePlayerFromMatch(UUID playerUuid, String reason) {
        UUID matchId = playerToMatch.remove(playerUuid);
        if (matchId != null) {
            Match match = activeMatches.get(matchId);
            if (match != null) {
                match.removeParticipant(playerUuid, reason);
            }
        }
    }

    /**
     * Checks if a player is in a match.
     */
    public boolean isPlayerInMatch(UUID playerUuid) {
        return playerToMatch.containsKey(playerUuid);
    }

    /**
     * Checks if an arena is currently in use (has an active non-FINISHED match).
     */
    public boolean isArenaInUse(String arenaId) {
        for (Match match : activeMatches.values()) {
            if (match.getArena().getId().equals(arenaId) && !match.isFinished()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Starts the match ticker (20 TPS).
     */
    private void startTicker() {
        tickTimer = new Timer("MatchTicker", true);
        tickTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                tick();
            }
        }, 0, TICK_INTERVAL_MS);
    }

    /**
     * Stops the match ticker.
     */
    private void stopTicker() {
        if (tickTimer != null) {
            tickTimer.cancel();
            tickTimer = null;
        }
    }

    /**
     * Ticks all active matches and cleans up finished ones.
     * Executes match ticks on their respective world threads for thread safety.
     */
    private void tick() {
        List<UUID> finishedMatches = new ArrayList<>();

        for (Match match : activeMatches.values()) {
            try {
                // Get the match's world and execute tick on its thread
                World world = match.getArena().getWorld();
                if (world != null) {
                    world.execute(() -> {
                        try {
                            match.tick();
                        } catch (Exception e) {
                            System.err.println("Error ticking match " + match.getMatchId() + ": " + e.getMessage());
                            e.printStackTrace();
                        }
                    });
                }

                // Check if finished (can be done outside world thread)
                if (match.isFinished()) {
                    finishedMatches.add(match.getMatchId());
                }
            } catch (Exception e) {
                System.err.println("Error processing match " + match.getMatchId() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }

        // Clean up finished matches
        for (UUID matchId : finishedMatches) {
            Match match = activeMatches.remove(matchId);
            if (match != null) {
                // Remove all player mappings
                for (UUID playerUuid : match.getParticipants().stream()
                        .map(p -> p.getUniqueId())
                        .toList()) {
                    playerToMatch.remove(playerUuid);
                }

                System.out.println("Cleaned up finished match: " + matchId);
            }
        }
    }
}
