package de.ragesith.hyarena2.ui.hud;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import de.ragesith.hyarena2.arena.Match;
import de.ragesith.hyarena2.arena.MatchManager;
import de.ragesith.hyarena2.queue.Matchmaker;
import de.ragesith.hyarena2.queue.QueueManager;
import de.ragesith.hyarena2.ui.hyml.HyMLDocument;
import de.ragesith.hyarena2.ui.hyml.HyMLPage;
import de.ragesith.hyarena2.ui.hyml.HyMLParser;
import de.ragesith.hyarena2.ui.page.CloseablePage;

import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Manages HUD lifecycle for all players.
 * Tracks active LobbyHuds, MatchHuds, and VictoryHuds.
 *
 * The LobbyHud combines both server stats and queue info in one HUD,
 * toggling the queue panel visibility based on queue status.
 */
public class HudManager {

    private final QueueManager queueManager;
    private final Matchmaker matchmaker;
    private final MatchManager matchManager;
    private final ScheduledExecutorService scheduler;
    private final Supplier<Integer> onlinePlayerCountSupplier;

    // Active HUDs per player
    private final Map<UUID, LobbyHud> lobbyHuds = new ConcurrentHashMap<>();
    private final Map<UUID, MatchHud> matchHuds = new ConcurrentHashMap<>();
    private final Map<UUID, VictoryHud> victoryHuds = new ConcurrentHashMap<>();

    // Active pages per player (for cleanup on disconnect or page replacement)
    private final Map<UUID, CloseablePage> activePages = new ConcurrentHashMap<>();

    // HyML directory for markup-based pages
    private Path hymlDir;

    public HudManager(QueueManager queueManager, Matchmaker matchmaker, MatchManager matchManager,
                      ScheduledExecutorService scheduler, Supplier<Integer> onlinePlayerCountSupplier) {
        this.queueManager = queueManager;
        this.matchmaker = matchmaker;
        this.matchManager = matchManager;
        this.scheduler = scheduler;
        this.onlinePlayerCountSupplier = onlinePlayerCountSupplier;
    }

    /**
     * Shows the lobby HUD for a player.
     * If an old HUD exists, it will be shut down and replaced with a fresh one.
     */
    public void showLobbyHud(UUID playerUuid) {
        // Shut down existing HUD if any (might have stale refresh task)
        LobbyHud oldHud = lobbyHuds.remove(playerUuid);
        if (oldHud != null) {
            oldHud.shutdown();
            System.out.println("[HudManager] Replaced old LobbyHud for " + playerUuid);
        }

        PlayerRef playerRef = Universe.get().getPlayer(playerUuid);
        if (playerRef == null) {
            return;
        }

        Player player = getPlayer(playerRef);
        if (player == null) {
            return;
        }

        LobbyHud hud = new LobbyHud(
            playerRef, playerUuid, queueManager, matchmaker, matchManager,
            onlinePlayerCountSupplier, scheduler
        );

        lobbyHuds.put(playerUuid, hud);

        try {
            player.getHudManager().setCustomHud(playerRef, hud);
            System.out.println("[HudManager] Showed LobbyHud for " + playerUuid);
        } catch (Exception e) {
            System.err.println("[HudManager] Failed to show LobbyHud: " + e.getMessage());
            lobbyHuds.remove(playerUuid);
        }
    }

    /**
     * Hides the lobby HUD for a player.
     */
    public void hideLobbyHud(UUID playerUuid) {
        LobbyHud hud = lobbyHuds.remove(playerUuid);
        if (hud != null) {
            hud.shutdown();
            System.out.println("[HudManager] Hid LobbyHud for " + playerUuid);
        }

        // Set empty HUD
        PlayerRef playerRef = Universe.get().getPlayer(playerUuid);
        if (playerRef != null) {
            Player player = getPlayer(playerRef);
            if (player != null) {
                try {
                    player.getHudManager().setCustomHud(playerRef, new EmptyHud(playerRef));
                } catch (Exception e) {
                    // Player might have disconnected
                }
            }
        }
    }

    /**
     * Checks if a player has the lobby HUD visible.
     */
    public boolean hasLobbyHud(UUID playerUuid) {
        return lobbyHuds.containsKey(playerUuid);
    }

    /**
     * Shows the match HUD for a player in an active match.
     * @param playerUuid the player's UUID
     * @param match the match to display info for
     * @param worldThreadExecutor executor to run UI updates on the world thread
     */
    public void showMatchHud(UUID playerUuid, Match match, Consumer<Runnable> worldThreadExecutor) {
        // Shut down existing match HUD if any
        MatchHud oldHud = matchHuds.remove(playerUuid);
        if (oldHud != null) {
            oldHud.shutdown();
        }

        // Also hide lobby HUD if showing
        hideLobbyHud(playerUuid);

        PlayerRef playerRef = Universe.get().getPlayer(playerUuid);
        if (playerRef == null) {
            return;
        }

        Player player = getPlayer(playerRef);
        if (player == null) {
            return;
        }

        MatchHud hud = new MatchHud(playerRef, playerUuid, match, scheduler, worldThreadExecutor);
        matchHuds.put(playerUuid, hud);

        try {
            player.getHudManager().setCustomHud(playerRef, hud);
            System.out.println("[HudManager] Showed MatchHud for " + playerUuid);
        } catch (Exception e) {
            System.err.println("[HudManager] Failed to show MatchHud: " + e.getMessage());
            matchHuds.remove(playerUuid);
        }
    }

    /**
     * Hides the match HUD for a player.
     */
    public void hideMatchHud(UUID playerUuid) {
        MatchHud hud = matchHuds.remove(playerUuid);
        if (hud != null) {
            hud.shutdown();
            System.out.println("[HudManager] Hid MatchHud for " + playerUuid);
        }

        // Set empty HUD
        PlayerRef playerRef = Universe.get().getPlayer(playerUuid);
        if (playerRef != null) {
            Player player = getPlayer(playerRef);
            if (player != null) {
                try {
                    player.getHudManager().setCustomHud(playerRef, new EmptyHud(playerRef));
                } catch (Exception e) {
                    // Player might have disconnected
                }
            }
        }
    }

    /**
     * Shows the victory screen as an interactive page for a player after a match ends.
     * The page persists after teleport to hub and must be dismissed manually by the player.
     * @param playerUuid the player's UUID
     * @param match the finished match
     * @param isWinner whether this player won
     * @param winnerName the name of the winner (or null for draw)
     */
    public void showVictoryHud(UUID playerUuid, Match match, boolean isWinner, String winnerName) {
        // Shut down existing victory page if any
        VictoryHud oldHud = victoryHuds.remove(playerUuid);
        if (oldHud != null) {
            oldHud.shutdown();
        }

        // Also hide match HUD
        hideMatchHud(playerUuid);

        PlayerRef playerRef = Universe.get().getPlayer(playerUuid);
        if (playerRef == null) {
            return;
        }

        Ref<EntityStore> ref = playerRef.getReference();
        if (ref == null) {
            return;
        }

        Store<EntityStore> store = ref.getStore();
        if (store == null) {
            return;
        }

        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            return;
        }

        VictoryHud hud = new VictoryHud(playerRef, playerUuid, match, isWinner, winnerName, this);
        victoryHuds.put(playerUuid, hud);

        try {
            player.getPageManager().openCustomPage(ref, store, hud);
            System.out.println("[HudManager] Showed VictoryHud page for " + playerUuid + " (winner: " + isWinner + ")");
        } catch (Exception e) {
            System.err.println("[HudManager] Failed to show VictoryHud: " + e.getMessage());
            victoryHuds.remove(playerUuid);
        }
    }

    /**
     * Hides the victory HUD for a player.
     */
    public void hideVictoryHud(UUID playerUuid) {
        VictoryHud hud = victoryHuds.remove(playerUuid);
        if (hud != null) {
            hud.shutdown();
            System.out.println("[HudManager] Hid VictoryHud for " + playerUuid);
        }

        // Set empty HUD (lobby HUD will be shown after teleport to hub)
        PlayerRef playerRef = Universe.get().getPlayer(playerUuid);
        if (playerRef != null) {
            Player player = getPlayer(playerRef);
            if (player != null) {
                try {
                    player.getHudManager().setCustomHud(playerRef, new EmptyHud(playerRef));
                } catch (Exception e) {
                    // Player might have disconnected
                }
            }
        }
    }

    /**
     * Registers an active page for a player.
     * If there's an existing page, it will be shut down first.
     * @param playerUuid the player's UUID
     * @param page the page to register
     */
    public void registerPage(UUID playerUuid, CloseablePage page) {
        CloseablePage oldPage = activePages.put(playerUuid, page);
        if (oldPage != null && oldPage != page) {
            oldPage.shutdown();
            System.out.println("[HudManager] Replaced old page for " + playerUuid);
        }
    }

    /**
     * Unregisters a page for a player.
     * Only unregisters if the given page is the currently active one.
     * @param playerUuid the player's UUID
     * @param page the page to unregister
     */
    public void unregisterPage(UUID playerUuid, CloseablePage page) {
        activePages.remove(playerUuid, page);
    }

    /**
     * Closes any active page for a player.
     */
    public void closeActivePage(UUID playerUuid) {
        CloseablePage page = activePages.remove(playerUuid);
        if (page != null) {
            page.shutdown();
            System.out.println("[HudManager] Closed active page for " + playerUuid);
        }
    }

    /**
     * Sets the directory for HyML markup files.
     */
    public void setHymlDir(Path hymlDir) {
        this.hymlDir = hymlDir;
    }

    /**
     * Opens a HyML-based page for a player.
     * Parses the .hyml file, substitutes variables, and opens as an interactive page.
     *
     * @param playerUuid the player's UUID
     * @param hymlFile   the .hyml filename (relative to hymlDir)
     * @param vars       placeholder variables ({key} → value), may be null
     */
    public void showHyMLPage(UUID playerUuid, String hymlFile, Map<String, String> vars) {
        if (hymlDir == null) {
            System.err.println("[HudManager] HyML directory not set");
            return;
        }

        Path filePath = hymlDir.resolve(hymlFile);
        HyMLDocument document = HyMLParser.parse(filePath, vars);
        if (document == null) {
            System.err.println("[HudManager] Failed to parse HyML file: " + hymlFile);
            return;
        }

        openHyMLDocument(playerUuid, document, hymlFile);
    }

    /**
     * Opens a HyML-based page from a classpath resource (inside the JAR).
     *
     * @param playerUuid   the player's UUID
     * @param resourcePath classpath resource path (e.g. "hyml/rules.hyml")
     * @param vars         placeholder variables ({key} → value), may be null
     */
    public void showHyMLResourcePage(UUID playerUuid, String resourcePath, Map<String, String> vars) {
        HyMLDocument document = HyMLParser.parseResource(resourcePath, vars);
        if (document == null) {
            System.err.println("[HudManager] Failed to parse HyML resource: " + resourcePath);
            return;
        }

        openHyMLDocument(playerUuid, document, resourcePath);
    }

    /**
     * Opens a pre-parsed HyMLDocument as a page for a player.
     */
    private void openHyMLDocument(UUID playerUuid, HyMLDocument document, String sourceName) {
        PlayerRef playerRef = Universe.get().getPlayer(playerUuid);
        if (playerRef == null) return;

        Ref<EntityStore> ref = playerRef.getReference();
        if (ref == null) return;

        Store<EntityStore> store = ref.getStore();
        if (store == null) return;

        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) return;

        closeActivePage(playerUuid);

        HyMLPage page = new HyMLPage(playerRef, playerUuid, document, this);

        try {
            player.getPageManager().openCustomPage(ref, store, page);
            System.out.println("[HudManager] Opened HyML page '" + sourceName + "' for " + playerUuid);
        } catch (Exception e) {
            System.err.println("[HudManager] Failed to open HyML page: " + e.getMessage());
        }
    }

    /**
     * Handles player disconnect - cleans up all HUDs and pages.
     */
    public void handlePlayerDisconnect(UUID playerUuid) {
        // Clean up pages first
        CloseablePage page = activePages.remove(playerUuid);
        if (page != null) {
            page.shutdown();
        }

        LobbyHud lobbyHud = lobbyHuds.remove(playerUuid);
        if (lobbyHud != null) {
            lobbyHud.shutdown();
        }

        MatchHud matchHud = matchHuds.remove(playerUuid);
        if (matchHud != null) {
            matchHud.shutdown();
        }

        VictoryHud victoryHud = victoryHuds.remove(playerUuid);
        if (victoryHud != null) {
            victoryHud.shutdown();
        }
    }

    /**
     * Shuts down all HUDs and pages.
     */
    public void shutdown() {
        // Shutdown pages first
        for (CloseablePage page : activePages.values()) {
            page.shutdown();
        }
        activePages.clear();

        for (LobbyHud hud : lobbyHuds.values()) {
            hud.shutdown();
        }
        lobbyHuds.clear();

        for (MatchHud hud : matchHuds.values()) {
            hud.shutdown();
        }
        matchHuds.clear();

        for (VictoryHud hud : victoryHuds.values()) {
            hud.shutdown();
        }
        victoryHuds.clear();

        System.out.println("[HudManager] Shutdown complete");
    }

    /**
     * Gets the Player entity from a PlayerRef.
     */
    private Player getPlayer(PlayerRef playerRef) {
        Ref<EntityStore> ref = playerRef.getReference();
        if (ref == null) return null;

        Store<EntityStore> store = ref.getStore();
        if (store == null) return null;

        return store.getComponent(ref, Player.getComponentType());
    }
}
