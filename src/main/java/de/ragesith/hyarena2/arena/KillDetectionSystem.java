package de.ragesith.hyarena2.arena;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatsModule;
import com.hypixel.hytale.server.core.modules.entitystats.asset.EntityStatType;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import de.ragesith.hyarena2.bot.BotManager;
import de.ragesith.hyarena2.bot.BotParticipant;
import de.ragesith.hyarena2.participant.Participant;
import de.ragesith.hyarena2.participant.ParticipantType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Damage detection system that:
 * - Allows damage ONLY during IN_PROGRESS matches
 * - Cancels all damage outside matches (prevents hub PvP, countdown damage, etc.)
 * - Detects kills and notifies the match
 * - Handles both player and bot victims/attackers
 * - Grants SignatureEnergy to attackers when damage is cancelled (compensates for bypassed weapon mechanics)
 */
public class KillDetectionSystem extends DamageEventSystem {
    private final MatchManager matchManager;
    private BotManager botManager;

    // Signature Energy amount to add when hitting a bot or dealing fatal damage
    // This compensates for cancelled damage not triggering normal weapon signature charge
    private static final float SIGNATURE_ENERGY_PER_HIT = 2.5f;

    // Cache the SignatureEnergy stat index
    private int signatureEnergyIndex = -1;

    // Running damage accumulator for multi-hit same-tick detection
    private final Map<UUID, Float> tickDamageAccumulator = new HashMap<>();
    private final Map<UUID, Float> tickBaseHealth = new HashMap<>();
    private long lastFilterNanos = 0;
    private static final long TICK_GAP_NANOS = 10_000_000; // 10ms — half a tick at 20 TPS

    public KillDetectionSystem(MatchManager matchManager) {
        this.matchManager = matchManager;
    }

    /**
     * Gets the SignatureEnergy stat index, caching it on first use.
     */
    private int getSignatureEnergyIndex() {
        if (signatureEnergyIndex == -1) {
            signatureEnergyIndex = EntityStatType.getAssetMap().getIndex("SignatureEnergy");
        }
        return signatureEnergyIndex;
    }

    /**
     * Sets the bot manager for bot damage handling.
     */
    public void setBotManager(BotManager botManager) {
        this.botManager = botManager;
    }

    @Override
    public SystemGroup<EntityStore> getGroup() {
        // Use Filter group - runs BEFORE damage is applied
        return DamageModule.get().getFilterDamageGroup();
    }

    @Override
    public Query<EntityStore> getQuery() {
        return Query.any();
    }

    @Override
    public void handle(int index, ArchetypeChunk<EntityStore> chunk,
                       Store<EntityStore> store, CommandBuffer<EntityStore> commandBuffer,
                       Damage damage) {
        // Tick boundary detection: if >10ms since last call, we're in a new tick
        long now = System.nanoTime();
        if (now - lastFilterNanos > TICK_GAP_NANOS) {
            tickDamageAccumulator.clear();
            tickBaseHealth.clear();
        }
        lastFilterNanos = now;

        try {
            // Get victim entity reference
            Ref<EntityStore> victimRef = chunk.getReferenceTo(index);

            // Try to identify victim as player first
            Player victimPlayer = store.getComponent(victimRef, Player.getComponentType());
            if (victimPlayer != null) {
                handlePlayerVictim(victimRef, store, damage);
                return;
            }

            // Not a player - check if it's a bot
            if (botManager != null) {
                UUIDComponent uuidComponent = store.getComponent(victimRef, UUIDComponent.getComponentType());
                if (uuidComponent != null) {
                    BotParticipant botVictim = botManager.getBotByEntityUuid(uuidComponent.getUuid());
                    if (botVictim != null) {
                        handleBotVictim(botVictim, victimRef, store, damage);
                        return;
                    }
                }
            }

            // Unknown entity type - allow normal damage processing
        } catch (Exception e) {
            System.err.println("Error in KillDetectionSystem: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Handles damage to a player victim.
     */
    private void handlePlayerVictim(Ref<EntityStore> victimRef, Store<EntityStore> store, Damage damage) {
        // Get PlayerRef component
        PlayerRef victimPlayerRef = store.getComponent(victimRef, PlayerRef.getComponentType());
        if (victimPlayerRef == null) {
            return;
        }

        UUID victimUuid = victimPlayerRef.getUuid();

        // Get player's match
        Match match = matchManager.getPlayerMatch(victimUuid);

        // ONLY allow damage if player is in an IN_PROGRESS match AND alive
        if (match == null || match.getState() != MatchState.IN_PROGRESS) {
            damage.setCancelled(true);
            return;
        }

        // Get participant
        Participant participant = match.getParticipant(victimUuid);
        if (participant == null) {
            damage.setCancelled(true);
            return;
        }

        // Check if victim is alive in match
        if (!participant.isAlive()) {
            damage.setCancelled(true);
            return;
        }

        // Check immunity (respawn protection)
        if (participant.isImmune()) {
            damage.setCancelled(true);
            return;
        }

        // Get attacker info (UUID and entity ref for signature energy)
        Ref<EntityStore> attackerEntityRef = getAttackerEntityRef(damage.getSource());
        UUID attackerUuid = getAttackerUuid(damage.getSource(), store);

        // Get victim's current health
        EntityStatMap stats = store.getComponent(victimRef,
                EntityStatsModule.get().getEntityStatMapComponentType());

        if (stats == null) {
            return;
        }

        int healthIndex = EntityStatType.getAssetMap().getIndex("health");
        EntityStatValue healthStat = stats.get(healthIndex);

        if (healthStat == null) {
            return;
        }

        float currentHealth = healthStat.get();
        float damageAmount = damage.getAmount();

        // Seed base health on first hit this tick
        tickBaseHealth.putIfAbsent(victimUuid, currentHealth);
        tickDamageAccumulator.putIfAbsent(victimUuid, 0f);

        // Effective health = base health minus damage already allowed through this tick
        float effectiveHealth = tickBaseHealth.get(victimUuid) - tickDamageAccumulator.get(victimUuid);

        if (effectiveHealth - damageAmount <= 0) {
            // FATAL — cancel this hit, record kill
            damage.setCancelled(true);

            // Grant signature energy to attacker since damage was cancelled
            if (attackerEntityRef != null) {
                grantSignatureEnergy(attackerEntityRef, store);
            }

            // Clear all status effects (burning, poison, etc.) to prevent DoT finishing them off
            EffectControllerComponent effectController = store.getComponent(victimRef,
                EffectControllerComponent.getComponentType());
            if (effectController != null) {
                effectController.clearEffects(victimRef, store);
            }

            // Record the fatal damage (only actual remaining HP, not overkill)
            match.recordDamage(victimUuid, attackerUuid, effectiveHealth);

            // Record the kill — setAlive(false) inside blocks further damage this tick
            boolean shouldEnd = match.recordKill(victimUuid, attackerUuid);

            if (shouldEnd) {
                match.end();
            }

            // Clean up tracking for this player (dead, no further tracking needed)
            tickBaseHealth.remove(victimUuid);
            tickDamageAccumulator.remove(victimUuid);
        } else {
            // NON-FATAL — let engine handle it (knockback, particles, sound, etc.)
            tickDamageAccumulator.merge(victimUuid, damageAmount, Float::sum);
            match.recordDamage(victimUuid, attackerUuid, damageAmount);
        }
    }

    /**
     * Handles damage to a bot victim.
     */
    private void handleBotVictim(BotParticipant botVictim, Ref<EntityStore> victimRef,
                                  Store<EntityStore> store, Damage damage) {
        // Get bot's match
        Match match = botManager.getBotMatch(botVictim.getUniqueId());

        // ONLY allow damage if bot is in an IN_PROGRESS match AND alive
        if (match == null || match.getState() != MatchState.IN_PROGRESS) {
            damage.setCancelled(true);
            return;
        }

        // Check if victim is alive in match
        if (!botVictim.isAlive()) {
            damage.setCancelled(true);
            return;
        }

        // Check immunity
        if (botVictim.isImmune()) {
            damage.setCancelled(true);
            return;
        }

        // Determine if attacker is a player (only player-on-bot goes through this path)
        Ref<EntityStore> attackerEntityRef = getAttackerEntityRef(damage.getSource());
        Player attackerPlayer = (attackerEntityRef != null)
            ? store.getComponent(attackerEntityRef, Player.getComponentType()) : null;

        // Non-player damage to bots (bot-on-bot, environment, etc.) is cancelled here;
        // bot-on-bot is handled solely by BotManager.handleBotDamage()
        if (attackerPlayer == null) {
            damage.setCancelled(true);
            return;
        }

        UUID attackerUuid = getAttackerUuid(damage.getSource(), store);
        float damageAmount = damage.getAmount();

        // Capture current health BEFORE applying damage (for accurate fatal damage tracking)
        double healthBeforeDamage = botVictim.getHealth();

        // Apply damage to bot's internal health
        boolean died = botVictim.takeDamage(damageAmount);

        // Register attacker as a threat on the victim's brain
        if (botVictim.getBrain() != null && attackerUuid != null) {
            botVictim.getBrain().registerThreat(attackerUuid);
        }

        // Also update the NPC entity health bar
        if (botManager != null) {
            botManager.applyDamageToNpcEntity(botVictim, damageAmount);
        }

        // Cancel the actual Hytale damage (we handle it internally)
        damage.setCancelled(true);

        // Grant signature energy to player attacker since damage was cancelled
        grantSignatureEnergy(attackerEntityRef, store);

        // Record damage - use actual HP removed for fatal hits (not overkill damage)
        double actualDamage = died ? healthBeforeDamage : damageAmount;
        match.recordDamage(botVictim.getUniqueId(), attackerUuid, actualDamage);

        if (died) {
            // Record the kill
            boolean shouldEnd = match.recordKill(botVictim.getUniqueId(), attackerUuid);

            if (shouldEnd) {
                match.end();
            }
        }
    }

    /**
     * Gets the attacker UUID from a damage source.
     * Handles both player and bot attackers.
     */
    private UUID getAttackerUuid(Damage.Source damageSource, Store<EntityStore> store) {
        if (!(damageSource instanceof Damage.EntitySource entitySource)) {
            return null;
        }

        Ref<EntityStore> attackerRef = entitySource.getRef();
        if (attackerRef == null || !attackerRef.isValid()) {
            return null;
        }

        // Check if attacker is a player
        Player attackerPlayer = store.getComponent(attackerRef, Player.getComponentType());
        if (attackerPlayer != null) {
            PlayerRef attackerPlayerRef = store.getComponent(attackerRef, PlayerRef.getComponentType());
            if (attackerPlayerRef != null) {
                return attackerPlayerRef.getUuid();
            }
        }

        // Check if attacker is a bot
        if (botManager != null) {
            UUIDComponent uuidComponent = store.getComponent(attackerRef, UUIDComponent.getComponentType());
            if (uuidComponent != null) {
                BotParticipant attackerBot = botManager.getBotByEntityUuid(uuidComponent.getUuid());
                if (attackerBot != null) {
                    return attackerBot.getUniqueId();
                }
            }
        }

        return null;
    }

    /**
     * Gets the attacker entity reference from a damage source.
     */
    private Ref<EntityStore> getAttackerEntityRef(Damage.Source damageSource) {
        if (!(damageSource instanceof Damage.EntitySource entitySource)) {
            return null;
        }

        Ref<EntityStore> attackerRef = entitySource.getRef();
        if (attackerRef == null || !attackerRef.isValid()) {
            return null;
        }

        return attackerRef;
    }

    /**
     * Grants Signature Energy to an attacking player.
     * This compensates for cancelled damage not triggering normal weapon signature charge.
     */
    private void grantSignatureEnergy(Ref<EntityStore> playerRef, Store<EntityStore> store) {
        try {
            EntityStatMap stats = store.getComponent(playerRef,
                EntityStatsModule.get().getEntityStatMapComponentType());

            if (stats == null) {
                return;
            }

            int sigEnergyIndex = getSignatureEnergyIndex();
            if (sigEnergyIndex < 0) {
                return;
            }

            // Check if the player has this stat
            EntityStatValue sigEnergyStat = stats.get(sigEnergyIndex);
            if (sigEnergyStat == null) {
                return; // Player doesn't have SignatureEnergy stat
            }

            float currentValue = sigEnergyStat.get();
            float maxValue = sigEnergyStat.getMax();

            // Only add if not already at max
            if (currentValue < maxValue) {
                stats.addStatValue(sigEnergyIndex, SIGNATURE_ENERGY_PER_HIT);
            }
        } catch (Exception e) {
            // Silently ignore - signature energy is optional
        }
    }
}
