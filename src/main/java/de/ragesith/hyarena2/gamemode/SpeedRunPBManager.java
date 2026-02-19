package de.ragesith.hyarena2.gamemode;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages personal best times for speedrun arenas.
 * PBs are stored as JSON files: config/speedrun/pbs/{playerUuid}/{arenaId}.json
 */
public class SpeedRunPBManager {
    private final Path pbRoot;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final ConcurrentHashMap<String, SpeedRunPB> cache = new ConcurrentHashMap<>();

    public SpeedRunPBManager(Path configRoot) {
        this.pbRoot = configRoot.resolve("speedrun").resolve("pbs");
        try {
            Files.createDirectories(pbRoot);
        } catch (IOException e) {
            System.err.println("[SpeedRunPBManager] Failed to create PB directory: " + e.getMessage());
        }
    }

    private String cacheKey(UUID playerUuid, String arenaId) {
        return playerUuid.toString() + ":" + arenaId;
    }

    /**
     * Loads the personal best for a player+arena. Returns null if no PB exists.
     */
    public SpeedRunPB loadPB(UUID playerUuid, String arenaId) {
        String key = cacheKey(playerUuid, arenaId);
        SpeedRunPB cached = cache.get(key);
        if (cached != null) return cached;

        Path file = pbRoot.resolve(playerUuid.toString()).resolve(arenaId + ".json");
        if (!Files.exists(file)) return null;

        try {
            String json = Files.readString(file);
            SpeedRunPB pb = gson.fromJson(json, SpeedRunPB.class);
            if (pb != null) {
                cache.put(key, pb);
            }
            return pb;
        } catch (Exception e) {
            System.err.println("[SpeedRunPBManager] Failed to load PB: " + e.getMessage());
            return null;
        }
    }

    /**
     * Saves a personal best asynchronously.
     */
    public void savePB(SpeedRunPB pb) {
        if (pb == null) return;
        UUID playerUuid = UUID.fromString(pb.getPlayerUuid());
        String key = cacheKey(playerUuid, pb.getArenaId());
        cache.put(key, pb);

        CompletableFuture.runAsync(() -> {
            try {
                Path playerDir = pbRoot.resolve(pb.getPlayerUuid());
                Files.createDirectories(playerDir);
                Path file = playerDir.resolve(pb.getArenaId() + ".json");
                Files.writeString(file, gson.toJson(pb));
                System.out.println("[SpeedRunPBManager] Saved PB for " + pb.getPlayerUuid()
                    + " on " + pb.getArenaId() + ": " + pb.getFormattedTime());
            } catch (IOException e) {
                System.err.println("[SpeedRunPBManager] Failed to save PB: " + e.getMessage());
            }
        });
    }

    /**
     * Checks if a time is a new personal best.
     */
    public boolean isNewPB(UUID playerUuid, String arenaId, long timeNanos) {
        SpeedRunPB existing = loadPB(playerUuid, arenaId);
        return existing == null || timeNanos < existing.getTotalTimeNanos();
    }
}
