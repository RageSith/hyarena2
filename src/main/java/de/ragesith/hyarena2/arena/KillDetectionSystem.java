package de.ragesith.hyarena2.arena;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
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

import java.util.UUID;

/**
 * Damage detection system that:
 * - Allows damage ONLY during IN_PROGRESS matches
 * - Cancels all damage outside matches (prevents hub PvP, countdown damage, etc.)
 * - Detects kills and notifies the match
 */
public class KillDetectionSystem extends DamageEventSystem {
    private final MatchManager matchManager;

    public KillDetectionSystem(MatchManager matchManager) {
        this.matchManager = matchManager;
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

            // Check if victim is a player
            Player victimPlayer = store.getComponent(victimRef, Player.getComponentType());
            if (victimPlayer == null) {
                // Not a player - allow normal damage (for now, bots in Phase 5)
                return;
            }

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
            var participant = match.getParticipant(victimUuid);

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

            // Get attacker UUID (if player)
            UUID attackerUuid = null;
            Damage.Source damageSource = damage.getSource();
            if (damageSource instanceof Damage.EntitySource entitySource) {
                Ref<EntityStore> attackerRef = entitySource.getRef();
                Player attackerPlayer = store.getComponent(attackerRef, Player.getComponentType());
                if (attackerPlayer != null) {
                    PlayerRef attackerPlayerRef = store.getComponent(attackerRef, PlayerRef.getComponentType());
                    if (attackerPlayerRef != null) {
                        attackerUuid = attackerPlayerRef.getUuid();
                    }
                }
            }

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

                // Record the kill - this will update stats and check if match should end
                boolean shouldEnd = match.recordKill(victimUuid, attackerUuid);

                if (shouldEnd) {
                    match.end();
                }
            } else {
                // Non-fatal damage - record it
                match.recordDamage(victimUuid, attackerUuid, damageAmount);
            }

        } catch (Exception e) {
            System.err.println("Error in KillDetectionSystem: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
