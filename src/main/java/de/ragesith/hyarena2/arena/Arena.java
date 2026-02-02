package de.ragesith.hyarena2.arena;

import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import de.ragesith.hyarena2.arena.ArenaConfig.SpawnPoint;

import java.util.List;

/**
 * Wrapper around ArenaConfig that provides runtime world access.
 * Does not store World references long-term (fetches fresh from Universe).
 */
public class Arena {
    private final ArenaConfig config;

    public Arena(ArenaConfig config) {
        this.config = config;
    }

    public ArenaConfig getConfig() {
        return config;
    }

    public String getId() {
        return config.getId();
    }

    public String getDisplayName() {
        return config.getDisplayName();
    }

    public String getGameMode() {
        return config.getGameMode();
    }

    public int getMinPlayers() {
        return config.getMinPlayers();
    }

    public int getMaxPlayers() {
        return config.getMaxPlayers();
    }

    public int getWaitTimeSeconds() {
        return config.getWaitTimeSeconds();
    }

    public List<SpawnPoint> getSpawnPoints() {
        return config.getSpawnPoints();
    }

    public ArenaConfig.Bounds getBounds() {
        return config.getBounds();
    }

    /**
     * Gets a fresh World reference from the Universe.
     * @return The world, or null if not loaded
     */
    public World getWorld() {
        return Universe.get().getWorld(config.getWorldName());
    }

    /**
     * Executes a task on the arena's world thread.
     * @param task The task to execute
     */
    public void executeOnWorld(Runnable task) {
        World world = getWorld();
        if (world != null) {
            world.execute(task);
        }
    }

    /**
     * Checks if a position is within the arena bounds
     */
    public boolean isInBounds(double x, double y, double z) {
        return config.getBounds().contains(x, y, z);
    }
}
