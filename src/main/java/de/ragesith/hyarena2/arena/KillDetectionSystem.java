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

import java.util.UUID;

/**
 * Damage detection system that:
 * - Allows damage ONLY during IN_PROGRESS matches
 * - Cancels all damage outside matches (prevents hub PvP, countdown damage, etc.)
 * - Detects kills and notifies the match
 * - Handles both player and bot victims/attackers
 */
public class KillDetectionSystem extends DamageEventSystem {
    private final MatchManager matchManager;
    private BotManager botManager;

    public KillDetectionSystem(MatchManager matchManager) {
        this.matchManager = matchManager;
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

        // Get attacker UUID (player or bot)
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

        // Check if damage would be fatal
        if (currentHealth - damageAmount <= 0) {
            // Cancel damage to prevent death screen
            damage.setCancelled(true);

            // Restore health to max
            float maxHealth = healthStat.getMax();
            stats.setStatValue(healthIndex, maxHealth);

            // Clear all status effects (burning, poison, etc.)
            EffectControllerComponent effectController = store.getComponent(victimRef,
                EffectControllerComponent.getComponentType());
            if (effectController != null) {
                effectController.clearEffects(victimRef, store);
            }

            // Record the fatal damage (only the actual HP removed, not overkill)
            match.recordDamage(victimUuid, attackerUuid, currentHealth);

            // Record the kill - this will update stats and check if match should end
            boolean shouldEnd = match.recordKill(victimUuid, attackerUuid);

            if (shouldEnd) {
                match.end();
            }
        } else {
            // Non-fatal damage - record it
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

        // Get attacker UUID (player or bot)
        UUID attackerUuid = getAttackerUuid(damage.getSource(), store);

        float damageAmount = damage.getAmount();

        // Capture current health BEFORE applying damage (for accurate fatal damage tracking)
        double healthBeforeDamage = botVictim.getHealth();

        // Apply damage to bot's internal health
        boolean died = botVictim.takeDamage(damageAmount);

        // Also update the NPC entity health bar
        if (botManager != null) {
            botManager.applyDamageToNpcEntity(botVictim, damageAmount);
        }

        // Cancel the actual Hytale damage (we handle it internally)
        damage.setCancelled(true);

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
}
