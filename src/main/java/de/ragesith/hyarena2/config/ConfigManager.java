package de.ragesith.hyarena2.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Centralized configuration manager for HyArena2.
 * Handles loading, saving, and accessing all configuration files.
 *
 * Phase 1 loads: global.json, hub.json
 * Future phases will add: arenas/, kits/, gamemodes/
 */
public class ConfigManager {

    private static final Gson GSON = new GsonBuilder()
        .setPrettyPrinting()
        .create();

    private final Path configRoot;
    private GlobalConfig globalConfig;
    private HubConfig hubConfig;

    /**
     * Creates a new ConfigManager.
     *
     * @param configRoot The root directory for configuration files (plugin data folder)
     */
    public ConfigManager(Path configRoot) {
        this.configRoot = configRoot;
    }

    /**
     * Loads all configuration files.
     * Creates default configs if files don't exist.
     *
     * @return this ConfigManager for method chaining
     */
    public ConfigManager load() {
        ensureDirectoryExists();
        globalConfig = loadConfig("global.json", GlobalConfig.class, new GlobalConfig());
        hubConfig = loadConfig("hub.json", HubConfig.class, new HubConfig());

        System.out.println("[HyArena2] Configuration loaded from " + configRoot);
        return this;
    }

    /**
     * Reloads all configuration files from disk.
     *
     * @return this ConfigManager for method chaining
     */
    public ConfigManager reload() {
        return load();
    }

    /**
     * Saves all configuration files to disk.
     */
    public void save() {
        saveConfig("global.json", globalConfig);
        saveConfig("hub.json", hubConfig);
        System.out.println("[HyArena2] Configuration saved to " + configRoot);
    }

    // ========== Config Accessors ==========

    /**
     * Gets the global configuration.
     */
    public GlobalConfig getGlobalConfig() {
        return globalConfig;
    }

    /**
     * Gets the hub configuration.
     */
    public HubConfig getHubConfig() {
        return hubConfig;
    }

    /**
     * Gets the configuration root directory.
     */
    public Path getConfigRoot() {
        return configRoot;
    }

    /**
     * Loads a config file of the specified type.
     * Returns null if file doesn't exist or fails to load.
     *
     * @param filename The config file path relative to config root (e.g., "arenas/test_duel")
     * @param type The class type to deserialize to
     * @return The loaded config, or null if not found
     */
    public <T> T loadConfig(String filename, Class<T> type) {
        // Ensure .json extension
        String fullPath = filename.endsWith(".json") ? filename : filename + ".json";
        Path file = configRoot.resolve(fullPath);

        if (!Files.exists(file)) {
            return null;
        }

        try (Reader reader = Files.newBufferedReader(file)) {
            return GSON.fromJson(reader, type);
        } catch (IOException e) {
            System.err.println("[HyArena2] Failed to load " + fullPath + ": " + e.getMessage());
            return null;
        } catch (Exception e) {
            System.err.println("[HyArena2] Error parsing " + fullPath + ": " + e.getMessage());
            return null;
        }
    }

    // ========== Private Helpers ==========

    private void ensureDirectoryExists() {
        try {
            Files.createDirectories(configRoot);
        } catch (IOException e) {
            System.err.println("[HyArena2] Failed to create config directory: " + e.getMessage());
        }
    }

    private <T> T loadConfig(String filename, Class<T> type, T defaultValue) {
        Path file = configRoot.resolve(filename);

        if (!Files.exists(file)) {
            // Create default config file
            saveConfig(filename, defaultValue);
            System.out.println("[HyArena2] Created default " + filename);
            return defaultValue;
        }

        try (Reader reader = Files.newBufferedReader(file)) {
            T config = GSON.fromJson(reader, type);
            if (config == null) {
                System.err.println("[HyArena2] Failed to parse " + filename + ", using defaults");
                return defaultValue;
            }
            System.out.println("[HyArena2] Loaded " + filename);
            return config;
        } catch (IOException e) {
            System.err.println("[HyArena2] Failed to load " + filename + ": " + e.getMessage());
            return defaultValue;
        } catch (Exception e) {
            System.err.println("[HyArena2] Error parsing " + filename + ": " + e.getMessage());
            return defaultValue;
        }
    }

    private <T> void saveConfig(String filename, T config) {
        Path file = configRoot.resolve(filename);

        try {
            Files.createDirectories(file.getParent());
            try (Writer writer = Files.newBufferedWriter(file)) {
                GSON.toJson(config, writer);
            }
        } catch (IOException e) {
            System.err.println("[HyArena2] Failed to save " + filename + ": " + e.getMessage());
        }
    }
}
