package de.ragesith.hyarena2.boundary;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.EntityModule;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import de.ragesith.hyarena2.Permissions;
import de.ragesith.hyarena2.config.GlobalConfig;
import de.ragesith.hyarena2.config.HubConfig;
import de.ragesith.hyarena2.hub.HubManager;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages boundary enforcement for players.
 * Phase 2: Skip boundary checks for players in matches.
 */
public class BoundaryManager {

    private final HubConfig hubConfig;
    private final GlobalConfig globalConfig;
    private final HubManager hubManager;

    // Track players for boundary checking
    private final Set<UUID> trackedPlayers = ConcurrentHashMap.newKeySet();

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
    public void registerPlayer(UUID playerId, Player player) {
        trackedPlayers.add(playerId);

        // Only check initial position if player is in hub world
        String playerWorld = player.getWorld() != null ? player.getWorld().getName() : null;
        String hubWorld = hubConfig.getEffectiveWorldName();
        if (playerWorld == null || !playerWorld.equals(hubWorld)) {
            return;
        }

        // Check initial position using TransformComponent
        Ref<EntityStore> ref = player.getReference();
        if (ref != null) {
            Store<EntityStore> store = ref.getStore();
            if (store != null) {
                TransformComponent transform = store.getComponent(ref,
                    EntityModule.get().getTransformComponentType());
                if (transform != null) {
                    Vector3d pos = transform.getPosition();
                    if (!hubConfig.isInBounds(pos.getX(), pos.getY(), pos.getZ())) {
                        // Player spawned outside hub, teleport to spawn
                        hubManager.teleportToHub(player, null);
                        grantTeleportGrace(playerId);
                    }
                }
            }
        }
    }

    /**
     * Unregisters a player from boundary checking.
     */
    public void unregisterPlayer(UUID playerId) {
        trackedPlayers.remove(playerId);
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

        for (UUID playerId : trackedPlayers) {
            try {
                PlayerRef playerRef = Universe.get().getPlayer(playerId);
                if (playerRef != null) {
                    Ref<EntityStore> ref = playerRef.getReference();
                    if (ref != null) {
                        Store<EntityStore> store = ref.getStore();
                        if (store != null) {
                            Player player = store.getComponent(ref, Player.getComponentType());
                            if (player != null) {
                                checkPlayer(playerId, player);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                // Player might be in a different world - skip silently
            }
        }
    }

    /**
     * Checks a single player's boundaries.
     * Only checks players in the hub world - players in arena worlds are skipped.
     */
    private void checkPlayer(UUID playerId, Player player) {
        try {
            // Skip if player is not in the hub world (they're in an arena)
            String playerWorld = player.getWorld() != null ? player.getWorld().getName() : null;
            String hubWorld = hubConfig.getEffectiveWorldName();
            if (playerWorld == null || !playerWorld.equals(hubWorld)) {
                return;
            }

            // Skip if player has bypass permission
            if (player.hasPermission(Permissions.BYPASS_BOUNDARY)) {
                return;
            }

            // Skip if in grace period
            if (isInTeleportGrace(playerId)) {
                return;
            }

            // Get player position using TransformComponent
            Ref<EntityStore> ref = player.getReference();
            if (ref == null) {
                return;
            }

            Store<EntityStore> store = ref.getStore();
            if (store == null) {
                return;
            }

            TransformComponent transform = store.getComponent(ref,
                EntityModule.get().getTransformComponentType());
            if (transform == null) {
                return;
            }

            Vector3d pos = transform.getPosition();

            // Only check hub bounds (arena bounds checked by arena system in future)
            if (!hubConfig.isInBounds(pos.getX(), pos.getY(), pos.getZ())) {
                // Out of bounds - teleport to hub spawn
                hubManager.teleportToHub(player, null);
                grantTeleportGrace(playerId);
            }

        } catch (Exception e) {
            // Player might have disconnected or be in wrong world
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
     * Gets the number of tracked players.
     */
    public int getPlayerCount() {
        return trackedPlayers.size();
    }
}
