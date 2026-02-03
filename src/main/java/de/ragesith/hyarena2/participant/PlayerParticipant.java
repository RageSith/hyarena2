package de.ragesith.hyarena2.participant;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import fi.sulku.hytale.TinyMsg;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.UUID;

/**
 * Player implementation of the Participant interface.
 * Never stores Player references long-term - always fetches fresh from Universe.
 */
public class PlayerParticipant implements Participant {
    private final UUID playerUuid;
    private final String playerName;
    private boolean alive;
    private int kills;
    private int deaths;
    private double damageDealt;
    private double damageTaken;
    private volatile long immunityEndTime = 0;
    private String selectedKitId;

    public PlayerParticipant(UUID playerUuid, String playerName) {
        this.playerUuid = playerUuid;
        this.playerName = playerName;
        this.alive = true;
        this.kills = 0;
        this.deaths = 0;
        this.damageDealt = 0.0;
        this.damageTaken = 0.0;
    }

    /**
     * Gets a fresh Player reference from the Universe.
     * @return The player, or null if offline
     */
    private Player getPlayer() {
        PlayerRef playerRef = Universe.get().getPlayer(playerUuid);
        if (playerRef == null) return null;

        Ref<EntityStore> ref = playerRef.getReference();
        if (ref == null) return null;

        Store<EntityStore> store = ref.getStore();
        if (store == null) return null;

        return store.getComponent(ref, Player.getComponentType());
    }

    @Override
    public UUID getUniqueId() {
        return playerUuid;
    }

    @Override
    public String getName() {
        return playerName;
    }

    @Override
    public ParticipantType getType() {
        return ParticipantType.PLAYER;
    }

    @Override
    public boolean isAlive() {
        return alive;
    }

    @Override
    public void setAlive(boolean alive) {
        this.alive = alive;
    }

    @Override
    public int getKills() {
        return kills;
    }

    @Override
    public void addKill() {
        this.kills++;
    }

    @Override
    public int getDeaths() {
        return deaths;
    }

    @Override
    public void addDeath() {
        this.deaths++;
    }

    @Override
    public double getDamageDealt() {
        return damageDealt;
    }

    @Override
    public void addDamageDealt(double damage) {
        this.damageDealt += damage;
    }

    @Override
    public double getDamageTaken() {
        return damageTaken;
    }

    @Override
    public void addDamageTaken(double damage) {
        this.damageTaken += damage;
    }

    @Override
    public void sendMessage(String message) {
        PlayerRef playerRef = Universe.get().getPlayer(playerUuid);
        if (playerRef == null) return;

        // Find the player's world by checking all worlds
        for (World world : Universe.get().getWorlds().values()) {
            world.execute(() -> {
                try {
                    Ref<EntityStore> ref = playerRef.getReference();
                    if (ref == null) return;

                    Store<EntityStore> store = ref.getStore();
                    if (store == null) return;

                    Player player = store.getComponent(ref, Player.getComponentType());
                    if (player != null && player.getWorld() == world) {
                        player.sendMessage(TinyMsg.parse(message));
                    }
                } catch (Exception e) {
                    // Not on this world, ignore
                }
            });
        }
    }

    @Override
    public boolean isValid() {
        // Check if player is online without accessing entity store (thread-safe)
        PlayerRef playerRef = Universe.get().getPlayer(playerUuid);
        return playerRef != null;
    }

    @Override
    public void grantImmunity(long durationMs) {
        this.immunityEndTime = System.currentTimeMillis() + durationMs;
    }

    @Override
    public boolean isImmune() {
        return System.currentTimeMillis() < immunityEndTime;
    }

    @Override
    public String getSelectedKitId() {
        return selectedKitId;
    }

    @Override
    public void setSelectedKitId(String kitId) {
        this.selectedKitId = kitId;
    }
}
