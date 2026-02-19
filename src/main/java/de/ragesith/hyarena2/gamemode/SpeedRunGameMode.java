package de.ragesith.hyarena2.gamemode;

import com.google.gson.JsonObject;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.NotificationStyle;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.modules.entity.EntityModule;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatsModule;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.EntityStatType;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.NotificationUtil;
import de.ragesith.hyarena2.arena.ArenaConfig;
import de.ragesith.hyarena2.arena.Match;
import de.ragesith.hyarena2.config.Position;
import de.ragesith.hyarena2.hub.HubManager;
import de.ragesith.hyarena2.participant.Participant;
import de.ragesith.hyarena2.participant.ParticipantType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Speed Run game mode — solo parkour with checkpoint zones, precision timer, kill plane, and PB tracking.
 * Timer pauses inside zones and runs between them.
 */
public class SpeedRunGameMode implements GameMode {
    private static final String ID = "speed_run";
    private static final String DISPLAY_NAME = "Speed Run";
    private static final double Y_TOLERANCE = 0.5;
    private static final long VERY_LONG_IMMUNITY_MS = 999_999_999L; // Effectively infinite (prevents fall damage)
    private static final int STAT_REFRESH_INTERVAL = 10; // Every 10 ticks (0.5s)

    private SpeedRunPBManager pbManager;
    private boolean staminaStatValid = true; // Becomes false if "stamina" stat doesn't exist

    // Per-match state keyed by matchId
    private final Map<UUID, SpeedRunState> matchStates = new ConcurrentHashMap<>();

    public void setPBManager(SpeedRunPBManager pbManager) {
        this.pbManager = pbManager;
    }

    @Override
    public String getId() { return ID; }

    @Override
    public String getDisplayName() { return DISPLAY_NAME; }

    @Override
    public String getWebDescription() {
        return "Solo parkour race through checkpoint zones. Timer pauses in zones and runs between them. Beat your personal best!";
    }

    @Override
    public String getDescription() {
        return "Group { LayoutMode: Top;"
            + " Label { Text: \"Speed Run\"; Anchor: (Height: 28); Style: (FontSize: 18, TextColor: #e8c872, RenderBold: true); }"
            + " Label { Text: \"Race through a parkour course as fast as possible. Hit checkpoints and reach the finish zone!\"; Anchor: (Height: 40, Top: 4); Style: (FontSize: 13, TextColor: #b7cedd, Wrap: true); }"
            + " Label { Text: \"Rules\"; Anchor: (Height: 24, Top: 12); Style: (FontSize: 15, TextColor: #e8c872, RenderBold: true); }"
            + " Label { Text: \"\u2022 Timer starts when you leave the start zone\"; Anchor: (Height: 18, Top: 4); Style: (FontSize: 12, TextColor: #96a9be); }"
            + " Label { Text: \"\u2022 Timer pauses inside checkpoint zones\"; Anchor: (Height: 18, Top: 2); Style: (FontSize: 12, TextColor: #96a9be); }"
            + " Label { Text: \"\u2022 Checkpoints must be reached in order\"; Anchor: (Height: 18, Top: 2); Style: (FontSize: 12, TextColor: #96a9be); }"
            + " Label { Text: \"\u2022 Falling below the kill plane costs a life\"; Anchor: (Height: 18, Top: 2); Style: (FontSize: 12, TextColor: #96a9be); }"
            + " Label { Text: \"\u2022 Lose all lives and it's a DNF\"; Anchor: (Height: 18, Top: 2); Style: (FontSize: 12, TextColor: #96a9be); }"
            + " Label { Text: \"Tips\"; Anchor: (Height: 24, Top: 12); Style: (FontSize: 15, TextColor: #e8c872, RenderBold: true); }"
            + " Label { Text: \"\u2022 Move through zones quickly to minimize paused time\"; Anchor: (Height: 18, Top: 4); Style: (FontSize: 12, TextColor: #96a9be); }"
            + " Label { Text: \"\u2022 Watch your split times vs your personal best\"; Anchor: (Height: 18, Top: 2); Style: (FontSize: 12, TextColor: #96a9be); }"
            + " }";
    }

    @Override
    public boolean usesDefaultMatchHud() { return false; }

    @Override
    public boolean usesDefaultVictoryHud() { return false; }

    @Override
    public void onMatchStart(ArenaConfig config, List<Participant> participants) {
        // No per-match init here — done in onGameplayBegin when we have the match context
    }

    @Override
    public void onGameplayBegin(ArenaConfig config, List<Participant> participants) {
        // State initialization happens in onTick for the first time (we need match context)
    }

    /**
     * Initializes per-match state. Called lazily from onTick when state doesn't exist yet.
     */
    private SpeedRunState initState(Match match, ArenaConfig config, Participant player) {
        SpeedRunState state = new SpeedRunState();
        int checkpointCount = config.getCheckpoints() != null ? config.getCheckpoints().size() : 0;
        state.checkpointSplitNanos = new long[checkpointCount];
        state.livesRemaining = config.getMaxRespawns();
        state.playerUuid = player.getUniqueId();
        state.inZone = true; // Start in start zone, timer paused

        // Load PB
        if (pbManager != null) {
            state.personalBest = pbManager.loadPB(player.getUniqueId(), config.getId());
        }

        // Grant very long immunity (prevents fall damage — speed run has no combat)
        player.grantImmunity(VERY_LONG_IMMUNITY_MS);

        matchStates.put(match.getMatchId(), state);
        System.out.println("[SpeedRunGameMode] Initialized state for match " + match.getMatchId()
            + " (checkpoints: " + checkpointCount + ", lives: " + state.livesRemaining + ")");
        return state;
    }

    @Override
    public void onTick(Match match, ArenaConfig config, List<Participant> participants, int tickCount) {
        // Find the single player participant
        Participant player = null;
        for (Participant p : participants) {
            if (p.getType() == ParticipantType.PLAYER) {
                player = p;
                break;
            }
        }
        if (player == null) return;

        // Lazy init state
        SpeedRunState state = matchStates.get(match.getMatchId());
        if (state == null) {
            state = initState(match, config, player);

            // Show SpeedRunHud
            if (match.getHudManager() != null) {
                match.getHudManager().showSpeedRunHud(
                    player.getUniqueId(), match,
                    match.getArena().getWorld()::execute, this
                );
            }
        }

        if (state.finished || state.isDNF) return;

        // Maintain full health and stamina
        if (tickCount % STAT_REFRESH_INTERVAL == 0) {
            maintainPlayerStats(player.getUniqueId());
        }

        // Decrement kill plane grace period (prevents multi-trigger from single fall)
        if (state.killPlaneGraceTicks > 0) {
            state.killPlaneGraceTicks--;
        }

        // Get player position
        Vector3d pos = getPlayerPosition(player.getUniqueId());
        if (pos == null) return;

        // Kill plane check (grace period prevents rapid life drain after teleport)
        if (pos.getY() < config.getKillPlaneY() && player.isAlive() && state.killPlaneGraceTicks <= 0) {
            handleKillPlane(match, config, state, player);
            return;
        }

        // Zone containment checks
        boolean inStart = isInZone(config.getStartZone(), pos);
        boolean inFinish = isInZone(config.getFinishZone(), pos);

        int checkpointIndex = -1;
        if (config.getCheckpoints() != null) {
            for (int i = 0; i < config.getCheckpoints().size(); i++) {
                if (isInZone(config.getCheckpoints().get(i), pos)) {
                    checkpointIndex = i;
                    break;
                }
            }
        }

        // Zone transition logic
        boolean wasInZone = state.inZone;

        if (state.lastCheckpointReached == -1) {
            // Still at start
            if (inStart) {
                state.inZone = true;
            } else if (wasInZone && !inStart) {
                // Left start zone — start timer
                state.inZone = false;
                startTimer(state);
                player.sendMessage("<color:#2ecc71>GO! Timer started!</color>");
            }
        }

        // Checkpoint check (sequential only, one-time trigger)
        if (checkpointIndex >= 0 && !state.triggeredCheckpoints.contains(checkpointIndex)) {
            int expectedNext = state.lastCheckpointReached + 1;
            if (checkpointIndex == expectedNext) {
                // Reached next checkpoint in order
                state.triggeredCheckpoints.add(checkpointIndex);
                state.lastCheckpointReached = checkpointIndex;
                state.inZone = true;
                pauseTimer(state);

                // Record split
                state.checkpointSplitNanos[checkpointIndex] = getAccumulatedTime(state);

                // Send split notification with PB delta
                String splitTime = SpeedRunPB.formatTime(state.checkpointSplitNanos[checkpointIndex]);
                String deltaMsg = "";
                if (state.personalBest != null && state.personalBest.getCheckpointSplitNanos() != null
                    && checkpointIndex < state.personalBest.getCheckpointSplitNanos().length) {
                    long pbSplit = state.personalBest.getCheckpointSplitNanos()[checkpointIndex];
                    long delta = state.checkpointSplitNanos[checkpointIndex] - pbSplit;
                    String deltaStr = SpeedRunPB.formatDelta(delta);
                    String color = delta <= 0 ? "#2ecc71" : "#e74c3c"; // Green if ahead, red if behind
                    deltaMsg = " <color:" + color + ">(" + deltaStr + ")</color>";
                }

                String cpName = config.getCheckpoints().get(checkpointIndex).getDisplayName();
                if (cpName == null || cpName.isEmpty()) cpName = "CP " + (checkpointIndex + 1);
                player.sendMessage("<color:#f1c40f>" + cpName + "</color> <color:#96a9be>" + splitTime + "</color>" + deltaMsg);

                sendNotification(player.getUniqueId(), cpName, splitTime, NotificationStyle.Success);
            }
        }

        // If player was in a checkpoint zone and is now leaving it, resume timer
        if (wasInZone && state.inZone && checkpointIndex < 0 && !inStart && !inFinish) {
            // Player left the checkpoint zone
            state.inZone = false;
            resumeTimer(state);
        }

        // Finish zone check
        if (inFinish && !state.finished) {
            // Check all checkpoints were hit
            int totalCheckpoints = config.getCheckpoints() != null ? config.getCheckpoints().size() : 0;
            if (state.lastCheckpointReached + 1 >= totalCheckpoints) {
                // Finish!
                pauseTimer(state);
                state.finishTimeNanos = getAccumulatedTime(state);
                state.finished = true;

                String finishTime = SpeedRunPB.formatTime(state.finishTimeNanos);
                player.sendMessage("<color:#f1c40f>FINISHED!</color> <color:#b7cedd>Time: " + finishTime + "</color>");

                // Check for new PB
                if (pbManager != null) {
                    boolean isNewPB = pbManager.isNewPB(player.getUniqueId(), config.getId(), state.finishTimeNanos);
                    state.isNewPB = isNewPB;
                    if (isNewPB) {
                        SpeedRunPB newPB = new SpeedRunPB(
                            config.getId(), player.getUniqueId(), state.finishTimeNanos,
                            state.checkpointSplitNanos.clone(), System.currentTimeMillis()
                        );
                        pbManager.savePB(newPB);
                        player.sendMessage("<color:#f1c40f>NEW PERSONAL BEST!</color>");
                    } else if (state.personalBest != null) {
                        long delta = state.finishTimeNanos - state.personalBest.getTotalTimeNanos();
                        String color = delta <= 0 ? "#2ecc71" : "#e74c3c";
                        player.sendMessage("<color:" + color + ">PB: " + SpeedRunPB.formatDelta(delta) + "</color>");
                    }
                }
            }
        }
    }

    private void handleKillPlane(Match match, ArenaConfig config, SpeedRunState state, Participant player) {
        // Pause timer
        pauseTimer(state);
        state.livesRemaining--;

        if (state.livesRemaining <= 0) {
            // DNF
            state.isDNF = true;
            state.finishTimeNanos = getAccumulatedTime(state);
            player.sendMessage("<color:#e74c3c>No lives remaining! DNF.</color>");
            return;
        }

        // Teleport to last checkpoint (or start zone)
        Position respawnPos = getCheckpointCenter(config, state.lastCheckpointReached);
        state.inZone = true;

        player.sendMessage("<color:#f39c12>Lives remaining: " + state.livesRemaining + "</color>");
        sendNotification(player.getUniqueId(), "Fell!", state.livesRemaining + " lives left", NotificationStyle.Warning);

        // Teleport player using Teleport component (same-world, resets velocity)
        PlayerRef playerRef = Universe.get().getPlayer(player.getUniqueId());
        if (playerRef != null) {
            Ref<EntityStore> ref = playerRef.getReference();
            if (ref != null) {
                Store<EntityStore> store = ref.getStore();
                if (store != null) {
                    Vector3d targetPos = new Vector3d(respawnPos.getX(), respawnPos.getY(), respawnPos.getZ());
                    Vector3f targetRot = new Vector3f(respawnPos.getPitch(), respawnPos.getYaw(), 0);
                    Teleport teleport = new Teleport(match.getArena().getWorld(), targetPos, targetRot);
                    store.addComponent(ref, Teleport.getComponentType(), teleport);
                }
            }
        }

        // Re-grant immunity
        player.grantImmunity(VERY_LONG_IMMUNITY_MS);

        // Grace period: ignore kill plane for 20 ticks (1 second) after teleport
        // This prevents velocity carrying the player back below killPlaneY before the teleport fully takes effect
        state.killPlaneGraceTicks = 20;
    }

    /**
     * Gets the center position of a checkpoint zone, or the start zone if index is -1.
     */
    private Position getCheckpointCenter(ArenaConfig config, int checkpointIndex) {
        ArenaConfig.CaptureZone zone;
        if (checkpointIndex < 0) {
            zone = config.getStartZone();
        } else if (config.getCheckpoints() != null && checkpointIndex < config.getCheckpoints().size()) {
            zone = config.getCheckpoints().get(checkpointIndex);
        } else {
            zone = config.getStartZone();
        }

        double cx = (zone.getMinX() + zone.getMaxX()) / 2.0;
        double cy = zone.getMinY() + 1.0; // Slightly above the floor
        double cz = (zone.getMinZ() + zone.getMaxZ()) / 2.0;
        return new Position(cx, cy, cz, 0, 0);
    }

    // Timer management
    private void startTimer(SpeedRunState state) {
        state.timerRunning = true;
        state.resumeNanoTime = System.nanoTime();
    }

    private void pauseTimer(SpeedRunState state) {
        if (state.timerRunning) {
            state.accumulatedNanos += System.nanoTime() - state.resumeNanoTime;
            state.timerRunning = false;
        }
    }

    private void resumeTimer(SpeedRunState state) {
        if (!state.timerRunning) {
            state.timerRunning = true;
            state.resumeNanoTime = System.nanoTime();
        }
    }

    private long getAccumulatedTime(SpeedRunState state) {
        long total = state.accumulatedNanos;
        if (state.timerRunning) {
            total += System.nanoTime() - state.resumeNanoTime;
        }
        return total;
    }

    @Override
    public void onMatchEnding(Match match, List<UUID> winners) {
        SpeedRunState state = matchStates.get(match.getMatchId());
        if (state == null) return;

        // Hide SpeedRunHud and show results page
        for (Participant p : match.getParticipants()) {
            if (p.getType() == ParticipantType.PLAYER && match.getHudManager() != null) {
                match.getHudManager().hideSpeedRunHud(p.getUniqueId());
                match.getHudManager().showSpeedRunResults(p.getUniqueId(), match, this);
            }
        }
    }

    @Override
    public boolean onParticipantKilled(ArenaConfig config, Participant victim, Participant killer, List<Participant> participants) {
        // Speed run doesn't use the standard kill system — kill plane is handled in onTick
        return false;
    }

    @Override
    public void onParticipantDamaged(ArenaConfig config, Participant victim, Participant attacker, double damage) {
        // No damage in speed run
    }

    @Override
    public boolean shouldAllowDamage(Participant attacker, Participant victim) {
        return false; // No combat in speed run
    }

    @Override
    public boolean shouldMatchEnd(ArenaConfig config, List<Participant> participants) {
        // Find any match state for a participant
        for (Map.Entry<UUID, SpeedRunState> entry : matchStates.entrySet()) {
            SpeedRunState state = entry.getValue();
            for (Participant p : participants) {
                if (p.getUniqueId().equals(state.playerUuid)) {
                    return state.finished || state.isDNF;
                }
            }
        }
        return false;
    }

    @Override
    public List<UUID> getWinners(ArenaConfig config, List<Participant> participants) {
        for (Map.Entry<UUID, SpeedRunState> entry : matchStates.entrySet()) {
            SpeedRunState state = entry.getValue();
            if (state.finished && !state.isDNF) {
                return List.of(state.playerUuid);
            }
        }
        return new ArrayList<>();
    }

    @Override
    public String getVictoryMessage(ArenaConfig config, List<Participant> winners) {
        if (winners.isEmpty()) {
            return "<color:#e74c3c>Did not finish!</color>";
        }
        return "<color:#f1c40f>" + winners.get(0).getName() + "</color> <color:#2ecc71>finished the course!</color>";
    }

    @Override
    public boolean shouldRespawn(ArenaConfig config, Participant participant) {
        return false; // Respawn handled by kill plane logic in onTick
    }

    @Override
    public int getRespawnDelayTicks(ArenaConfig config) {
        return 0;
    }

    @Override
    public void onMatchFinished(List<Participant> participants) {
        // Clean up state for any matches involving these participants
        List<UUID> toRemove = new ArrayList<>();
        for (Map.Entry<UUID, SpeedRunState> entry : matchStates.entrySet()) {
            SpeedRunState state = entry.getValue();
            for (Participant p : participants) {
                if (p.getUniqueId().equals(state.playerUuid)) {
                    toRemove.add(entry.getKey());
                    break;
                }
            }
        }
        for (UUID matchId : toRemove) {
            matchStates.remove(matchId);
        }
    }

    // Public accessors for HUD

    /**
     * Gets the current elapsed time in nanoseconds for a match.
     */
    public long getElapsedNanos(UUID matchId) {
        SpeedRunState state = matchStates.get(matchId);
        return state != null ? getAccumulatedTime(state) : 0;
    }

    /**
     * Gets the SpeedRunState for a match (used by HUD).
     */
    public SpeedRunState getSpeedRunState(UUID matchId) {
        return matchStates.get(matchId);
    }

    /**
     * Builds JSON data for stats submission.
     */
    public String getSpeedRunJsonData(UUID matchId, UUID playerUuid) {
        SpeedRunState state = matchStates.get(matchId);
        if (state == null) return null;

        JsonObject json = new JsonObject();
        json.addProperty("finish_time_nanos", state.finishTimeNanos);
        json.addProperty("is_dnf", state.isDNF);
        json.addProperty("lives_used", (state.personalBest != null ? 0 : 0) + // placeholder
            (getInitialLives(matchId) - state.livesRemaining));
        json.addProperty("checkpoints_reached", state.lastCheckpointReached + 1);
        json.addProperty("is_new_pb", state.isNewPB);

        if (state.checkpointSplitNanos != null) {
            com.google.gson.JsonArray splits = new com.google.gson.JsonArray();
            for (long split : state.checkpointSplitNanos) {
                splits.add(split);
            }
            json.add("checkpoint_splits", splits);
        }

        return json.toString();
    }

    private int getInitialLives(UUID matchId) {
        // Not stored separately, but we can look up from config via match
        // For simplicity, return maxRespawns default
        return 3;
    }

    // Zone helpers

    private boolean isInZone(ArenaConfig.CaptureZone zone, Vector3d pos) {
        if (zone == null || pos == null) return false;
        double x = pos.getX(), y = pos.getY(), z = pos.getZ();
        return x >= zone.getMinX() && x <= zone.getMaxX() &&
               y >= zone.getMinY() - Y_TOLERANCE && y <= zone.getMaxY() + Y_TOLERANCE &&
               z >= zone.getMinZ() && z <= zone.getMaxZ();
    }

    private void maintainPlayerStats(UUID playerUuid) {
        try {
            PlayerRef playerRef = Universe.get().getPlayer(playerUuid);
            if (playerRef == null) return;

            Ref<EntityStore> ref = playerRef.getReference();
            if (ref == null) return;

            Store<EntityStore> store = ref.getStore();
            if (store == null) return;

            EntityStatMap stats = store.getComponent(ref,
                EntityStatsModule.get().getEntityStatMapComponentType());
            if (stats == null) return;

            // Health
            int healthIndex = EntityStatType.getAssetMap().getIndex("health");
            EntityStatValue healthStat = stats.get(healthIndex);
            if (healthStat != null && healthStat.get() < healthStat.getMax()) {
                stats.setStatValue(healthIndex, healthStat.getMax());
            }

            // Stamina
            if (staminaStatValid) {
                try {
                    int staminaIndex = EntityStatType.getAssetMap().getIndex("stamina");
                    EntityStatValue staminaStat = stats.get(staminaIndex);
                    if (staminaStat != null && staminaStat.get() < staminaStat.getMax()) {
                        stats.setStatValue(staminaIndex, staminaStat.getMax());
                    }
                } catch (Exception e) {
                    staminaStatValid = false;
                    System.err.println("[SpeedRunGameMode] 'stamina' stat not found — stamina maintenance disabled. " + e.getMessage());
                }
            }
        } catch (Exception e) {
            // Silently ignore — player may have disconnected
        }
    }

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

    private void sendNotification(UUID playerUuid, String title, String message, NotificationStyle style) {
        PlayerRef playerRef = Universe.get().getPlayer(playerUuid);
        if (playerRef != null) {
            try {
                NotificationUtil.sendNotification(
                    playerRef.getPacketHandler(),
                    Message.raw(title),
                    Message.raw(message),
                    style
                );
            } catch (Exception e) {
                // Ignore
            }
        }
    }

    /**
     * Per-match state for speed run.
     */
    public static class SpeedRunState {
        public UUID playerUuid;
        public boolean timerRunning;
        public long accumulatedNanos;
        public long resumeNanoTime;
        public int lastCheckpointReached = -1; // -1 = at start
        public Set<Integer> triggeredCheckpoints = new HashSet<>();
        public long[] checkpointSplitNanos;
        public long finishTimeNanos;
        public int livesRemaining;
        public boolean finished;
        public boolean isDNF;
        public boolean inZone;
        public boolean isNewPB;
        public SpeedRunPB personalBest;
        public int killPlaneGraceTicks;
    }
}
