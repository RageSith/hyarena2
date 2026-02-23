package de.ragesith.hyarena2.economy;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import de.ragesith.hyarena2.event.EventBus;
import de.ragesith.hyarena2.event.economy.ArenaPointsEarnedEvent;
import de.ragesith.hyarena2.event.economy.ArenaPointsSpentEvent;
import de.ragesith.hyarena2.event.match.PlayerMatchRewardEvent;
import fi.sulku.hytale.TinyMsg;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages ArenaPoints and match reward distribution.
 * Subscribes to PlayerMatchRewardEvent for automatic per-player reward calculation.
 */
public class EconomyManager {

    private final EconomyConfig config;
    private final PlayerDataManager playerDataManager;
    private final EventBus eventBus;
    private HonorManager honorManager;

    // Cache last match reward per player (for VictoryHud display)
    private final Map<UUID, MatchRewardResult> lastMatchRewards = new ConcurrentHashMap<>();

    public EconomyManager(EconomyConfig config, PlayerDataManager playerDataManager, EventBus eventBus) {
        this.config = config;
        this.playerDataManager = playerDataManager;
        this.eventBus = eventBus;
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
     * Minigame matches earn half AP and no honor.
     */
    public MatchRewardResult rewardMatch(UUID uuid, boolean isWinner, int kills, String matchId, boolean hasBots, boolean minigame) {
        PlayerEconomyData data = playerDataManager.getData(uuid);
        if (data == null) return new MatchRewardResult(0, 0);

        // Calculate AP (halved for bot matches or minigames)
        int apBase = isWinner ? config.getApWinReward() : config.getApLossReward();
        int apKills = kills * config.getApPerKill();
        int totalAp = apBase + apKills;
        if (hasBots || minigame) {
            totalAp = totalAp / 2;
        }

        // Calculate Honor (minigames earn no honor)
        double totalHonor = 0;
        if (!minigame) {
            totalHonor = isWinner ? config.getHonorWinReward() : config.getHonorLossReward();
        }

        // Apply AP
        addArenaPoints(uuid, totalAp, "Match " + matchId + (isWinner ? " (win)" : " (loss)") + (hasBots ? " (bot)" : "") + (minigame ? " (minigame)" : ""));

        // Apply Honor via HonorManager (handles rank checks)
        if (honorManager != null && totalHonor > 0) {
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
     * Subscribes to PlayerMatchRewardEvent for per-player reward distribution.
     */
    public void subscribeToEvents() {
        eventBus.subscribe(PlayerMatchRewardEvent.class, this::onPlayerMatchReward);
        System.out.println("[EconomyManager] Subscribed to PlayerMatchRewardEvent");
    }

    private void onPlayerMatchReward(PlayerMatchRewardEvent event) {
        UUID playerUuid = event.getPlayerUuid();

        MatchRewardResult result = rewardMatch(playerUuid, event.isWinner(), event.getKills(),
            event.getMatchId().toString().substring(0, 8), event.hasBots(), event.isMinigame());

        // Send reward message to player
        String msg = "<color:#f1c40f>+" + result.getApEarned() + " AP</color>";
        if (!event.isMinigame()) {
            msg += " <color:#7f8c8d>|</color> "
                + "<color:#3498db>+" + (int) result.getHonorEarned() + " Honor</color>";
        }
        if (event.isMinigame()) {
            msg += " <color:#7f8c8d>(minigame, half AP, no honor)</color>";
        } else if (event.hasBots()) {
            msg += " <color:#7f8c8d>(bot match, half AP)</color>";
        }
        sendMessageToPlayer(playerUuid, msg);
    }

    /**
     * Sends a TinyMsg chat message to a player by UUID.
     * Iterates worlds to find the player (they may have been teleported to hub).
     */
    private void sendMessageToPlayer(UUID playerUuid, String message) {
        PlayerRef playerRef = Universe.get().getPlayer(playerUuid);
        if (playerRef == null) return;

        Ref<EntityStore> ref = playerRef.getReference();
        if (ref == null) return;
        Store<EntityStore> store = ref.getStore();
        if (store == null) return;
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) return;

        player.sendMessage(TinyMsg.parse(message));
    }
}
