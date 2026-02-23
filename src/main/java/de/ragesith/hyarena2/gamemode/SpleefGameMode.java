package de.ragesith.hyarena2.gamemode;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.modules.entity.EntityModule;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import de.ragesith.hyarena2.arena.ArenaConfig;
import de.ragesith.hyarena2.arena.Match;
import de.ragesith.hyarena2.participant.Participant;
import de.ragesith.hyarena2.participant.ParticipantType;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Spleef game mode — break blocks under opponents to make them fall.
 * <p>
 * Players can only break blocks that match the configured block type within
 * defined floor regions. Players who fall below the elimination Y threshold
 * are eliminated. Last player standing wins. Floors are filled before each
 * match starts. No PvP damage, no respawns.
 */
public class SpleefGameMode implements GameMode {
    private static final String ID = "spleef";
    private static final String DISPLAY_NAME = "Spleef";
    private static final int DECAY_SECONDS = 30;
    private static final int TICKS_PER_SECOND = 20;

    // Stored on match start so onMatchFinished can access it for floor cleanup
    private volatile ArenaConfig activeConfig;

    // Floor decay state — built lazily when decay period begins
    private List<int[]> decayPositions; // block positions sorted highest Y first, shuffled per layer
    private int decayCursor;            // next index to clear
    private int decayBlocksPerTick;     // how many blocks to clear each tick

    @Override
    public GameModeCategory getCategory() { return GameModeCategory.MINIGAME; }

    @Override
    public String getId() { return ID; }

    @Override
    public String getDisplayName() { return DISPLAY_NAME; }

    @Override
    public String getWebDescription() {
        return "Break the floor under your opponents to make them fall! Last player standing wins.";
    }

    @Override
    public String getDescription() {
        return "Group { LayoutMode: Top;"
            + " Label { Text: \"Spleef\"; Anchor: (Height: 28); Style: (FontSize: 18, TextColor: #e8c872, RenderBold: true); }"
            + " Label { Text: \"Break the blocks beneath your opponents to make them fall into the void. The last player standing wins!\"; Anchor: (Height: 40, Top: 4); Style: (FontSize: 13, TextColor: #b7cedd, Wrap: true); }"
            + " Label { Text: \"Rules\"; Anchor: (Height: 24, Top: 12); Style: (FontSize: 15, TextColor: #e8c872, RenderBold: true); }"
            + " Label { Text: \"\u2022 Only configured floor blocks can be broken\"; Anchor: (Height: 18, Top: 4); Style: (FontSize: 12, TextColor: #96a9be); }"
            + " Label { Text: \"\u2022 No PvP damage \u2014 win by strategy, not combat\"; Anchor: (Height: 18, Top: 2); Style: (FontSize: 12, TextColor: #96a9be); }"
            + " Label { Text: \"\u2022 Fall below the elimination line and you're out\"; Anchor: (Height: 18, Top: 2); Style: (FontSize: 12, TextColor: #96a9be); }"
            + " Label { Text: \"\u2022 No respawns \u2014 one life only\"; Anchor: (Height: 18, Top: 2); Style: (FontSize: 12, TextColor: #96a9be); }"
            + " Label { Text: \"\u2022 Last player standing wins\"; Anchor: (Height: 18, Top: 2); Style: (FontSize: 12, TextColor: #96a9be); }"
            + " Label { Text: \"Tips\"; Anchor: (Height: 24, Top: 12); Style: (FontSize: 15, TextColor: #e8c872, RenderBold: true); }"
            + " Label { Text: \"\u2022 Keep moving \u2014 standing still makes you an easy target\"; Anchor: (Height: 18, Top: 4); Style: (FontSize: 12, TextColor: #96a9be); }"
            + " Label { Text: \"\u2022 Break blocks around opponents to cut off their escape\"; Anchor: (Height: 18, Top: 2); Style: (FontSize: 12, TextColor: #96a9be); }"
            + " Label { Text: \"\u2022 Stay near the center where you have more room\"; Anchor: (Height: 18, Top: 2); Style: (FontSize: 12, TextColor: #96a9be); }"
            + " }";
    }

    @Override
    public void onMatchStart(ArenaConfig config, List<Participant> participants) {
        for (Participant p : participants) {
            p.setAlive(true);
        }

        activeConfig = config;
        decayPositions = null;
        decayCursor = 0;

        // Fill all spleef floor regions before gameplay begins
        generateFloors(config);
    }

    @Override
    public void onGameplayBegin(ArenaConfig config, List<Participant> participants) {
        long aliveCount = participants.stream().filter(Participant::isAlive).count();
        for (Participant p : participants) {
            p.sendMessage("<gradient:#2ecc71:#27ae60><b>SPLEEF!</b></gradient> <color:#f1c40f>Break the floor beneath your opponents!</color>");
            p.sendMessage("<color:#f1c40f>" + aliveCount + " players remain</color>");
        }
    }

    @Override
    public void onTick(Match match, ArenaConfig config, List<Participant> participants, int tickCount) {
        // Floor decay in the last 30 seconds
        int durationSeconds = config.getMatchDurationSeconds();
        if (durationSeconds > DECAY_SECONDS) {
            int decayStartTick = (durationSeconds - DECAY_SECONDS) * TICKS_PER_SECOND;

            if (tickCount >= decayStartTick) {
                // Initialize decay on first tick of the decay period
                if (decayPositions == null) {
                    initDecay(config, participants);
                }
                tickDecay(config);
            }
        }

        // Check every 4 ticks (5 times per second) for Y-threshold elimination
        if (tickCount % 4 != 0) return;

        double eliminationY = config.getSpleefEliminationY();

        for (Participant p : participants) {
            if (!p.isAlive()) continue;
            if (p.getType() != ParticipantType.PLAYER) continue;

            Vector3d pos = getPlayerPosition(p.getUniqueId());
            if (pos == null) continue;

            if (pos.getY() < eliminationY) {
                match.recordKill(p.getUniqueId(), null);
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

        long aliveCount = participants.stream().filter(Participant::isAlive).count();

        String eliminationMsg = "<color:#e74c3c>" + victim.getName() + "</color> <color:#95a5a6>fell into the void!</color> <color:#f1c40f>" + aliveCount + " players remain</color>";
        for (Participant p : participants) {
            p.sendMessage(eliminationMsg);
        }

        return aliveCount <= 1;
    }

    @Override
    public void onParticipantDamaged(ArenaConfig config, Participant victim, Participant attacker, double damage) {
        victim.addDamageTaken(damage);
        if (attacker != null) {
            attacker.addDamageDealt(damage);
        }
    }

    @Override
    public boolean shouldAllowDamage(Participant attacker, Participant victim) {
        return false; // No PvP damage in spleef
    }

    @Override
    public boolean shouldMatchEnd(ArenaConfig config, List<Participant> participants) {
        long aliveCount = participants.stream().filter(Participant::isAlive).count();
        return aliveCount <= 1;
    }

    @Override
    public List<UUID> getWinners(ArenaConfig config, List<Participant> participants) {
        List<Participant> alive = participants.stream()
                .filter(Participant::isAlive)
                .collect(Collectors.toList());

        if (alive.size() == 1) {
            return List.of(alive.get(0).getUniqueId());
        }

        return new ArrayList<>();
    }

    @Override
    public String getVictoryMessage(ArenaConfig config, List<Participant> winners) {
        if (winners.isEmpty()) {
            return "<color:#f39c12>Everyone fell!</color>";
        }

        Participant winner = winners.stream().findFirst().orElse(null);
        if (winner != null) {
            return "<gradient:#f1c40f:#f39c12><b>" + winner.getName() + "</b></gradient> <color:#f1c40f>is the last one standing!</color>";
        }

        return "<color:#f39c12>Match ended!</color>";
    }

    @Override
    public boolean shouldRespawn(ArenaConfig config, Participant participant) {
        return false;
    }

    @Override
    public int getRespawnDelayTicks(ArenaConfig config) {
        return 0;
    }

    // ========== Block Break Logic ==========

    @Override
    public boolean shouldAllowBlockBreak(ArenaConfig config, UUID playerUuid,
            int x, int y, int z, String blockTypeId) {
        List<ArenaConfig.SpleefFloor> floors = config.getSpleefFloors();
        if (floors == null || floors.isEmpty()) {
            return false;
        }

        for (ArenaConfig.SpleefFloor floor : floors) {
            if (floor.contains(x, y, z) && blockTypeId.equals(floor.getBlockId())) {
                // Clear the block ourselves instead of letting the engine break it
                World world = Universe.get().getWorld(config.getWorldName());
                if (world != null) {
                    world.setBlock(x, y, z, "Empty");
                }
                return false;
            }
        }
        return false;
    }

    @Override
    public void onMatchFinished(List<Participant> participants) {
        ArenaConfig config = activeConfig;
        activeConfig = null;
        decayPositions = null;
        decayCursor = 0;
        if (config == null) return;
        clearFloors(config);
    }

    // ========== Floor Generation ==========

    /**
     * Fills every block position in each SpleefFloor region with its configured block type.
     * Called before match starts so floors are always intact when players begin.
     */
    private void generateFloors(ArenaConfig config) {
        List<ArenaConfig.SpleefFloor> floors = config.getSpleefFloors();
        if (floors == null || floors.isEmpty()) {
            System.out.println("[SpleefGameMode] generateFloors: no floors configured");
            return;
        }

        String worldName = config.getWorldName();
        World world = Universe.get().getWorld(worldName);
        if (world == null) {
            System.err.println("[SpleefGameMode] generateFloors: world '" + worldName + "' not found");
            return;
        }

        System.out.println("[SpleefGameMode] Generating " + floors.size() + " floor(s) on world '" + worldName + "'");

        world.execute(() -> {
            int totalPlaced = 0;
            for (ArenaConfig.SpleefFloor floor : floors) {
                int minX = (int) Math.floor(Math.min(floor.getMinX(), floor.getMaxX()));
                int minY = (int) Math.floor(Math.min(floor.getMinY(), floor.getMaxY()));
                int minZ = (int) Math.floor(Math.min(floor.getMinZ(), floor.getMaxZ()));
                int maxX = (int) Math.ceil(Math.max(floor.getMinX(), floor.getMaxX()));
                int maxY = (int) Math.ceil(Math.max(floor.getMinY(), floor.getMaxY()));
                int maxZ = (int) Math.ceil(Math.max(floor.getMinZ(), floor.getMaxZ()));
                String blockId = floor.getBlockId();

                int placed = 0;
                for (int x = minX; x < maxX; x++) {
                    for (int y = minY; y < maxY; y++) {
                        for (int z = minZ; z < maxZ; z++) {
                            try {
                                world.setBlock(x, y, z, blockId);
                                placed++;
                            } catch (Exception e) {
                                System.err.println("[SpleefGameMode] setBlock failed at (" + x + "," + y + "," + z + ") blockId='" + blockId + "': " + e.getMessage());
                            }
                        }
                    }
                }
                System.out.println("[SpleefGameMode] Floor blockId='" + blockId + "' filled " + placed + " blocks"
                    + " (" + minX + "," + minY + "," + minZ + " -> " + maxX + "," + maxY + "," + maxZ + ")");
                totalPlaced += placed;
            }
            System.out.println("[SpleefGameMode] Floor generation complete: " + totalPlaced + " blocks placed");
        });
    }

    /**
     * Clears all blocks in each SpleefFloor region by setting them to block ID 0 (air).
     * Called on match finish to clean up the arena.
     */
    private void clearFloors(ArenaConfig config) {
        List<ArenaConfig.SpleefFloor> floors = config.getSpleefFloors();
        if (floors == null || floors.isEmpty()) return;

        String worldName = config.getWorldName();
        World world = Universe.get().getWorld(worldName);
        if (world == null) {
            System.err.println("[SpleefGameMode] clearFloors: world '" + worldName + "' not found");
            return;
        }

        System.out.println("[SpleefGameMode] Clearing " + floors.size() + " floor(s) on world '" + worldName + "'");

        world.execute(() -> {
            int totalCleared = 0;
            for (ArenaConfig.SpleefFloor floor : floors) {
                int minX = (int) Math.floor(Math.min(floor.getMinX(), floor.getMaxX()));
                int minY = (int) Math.floor(Math.min(floor.getMinY(), floor.getMaxY()));
                int minZ = (int) Math.floor(Math.min(floor.getMinZ(), floor.getMaxZ()));
                int maxX = (int) Math.ceil(Math.max(floor.getMinX(), floor.getMaxX()));
                int maxY = (int) Math.ceil(Math.max(floor.getMinY(), floor.getMaxY()));
                int maxZ = (int) Math.ceil(Math.max(floor.getMinZ(), floor.getMaxZ()));

                int cleared = 0;
                for (int x = minX; x < maxX; x++) {
                    for (int y = minY; y < maxY; y++) {
                        for (int z = minZ; z < maxZ; z++) {
                            try {
                                world.setBlock(x, y, z, "Empty");
                                cleared++;
                            } catch (Exception e) {
                                System.err.println("[SpleefGameMode] clearBlock failed at (" + x + "," + y + "," + z + "): " + e.getMessage());
                            }
                        }
                    }
                }
                totalCleared += cleared;
            }
            System.out.println("[SpleefGameMode] Floor clearing complete: " + totalCleared + " blocks cleared");
        });
    }

    // ========== Floor Decay ==========

    /**
     * Builds the decay position list: all floor block positions grouped by Y (highest first),
     * shuffled within each layer. Calculates blocks-per-tick to clear everything in 30 seconds.
     */
    private void initDecay(ArenaConfig config, List<Participant> participants) {
        List<ArenaConfig.SpleefFloor> floors = config.getSpleefFloors();
        if (floors == null || floors.isEmpty()) {
            decayPositions = Collections.emptyList();
            return;
        }

        // Collect all positions grouped by Y level
        Map<Integer, List<int[]>> byY = new TreeMap<>(Collections.reverseOrder());
        for (ArenaConfig.SpleefFloor floor : floors) {
            int minX = (int) Math.floor(Math.min(floor.getMinX(), floor.getMaxX()));
            int minY = (int) Math.floor(Math.min(floor.getMinY(), floor.getMaxY()));
            int minZ = (int) Math.floor(Math.min(floor.getMinZ(), floor.getMaxZ()));
            int maxX = (int) Math.ceil(Math.max(floor.getMinX(), floor.getMaxX()));
            int maxY = (int) Math.ceil(Math.max(floor.getMinY(), floor.getMaxY()));
            int maxZ = (int) Math.ceil(Math.max(floor.getMinZ(), floor.getMaxZ()));

            for (int x = minX; x < maxX; x++) {
                for (int y = minY; y < maxY; y++) {
                    for (int z = minZ; z < maxZ; z++) {
                        byY.computeIfAbsent(y, k -> new ArrayList<>()).add(new int[]{x, y, z});
                    }
                }
            }
        }

        // Shuffle within each Y layer, then flatten (highest Y first due to reverse order TreeMap)
        Random rng = new Random();
        decayPositions = new ArrayList<>();
        for (List<int[]> layer : byY.values()) {
            Collections.shuffle(layer, rng);
            decayPositions.addAll(layer);
        }

        decayCursor = 0;
        int totalDecayTicks = DECAY_SECONDS * TICKS_PER_SECOND;
        decayBlocksPerTick = Math.max(1, (int) Math.ceil((double) decayPositions.size() / totalDecayTicks));

        System.out.println("[SpleefGameMode] Floor decay started: " + decayPositions.size()
            + " blocks, " + decayBlocksPerTick + " per tick");

        // Broadcast warning
        for (Participant p : participants) {
            p.sendMessage("<color:#e74c3c><b>WARNING!</b></color> <color:#f39c12>The floor is crumbling! 30 seconds remaining!</color>");
        }
    }

    /**
     * Removes the next batch of blocks in the decay sequence.
     */
    private void tickDecay(ArenaConfig config) {
        if (decayPositions == null || decayCursor >= decayPositions.size()) return;

        World world = Universe.get().getWorld(config.getWorldName());
        if (world == null) return;

        int end = Math.min(decayCursor + decayBlocksPerTick, decayPositions.size());
        for (int i = decayCursor; i < end; i++) {
            int[] pos = decayPositions.get(i);
            try {
                world.setBlock(pos[0], pos[1], pos[2], "Empty");
            } catch (Exception e) {
                // Silently ignore — block may already be broken by player
            }
        }
        decayCursor = end;
    }

    // ========== Helpers ==========

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
}
