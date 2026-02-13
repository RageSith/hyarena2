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
import de.ragesith.hyarena2.arena.Arena;
import de.ragesith.hyarena2.arena.ArenaConfig;
import de.ragesith.hyarena2.arena.Match;
import de.ragesith.hyarena2.arena.MatchManager;
import de.ragesith.hyarena2.arena.MatchState;
import de.ragesith.hyarena2.config.GlobalConfig;
import de.ragesith.hyarena2.config.HubConfig;
import de.ragesith.hyarena2.config.Position;
import de.ragesith.hyarena2.hub.HubManager;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages boundary enforcement for players using state-driven logic.
 * Hub players are kept within hub bounds (teleport to hub spawn).
 * In-match players are kept within arena bounds (teleport to last safe position).
 */
public class BoundaryManager {

    private final HubConfig hubConfig;
    private final GlobalConfig globalConfig;
    private final HubManager hubManager;

    private MatchManager matchManager;

    // Track players for boundary checking
    private final Set<UUID> trackedPlayers = ConcurrentHashMap.newKeySet();

    // Grace period after teleport to prevent loops
    private final Map<UUID, Long> teleportGracePeriod = new ConcurrentHashMap<>();
    private static final long TELEPORT_GRACE_MS = 3000;

    // Last known safe position per player (inside arena bounds with margin)
    private final Map<UUID, Position> lastSafePositions = new ConcurrentHashMap<>();

    public BoundaryManager(HubConfig hubConfig, GlobalConfig globalConfig, HubManager hubManager) {
        this.hubConfig = hubConfig;
        this.globalConfig = globalConfig;
        this.hubManager = hubManager;
    }

    /**
     * Sets the MatchManager reference (called after both are constructed).
     */
    public void setMatchManager(MatchManager matchManager) {
        this.matchManager = matchManager;
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
        lastSafePositions.remove(playerId);
    }

    /**
     * Grants a teleport grace period to prevent re-teleportation loops.
     */
    public void grantTeleportGrace(UUID playerId) {
        teleportGracePeriod.put(playerId, System.currentTimeMillis());
    }

    /**
     * Hub boundary tick - enforces hub bounds for players NOT in a match.
     * Dispatches all player component access onto the hub world thread.
     * Called from scheduler at boundaryCheckIntervalMs interval.
     */
    public void tickHub() {
        if (!globalConfig.isBoundaryEnforcementEnabled()) {
            return;
        }

        World hubWorld = hubManager.getHubWorld();
        if (hubWorld == null) {
            return;
        }

        String hubWorldName = hubConfig.getEffectiveWorldName();

        hubWorld.execute(() -> {
            for (UUID playerId : trackedPlayers) {
                try {
                    // Skip players in a match - they're handled by tickArena()
                    if (matchManager != null && matchManager.isPlayerInMatch(playerId)) {
                        continue;
                    }

                    // Skip grace period (cheap check before player resolution)
                    if (isInTeleportGrace(playerId)) {
                        continue;
                    }

                    PlayerRef playerRef = Universe.get().getPlayer(playerId);
                    if (playerRef == null) {
                        continue;
                    }

                    Ref<EntityStore> ref = playerRef.getReference();
                    if (ref == null) {
                        continue;
                    }

                    Store<EntityStore> store = ref.getStore();
                    if (store == null) {
                        continue;
                    }

                    Player player = store.getComponent(ref, Player.getComponentType());
                    if (player == null) {
                        // Player is likely in a different world — can't resolve from hub thread.
                        // If they're not in a match, they shouldn't be in another world.
                        // We'll catch them when they return to hub world.
                        continue;
                    }

                    // Skip bypass permission
                    if (player.hasPermission(Permissions.BYPASS_BOUNDARY)) {
                        continue;
                    }

                    World playerWorld = player.getWorld();
                    if (playerWorld == null) {
                        continue;
                    }

                    if (!playerWorld.getName().equals(hubWorldName)) {
                        // Player is in hub world store but reports different world — teleport back
                        hubManager.teleportToHub(player, null);
                        grantTeleportGrace(playerId);
                        continue;
                    }

                    // Player is in hub world → check hub bounds
                    TransformComponent transform = store.getComponent(ref,
                        EntityModule.get().getTransformComponentType());
                    if (transform == null) {
                        continue;
                    }

                    Vector3d pos = transform.getPosition();
                    if (!hubConfig.isInBounds(pos.getX(), pos.getY(), pos.getZ())) {
                        hubManager.teleportToHub(player, null);
                        grantTeleportGrace(playerId);
                    }
                } catch (Exception e) {
                    // Player might have disconnected - skip silently
                }
            }
        });
    }

    /**
     * Arena boundary tick - enforces arena bounds for players IN a match.
     * Tracks last safe position; teleports back when out of bounds.
     * Called from scheduler at boundaryCheckIntervalMs interval (same as hub).
     */
    public void tickArena() {
        if (!globalConfig.isBoundaryEnforcementEnabled()) {
            return;
        }

        if (matchManager == null) {
            return;
        }

        double margin = globalConfig.getArenaBoundaryMargin();
        boolean debug = globalConfig.isDebugLogging();

        for (UUID playerId : trackedPlayers) {
            try {
                Match match = matchManager.getPlayerMatch(playerId);
                if (match == null) {
                    continue;
                }

                // Only enforce during active match states
                MatchState state = match.getState();
                if (state == MatchState.FINISHED || state == MatchState.WAITING || state == MatchState.STARTING) {
                    continue;
                }

                Arena arena = match.getArena();
                if (arena == null) {
                    continue;
                }

                ArenaConfig.Bounds bounds = arena.getBounds();
                if (bounds == null) {
                    continue;
                }

                World arenaWorld = arena.getWorld();
                if (arenaWorld == null) {
                    continue;
                }

                // Dispatch all player component access onto the arena world thread
                arenaWorld.execute(() -> {
                    try {
                        // Skip grace period
                        if (isInTeleportGrace(playerId)) {
                            return;
                        }

                        PlayerRef playerRef = Universe.get().getPlayer(playerId);
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

                        // Skip bypass permission
                        if (player.hasPermission(Permissions.BYPASS_BOUNDARY)) {
                            return;
                        }

                        TransformComponent transform = store.getComponent(ref,
                            EntityModule.get().getTransformComponentType());
                        if (transform == null) {
                            return;
                        }

                        Vector3d pos = transform.getPosition();
                        double px = pos.getX();
                        double py = pos.getY();
                        double pz = pos.getZ();

                        if (bounds.containsWithMargin(px, py, pz, margin)) {
                            // Inside bounds — save as last safe position
                            lastSafePositions.put(playerId, new Position(px, py, pz));
                        } else {
                            // Out of bounds — teleport to last safe position
                            Position safePos = lastSafePositions.get(playerId);
                            if (safePos == null) {
                                // No safe position yet — use arena spawn as fallback
                                List<ArenaConfig.SpawnPoint> spawnPoints = arena.getSpawnPoints();
                                if (spawnPoints != null && !spawnPoints.isEmpty()) {
                                    ArenaConfig.SpawnPoint spawn = spawnPoints.get(0);
                                    safePos = new Position(
                                        spawn.getX(), spawn.getY(), spawn.getZ(),
                                        spawn.getYaw(), spawn.getPitch());
                                } else {
                                    return;
                                }
                            }

                            if (debug) {
                                System.out.println("[BoundaryManager] Arena boundary: teleporting " + playerId
                                    + " from (" + px + ", " + py + ", " + pz + ")"
                                    + " to safe pos (" + safePos.getX() + ", " + safePos.getY() + ", " + safePos.getZ() + ")");
                            }

                            hubManager.teleportPlayerToWorld(player, safePos, arenaWorld);
                            grantTeleportGrace(playerId);
                        }
                    } catch (Exception e) {
                        System.err.println("[BoundaryManager] Arena tick error for " + playerId + ": " + e.getMessage());
                    }
                });
            } catch (Exception e) {
                // Player might have disconnected - skip silently
            }
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

    /**
     * Checks if a player is in a specific world.
     */
    public boolean isPlayerInWorld(UUID playerId, String worldName) {
        try {
            PlayerRef playerRef = Universe.get().getPlayer(playerId);
            if (playerRef == null) {
                return false;
            }

            Ref<EntityStore> ref = playerRef.getReference();
            if (ref == null) {
                return false;
            }

            Store<EntityStore> store = ref.getStore();
            if (store == null) {
                return false;
            }

            Player player = store.getComponent(ref, Player.getComponentType());
            if (player == null) {
                return false;
            }

            World playerWorld = player.getWorld();
            if (playerWorld == null) {
                return false;
            }

            return worldName.equals(playerWorld.getName());
        } catch (Exception e) {
            return false;
        }
    }
}
