package de.ragesith.hyarena2.economy;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages per-player economy data persistence.
 * In-memory cache backed by JSON files in data/players/.
 */
public class PlayerDataManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type TRANSACTION_LIST_TYPE = new TypeToken<List<TransactionRecord>>() {}.getType();

    private final Path playersDir;
    private final ConcurrentHashMap<UUID, PlayerEconomyData> cache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Object> transactionLocks = new ConcurrentHashMap<>();

    public PlayerDataManager(Path pluginDataRoot) {
        this.playersDir = pluginDataRoot.resolve("data").resolve("players");
        try {
            Files.createDirectories(playersDir);
        } catch (IOException e) {
            System.err.println("[PlayerDataManager] Failed to create players data directory: " + e.getMessage());
        }
    }

    /**
     * Loads player data from file, or creates a new entry if none exists.
     */
    public PlayerEconomyData loadOrCreate(UUID uuid, String name) {
        // Check cache first
        PlayerEconomyData existing = cache.get(uuid);
        if (existing != null) {
            existing.setPlayerName(name);
            existing.setLastOnlineTimestamp(System.currentTimeMillis());
            return existing;
        }

        // Try to load from file
        Path file = playersDir.resolve(uuid.toString() + ".json");
        if (Files.exists(file)) {
            try (Reader reader = Files.newBufferedReader(file)) {
                PlayerEconomyData data = GSON.fromJson(reader, PlayerEconomyData.class);
                if (data != null) {
                    data.setPlayerName(name);
                    data.setLastOnlineTimestamp(System.currentTimeMillis());
                    cache.put(uuid, data);
                    System.out.println("[PlayerDataManager] Loaded economy data for " + name);
                    return data;
                }
            } catch (Exception e) {
                System.err.println("[PlayerDataManager] Failed to load data for " + uuid + ": " + e.getMessage());
            }
        }

        // Create new
        PlayerEconomyData data = new PlayerEconomyData(uuid, name);
        cache.put(uuid, data);
        System.out.println("[PlayerDataManager] Created new economy data for " + name);
        return data;
    }

    /**
     * Saves a player's data to file asynchronously.
     */
    public void save(UUID uuid) {
        PlayerEconomyData data = cache.get(uuid);
        if (data == null) return;

        CompletableFuture.runAsync(() -> {
            Object lock = transactionLocks.computeIfAbsent(uuid, k -> new Object());
            synchronized (lock) {
                saveSync(uuid, data);
            }
        });
    }

    /**
     * Saves all cached player data to files.
     */
    public void saveAll() {
        for (var entry : cache.entrySet()) {
            saveSync(entry.getKey(), entry.getValue());
        }
        System.out.println("[PlayerDataManager] Saved all player economy data (" + cache.size() + " players)");
    }

    /**
     * Gets cached data for a player, or null if not loaded.
     */
    public PlayerEconomyData getData(UUID uuid) {
        return cache.get(uuid);
    }

    /**
     * Saves and removes a player from the cache.
     */
    public void unloadPlayer(UUID uuid) {
        PlayerEconomyData data = cache.remove(uuid);
        if (data != null) {
            saveSync(uuid, data);
            System.out.println("[PlayerDataManager] Unloaded economy data for " + data.getPlayerName());
        }
    }

    /**
     * Appends a transaction record to the player's transaction log file.
     */
    public void logTransaction(UUID uuid, TransactionRecord record) {
        CompletableFuture.runAsync(() -> {
            Object lock = transactionLocks.computeIfAbsent(uuid, k -> new Object());
            synchronized (lock) {
                Path file = playersDir.resolve(uuid.toString() + "_transactions.json");
                try {
                    List<TransactionRecord> records;
                    if (Files.exists(file)) {
                        try (Reader reader = Files.newBufferedReader(file)) {
                            records = GSON.fromJson(reader, TRANSACTION_LIST_TYPE);
                            if (records == null) records = new ArrayList<>();
                        }
                    } else {
                        records = new ArrayList<>();
                    }
                    records.add(record);
                    try (Writer writer = Files.newBufferedWriter(file)) {
                        GSON.toJson(records, writer);
                    }
                } catch (Exception e) {
                    System.err.println("[PlayerDataManager] Failed to log transaction for " + uuid + ": " + e.getMessage());
                }
            }
        });
    }

    /**
     * Gets all loaded player data for bulk operations (e.g., decay tick).
     */
    public Collection<PlayerEconomyData> getAllLoadedPlayers() {
        return cache.values();
    }

    /**
     * Synchronously writes player data to file.
     */
    private void saveSync(UUID uuid, PlayerEconomyData data) {
        Path file = playersDir.resolve(uuid.toString() + ".json");
        try (Writer writer = Files.newBufferedWriter(file)) {
            GSON.toJson(data, writer);
        } catch (IOException e) {
            System.err.println("[PlayerDataManager] Failed to save data for " + uuid + ": " + e.getMessage());
        }
    }
}
