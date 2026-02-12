package de.ragesith.hyarena2.economy;

import de.ragesith.hyarena2.arena.Match;
import de.ragesith.hyarena2.arena.MatchManager;
import de.ragesith.hyarena2.event.EventBus;
import de.ragesith.hyarena2.event.economy.ArenaPointsEarnedEvent;
import de.ragesith.hyarena2.event.economy.ArenaPointsSpentEvent;
import de.ragesith.hyarena2.event.match.MatchEndedEvent;
import de.ragesith.hyarena2.participant.Participant;
import de.ragesith.hyarena2.participant.ParticipantType;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages ArenaPoints and match reward distribution.
 * Subscribes to MatchEndedEvent for automatic reward calculation.
 */
public class EconomyManager {

    private final EconomyConfig config;
    private final PlayerDataManager playerDataManager;
    private final EventBus eventBus;
    private MatchManager matchManager;
    private HonorManager honorManager;

    // Cache last match reward per player (for VictoryHud display)
    private final Map<UUID, MatchRewardResult> lastMatchRewards = new ConcurrentHashMap<>();

    public EconomyManager(EconomyConfig config, PlayerDataManager playerDataManager, EventBus eventBus) {
        this.config = config;
        this.playerDataManager = playerDataManager;
        this.eventBus = eventBus;
    }

    public void setMatchManager(MatchManager matchManager) {
        this.matchManager = matchManager;
    }

    public void setHonorManager(HonorManager honorManager) {
        this.honorManager = honorManager;
    }

    // ========== Player Lifecycle ==========

    public void loadPlayer(UUID uuid, String name) {
        playerDataManager.loadOrCreate(uuid, name);
    }

    public void unloadPlayer(UUID uuid) {
        lastMatchRewards.remove(uuid);
        playerDataManager.unloadPlayer(uuid);
    }

    public void savePlayer(UUID uuid) {
        playerDataManager.save(uuid);
    }

    public void saveAll() {
        playerDataManager.saveAll();
    }

    // ========== AP Operations ==========

    public int getArenaPoints(UUID uuid) {
        PlayerEconomyData data = playerDataManager.getData(uuid);
        return data != null ? data.getArenaPoints() : 0;
    }

    public void addArenaPoints(UUID uuid, int amount, String reason) {
        PlayerEconomyData data = playerDataManager.getData(uuid);
        if (data == null || amount <= 0) return;

        data.setArenaPoints(data.getArenaPoints() + amount);
        eventBus.publish(new ArenaPointsEarnedEvent(uuid, amount, reason));
    }

    public boolean spendArenaPoints(UUID uuid, int amount, String reason) {
        PlayerEconomyData data = playerDataManager.getData(uuid);
        if (data == null || amount <= 0) return false;

        if (data.getArenaPoints() < amount) return false;

        data.setArenaPoints(data.getArenaPoints() - amount);
        eventBus.publish(new ArenaPointsSpentEvent(uuid, amount, reason));
        return true;
    }

    // ========== Honor Read ==========

    public double getHonor(UUID uuid) {
        PlayerEconomyData data = playerDataManager.getData(uuid);
        return data != null ? data.getHonor() : 0;
    }

    // ========== Data Access ==========

    public PlayerEconomyData getPlayerData(UUID uuid) {
        return playerDataManager.getData(uuid);
    }

    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }

    public EconomyConfig getConfig() {
        return config;
    }

    // ========== Match Rewards ==========

    /**
     * Calculates and applies match rewards for a player.
     * Bot matches (any match with at least 1 bot) earn half AP.
     */
    public MatchRewardResult rewardMatch(UUID uuid, boolean isWinner, int kills, String matchId, boolean hasBots) {
        PlayerEconomyData data = playerDataManager.getData(uuid);
        if (data == null) return new MatchRewardResult(0, 0);

        // Calculate AP (halved for bot matches)
        int apBase = isWinner ? config.getApWinReward() : config.getApLossReward();
        int apKills = kills * config.getApPerKill();
        int totalAp = apBase + apKills;
        if (hasBots) {
            totalAp = totalAp / 2;
        }

        // Calculate Honor
        double totalHonor = isWinner ? config.getHonorWinReward() : config.getHonorLossReward();

        // Apply AP
        addArenaPoints(uuid, totalAp, "Match " + matchId + (isWinner ? " (win)" : " (loss)") + (hasBots ? " (bot)" : ""));

        // Apply Honor via HonorManager (handles rank checks)
        if (honorManager != null) {
            honorManager.addHonor(uuid, totalHonor);
        }

        MatchRewardResult result = new MatchRewardResult(totalAp, totalHonor);
        lastMatchRewards.put(uuid, result);
        return result;
    }

    /**
     * Gets the last match reward for a player (for VictoryHud display).
     */
    public MatchRewardResult getLastMatchReward(UUID uuid) {
        return lastMatchRewards.get(uuid);
    }

    // ========== Event Subscription ==========

    /**
     * Subscribes to MatchEndedEvent for automatic reward distribution.
     */
    public void subscribeToEvents() {
        eventBus.subscribe(MatchEndedEvent.class, this::onMatchEnded);
        System.out.println("[EconomyManager] Subscribed to MatchEndedEvent");
    }

    private void onMatchEnded(MatchEndedEvent event) {
        if (matchManager == null) return;

        Match match = matchManager.getMatch(event.getMatchId());
        if (match == null) return;

        // Bot matches (any match with at least 1 bot) earn half AP
        boolean hasBots = match.getParticipants().stream()
            .anyMatch(p -> p.getType() == ParticipantType.BOT);

        for (Participant participant : match.getParticipants()) {
            if (participant.getType() != ParticipantType.PLAYER) continue;

            UUID playerUuid = participant.getUniqueId();
            boolean isWinner = event.getWinners().contains(playerUuid);
            int kills = participant.getKills();

            MatchRewardResult result = rewardMatch(playerUuid, isWinner, kills,
                event.getMatchId().toString().substring(0, 8), hasBots);

            // Send reward message to player
            String msg = "<color:#f1c40f>+" + result.getApEarned() + " AP</color>"
                + " <color:#7f8c8d>|</color> "
                + "<color:#3498db>+" + (int) result.getHonorEarned() + " Honor</color>";
            if (hasBots) {
                msg += " <color:#7f8c8d>(bot match, half AP)</color>";
            }
            participant.sendMessage(msg);
        }
    }
}
