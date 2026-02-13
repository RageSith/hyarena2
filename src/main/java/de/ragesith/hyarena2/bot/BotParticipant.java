package de.ragesith.hyarena2.bot;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.npc.INonPlayerCharacter;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import de.ragesith.hyarena2.config.Position;
import de.ragesith.hyarena2.participant.Participant;
import de.ragesith.hyarena2.participant.ParticipantType;

import java.util.UUID;

/**
 * Bot implementation of the Participant interface.
 * Tracks internal health and state rather than relying on entity stats.
 */
public class BotParticipant implements Participant {
    private static final double BASE_HEALTH = 100.0;

    private final UUID botUuid;
    private final String botName;
    private final BotDifficulty difficulty;
    private final String roleId;

    // Stats
    private boolean alive;
    private int kills;
    private int deaths;
    private double damageDealt;
    private double damageTaken;

    // Health (internal tracking)
    private double health;
    private double maxHealth;

    // Immunity
    private volatile long immunityEndTime = 0;

    // Kit
    private String selectedKitId;

    // AI controller
    private BotAI ai;
    private BotBrain brain;

    // Entity references (set after spawning)
    private Ref<EntityStore> entityRef;
    private UUID entityUuid;
    private INonPlayerCharacter npc;
    private NPCEntity npcEntity;

    // Position tracking
    private Position spawnPosition;
    private Position currentPosition;

    // Combat timing
    private long lastAttackTime = 0;

    // Last attacker tracking for kill attribution on environmental deaths
    private UUID lastAttackerUuid;
    private long lastDamageTimestamp;

    // Flag: NPC state needs forced re-apply after taking damage (hit reaction may interrupt current state)
    private volatile boolean needsStateRefresh = false;

    public BotParticipant(String name, BotDifficulty difficulty, String roleId) {
        this.botUuid = UUID.randomUUID();
        this.botName = name;
        this.difficulty = difficulty;
        this.roleId = roleId;
        this.alive = true;
        this.kills = 0;
        this.deaths = 0;
        this.damageDealt = 0.0;
        this.damageTaken = 0.0;
        this.maxHealth = difficulty.calculateMaxHealth(BASE_HEALTH);
        this.health = this.maxHealth;
    }

    // ========== Participant Interface ==========

    @Override
    public UUID getUniqueId() {
        return botUuid;
    }

    @Override
    public String getName() {
        return botName;
    }

    @Override
    public ParticipantType getType() {
        return ParticipantType.BOT;
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
        // Bots don't receive chat messages - no-op
    }

    @Override
    public boolean isValid() {
        // Bot is valid if entity reference exists and is valid
        return entityRef != null && entityRef.isValid();
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

    @Override
    public UUID getLastAttackerUuid() {
        return lastAttackerUuid;
    }

    @Override
    public long getLastDamageTimestamp() {
        return lastDamageTimestamp;
    }

    @Override
    public void setLastAttacker(UUID attackerUuid) {
        this.lastAttackerUuid = attackerUuid;
        this.lastDamageTimestamp = System.currentTimeMillis();
    }

    // ========== Bot-Specific Methods ==========

    /**
     * Gets the bot's difficulty level.
     */
    public BotDifficulty getDifficulty() {
        return difficulty;
    }

    /**
     * Gets the NPC role ID assigned to this bot (persists through respawns).
     */
    public String getRoleId() {
        return roleId;
    }

    /**
     * Gets the bot's current health.
     */
    public double getHealth() {
        return health;
    }

    /**
     * Sets the bot's current health.
     */
    public void setHealth(double health) {
        this.health = Math.max(0, Math.min(health, maxHealth));
    }

    /**
     * Gets the bot's max health.
     */
    public double getMaxHealth() {
        return maxHealth;
    }

    /**
     * Sets the bot's max health.
     */
    public void setMaxHealth(double maxHealth) {
        this.maxHealth = maxHealth;
    }

    /**
     * Applies damage to the bot.
     * @param damage amount of damage to apply
     * @return true if the bot died from this damage
     */
    public boolean takeDamage(double damage) {
        if (!alive || isImmune()) {
            return false;
        }

        this.health -= damage;
        this.damageTaken += damage;
        this.needsStateRefresh = true;

        if (this.health <= 0) {
            this.health = 0;
            this.alive = false;
            return true;
        }
        return false;
    }

    /**
     * Heals the bot by the specified amount.
     */
    public void heal(double amount) {
        this.health = Math.min(maxHealth, health + amount);
    }

    /**
     * Resets the bot to full health.
     */
    public void resetHealth() {
        this.health = maxHealth;
        this.alive = true;
    }

    /**
     * Gets the health percentage (0.0 to 1.0).
     */
    public double getHealthPercentage() {
        return maxHealth > 0 ? health / maxHealth : 0;
    }

    /**
     * Returns true if the bot needs its NPC state forcibly re-applied
     * (e.g. after taking damage, the NPC's native hit reaction may have interrupted the current state).
     * Calling this consumes the flag.
     */
    public boolean consumeStateRefresh() {
        if (needsStateRefresh) {
            needsStateRefresh = false;
            return true;
        }
        return false;
    }

    /**
     * Gets the AI controller for this bot.
     */
    public BotAI getAI() {
        return ai;
    }

    /**
     * Sets the AI controller for this bot.
     */
    public void setAI(BotAI ai) {
        this.ai = ai;
    }

    /**
     * Gets the BotBrain decision engine.
     */
    public BotBrain getBrain() {
        return brain;
    }

    /**
     * Sets the BotBrain decision engine.
     */
    public void setBrain(BotBrain brain) {
        this.brain = brain;
    }

    /**
     * Gets the Hytale entity reference.
     */
    public Ref<EntityStore> getEntityRef() {
        return entityRef;
    }

    /**
     * Sets the Hytale entity reference.
     */
    public void setEntityRef(Ref<EntityStore> entityRef) {
        this.entityRef = entityRef;
    }

    /**
     * Gets the entity's UUID (from Hytale's UUIDComponent).
     */
    public UUID getEntityUuid() {
        return entityUuid;
    }

    /**
     * Sets the entity's UUID.
     */
    public void setEntityUuid(UUID entityUuid) {
        this.entityUuid = entityUuid;
    }

    /**
     * Gets the NPC interface.
     */
    public INonPlayerCharacter getNpc() {
        return npc;
    }

    /**
     * Sets the NPC interface.
     */
    public void setNpc(INonPlayerCharacter npc) {
        this.npc = npc;
    }

    /**
     * Gets the NPCEntity for role access.
     */
    public NPCEntity getNpcEntity() {
        return npcEntity;
    }

    /**
     * Sets the NPCEntity.
     */
    public void setNpcEntity(NPCEntity npcEntity) {
        this.npcEntity = npcEntity;
    }

    /**
     * Gets the spawn position.
     */
    public Position getSpawnPosition() {
        return spawnPosition;
    }

    /**
     * Sets the spawn position.
     */
    public void setSpawnPosition(Position spawnPosition) {
        this.spawnPosition = spawnPosition;
    }

    /**
     * Gets the current position.
     */
    public Position getCurrentPosition() {
        return currentPosition;
    }

    /**
     * Sets the current position.
     */
    public void setCurrentPosition(Position currentPosition) {
        this.currentPosition = currentPosition;
    }

    /**
     * Gets the last attack timestamp.
     */
    public long getLastAttackTime() {
        return lastAttackTime;
    }

    /**
     * Sets the last attack timestamp.
     */
    public void setLastAttackTime(long lastAttackTime) {
        this.lastAttackTime = lastAttackTime;
    }

    /**
     * Checks if the bot can attack (cooldown expired).
     */
    public boolean canAttack() {
        return System.currentTimeMillis() - lastAttackTime >= difficulty.getAttackCooldownMs();
    }

    @Override
    public String toString() {
        return "BotParticipant{" +
                "name='" + botName + '\'' +
                ", uuid=" + botUuid +
                ", difficulty=" + difficulty +
                ", health=" + health + "/" + maxHealth +
                ", alive=" + alive +
                '}';
    }
}
