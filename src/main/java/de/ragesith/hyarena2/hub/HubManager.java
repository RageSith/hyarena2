package de.ragesith.hyarena2.hub;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.EntityModule;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatsModule;
import com.hypixel.hytale.server.core.modules.entitystats.asset.EntityStatType;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import de.ragesith.hyarena2.config.HubConfig;
import de.ragesith.hyarena2.config.Position;
import de.ragesith.hyarena2.utils.HologramUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Manages the hub spawn area and player teleportation.
 * Handles both same-world and cross-world teleportation with proper chunk loading.
 */
public class HubManager {

    private HubConfig config;

    // Delay for cross-world teleports (milliseconds) to allow client fade animations
    private static final long CROSS_WORLD_TELEPORT_DELAY_MS = 1500;

    // Active hologram entity refs in the hub world
    private final List<Ref<EntityStore>> hubHolograms = new ArrayList<>();
    private boolean hologramsSpawned = false;

    public HubManager(HubConfig config) {
        this.config = config;
    }

    /**
     * Updates the hub configuration at runtime.
     * Called after saving hub.json from the admin UI.
     */
    public void updateConfig(HubConfig newConfig) {
        this.config = newConfig;
        respawnHubHolograms();
        System.out.println("[HubManager] Config updated at runtime");
    }

    /**
     * Gets the current hub configuration.
     */
    public HubConfig getConfig() {
        return config;
    }

    /**
     * Gets the hub world using Universe API.
     */
    public World getHubWorld() {
        String worldName = config.getEffectiveWorldName();
        World world = Universe.get().getWorld(worldName);

        if (world == null) {
            System.err.println("[HyArena2] Hub world not found: " + worldName + ", trying 'default'");
            world = Universe.get().getWorld("default");
        }

        return world;
    }

    /**
     * Teleports a player to the hub spawn point.
     */
    public void teleportToHub(Player player) {
        teleportToHub(player, null);
    }

    /**
     * Teleports a player to the hub spawn point with a completion callback.
     * Automatically heals the player to full health on arrival.
     */
    public void teleportToHub(Player player, Runnable onComplete) {
        Position spawn = config.getSpawnPoint();

        if (spawn == null) {
            System.err.println("[HyArena2] Hub spawn point is null!");
            return;
        }

        World hubWorld = getHubWorld();

        // Wrap callback to heal player after a short delay (entity needs time to settle after teleport)
        Runnable wrappedCallback = () -> {
            if (onComplete != null) {
                onComplete.run();
            }
            World hw = getHubWorld();
            if (hw != null) {
                CompletableFuture.delayedExecutor(500, TimeUnit.MILLISECONDS).execute(() -> {
                    hw.execute(() -> healPlayer(player));
                });
            }
        };

        if (hubWorld != null) {
            teleportPlayerToWorld(player, spawn, hubWorld, wrappedCallback);
        } else {
            // Fallback: same-world teleport
            teleportPlayer(player, spawn);
            wrappedCallback.run();
        }
    }

    /**
     * Teleports a player to a position in their current world.
     */
    public void teleportPlayer(Player player, Position position) {
        World world = player.getWorld();
        if (world != null) {
            teleportPlayerToWorld(player, position, world, null);
        }
    }

    /**
     * Teleports a player to a position in a specific world.
     */
    public void teleportPlayerToWorld(Player player, Position position, World targetWorld) {
        teleportPlayerToWorld(player, position, targetWorld, null);
    }

    /**
     * Teleports a player to a position in a specific world with a completion callback.
     * Handles cross-world teleportation with chunk pre-loading and delay.
     */
    public void teleportPlayerToWorld(Player player, Position position, World targetWorld, Runnable onComplete) {
        World currentWorld = player.getWorld();
        if (currentWorld == null) {
            System.err.println("[HyArena2] Player has no current world!");
            return;
        }

        final World destWorld = (targetWorld != null) ? targetWorld : currentWorld;
        boolean isCrossWorld = !currentWorld.getName().equals(destWorld.getName());

        if (isCrossWorld) {
            preloadChunksAndTeleport(player, position, currentWorld, destWorld, onComplete);
        } else {
            performTeleport(player, position, destWorld);
            if (onComplete != null) {
                onComplete.run();
            }
        }
    }

    /**
     * Pre-loads chunks at destination before cross-world teleport.
     */
    private void preloadChunksAndTeleport(Player player, Position position, World currentWorld, World destWorld, Runnable onComplete) {
        int blockX = (int) position.getX();
        int blockY = (int) position.getY();
        int blockZ = (int) position.getZ();

        // Execute chunk loading on destination world thread
        destWorld.execute(() -> {
            try {
                // Trigger chunk loading by reading blocks
                destWorld.getBlockType(blockX, blockY, blockZ);
                destWorld.getBlockType(blockX + 16, blockY, blockZ);
                destWorld.getBlockType(blockX - 16, blockY, blockZ);
                destWorld.getBlockType(blockX, blockY, blockZ + 16);
                destWorld.getBlockType(blockX, blockY, blockZ - 16);

                // Delay teleport to allow client fade animations
                CompletableFuture.delayedExecutor(
                    CROSS_WORLD_TELEPORT_DELAY_MS,
                    TimeUnit.MILLISECONDS
                ).execute(() -> {
                    currentWorld.execute(() -> {
                        performTeleport(player, position, destWorld);
                        if (onComplete != null) {
                            destWorld.execute(onComplete);
                        }
                    });
                });
            } catch (Exception e) {
                System.err.println("[HyArena2] Error pre-loading chunks: " + e.getMessage());
                // Try teleport anyway with delay
                CompletableFuture.delayedExecutor(
                    CROSS_WORLD_TELEPORT_DELAY_MS,
                    TimeUnit.MILLISECONDS
                ).execute(() -> {
                    currentWorld.execute(() -> {
                        performTeleport(player, position, destWorld);
                        if (onComplete != null) {
                            destWorld.execute(onComplete);
                        }
                    });
                });
            }
        });
    }

    /**
     * Performs the actual teleport by adding Teleport component.
     */
    private void performTeleport(Player player, Position position, World destWorld) {
        Ref<EntityStore> ref = player.getReference();
        if (ref == null) return;

        Store<EntityStore> store = ref.getStore();
        if (store == null) return;

        Vector3d targetPos = new Vector3d(position.getX(), position.getY(), position.getZ());
        Vector3f targetRot = new Vector3f(position.getPitch(), position.getYaw(), 0);

        Teleport teleport = new Teleport(destWorld, targetPos, targetRot);
        store.addComponent(ref, Teleport.getComponentType(), teleport);
    }

    /**
     * Checks if a position is within the hub bounds.
     */
    public boolean isInBounds(double x, double y, double z) {
        return config.isInBounds(x, y, z);
    }

    /**
     * Checks if a player is currently in the hub bounds.
     */
    public boolean isPlayerInHub(Player player) {
        Ref<EntityStore> ref = player.getReference();
        if (ref == null) return false;

        Store<EntityStore> store = ref.getStore();
        if (store == null) return false;

        TransformComponent transform = store.getComponent(ref,
            EntityModule.get().getTransformComponentType());
        if (transform == null) return false;

        Vector3d pos = transform.getPosition();
        return config.isInBounds(pos.getX(), pos.getY(), pos.getZ());
    }

    /**
     * Gets the hub spawn position.
     */
    public Position getSpawnPoint() {
        return config.getSpawnPoint();
    }

    /**
     * Gets the current player position as a Position object.
     */
    public Position getPlayerPosition(Player player) {
        Ref<EntityStore> ref = player.getReference();
        if (ref == null) return null;

        Store<EntityStore> store = ref.getStore();
        if (store == null) return null;

        TransformComponent transform = store.getComponent(ref,
            EntityModule.get().getTransformComponentType());
        if (transform == null) return null;

        Vector3d pos = transform.getPosition();
        Vector3f rot = transform.getRotation();

        return new Position(pos.getX(), pos.getY(), pos.getZ(), rot.getY(), rot.getX());
    }

    /**
     * Heals a player to full health.
     * Must be called on the player's current world thread.
     */
    private void healPlayer(Player player) {
        try {
            Ref<EntityStore> ref = player.getReference();
            if (ref == null) return;
            Store<EntityStore> store = ref.getStore();
            if (store == null) return;

            EntityStatMap stats = store.getComponent(ref,
                EntityStatsModule.get().getEntityStatMapComponentType());
            if (stats == null) return;

            int healthIndex = EntityStatType.getAssetMap().getIndex("health");
            EntityStatValue healthStat = stats.get(healthIndex);
            if (healthStat == null) return;

            float maxHealth = healthStat.getMax();
            if (healthStat.get() < maxHealth) {
                stats.setStatValue(healthIndex, maxHealth);
            }
        } catch (Exception e) {
            System.err.println("[HubManager] Error healing player: " + e.getMessage());
        }
    }

    // ========== Hub Holograms ==========

    /**
     * Spawns all hub holograms from config.
     * First cleans up any orphaned holograms from previous sessions (survives restarts/crashes).
     * Called when the first player joins (hub world guaranteed loaded).
     */
    public void spawnHubHolograms() {
        if (hologramsSpawned) return;

        World hubWorld = getHubWorld();
        if (hubWorld == null) return;

        hologramsSpawned = true;
        List<HubConfig.HologramEntry> entries = config.getHolograms();

        hubWorld.execute(() -> {
            // Clean up any persisted holograms from previous server sessions
            int cleaned = HologramUtil.cleanupAllHolograms(hubWorld);
            if (cleaned > 0) {
                System.out.println("[HubManager] Cleaned up " + cleaned + " orphaned hologram(s) from previous session");
            }

            // Spawn fresh holograms from config
            for (HubConfig.HologramEntry entry : entries) {
                if (entry.getText() == null || entry.getText().isEmpty()) continue;
                Ref<EntityStore> ref = HologramUtil.spawnHologram(
                    hubWorld, entry.getX(), entry.getY(), entry.getZ(), entry.getText()
                );
                if (ref != null) {
                    hubHolograms.add(ref);
                }
            }
            System.out.println("[HubManager] Spawned " + hubHolograms.size() + " hub holograms");
        });
    }

    /**
     * Despawns all hub holograms.
     */
    public void despawnHubHolograms() {
        if (hubHolograms.isEmpty()) {
            hologramsSpawned = false;
            return;
        }

        World hubWorld = getHubWorld();
        if (hubWorld == null) {
            hubHolograms.clear();
            hologramsSpawned = false;
            return;
        }

        hubWorld.execute(() -> {
            for (Ref<EntityStore> ref : hubHolograms) {
                HologramUtil.despawnHologram(ref);
            }
            hubHolograms.clear();
            hologramsSpawned = false;
        });
    }

    /**
     * Despawns and re-spawns all hub holograms.
     * Called after admin edits or config reload.
     */
    public void respawnHubHolograms() {
        World hubWorld = getHubWorld();
        if (hubWorld == null) {
            hubHolograms.clear();
            hologramsSpawned = false;
            return;
        }

        hubWorld.execute(() -> {
            // Despawn existing
            for (Ref<EntityStore> ref : hubHolograms) {
                HologramUtil.despawnHologram(ref);
            }
            hubHolograms.clear();
            hologramsSpawned = false;

            // Spawn from current config
            List<HubConfig.HologramEntry> entries = config.getHolograms();
            for (HubConfig.HologramEntry entry : entries) {
                if (entry.getText() == null || entry.getText().isEmpty()) continue;
                Ref<EntityStore> ref = HologramUtil.spawnHologram(
                    hubWorld, entry.getX(), entry.getY(), entry.getZ(), entry.getText()
                );
                if (ref != null) {
                    hubHolograms.add(ref);
                }
            }
            hologramsSpawned = true;
            System.out.println("[HubManager] Respawned " + hubHolograms.size() + " hub holograms");
        });
    }
}
