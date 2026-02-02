package de.ragesith.hyarena2.boundary;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.EntityModule;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import de.ragesith.hyarena2.Permissions;
import de.ragesith.hyarena2.config.GlobalConfig;
import de.ragesith.hyarena2.config.HubConfig;
import de.ragesith.hyarena2.config.Position;
import de.ragesith.hyarena2.hub.HubManager;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages boundary enforcement for players.
 * Phase 1: Simple hub boundary enforcement.
 * Future phases will add arena boundaries and spatial optimization.
 */
public class BoundaryManager {

    private final HubConfig hubConfig;
    private final GlobalConfig globalConfig;
    private final HubManager hubManager;

    // Track players for boundary checking
    private final Map<UUID, PlayerRef> playerRefs = new ConcurrentHashMap<>();
    private final Map<UUID, Player> playerEntities = new ConcurrentHashMap<>();

    // Grace period after teleport to prevent loops
    private final Map<UUID, Long> teleportGracePeriod = new ConcurrentHashMap<>();
    private static final long TELEPORT_GRACE_MS = 3000;

    private long lastCheckMs = 0;

    public BoundaryManager(HubConfig hubConfig, GlobalConfig globalConfig, HubManager hubManager) {
        this.hubConfig = hubConfig;
        this.globalConfig = globalConfig;
        this.hubManager = hubManager;
    }

    /**
     * Registers a player for boundary checking.
     */
    public void registerPlayer(PlayerRef playerRef, Player player) {
        UUID playerId = playerRef.getUuid();
        playerRefs.put(playerId, playerRef);
        playerEntities.put(playerId, player);

        // Check initial position
        Position pos = getPlayerPosition(player);
        if (pos != null && !hubConfig.isInBounds(pos.getX(), pos.getY(), pos.getZ())) {
            // Player spawned outside hub, teleport to spawn
            hubManager.teleportToHub(player);
            grantTeleportGrace(playerId);
        }
    }

    /**
     * Unregisters a player from boundary checking.
     */
    public void unregisterPlayer(PlayerRef playerRef) {
        UUID playerId = playerRef.getUuid();
        playerRefs.remove(playerId);
        playerEntities.remove(playerId);
        teleportGracePeriod.remove(playerId);
    }

    /**
     * Grants a teleport grace period to prevent re-teleportation loops.
     */
    public void grantTeleportGrace(UUID playerId) {
        teleportGracePeriod.put(playerId, System.currentTimeMillis());
    }

    /**
     * Main tick - checks all players and enforces boundaries.
     * Should be called periodically on the world thread.
     */
    public void tick() {
        if (!globalConfig.isBoundaryEnforcementEnabled()) {
            return;
        }

        long now = System.currentTimeMillis();
        if (now - lastCheckMs < globalConfig.getBoundaryCheckIntervalMs()) {
            return;
        }
        lastCheckMs = now;

        for (Map.Entry<UUID, Player> entry : playerEntities.entrySet()) {
            checkPlayer(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Checks a single player's boundaries.
     */
    private void checkPlayer(UUID playerId, Player player) {
        try {
            // Skip if player has bypass permission
            if (player.hasPermission(Permissions.BYPASS_BOUNDARY)) {
                return;
            }

            // Skip if in grace period
            if (isInTeleportGrace(playerId)) {
                return;
            }

            Position pos = getPlayerPosition(player);
            if (pos == null) {
                return;
            }

            // Phase 1: Just check hub bounds
            if (!hubConfig.isInBounds(pos.getX(), pos.getY(), pos.getZ())) {
                // Out of bounds - teleport to hub spawn
                hubManager.teleportToHub(player);
                grantTeleportGrace(playerId);
            }

        } catch (Exception e) {
            // Player might have disconnected
        }
    }

    /**
     * Checks if a player is in teleport grace period.
     */
    private boolean isInTeleportGrace(UUID playerId) {
        Long graceStart = teleportGracePeriod.get(playerId);
        if (graceStart == null) {
            return false;
        }
        if (System.currentTimeMillis() - graceStart > TELEPORT_GRACE_MS) {
            teleportGracePeriod.remove(playerId);
            return false;
        }
        return true;
    }

    /**
     * Gets a player's current position.
     */
    private Position getPlayerPosition(Player player) {
        Ref<EntityStore> ref = player.getReference();
        if (ref == null) return null;

        Store<EntityStore> store = ref.getStore();
        if (store == null) return null;

        TransformComponent transform = store.getComponent(ref,
            EntityModule.get().getTransformComponentType());
        if (transform == null) return null;

        Vector3d vec = transform.getPosition();
        return new Position(vec.getX(), vec.getY(), vec.getZ());
    }

    /**
     * Gets the number of tracked players.
     */
    public int getPlayerCount() {
        return playerEntities.size();
    }
}
