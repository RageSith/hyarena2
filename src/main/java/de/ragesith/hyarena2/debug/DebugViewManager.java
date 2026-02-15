package de.ragesith.hyarena2.debug;

import com.hypixel.hytale.math.matrix.Matrix4d;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.protocol.DebugShape;
import com.hypixel.hytale.protocol.packets.player.ClearDebugShapes;
import com.hypixel.hytale.protocol.packets.player.DisplayDebug;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import de.ragesith.hyarena2.arena.Arena;
import de.ragesith.hyarena2.arena.ArenaConfig;
import de.ragesith.hyarena2.arena.MatchManager;
import de.ragesith.hyarena2.config.BoundingBox;
import de.ragesith.hyarena2.config.ConfigManager;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages per-player debug visualization layers.
 * Renders wireframe overlays for arena bounds, spawn points, capture zones, and barrier blocks.
 */
public class DebugViewManager {

    // Fixed type colors (protocol Vector3f)
    private static final com.hypixel.hytale.protocol.Vector3f COLOR_BARRIER_DEFAULT = new com.hypixel.hytale.protocol.Vector3f(1.0f, 0.0f, 0.0f);
    private static final com.hypixel.hytale.protocol.Vector3f COLOR_ARENA_BOUNDS = new com.hypixel.hytale.protocol.Vector3f(1.0f, 1.0f, 0.0f);
    private static final com.hypixel.hytale.protocol.Vector3f COLOR_HUB_BOUNDARY = new com.hypixel.hytale.protocol.Vector3f(0.0f, 1.0f, 1.0f);
    private static final com.hypixel.hytale.protocol.Vector3f COLOR_KOTH_ZONE = new com.hypixel.hytale.protocol.Vector3f(1.0f, 0.0f, 1.0f);
    private static final com.hypixel.hytale.protocol.Vector3f COLOR_PLAYER_SPAWN = new com.hypixel.hytale.protocol.Vector3f(0.0f, 1.0f, 0.0f);
    private static final com.hypixel.hytale.protocol.Vector3f COLOR_WAVE_SPAWN = new com.hypixel.hytale.protocol.Vector3f(1.0f, 0.5f, 0.0f);

    private static final float EDGE_THICKNESS = 0.05f;
    private static final float SHAPE_DURATION = 2.0f;
    private static final float MARKER_SIZE = 0.5f;
    private static final int SCAN_RADIUS = 32;
    private static final int MAX_BARRIER_RENDERS = 200;

    // Rainbow hue cycle colors (8 steps)
    private static final com.hypixel.hytale.protocol.Vector3f[] RAINBOW = {
        new com.hypixel.hytale.protocol.Vector3f(1.0f, 0.0f, 0.0f),
        new com.hypixel.hytale.protocol.Vector3f(1.0f, 0.5f, 0.0f),
        new com.hypixel.hytale.protocol.Vector3f(1.0f, 1.0f, 0.0f),
        new com.hypixel.hytale.protocol.Vector3f(0.0f, 1.0f, 0.0f),
        new com.hypixel.hytale.protocol.Vector3f(0.0f, 1.0f, 1.0f),
        new com.hypixel.hytale.protocol.Vector3f(0.0f, 0.0f, 1.0f),
        new com.hypixel.hytale.protocol.Vector3f(0.5f, 0.0f, 1.0f),
        new com.hypixel.hytale.protocol.Vector3f(1.0f, 0.0f, 0.5f),
    };

    // Per-player state
    private final Map<UUID, EnumSet<DebugLayer>> enabledLayers = new ConcurrentHashMap<>();
    private final Map<UUID, com.hypixel.hytale.protocol.Vector3f> barrierColors = new ConcurrentHashMap<>();
    private final Set<UUID> rainbowPlayers = ConcurrentHashMap.newKeySet();

    private final ConfigManager configManager;
    private final MatchManager matchManager;

    private long tickCount = 0;

    public DebugViewManager(ConfigManager configManager, MatchManager matchManager) {
        this.configManager = configManager;
        this.matchManager = matchManager;
    }

    // ========== Toggle Methods ==========

    /**
     * Toggles a single layer for a player. Returns true if the layer is now ON.
     */
    public boolean toggleLayer(UUID playerId, DebugLayer layer) {
        EnumSet<DebugLayer> layers = enabledLayers.computeIfAbsent(playerId, k -> EnumSet.noneOf(DebugLayer.class));
        if (layers.contains(layer)) {
            layers.remove(layer);
            if (layers.isEmpty()) {
                clearShapes(playerId);
            }
            return false;
        } else {
            layers.add(layer);
            return true;
        }
    }

    /**
     * Toggles all layers on/off. If any are on, turns all off. If all off, turns all on.
     * Returns true if layers are now ON.
     */
    public boolean toggleAll(UUID playerId) {
        EnumSet<DebugLayer> layers = enabledLayers.computeIfAbsent(playerId, k -> EnumSet.noneOf(DebugLayer.class));
        if (layers.isEmpty()) {
            layers.addAll(EnumSet.allOf(DebugLayer.class));
            return true;
        } else {
            layers.clear();
            clearShapes(playerId);
            return false;
        }
    }

    /**
     * Sets custom barrier color for a player.
     */
    public void setBarrierColor(UUID playerId, com.hypixel.hytale.protocol.Vector3f color) {
        barrierColors.put(playerId, color);
        rainbowPlayers.remove(playerId);
    }

    /**
     * Toggles rainbow mode for barriers. Returns true if now ON.
     */
    public boolean toggleRainbow(UUID playerId) {
        if (rainbowPlayers.contains(playerId)) {
            rainbowPlayers.remove(playerId);
            return false;
        } else {
            rainbowPlayers.add(playerId);
            return true;
        }
    }

    /**
     * Gets the currently enabled layers for a player.
     */
    public EnumSet<DebugLayer> getEnabledLayers(UUID playerId) {
        return enabledLayers.getOrDefault(playerId, EnumSet.noneOf(DebugLayer.class));
    }

    /**
     * Checks if rainbow mode is active for a player.
     */
    public boolean isRainbow(UUID playerId) {
        return rainbowPlayers.contains(playerId);
    }

    /**
     * Gets the custom barrier color name for status display.
     */
    public String getBarrierColorName(UUID playerId) {
        if (rainbowPlayers.contains(playerId)) return "Rainbow";
        com.hypixel.hytale.protocol.Vector3f color = barrierColors.get(playerId);
        if (color == null) return "Red";
        return colorToHex(color);
    }

    // ========== Tick ==========

    /**
     * Called every 1.5s from the scheduler. Renders debug shapes for all active players.
     * Iterates all known worlds and detects the player's current world via entity store comparison.
     */
    public void tick() {
        tickCount++;

        if (enabledLayers.isEmpty()) return;

        Set<String> knownWorlds = getKnownWorldNames();

        for (Map.Entry<UUID, EnumSet<DebugLayer>> entry : enabledLayers.entrySet()) {
            UUID playerId = entry.getKey();
            EnumSet<DebugLayer> layers = entry.getValue();
            if (layers.isEmpty()) continue;

            PlayerRef playerRef = Universe.get().getPlayer(playerId);
            if (playerRef == null) continue;

            // Try each known world — dispatch to its thread and check if the player is there
            for (String worldName : knownWorlds) {
                World world = Universe.get().getWorld(worldName);
                if (world == null) continue;

                final String wn = worldName;
                world.execute(() -> {
                    try {
                        if (!isPlayerInWorld(playerRef, world)) return;

                        // Player confirmed in this world — render active layers
                        if (layers.contains(DebugLayer.ZONES)) {
                            renderZones(playerId, playerRef, wn);
                        }
                        if (layers.contains(DebugLayer.SPAWNS)) {
                            renderSpawns(playerId, playerRef, wn);
                        }
                        if (layers.contains(DebugLayer.BARRIERS)) {
                            renderBarriers(playerId, playerRef, world);
                        }
                    } catch (Exception e) {
                        // Silently ignore — player may have disconnected or world changed
                    }
                });
            }
        }
    }

    // ========== World Detection ==========

    /**
     * Checks if a player's entity belongs to the given world by comparing entity stores.
     * Must be called on the world's thread.
     */
    private boolean isPlayerInWorld(PlayerRef playerRef, World world) {
        var ref = playerRef.getReference();
        if (ref == null) return false;
        var playerStore = ref.getStore();
        if (playerStore == null) return false;
        var worldStore = world.getEntityStore().getStore();
        return playerStore == worldStore;
    }

    /**
     * Collects all unique world names from hub config and arena configs.
     */
    private Set<String> getKnownWorldNames() {
        Set<String> names = new HashSet<>();
        names.add(configManager.getHubConfig().getEffectiveWorldName());
        for (Arena arena : matchManager.getArenas()) {
            names.add(arena.getConfig().getWorldName());
        }
        return names;
    }

    // ========== Zone Rendering ==========

    private void renderZones(UUID playerId, PlayerRef playerRef, String worldName) {
        // Arena bounds (yellow) — all arenas in the same world
        for (Arena arena : matchManager.getArenas()) {
            ArenaConfig config = arena.getConfig();
            if (!worldName.equals(config.getWorldName())) continue;

            ArenaConfig.Bounds bounds = config.getBounds();
            if (bounds == null) continue;

            renderWireframeBox(playerRef,
                bounds.getMinX(), bounds.getMinY(), bounds.getMinZ(),
                bounds.getMaxX(), bounds.getMaxY(), bounds.getMaxZ(),
                COLOR_ARENA_BOUNDS);

            // KOTH capture zones (magenta)
            if ("koth".equals(config.getGameMode()) && config.getCaptureZones() != null) {
                for (ArenaConfig.CaptureZone zone : config.getCaptureZones()) {
                    renderWireframeBox(playerRef,
                        zone.getMinX(), zone.getMinY(), zone.getMinZ(),
                        zone.getMaxX(), zone.getMaxY(), zone.getMaxZ(),
                        COLOR_KOTH_ZONE);
                }
            }
        }

        // Hub boundary (cyan) — only if player is in hub world
        String hubWorldName = configManager.getHubConfig().getEffectiveWorldName();
        if (worldName.equals(hubWorldName)) {
            BoundingBox hubBounds = configManager.getHubConfig().getBounds();
            if (hubBounds != null && hubBounds.isValid()) {
                renderWireframeBox(playerRef,
                    hubBounds.getMinX(), hubBounds.getMinY(), hubBounds.getMinZ(),
                    hubBounds.getMaxX(), hubBounds.getMaxY(), hubBounds.getMaxZ(),
                    COLOR_HUB_BOUNDARY);
            }
        }
    }

    // ========== Spawn Rendering ==========

    private void renderSpawns(UUID playerId, PlayerRef playerRef, String worldName) {
        for (Arena arena : matchManager.getArenas()) {
            ArenaConfig config = arena.getConfig();
            if (!worldName.equals(config.getWorldName())) continue;

            // Player spawn points (green)
            if (config.getSpawnPoints() != null) {
                for (ArenaConfig.SpawnPoint sp : config.getSpawnPoints()) {
                    renderSmallMarker(playerRef, sp.getX(), sp.getY(), sp.getZ(), COLOR_PLAYER_SPAWN);
                }
            }

            // Wave spawn points (orange)
            if (config.getWaveSpawnPoints() != null) {
                for (ArenaConfig.SpawnPoint sp : config.getWaveSpawnPoints()) {
                    renderSmallMarker(playerRef, sp.getX(), sp.getY(), sp.getZ(), COLOR_WAVE_SPAWN);
                }
            }
        }
    }

    // ========== Barrier Rendering ==========

    /**
     * Scans for barrier blocks around the player and renders a single cube per block.
     * Must run on the world thread.
     */
    private void renderBarriers(UUID playerId, PlayerRef playerRef, World world) {
        var ref = playerRef.getReference();
        if (ref == null) return;
        var store = ref.getStore();
        if (store == null) return;

        TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
        if (transform == null) return;
        Vector3d pos = transform.getPosition();

        int cx = (int) Math.floor(pos.getX());
        int cy = (int) Math.floor(pos.getY());
        int cz = (int) Math.floor(pos.getZ());

        // Determine barrier color
        com.hypixel.hytale.protocol.Vector3f color;
        if (rainbowPlayers.contains(playerId)) {
            color = RAINBOW[(int) (tickCount % RAINBOW.length)];
        } else {
            color = barrierColors.getOrDefault(playerId, COLOR_BARRIER_DEFAULT);
        }

        // Scan blocks in radius, render a single cube per barrier block
        int rendered = 0;
        for (int x = cx - SCAN_RADIUS; x <= cx + SCAN_RADIUS && rendered < MAX_BARRIER_RENDERS; x++) {
            for (int z = cz - SCAN_RADIUS; z <= cz + SCAN_RADIUS && rendered < MAX_BARRIER_RENDERS; z++) {
                for (int y = cy - SCAN_RADIUS; y <= cy + SCAN_RADIUS && rendered < MAX_BARRIER_RENDERS; y++) {
                    var blockType = world.getBlockType(x, y, z);
                    if (blockType == null) continue;
                    if (!blockType.getId().toString().contains("Barrier")) continue;

                    renderSmallMarker(playerRef, x + 0.5, y + 0.5, z + 0.5, 1.0f, color);
                    rendered++;
                }
            }
        }
    }

    // ========== Wireframe Helpers ==========

    /**
     * Renders a 12-edge wireframe box (extracted from KOTH pattern).
     */
    private void renderWireframeBox(PlayerRef playerRef,
                                     double minX, double minY, double minZ,
                                     double maxX, double maxY, double maxZ,
                                     com.hypixel.hytale.protocol.Vector3f color) {
        double x1 = Math.floor(Math.min(minX, maxX));
        double x2 = Math.ceil(Math.max(minX, maxX));
        double y1 = Math.floor(Math.min(minY, maxY));
        double y2 = Math.ceil(Math.max(minY, maxY));
        double z1 = Math.floor(Math.min(minZ, maxZ));
        double z2 = Math.ceil(Math.max(minZ, maxZ));

        double centerX = (x1 + x2) / 2.0;
        double centerY = (y1 + y2) / 2.0;
        double centerZ = (z1 + z2) / 2.0;
        double sizeX = x2 - x1;
        double sizeY = y2 - y1;
        double sizeZ = z2 - z1;
        double t = EDGE_THICKNESS;

        double extX = sizeX + t;
        double extZ = sizeZ + t;

        float[][] edgeMatrices = new float[12][];

        // Bottom edges (y1)
        Matrix4d m = new Matrix4d().identity();
        m.translate(centerX, y1, z1); m.scale(extX, t, t);
        edgeMatrices[0] = m.asFloatData();

        m = new Matrix4d().identity();
        m.translate(centerX, y1, z2); m.scale(extX, t, t);
        edgeMatrices[1] = m.asFloatData();

        m = new Matrix4d().identity();
        m.translate(x1, y1, centerZ); m.scale(t, t, extZ);
        edgeMatrices[2] = m.asFloatData();

        m = new Matrix4d().identity();
        m.translate(x2, y1, centerZ); m.scale(t, t, extZ);
        edgeMatrices[3] = m.asFloatData();

        // Top edges (y2)
        m = new Matrix4d().identity();
        m.translate(centerX, y2, z1); m.scale(extX, t, t);
        edgeMatrices[4] = m.asFloatData();

        m = new Matrix4d().identity();
        m.translate(centerX, y2, z2); m.scale(extX, t, t);
        edgeMatrices[5] = m.asFloatData();

        m = new Matrix4d().identity();
        m.translate(x1, y2, centerZ); m.scale(t, t, extZ);
        edgeMatrices[6] = m.asFloatData();

        m = new Matrix4d().identity();
        m.translate(x2, y2, centerZ); m.scale(t, t, extZ);
        edgeMatrices[7] = m.asFloatData();

        // Vertical edges (4 corners)
        m = new Matrix4d().identity();
        m.translate(x1, centerY, z1); m.scale(t, sizeY, t);
        edgeMatrices[8] = m.asFloatData();

        m = new Matrix4d().identity();
        m.translate(x2, centerY, z1); m.scale(t, sizeY, t);
        edgeMatrices[9] = m.asFloatData();

        m = new Matrix4d().identity();
        m.translate(x1, centerY, z2); m.scale(t, sizeY, t);
        edgeMatrices[10] = m.asFloatData();

        m = new Matrix4d().identity();
        m.translate(x2, centerY, z2); m.scale(t, sizeY, t);
        edgeMatrices[11] = m.asFloatData();

        sendEdges(playerRef, edgeMatrices, color);
    }

    /**
     * Renders a marker cube at a position with default MARKER_SIZE.
     */
    private void renderSmallMarker(PlayerRef playerRef, double x, double y, double z,
                                    com.hypixel.hytale.protocol.Vector3f color) {
        renderSmallMarker(playerRef, x, y, z, MARKER_SIZE, color);
    }

    /**
     * Renders a marker cube at a position with custom size.
     */
    private void renderSmallMarker(PlayerRef playerRef, double x, double y, double z,
                                    float size, com.hypixel.hytale.protocol.Vector3f color) {
        Matrix4d m = new Matrix4d().identity();
        m.translate(x, y, z);
        m.scale(size, size, size);

        try {
            DisplayDebug packet = new DisplayDebug();
            packet.shape = DebugShape.Cube;
            packet.matrix = m.asFloatData();
            packet.color = color;
            packet.time = SHAPE_DURATION;
            packet.fade = true;
            packet.frustumProjection = null;
            playerRef.getPacketHandler().write(packet);
        } catch (Exception e) {
            // Silently ignore
        }
    }

    /**
     * Sends edge matrices as DisplayDebug packets to a player.
     */
    private void sendEdges(PlayerRef playerRef, float[][] edgeMatrices,
                           com.hypixel.hytale.protocol.Vector3f color) {
        try {
            for (float[] edgeMatrix : edgeMatrices) {
                DisplayDebug packet = new DisplayDebug();
                packet.shape = DebugShape.Cube;
                packet.matrix = edgeMatrix;
                packet.color = color;
                packet.time = SHAPE_DURATION;
                packet.fade = true;
                packet.frustumProjection = null;
                playerRef.getPacketHandler().write(packet);
            }
        } catch (Exception e) {
            // Silently ignore — player may have disconnected
        }
    }

    // ========== Cleanup ==========

    /**
     * Clears all debug shapes for a player.
     */
    private void clearShapes(UUID playerId) {
        PlayerRef playerRef = Universe.get().getPlayer(playerId);
        if (playerRef == null) return;
        try {
            playerRef.getPacketHandler().write(new ClearDebugShapes());
        } catch (Exception e) {
            // Silently ignore
        }
    }

    /**
     * Removes all state for a disconnecting player.
     */
    public void handlePlayerDisconnect(UUID playerId) {
        enabledLayers.remove(playerId);
        barrierColors.remove(playerId);
        rainbowPlayers.remove(playerId);
    }

    // ========== Color Utilities ==========

    /**
     * Parses a hex color string (e.g., "#FF0000") into a protocol Vector3f.
     * Returns null if invalid.
     */
    public static com.hypixel.hytale.protocol.Vector3f parseHexColor(String hex) {
        if (hex == null) return null;
        hex = hex.startsWith("#") ? hex.substring(1) : hex;
        if (hex.length() != 6) return null;
        try {
            int r = Integer.parseInt(hex.substring(0, 2), 16);
            int g = Integer.parseInt(hex.substring(2, 4), 16);
            int b = Integer.parseInt(hex.substring(4, 6), 16);
            return new com.hypixel.hytale.protocol.Vector3f(r / 255.0f, g / 255.0f, b / 255.0f);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Resolves a preset color name or hex string to a Vector3f. Returns null if invalid.
     */
    public static com.hypixel.hytale.protocol.Vector3f resolveColor(String input) {
        if (input == null) return null;
        switch (input.toLowerCase()) {
            case "red":     return new com.hypixel.hytale.protocol.Vector3f(1.0f, 0.0f, 0.0f);
            case "green":   return new com.hypixel.hytale.protocol.Vector3f(0.0f, 1.0f, 0.0f);
            case "blue":    return new com.hypixel.hytale.protocol.Vector3f(0.0f, 0.0f, 1.0f);
            case "yellow":  return new com.hypixel.hytale.protocol.Vector3f(1.0f, 1.0f, 0.0f);
            case "cyan":    return new com.hypixel.hytale.protocol.Vector3f(0.0f, 1.0f, 1.0f);
            case "magenta": return new com.hypixel.hytale.protocol.Vector3f(1.0f, 0.0f, 1.0f);
            case "orange":  return new com.hypixel.hytale.protocol.Vector3f(1.0f, 0.5f, 0.0f);
            case "white":   return new com.hypixel.hytale.protocol.Vector3f(1.0f, 1.0f, 1.0f);
            default:        return parseHexColor(input);
        }
    }

    private static String colorToHex(com.hypixel.hytale.protocol.Vector3f c) {
        int r = Math.round(c.x * 255);
        int g = Math.round(c.y * 255);
        int b = Math.round(c.z * 255);
        return String.format("#%02X%02X%02X", r, g, b);
    }
}
