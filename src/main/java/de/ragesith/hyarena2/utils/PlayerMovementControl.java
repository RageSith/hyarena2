package de.ragesith.hyarena2.utils;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect;
import com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class PlayerMovementControl {

    // ArenaFreeze with HUD overlay (for waiting/countdown)
    private static final String[] FREEZE_WITH_HUD_NAMES = {
        "ArenaFreeze",
        "hyarena2:ArenaFreeze",
        "Status/ArenaFreeze",
        "hyarena2:Status/ArenaFreeze"
    };

    // ArenaFreezeNoHud without overlay (for match ended)
    private static final String[] FREEZE_NO_HUD_NAMES = {
        "ArenaFreezeNoHud",
        "hyarena2:ArenaFreezeNoHud",
        "Status/ArenaFreezeNoHud",
        "hyarena2:Status/ArenaFreezeNoHud"
    };

    // Fallback to built-in Stun
    private static final String[] STUN_FALLBACK_NAMES = {
        "Stun",
        "hytale:Stun",
        "Status/Stun",
        "hytale:Status/Stun"
    };

    /**
     * Disables movement for a player with HUD overlay (for waiting/countdown).
     * Executes on the provided world thread.
     */
    public static void disableMovementForPlayer(PlayerRef playerRef, World world) {
        if (world == null) {
            System.err.println("[PlayerMovementControl] Cannot freeze - world is null");
            return;
        }
        world.execute(() -> applyFreezeEffect(playerRef, true));
    }

    /**
     * Disables movement for a player without HUD overlay (for match ended).
     * Executes on the provided world thread.
     */
    public static void disableMovementForPlayerNoHud(PlayerRef playerRef, World world) {
        if (world == null) {
            System.err.println("[PlayerMovementControl] Cannot freeze - world is null");
            return;
        }
        world.execute(() -> applyFreezeEffect(playerRef, false));
    }

    /**
     * Enables movement for a player by removing all effects.
     * Executes on the provided world thread.
     */
    public static void enableMovementForPlayer(PlayerRef playerRef, World world) {
        if (world == null) {
            System.err.println("[PlayerMovementControl] Cannot unfreeze - world is null");
            return;
        }
        world.execute(() -> clearEffects(playerRef));
    }

    /**
     * Applies freeze effect to a player (must be called on world thread).
     */
    private static void applyFreezeEffect(PlayerRef playerRef, boolean withHud) {
        Ref<EntityStore> ref = playerRef.getReference();
        if (ref == null) return;

        Store<EntityStore> store = ref.getStore();

        EffectControllerComponent effectController = store.getComponent(ref,
                EffectControllerComponent.getComponentType());

        if (effectController == null) {
            System.out.println("[PlayerMovementControl] No EffectControllerComponent found");
            return;
        }

        // Try primary effect names first
        String[] primaryNames = withHud ? FREEZE_WITH_HUD_NAMES : FREEZE_NO_HUD_NAMES;
        for (String effectName : primaryNames) {
            try {
                EntityEffect freezeEffect = EntityEffect.getAssetMap().getAsset(effectName);
                if (freezeEffect != null) {
                    effectController.addEffect(ref, freezeEffect, store);
                    System.out.println("[PlayerMovementControl] Applied freeze effect: " + effectName);
                    return;
                }
            } catch (Exception e) {
                // Try next name
            }
        }

        // Fallback to Stun
        for (String effectName : STUN_FALLBACK_NAMES) {
            try {
                EntityEffect freezeEffect = EntityEffect.getAssetMap().getAsset(effectName);
                if (freezeEffect != null) {
                    effectController.addEffect(ref, freezeEffect, store);
                    System.out.println("[PlayerMovementControl] Applied fallback stun effect: " + effectName);
                    return;
                }
            } catch (Exception e) {
                // Try next name
            }
        }

        System.out.println("[PlayerMovementControl] Could not find any freeze effect");
    }

    /**
     * Clears all effects from a player (must be called on world thread).
     */
    private static void clearEffects(PlayerRef playerRef) {
        Ref<EntityStore> ref = playerRef.getReference();
        if (ref == null) return;

        Store<EntityStore> store = ref.getStore();

        EffectControllerComponent effectController = store.getComponent(ref,
                EffectControllerComponent.getComponentType());

        if (effectController != null) {
            try {
                effectController.clearEffects(ref, store);
                System.out.println("[PlayerMovementControl] Cleared all effects");
            } catch (Exception e) {
                System.err.println("[PlayerMovementControl] Failed to clear effects: " + e.getMessage());
            }
        }
    }

    // === Entity-based methods for NPCs/Bots ===

    /**
     * Disables movement for an entity (NPC/Bot) with freeze effect.
     * Must be called on the world thread.
     */
    public static void disableMovementForEntity(Ref<EntityStore> entityRef, Store<EntityStore> store) {
        applyFreezeEffectToEntity(entityRef, store, true);
    }

    /**
     * Enables movement for an entity (NPC/Bot) by clearing effects.
     * Must be called on the world thread.
     */
    public static void enableMovementForEntity(Ref<EntityStore> entityRef, Store<EntityStore> store) {
        clearEffectsFromEntity(entityRef, store);
    }

    /**
     * Applies freeze effect to an entity (must be called on world thread).
     */
    private static void applyFreezeEffectToEntity(Ref<EntityStore> ref, Store<EntityStore> store, boolean withHud) {
        if (ref == null || !ref.isValid() || store == null) return;

        EffectControllerComponent effectController = store.getComponent(ref,
                EffectControllerComponent.getComponentType());

        if (effectController == null) {
            System.out.println("[PlayerMovementControl] No EffectControllerComponent found for entity");
            return;
        }

        // Try primary effect names first
        String[] primaryNames = withHud ? FREEZE_WITH_HUD_NAMES : FREEZE_NO_HUD_NAMES;
        for (String effectName : primaryNames) {
            try {
                EntityEffect freezeEffect = EntityEffect.getAssetMap().getAsset(effectName);
                if (freezeEffect != null) {
                    effectController.addEffect(ref, freezeEffect, store);
                    System.out.println("[PlayerMovementControl] Applied freeze effect to entity: " + effectName);
                    return;
                }
            } catch (Exception e) {
                // Try next name
            }
        }

        // Fallback to Stun
        for (String effectName : STUN_FALLBACK_NAMES) {
            try {
                EntityEffect freezeEffect = EntityEffect.getAssetMap().getAsset(effectName);
                if (freezeEffect != null) {
                    effectController.addEffect(ref, freezeEffect, store);
                    System.out.println("[PlayerMovementControl] Applied fallback stun to entity: " + effectName);
                    return;
                }
            } catch (Exception e) {
                // Try next name
            }
        }

        System.out.println("[PlayerMovementControl] Could not find any freeze effect for entity");
    }

    /**
     * Clears all effects from an entity (must be called on world thread).
     */
    private static void clearEffectsFromEntity(Ref<EntityStore> ref, Store<EntityStore> store) {
        if (ref == null || !ref.isValid() || store == null) return;

        EffectControllerComponent effectController = store.getComponent(ref,
                EffectControllerComponent.getComponentType());

        if (effectController != null) {
            try {
                effectController.clearEffects(ref, store);
                System.out.println("[PlayerMovementControl] Cleared all effects from entity");
            } catch (Exception e) {
                System.err.println("[PlayerMovementControl] Failed to clear entity effects: " + e.getMessage());
            }
        }
    }
}
