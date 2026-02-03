package de.ragesith.hyarena2.ui.hud;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import de.ragesith.hyarena2.arena.MatchManager;
import de.ragesith.hyarena2.queue.Matchmaker;
import de.ragesith.hyarena2.queue.QueueManager;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Supplier;

/**
 * Manages HUD lifecycle for all players.
 * Tracks active LobbyHuds and provides methods to show/hide them.
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
     */
    public void showLobbyHud(UUID playerUuid) {
        // Don't show if already has one
        if (lobbyHuds.containsKey(playerUuid)) {
            return;
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
     * Handles player disconnect - cleans up HUDs.
     */
    public void handlePlayerDisconnect(UUID playerUuid) {
        LobbyHud hud = lobbyHuds.remove(playerUuid);
        if (hud != null) {
            hud.shutdown();
        }
    }

    /**
     * Shuts down all HUDs.
     */
    public void shutdown() {
        for (LobbyHud hud : lobbyHuds.values()) {
            hud.shutdown();
        }
        lobbyHuds.clear();

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
