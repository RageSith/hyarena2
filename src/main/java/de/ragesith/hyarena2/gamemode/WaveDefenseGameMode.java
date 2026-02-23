package de.ragesith.hyarena2.gamemode;

import de.ragesith.hyarena2.arena.ArenaConfig;
import de.ragesith.hyarena2.arena.Match;
import de.ragesith.hyarena2.bot.BotDifficulty;
import de.ragesith.hyarena2.bot.BotManager;
import de.ragesith.hyarena2.bot.BotParticipant;
import de.ragesith.hyarena2.config.Position;
import de.ragesith.hyarena2.economy.EconomyManager;
import de.ragesith.hyarena2.participant.Participant;
import de.ragesith.hyarena2.participant.ParticipantType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Wave Defense game mode — cooperative PvE where players fight escalating waves of bots.
 * No player respawns (one life), friendly fire disabled, bots hidden from HUD.
 * AP rewarded per wave. Match ends when all players die.
 */
public class WaveDefenseGameMode implements GameMode {
    private static final String ID = "wave_defense";
    private static final String DISPLAY_NAME = "Wave Defense";

    private static final int TICKS_PER_SECOND = 20;
    private static final int WAVE_BREAK_TICKS = 5 * TICKS_PER_SECOND; // 5 second break between waves
    private static final int MAX_BOTS_PER_WAVE = 20;

    // Per-match state keyed by matchId (avoids singleton state clobbering for concurrent matches)
    private final Map<UUID, WaveState> matchStates = new ConcurrentHashMap<>();

    private EconomyManager economyManager;

    private final Random random = new Random();

    public void setEconomyManager(EconomyManager economyManager) {
        this.economyManager = economyManager;
    }

    @Override
    public GameModeCategory getCategory() { return GameModeCategory.MINIGAME; }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getDisplayName() {
        return DISPLAY_NAME;
    }

    @Override
    public String getWebDescription() {
        return "Cooperative PvE survival. Team up against endless waves of bots that grow stronger each round. No respawns — survive as long as you can.";
    }

    @Override
    public String getDescription() {
        return "Group { LayoutMode: Top;"
            + " Label { Text: \"Wave Defense\"; Anchor: (Height: 28); Style: (FontSize: 18, TextColor: #e8c872, RenderBold: true); }"
            + " Label { Text: \"Fight endless waves of enemies with your team. Survive as long as you can!\"; Anchor: (Height: 40, Top: 4); Style: (FontSize: 13, TextColor: #b7cedd, Wrap: true); }"
            + " Label { Text: \"Rules\"; Anchor: (Height: 24, Top: 12); Style: (FontSize: 15, TextColor: #e8c872, RenderBold: true); }"
            + " Label { Text: \"\u2022 Cooperative PvE \u2014 fight together against waves of bots\"; Anchor: (Height: 18, Top: 4); Style: (FontSize: 12, TextColor: #96a9be); }"
            + " Label { Text: \"\u2022 No respawns \u2014 once you die, you're out\"; Anchor: (Height: 18, Top: 2); Style: (FontSize: 12, TextColor: #96a9be); }"
            + " Label { Text: \"\u2022 Friendly fire is disabled \u2014 you can't hurt teammates\"; Anchor: (Height: 18, Top: 2); Style: (FontSize: 12, TextColor: #96a9be); }"
            + " Label { Text: \"\u2022 Each wave gets harder with more and stronger enemies\"; Anchor: (Height: 18, Top: 2); Style: (FontSize: 12, TextColor: #96a9be); }"
            + " Label { Text: \"\u2022 Earn AP for each wave survived\"; Anchor: (Height: 18, Top: 2); Style: (FontSize: 12, TextColor: #96a9be); }"
            + " Label { Text: \"Tips\"; Anchor: (Height: 24, Top: 12); Style: (FontSize: 15, TextColor: #e8c872, RenderBold: true); }"
            + " Label { Text: \"\u2022 Stick together \u2014 focus fire on one enemy at a time\"; Anchor: (Height: 18, Top: 4); Style: (FontSize: 12, TextColor: #96a9be); }"
            + " Label { Text: \"\u2022 Use the break between waves to reposition\"; Anchor: (Height: 18, Top: 2); Style: (FontSize: 12, TextColor: #96a9be); }"
            + " Label { Text: \"\u2022 Watch your health \u2014 there are no second chances\"; Anchor: (Height: 18, Top: 2); Style: (FontSize: 12, TextColor: #96a9be); }"
            + " }";
    }

    @Override
    public void onMatchStart(ArenaConfig config, List<Participant> participants) {
        // All participants start alive
        for (Participant p : participants) {
            p.setAlive(true);
        }
    }

    @Override
    public void onGameplayBegin(ArenaConfig config, List<Participant> participants) {
        // Initialize wave state for this match — find the matchId from the first participant context
        // (onGameplayBegin doesn't have match reference, but onTick does — state will be created there)
        for (Participant p : participants) {
            p.sendMessage("<gradient:#e74c3c:#c0392b><b>WAVE DEFENSE!</b></gradient>");
            p.sendMessage("<color:#f1c40f>Survive as long as you can! Wave 1 incoming...</color>");
        }
    }

    @Override
    public void onTick(Match match, ArenaConfig config, List<Participant> participants, int tickCount) {
        UUID matchId = match.getMatchId();
        WaveState state = matchStates.computeIfAbsent(matchId, k -> new WaveState());
        state.match = match;

        if (state.waveInProgress) {
            // Count alive wave bots
            long aliveWaveBots = 0;
            for (Participant p : participants) {
                if (p.isAlive() && p.getType() == ParticipantType.BOT && p instanceof BotParticipant bp && bp.isWaveEnemy()) {
                    aliveWaveBots++;
                }
            }

            if (aliveWaveBots == 0) {
                // Wave cleared!
                state.waveInProgress = false;
                state.waveBreakTicks = WAVE_BREAK_TICKS;

                // Add bonus time on wave clear (only for timed matches)
                int matchDuration = config.getMatchDurationSeconds();
                int waveClearBonus = config.getWaveBonusSecondsPerWaveClear();
                if (matchDuration > 0 && waveClearBonus > 0) {
                    match.addBonusTime(waveClearBonus * TICKS_PER_SECOND);
                }

                // Award AP to surviving players
                int apReward = 1 + state.currentWave / 3;
                for (Participant p : participants) {
                    if (p.isAlive() && p.getType() == ParticipantType.PLAYER) {
                        state.apEarnedPerPlayer.merge(p.getUniqueId(), apReward, Integer::sum);
                        if (economyManager != null) {
                            economyManager.addArenaPoints(p.getUniqueId(), apReward,
                                "wave_defense_wave_" + state.currentWave);
                        }
                        String bonusText = (matchDuration > 0 && waveClearBonus > 0) ? " +" + waveClearBonus + "s" : "";
                        p.sendMessage("<color:#f1c40f>Wave " + state.currentWave + " cleared! +" + apReward + " AP" + bonusText + "</color>");
                    }
                }
            }
        } else {
            // Wave break countdown
            state.waveBreakTicks--;

            // Countdown notifications at 3, 2, 1
            int secondsLeft = state.waveBreakTicks / TICKS_PER_SECOND;
            if (state.waveBreakTicks % TICKS_PER_SECOND == 0 && secondsLeft > 0 && secondsLeft <= 3) {
                for (Participant p : participants) {
                    if (p.isAlive() && p.getType() == ParticipantType.PLAYER) {
                        p.sendMessage("<color:#f39c12>Wave " + (state.currentWave + 1) + " in " + secondsLeft + "...</color>");
                    }
                }
            }

            if (state.waveBreakTicks <= 0) {
                // Spawn next wave
                state.currentWave++;
                state.waveInProgress = true;
                spawnWave(match, config, participants, state);
            }
        }
    }

    /**
     * Spawns a wave of bots in the arena.
     */
    private void spawnWave(Match match, ArenaConfig config, List<Participant> participants, WaveState state) {
        BotManager botManager = match.getBotManager();
        if (botManager == null) {
            System.err.println("[WaveDefense] Cannot spawn wave - botManager is null");
            return;
        }

        int wave = state.currentWave;
        int botCount = Math.min(3 + wave * 2, MAX_BOTS_PER_WAVE);

        // Difficulty scales with wave number
        BotDifficulty difficulty;
        if (wave <= 3) {
            difficulty = BotDifficulty.EASY;
        } else if (wave <= 7) {
            difficulty = BotDifficulty.MEDIUM;
        } else {
            difficulty = BotDifficulty.HARD;
        }

        // Health multiplier increases per wave
        double healthMultiplier = 1.0 + (wave - 1) * 0.15;

        // Get spawn points (prefer waveSpawnPoints, fallback to regular spawnPoints)
        List<ArenaConfig.SpawnPoint> spawnPoints = config.getWaveSpawnPoints();
        if (spawnPoints == null || spawnPoints.isEmpty()) {
            spawnPoints = config.getSpawnPoints();
        }

        if (spawnPoints == null || spawnPoints.isEmpty()) {
            System.err.println("[WaveDefense] No spawn points for wave bots");
            return;
        }

        // Get a kit for bots (first allowed kit, or null)
        String botKit = null;
        if (config.getAllowedKits() != null && !config.getAllowedKits().isEmpty()) {
            botKit = config.getAllowedKits().get(0);
        }

        // Broadcast wave start
        for (Participant p : participants) {
            if (p.isAlive() && p.getType() == ParticipantType.PLAYER) {
                p.sendMessage("<gradient:#e74c3c:#c0392b><b>Wave " + wave + "!</b></gradient> <color:#f39c12>" + botCount + " enemies incoming!</color>");
            }
        }

        System.out.println("[WaveDefense] Spawning wave " + wave + ": " + botCount + " bots (difficulty: " + difficulty + ", health: " + String.format("%.0f%%", healthMultiplier * 100) + ")");

        for (int i = 0; i < botCount; i++) {
            ArenaConfig.SpawnPoint sp = spawnPoints.get(random.nextInt(spawnPoints.size()));
            Position spawnPos = new Position(sp.getX(), sp.getY(), sp.getZ(), sp.getYaw(), sp.getPitch());

            BotParticipant bot = botManager.spawnBot(match, spawnPos, botKit, difficulty);
            if (bot != null) {
                bot.setWaveEnemy(true);
                bot.setWaveLevel(wave);

                // Update nameplate with level indicator
                botManager.updateNameplate(bot, bot.getName() + " Lv." + wave);

                // Scale health
                double scaledMaxHealth = bot.getMaxHealth() * healthMultiplier;
                bot.setMaxHealth(scaledMaxHealth);
                bot.setHealth(scaledMaxHealth);

                match.addBot(bot);
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

        // Add bonus time per wave mob killed (only for timed matches)
        if (victim.getType() == ParticipantType.BOT && victim instanceof BotParticipant bp && bp.isWaveEnemy()) {
            int matchDuration = config.getMatchDurationSeconds();
            int killBonus = config.getWaveBonusSecondsPerKill();
            if (matchDuration > 0 && killBonus > 0) {
                for (WaveState state : matchStates.values()) {
                    if (state.match != null && state.match.hasParticipant(victim.getUniqueId())) {
                        state.match.addBonusTime(killBonus * TICKS_PER_SECOND);
                        break;
                    }
                }
            }
        }

        // Record the last fully cleared wave for this player
        if (victim.getType() == ParticipantType.PLAYER) {
            for (WaveState state : matchStates.values()) {
                if (state.apEarnedPerPlayer.containsKey(victim.getUniqueId())
                        || participants.stream().anyMatch(p -> p.getUniqueId().equals(victim.getUniqueId()))) {
                    int cleared = state.waveInProgress ? state.currentWave - 1 : state.currentWave;
                    state.wavesSurvivedPerPlayer.put(victim.getUniqueId(), Math.max(0, cleared));
                    break;
                }
            }
        }

        // Check if match should end (all players dead)
        return shouldMatchEnd(config, participants);
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
        // Disable friendly fire: players can't damage other players
        if (attacker.getType() == ParticipantType.PLAYER && victim.getType() == ParticipantType.PLAYER) {
            return false;
        }
        return true;
    }

    @Override
    public boolean shouldMatchEnd(ArenaConfig config, List<Participant> participants) {
        // Match ends when no alive PLAYER participants remain
        for (Participant p : participants) {
            if (p.getType() == ParticipantType.PLAYER && p.isAlive()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public List<UUID> getWinners(ArenaConfig config, List<Participant> participants) {
        // Endless mode — always a defeat, no winners
        return new ArrayList<>();
    }

    // Stash the wave count from the last ended match for getVictoryMessage()
    // (since getVictoryMessage doesn't receive a matchId)
    private volatile int lastEndedWave = 0;

    @Override
    public String getVictoryMessage(ArenaConfig config, List<Participant> winners) {
        if (lastEndedWave > 0) {
            return "<gradient:#e74c3c:#f39c12><b>Wave " + lastEndedWave + " reached!</b></gradient> <color:#f1c40f>Well fought!</color>";
        }
        return "<color:#e74c3c>Defeated!</color> <color:#f1c40f>Better luck next time!</color>";
    }

    @Override
    public boolean shouldRespawn(ArenaConfig config, Participant participant) {
        // No respawns — one life
        return false;
    }

    @Override
    public int getRespawnDelayTicks(ArenaConfig config) {
        return 0;
    }

    @Override
    public int getParticipantScore(UUID participantId) {
        // Score = current wave across all matches this participant is in
        for (WaveState state : matchStates.values()) {
            return state.currentWave;
        }
        return 0;
    }

    @Override
    public String getScoreLabel() {
        return "Wave";
    }

    @Override
    public int getScoreTarget(ArenaConfig config) {
        return -1; // No target — endless
    }

    @Override
    public void onMatchFinished(List<Participant> participants) {
        // Find the matching state entry by checking which state's apEarnedPerPlayer
        // contains any of the finished match's participant UUIDs
        Set<UUID> participantIds = new HashSet<>();
        for (Participant p : participants) {
            participantIds.add(p.getUniqueId());
        }

        matchStates.entrySet().removeIf(entry -> {
            WaveState state = entry.getValue();
            for (UUID id : state.apEarnedPerPlayer.keySet()) {
                if (participantIds.contains(id)) {
                    lastEndedWave = state.currentWave;
                    return true;
                }
            }
            // Also remove if wave > 0 but no AP was earned (e.g., all died on wave 1)
            // In this case, match any non-zero state as a fallback
            if (state.currentWave > 0) {
                lastEndedWave = state.currentWave;
                return true;
            }
            return false;
        });
    }

    /**
     * Gets the current wave number for a match (for HUD display).
     */
    public int getCurrentWave(UUID matchId) {
        WaveState state = matchStates.get(matchId);
        return state != null ? state.currentWave : 0;
    }

    /**
     * Gets the number of alive wave enemy bots for a match (for HUD display).
     */
    public int getAliveEnemyCount(Match match) {
        int count = 0;
        for (Participant p : match.getParticipants()) {
            if (p.isAlive() && p.getType() == ParticipantType.BOT && p instanceof BotParticipant bp && bp.isWaveEnemy()) {
                count++;
            }
        }
        return count;
    }

    @Override
    public int getParticipantWavesSurvived(UUID matchId, UUID participantId) {
        WaveState state = matchStates.get(matchId);
        if (state == null) return -1;
        return state.wavesSurvivedPerPlayer.getOrDefault(participantId, -1);
    }

    /**
     * Per-match wave state.
     */
    private static class WaveState {
        int currentWave = 0;
        int waveBreakTicks = 3 * TICKS_PER_SECOND; // Short initial delay before wave 1
        boolean waveInProgress = false;
        Match match; // Stored from onTick for use in onParticipantKilled
        final Map<UUID, Integer> apEarnedPerPlayer = new HashMap<>();
        final Map<UUID, Integer> wavesSurvivedPerPlayer = new HashMap<>();
    }
}
