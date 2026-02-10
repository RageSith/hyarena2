package de.ragesith.hyarena2.gamemode;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.matrix.Matrix4d;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.protocol.DebugShape;
import com.hypixel.hytale.protocol.packets.player.ClearDebugShapes;
import com.hypixel.hytale.protocol.packets.player.DisplayDebug;
import com.hypixel.hytale.protocol.packets.interface_.NotificationStyle;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.modules.entity.EntityModule;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.NotificationUtil;
import de.ragesith.hyarena2.arena.ArenaConfig;
import de.ragesith.hyarena2.bot.BotObjective;
import de.ragesith.hyarena2.bot.BotParticipant;
import de.ragesith.hyarena2.config.Position;
import de.ragesith.hyarena2.participant.Participant;
import de.ragesith.hyarena2.participant.ParticipantType;
import de.ragesith.hyarena2.utils.HologramUtil;

import java.util.*;
import java.util.stream.Collectors;

/**
 * King of the Hill game mode.
 * Players fight over a capture zone — standing alone in it earns points over time.
 * First to the score target wins. Multiple zones rotate on a timer.
 */
public class KingOfTheHillGameMode implements GameMode {
    private static final String ID = "koth";
    private static final String DISPLAY_NAME = "King of the Hill";

    // Zone visualization colors (per-player perspective) — uses protocol Vector3f for DisplayDebug packet
    private static final com.hypixel.hytale.protocol.Vector3f COLOR_UNCLAIMED = new com.hypixel.hytale.protocol.Vector3f(0.5f, 0.5f, 0.5f);
    private static final com.hypixel.hytale.protocol.Vector3f COLOR_HOLDING = new com.hypixel.hytale.protocol.Vector3f(0.0f, 1.0f, 0.0f);
    private static final com.hypixel.hytale.protocol.Vector3f COLOR_ENEMY = new com.hypixel.hytale.protocol.Vector3f(1.0f, 0.0f, 0.0f);
    private static final com.hypixel.hytale.protocol.Vector3f COLOR_CONTESTED = new com.hypixel.hytale.protocol.Vector3f(1.0f, 0.65f, 0.0f);
    private static final float SHAPE_DURATION = 1.5f;
    private static final double EDGE_THICKNESS = 0.03;

    private final Map<UUID, Integer> controlTicks = new HashMap<>();
    private final List<Ref<EntityStore>> zoneNameHolograms = new ArrayList<>();
    private final List<UUID> participantsInZoneLive = new ArrayList<>();
    private final Set<UUID> previouslyInZone = new HashSet<>();
    private String worldName;
    private int activeZoneIndex = 0;
    private UUID currentController = null;
    private boolean contested = false;

    // Hysteresis margin: participants already inside must move this far beyond the boundary to count as "outside"
    private static final double ZONE_EXIT_MARGIN = 0.75;

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getDisplayName() {
        return DISPLAY_NAME;
    }

    @Override
    public String getDescription() {
        return "Group { LayoutMode: Top;"
            + " Label { Text: \"King of the Hill\"; Anchor: (Height: 28); Style: (FontSize: 18, TextColor: #e8c872, RenderBold: true); }"
            + " Label { Text: \"Fight over a capture zone. Stand alone on the hill to earn points. First to the score target wins.\"; Anchor: (Height: 40, Top: 4); Style: (FontSize: 13, TextColor: #b7cedd, Wrap: true); }"
            + " Label { Text: \"Rules\"; Anchor: (Height: 24, Top: 12); Style: (FontSize: 15, TextColor: #e8c872, RenderBold: true); }"
            + " Label { Text: \"\u2022 Stand alone in the capture zone to earn points\"; Anchor: (Height: 18, Top: 4); Style: (FontSize: 12, TextColor: #96a9be); }"
            + " Label { Text: \"\u2022 If multiple players are in the zone, it is contested\"; Anchor: (Height: 18, Top: 2); Style: (FontSize: 12, TextColor: #96a9be); }"
            + " Label { Text: \"\u2022 First player to reach the score target wins\"; Anchor: (Height: 18, Top: 2); Style: (FontSize: 12, TextColor: #96a9be); }"
            + " Label { Text: \"\u2022 Players respawn after death\"; Anchor: (Height: 18, Top: 2); Style: (FontSize: 12, TextColor: #96a9be); }"
            + " Label { Text: \"\u2022 Multiple zones may rotate on a timer\"; Anchor: (Height: 18, Top: 2); Style: (FontSize: 12, TextColor: #96a9be); }"
            + " Label { Text: \"Tips\"; Anchor: (Height: 24, Top: 12); Style: (FontSize: 15, TextColor: #e8c872, RenderBold: true); }"
            + " Label { Text: \"\u2022 Control the hill \u2014 kills alone won't win the game\"; Anchor: (Height: 18, Top: 4); Style: (FontSize: 12, TextColor: #96a9be); }"
            + " Label { Text: \"\u2022 Push enemies off the zone to stop their scoring\"; Anchor: (Height: 18, Top: 2); Style: (FontSize: 12, TextColor: #96a9be); }"
            + " Label { Text: \"\u2022 Watch for zone rotations and reposition quickly\"; Anchor: (Height: 18, Top: 2); Style: (FontSize: 12, TextColor: #96a9be); }"
            + " }";
    }

    @Override
    public void onMatchStart(ArenaConfig config, List<Participant> participants) {
        controlTicks.clear();
        previouslyInZone.clear();
        activeZoneIndex = 0;
        currentController = null;
        contested = false;

        for (Participant p : participants) {
            p.setAlive(true);
            controlTicks.put(p.getUniqueId(), 0);
        }
    }

    @Override
    public BotObjective getBotObjective(ArenaConfig config) {
        List<ArenaConfig.CaptureZone> zones = config.getCaptureZones();
        if (zones == null || zones.isEmpty()) return null;

        ArenaConfig.CaptureZone zone = zones.get(activeZoneIndex);
        double x1 = Math.min(zone.getMinX(), zone.getMaxX());
        double x2 = Math.max(zone.getMinX(), zone.getMaxX());
        double y1 = Math.min(zone.getMinY(), zone.getMaxY());
        double y2 = Math.max(zone.getMinY(), zone.getMaxY());
        double z1 = Math.min(zone.getMinZ(), zone.getMaxZ());
        double z2 = Math.max(zone.getMinZ(), zone.getMaxZ());

        double cx = (x1 + x2) / 2.0;
        double cy = y1; // Ground level
        double cz = (z1 + z2) / 2.0;
        double rx = (x2 - x1) / 2.0;

        return new BotObjective(new Position(cx, cy, cz), rx, "capture", x1, y1, z1, x2, y2, z2,
            currentController, contested, List.copyOf(participantsInZoneLive));
    }

    @Override
    public void onGameplayBegin(ArenaConfig config, List<Participant> participants) {
        List<ArenaConfig.CaptureZone> zones = config.getCaptureZones();
        String zoneName = (zones != null && !zones.isEmpty()) ? zones.get(0).getDisplayName() : "the hill";
        this.worldName = config.getWorldName();

        for (Participant p : participants) {
            p.sendMessage("<gradient:#2ecc71:#27ae60><b>FIGHT!</b></gradient>");
            p.sendMessage("<color:#f1c40f>First to " + config.getScoreTarget() + " score wins!</color>");
            if (zones != null && zones.size() > 1) {
                p.sendMessage("<color:#e8c872>Active zone: " + zoneName + "</color>");
            }
        }

        // Spawn zone name holograms above each capture zone
        spawnZoneNameHolograms(config);
    }

    @Override
    public void onTick(ArenaConfig config, List<Participant> participants, int tickCount) {
        List<ArenaConfig.CaptureZone> zones = config.getCaptureZones();
        if (zones == null || zones.isEmpty()) {
            return;
        }

        // Zone rotation warning (5 seconds before)
        int rotationTicks = config.getZoneRotationSeconds() * 20;
        if (zones.size() > 1 && tickCount > 0 && (tickCount + 100) % rotationTicks == 0) {
            showZoneStatus(participants, "Get ready!", "Zone changing in 5s");
        }

        // Zone rotation
        if (zones.size() > 1 && tickCount > 0 && tickCount % rotationTicks == 0) {
            activeZoneIndex = (activeZoneIndex + 1) % zones.size();
            String newZoneName = zones.get(activeZoneIndex).getDisplayName();
            showZoneStatus(participants, newZoneName, "Zone rotated!");
            // Reset controller state on rotation
            currentController = null;
            contested = false;
            previouslyInZone.clear();
        }

        ArenaConfig.CaptureZone activeZone = zones.get(activeZoneIndex);

        // Scan positions of all alive participants (players and bots)
        // Uses hysteresis: entering requires being inside exact bounds,
        // leaving requires moving ZONE_EXIT_MARGIN beyond the boundary.
        List<UUID> participantsInZone = new ArrayList<>();
        for (Participant p : participants) {
            if (!p.isAlive()) {
                continue;
            }

            double px, py, pz;
            if (p.getType() == ParticipantType.BOT) {
                BotParticipant bot = (BotParticipant) p;
                Position botPos = bot.getCurrentPosition();
                if (botPos == null) continue;
                px = botPos.getX(); py = botPos.getY(); pz = botPos.getZ();
            } else {
                Vector3d pos = getPlayerPosition(p.getUniqueId());
                if (pos == null) continue;
                px = pos.getX(); py = pos.getY(); pz = pos.getZ();
            }

            boolean wasInside = previouslyInZone.contains(p.getUniqueId());
            boolean inside;
            if (wasInside) {
                // Already inside — use wider exit boundary (must move further out to leave)
                inside = isInZoneWithMargin(activeZone, px, py, pz, ZONE_EXIT_MARGIN);
            } else {
                // Outside — use exact boundary to enter
                inside = isInZone(activeZone, px, py, pz);
            }

            if (inside) {
                participantsInZone.add(p.getUniqueId());
            }
        }

        // Update hysteresis tracking
        previouslyInZone.clear();
        previouslyInZone.addAll(participantsInZone);

        // Store for bot access (getBotObjective reads this)
        participantsInZoneLive.clear();
        participantsInZoneLive.addAll(participantsInZone);

        // Scoring logic
        UUID previousController = currentController;
        boolean previousContested = contested;

        if (participantsInZone.isEmpty()) {
            currentController = null;
            contested = false;
        } else if (participantsInZone.size() == 1) {
            currentController = participantsInZone.get(0);
            contested = false;
            controlTicks.merge(currentController, 1, Integer::sum);
        } else {
            currentController = null;
            contested = true;
        }

        // Zone state change — broadcast immediately via event title + update zone shape
        if (!Objects.equals(previousController, currentController) || previousContested != contested) {
            String zoneName = activeZone.getDisplayName();
            if (contested) {
                showZoneStatus(participants, zoneName, "Contested!");
            } else if (currentController != null) {
                Participant controller = findParticipant(participants, currentController);
                if (controller != null) {
                    showZoneStatus(participants, zoneName, controller.getName() + " is holding!");
                }
            } else if (previousController != null || previousContested) {
                showZoneStatus(participants, zoneName, "Unclaimed!");
            }
            // Immediately update zone shape on state change
            sendZoneShape(activeZone, participants);
        }

        // Periodic zone shape refresh (every second)
        if (tickCount % 20 == 0) {
            sendZoneShape(activeZone, participants);
        }

        // Score milestone to controller (every second)
        if (tickCount % 20 == 0 && currentController != null) {
            int ticks = controlTicks.getOrDefault(currentController, 0);
            int score = ticks / 20;
            Participant controller = findParticipant(participants, currentController);
            if (controller != null) {
                controller.sendMessage("<color:#2ecc71>Score: " + score + "/" + config.getScoreTarget() + "</color>");
            }
        }
    }

    @Override
    public boolean onParticipantKilled(ArenaConfig config, Participant victim, Participant killer, List<Participant> participants) {
        victim.setAlive(false);
        victim.addDeath();

        if (killer != null) {
            killer.addKill();
        }

        // Broadcast kill
        String killMsg;
        if (killer != null) {
            killMsg = "<color:#e74c3c>" + victim.getName() + "</color> <color:#7f8c8d>was killed by</color> <color:#2ecc71>" + killer.getName() + "</color>";
        } else {
            killMsg = "<color:#e74c3c>" + victim.getName() + "</color> <color:#7f8c8d>died</color>";
        }
        for (Participant p : participants) {
            p.sendMessage(killMsg);
        }

        // Kills don't end the match in KOTH
        return false;
    }

    @Override
    public void onParticipantDamaged(ArenaConfig config, Participant victim, Participant attacker, double damage) {
        victim.addDamageTaken(damage);
        if (attacker != null) {
            attacker.addDamageDealt(damage);
        }
    }

    @Override
    public boolean shouldMatchEnd(ArenaConfig config, List<Participant> participants) {
        if (config.getScoreTarget() <= 0) {
            return false; // No score target — match ends by time only
        }
        int targetTicks = config.getScoreTarget() * 20;
        return controlTicks.values().stream().anyMatch(t -> t >= targetTicks);
    }

    @Override
    public int getParticipantScore(UUID participantId) {
        return controlTicks.getOrDefault(participantId, 0) / 20;
    }

    @Override
    public int getScoreTarget(ArenaConfig config) {
        return config.getScoreTarget();
    }

    @Override
    public String getScoreLabel() {
        return "Pts";
    }

    @Override
    public boolean shouldRespawn(ArenaConfig config, Participant participant) {
        return true;
    }

    @Override
    public int getRespawnDelayTicks(ArenaConfig config) {
        return config.getRespawnDelaySeconds() * 20;
    }

    @Override
    public List<UUID> getWinners(ArenaConfig config, List<Participant> participants) {
        // 1. Most zone control points
        int maxTicks = controlTicks.values().stream()
                .mapToInt(Integer::intValue)
                .max()
                .orElse(0);

        if (maxTicks == 0) {
            return new ArrayList<>();
        }

        List<UUID> topControllers = controlTicks.entrySet().stream()
                .filter(e -> e.getValue() == maxTicks)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        if (topControllers.size() == 1) {
            return topControllers;
        }

        // 2. Tiebreaker: most kills
        int maxKills = -1;
        List<UUID> killLeaders = new ArrayList<>();
        for (UUID uuid : topControllers) {
            Participant p = findParticipant(participants, uuid);
            if (p == null) continue;
            int kills = p.getKills();
            if (kills > maxKills) {
                maxKills = kills;
                killLeaders.clear();
                killLeaders.add(uuid);
            } else if (kills == maxKills) {
                killLeaders.add(uuid);
            }
        }

        if (killLeaders.size() == 1) {
            return killLeaders;
        }

        // 3. Still tied — draw
        return new ArrayList<>();
    }

    @Override
    public String getVictoryMessage(ArenaConfig config, List<Participant> winners) {
        if (winners.isEmpty()) {
            return "<color:#f39c12>No one controlled the hill!</color>";
        }

        Participant winner = winners.get(0);
        int ticks = controlTicks.getOrDefault(winner.getUniqueId(), 0);
        int score = ticks / 20;
        return "<gradient:#f1c40f:#f39c12><b>" + winner.getName() + "</b></gradient> <color:#f1c40f>controls the hill with " + score + " score!</color>";
    }

    /**
     * Sends a wireframe cube (12 thin edges) showing the capture zone boundary to each player.
     * Color depends on the player's relationship to the current zone state.
     */
    private void sendZoneShape(ArenaConfig.CaptureZone zone, List<Participant> participants) {
        // Snap to full blocks: floor mins, ceil maxes — visual always covers the full zone
        double x1 = Math.floor(Math.min(zone.getMinX(), zone.getMaxX()));
        double x2 = Math.ceil(Math.max(zone.getMinX(), zone.getMaxX()));
        double y1 = Math.floor(Math.min(zone.getMinY(), zone.getMaxY()));
        double y2 = Math.ceil(Math.max(zone.getMinY(), zone.getMaxY()));
        double z1 = Math.floor(Math.min(zone.getMinZ(), zone.getMaxZ()));
        double z2 = Math.ceil(Math.max(zone.getMinZ(), zone.getMaxZ()));

        double centerX = (x1 + x2) / 2.0;
        double centerY = (y1 + y2) / 2.0;
        double centerZ = (z1 + z2) / 2.0;
        double sizeX = x2 - x1;
        double sizeY = y2 - y1;
        double sizeZ = z2 - z1;
        double t = EDGE_THICKNESS;

        // 12 edges of a wireframe cube + 1 floor pane: 4 bottom, 4 top, 4 vertical, 1 floor
        float[][] edgeMatrices = new float[13][];

        // Extend horizontal edges by t on each end so they overlap the vertical pillars at corners
        double extX = sizeX + t;
        double extZ = sizeZ + t;

        // --- Bottom edges (y1) ---
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

        // --- Top edges (y2) ---
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

        // --- Vertical edges (4 corners) ---
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

        // --- Floor pane (thin slab at y1 covering full XZ) ---
        m = new Matrix4d().identity();
        m.translate(centerX, y1, centerZ); m.scale(sizeX, t, sizeZ);
        edgeMatrices[12] = m.asFloatData();

        for (Participant p : participants) {
            if (p.getType() != ParticipantType.PLAYER) continue;

            PlayerRef playerRef = Universe.get().getPlayer(p.getUniqueId());
            if (playerRef == null) continue;

            // Determine color based on this player's perspective
            com.hypixel.hytale.protocol.Vector3f color;
            if (contested) {
                color = COLOR_CONTESTED;
            } else if (currentController != null && currentController.equals(p.getUniqueId())) {
                color = COLOR_HOLDING;
            } else if (currentController != null) {
                color = COLOR_ENEMY;
            } else {
                color = COLOR_UNCLAIMED;
            }

            try {
                // Clear previous shapes to prevent stacking
                playerRef.getPacketHandler().write(new ClearDebugShapes());

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
    }

    @Override
    public void onMatchFinished(List<Participant> participants) {
        // Clear debug shapes for all player participants
        for (Participant p : participants) {
            if (p.getType() != ParticipantType.PLAYER) continue;

            PlayerRef playerRef = Universe.get().getPlayer(p.getUniqueId());
            if (playerRef == null) continue;

            try {
                playerRef.getPacketHandler().write(new ClearDebugShapes());
            } catch (Exception e) {
                // Silently ignore
            }
        }

        // Despawn zone name holograms
        despawnZoneNameHolograms();
    }

    /**
     * Spawns floating text holograms above each capture zone.
     */
    private void spawnZoneNameHolograms(ArenaConfig config) {
        List<ArenaConfig.CaptureZone> zones = config.getCaptureZones();
        if (zones == null || zones.isEmpty()) return;

        World world = Universe.get().getWorld(worldName);
        if (world == null) return;

        world.execute(() -> {
            for (ArenaConfig.CaptureZone zone : zones) {
                double cx = (Math.min(zone.getMinX(), zone.getMaxX()) + Math.max(zone.getMinX(), zone.getMaxX())) / 2.0;
                double topY = Math.max(zone.getMinY(), zone.getMaxY()) + 1.5;
                double cz = (Math.min(zone.getMinZ(), zone.getMaxZ()) + Math.max(zone.getMinZ(), zone.getMaxZ())) / 2.0;

                Ref<EntityStore> ref = HologramUtil.spawnHologram(world, cx, topY, cz, zone.getDisplayName());
                if (ref != null) {
                    zoneNameHolograms.add(ref);
                }
            }
        });
    }

    /**
     * Removes all zone name hologram entities.
     */
    private void despawnZoneNameHolograms() {
        if (zoneNameHolograms.isEmpty()) return;

        World world = worldName != null ? Universe.get().getWorld(worldName) : null;
        if (world == null) {
            zoneNameHolograms.clear();
            return;
        }

        world.execute(() -> {
            for (Ref<EntityStore> ref : zoneNameHolograms) {
                HologramUtil.despawnHologram(ref);
            }
            zoneNameHolograms.clear();
        });
    }

    /**
     * Gets the current position of a player from the world thread.
     * This method is called from onTick which already runs on the world thread.
     */
    private Vector3d getPlayerPosition(UUID playerUuid) {
        try {
            PlayerRef playerRef = Universe.get().getPlayer(playerUuid);
            if (playerRef == null) return null;

            Ref<EntityStore> ref = playerRef.getReference();
            if (ref == null) return null;

            Store<EntityStore> store = ref.getStore();
            if (store == null) return null;

            TransformComponent transform = store.getComponent(ref,
                EntityModule.get().getTransformComponentType());
            if (transform == null) return null;

            return transform.getPosition();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Zone containment check with Y tolerance.
     * Adds 0.5 blocks of vertical padding to prevent flickering from physics jitter
     * (player Y position fluctuates slightly when walking on small/thin zones).
     */
    private static final double Y_TOLERANCE = 0.5;

    private boolean isInZone(ArenaConfig.CaptureZone zone, double x, double y, double z) {
        return isInZoneWithMargin(zone, x, y, z, 0);
    }

    /**
     * Zone containment check with configurable XZ margin.
     * margin > 0 expands the boundary outward (used for hysteresis exit check).
     */
    private boolean isInZoneWithMargin(ArenaConfig.CaptureZone zone, double x, double y, double z, double margin) {
        double x1 = Math.min(zone.getMinX(), zone.getMaxX()) - margin;
        double x2 = Math.max(zone.getMinX(), zone.getMaxX()) + margin;
        double y1 = Math.min(zone.getMinY(), zone.getMaxY());
        double y2 = Math.max(zone.getMinY(), zone.getMaxY());
        double z1 = Math.min(zone.getMinZ(), zone.getMaxZ()) - margin;
        double z2 = Math.max(zone.getMinZ(), zone.getMaxZ()) + margin;
        return x >= x1 && x <= x2 &&
               y >= y1 - Y_TOLERANCE && y <= y2 + Y_TOLERANCE &&
               z >= z1 && z <= z2;
    }

    /**
     * Shows a zone status notification to all player participants.
     */
    private void showZoneStatus(List<Participant> participants, String zoneName, String status) {
        for (Participant p : participants) {
            if (p.getType() != ParticipantType.PLAYER) continue;
            PlayerRef playerRef = Universe.get().getPlayer(p.getUniqueId());
            if (playerRef != null) {
                try {
                    NotificationUtil.sendNotification(
                        playerRef.getPacketHandler(),
                        Message.raw(status),
                        Message.raw(zoneName),
                        NotificationStyle.Warning
                    );
                } catch (Exception e) {
                    p.sendMessage("<color:#e8c872>" + zoneName + ": " + status + "</color>");
                }
            }
        }
    }

    private Participant findParticipant(List<Participant> participants, UUID uuid) {
        for (Participant p : participants) {
            if (p.getUniqueId().equals(uuid)) {
                return p;
            }
        }
        return null;
    }
}
