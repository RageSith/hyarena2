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
 * Tracks active HUDs and provides methods to show/hide them.
 */
public class HudManager {

    private final QueueManager queueManager;
    private final Matchmaker matchmaker;
    private final MatchManager matchManager;
    private final ScheduledExecutorService scheduler;
    private final Supplier<Integer> onlinePlayerCountSupplier;

    // Active HUDs per player
    private final Map<UUID, LobbyHud> lobbyHuds = new ConcurrentHashMap<>();
    private final Map<UUID, QueueHud> queueHuds = new ConcurrentHashMap<>();

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
     * Stops any existing QueueHud since LobbyHud will replace it.
     */
    public void showLobbyHud(UUID playerUuid) {
        // Don't show if already has one
        if (lobbyHuds.containsKey(playerUuid)) {
            return;
        }

        // Stop QueueHud if running (it's being replaced)
        QueueHud queueHud = queueHuds.remove(playerUuid);
        if (queueHud != null) {
            queueHud.shutdown();
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
            playerRef, playerUuid, queueManager, matchManager,
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
     * Also stops QueueHud if running (player leaving hub).
     */
    public void hideLobbyHud(UUID playerUuid) {
        // Stop QueueHud too (player is leaving hub, shouldn't be in queue display)
        QueueHud queueHud = queueHuds.remove(playerUuid);
        if (queueHud != null) {
            queueHud.shutdown();
        }

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
     * Shows the queue HUD for a player.
     * Stops LobbyHud refresh task since it will be replaced.
     */
    public void showQueueHud(UUID playerUuid) {
        // Don't show if already has one
        if (queueHuds.containsKey(playerUuid)) {
            return;
        }

        // Stop LobbyHud refresh task (it's being replaced, don't let it keep updating)
        LobbyHud lobbyHud = lobbyHuds.remove(playerUuid);
        if (lobbyHud != null) {
            lobbyHud.shutdown();
        }

        PlayerRef playerRef = Universe.get().getPlayer(playerUuid);
        if (playerRef == null) {
            return;
        }

        Player player = getPlayer(playerRef);
        if (player == null) {
            return;
        }

        QueueHud hud = new QueueHud(
            playerRef, playerUuid, queueManager, matchmaker, matchManager,
            scheduler
        );

        queueHuds.put(playerUuid, hud);

        try {
            player.getHudManager().setCustomHud(playerRef, hud);
            System.out.println("[HudManager] Showed QueueHud for " + playerUuid);
        } catch (Exception e) {
            System.err.println("[HudManager] Failed to show QueueHud: " + e.getMessage());
            queueHuds.remove(playerUuid);
        }
    }

    /**
     * Hides the queue HUD for a player.
     * Re-shows the LobbyHud if player is still in hub.
     */
    public void hideQueueHud(UUID playerUuid) {
        QueueHud hud = queueHuds.remove(playerUuid);
        if (hud != null) {
            hud.shutdown();
            System.out.println("[HudManager] Hid QueueHud for " + playerUuid);

            // Re-show LobbyHud (it was shutdown when QueueHud was shown)
            showLobbyHud(playerUuid);
        }
    }

    /**
     * Hides all HUDs for a player.
     */
    public void hideAllHuds(UUID playerUuid) {
        hideLobbyHud(playerUuid);
        hideQueueHud(playerUuid);
    }

    /**
     * Handles player disconnect - cleans up HUDs.
     */
    public void handlePlayerDisconnect(UUID playerUuid) {
        // Just remove from maps - shutdown and HudManager cleanup not needed for disconnected player
        LobbyHud lobbyHud = lobbyHuds.remove(playerUuid);
        if (lobbyHud != null) {
            lobbyHud.shutdown();
        }

        QueueHud queueHud = queueHuds.remove(playerUuid);
        if (queueHud != null) {
            queueHud.shutdown();
        }
    }

    /**
     * Checks if a player has the lobby HUD visible.
     */
    public boolean hasLobbyHud(UUID playerUuid) {
        return lobbyHuds.containsKey(playerUuid);
    }

    /**
     * Checks if a player has the queue HUD visible.
     */
    public boolean hasQueueHud(UUID playerUuid) {
        return queueHuds.containsKey(playerUuid);
    }

    /**
     * Shuts down all HUDs.
     */
    public void shutdown() {
        for (LobbyHud hud : lobbyHuds.values()) {
            hud.shutdown();
        }
        lobbyHuds.clear();

        for (QueueHud hud : queueHuds.values()) {
            hud.shutdown();
        }
        queueHuds.clear();

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
