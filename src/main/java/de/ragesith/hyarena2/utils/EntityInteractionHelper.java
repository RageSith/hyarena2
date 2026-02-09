package de.ragesith.hyarena2.utils;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionChain;
import com.hypixel.hytale.server.core.entity.InteractionManager;
import com.hypixel.hytale.server.core.modules.interaction.InteractionModule;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class EntityInteractionHelper {

    public enum InteractionKind {
        UNKNOWN,
        ATTACK,
        BLOCK,
        ITEM_THROW,
        POTION_CONSUME
    }

    private static ComponentType<EntityStore, InteractionManager> imType;

    private static ComponentType<EntityStore, InteractionManager> getImType() {
        if (imType == null) {
            imType = InteractionModule.get().getInteractionManagerComponent();
        }
        return imType;
    }

    /**
     * Returns the root interaction ID string for the currently executing interaction chain
     * of the given type on any entity (player or NPC). Returns null if nothing is active.
     */
    public static String getActiveInteraction(Ref<EntityStore> ref, Store<EntityStore> store, InteractionType queryType) {
        if (ref == null || !ref.isValid() || store == null) return null;
        try {
            InteractionManager im = store.getComponent(ref, getImType());
            if (im == null) return null;

            for (InteractionChain chain : im.getChains().values()) {
                if (chain.getServerState() != InteractionState.NotFinished) continue;
                if (chain.getType() == queryType) {
                    return chain.getInitialRootInteraction().getId();
                }
            }
        } catch (Exception e) {
            // Silently fail — entity may have been removed mid-tick
        }
        return null;
    }

    public static String getPrimaryInteraction(Ref<EntityStore> ref, Store<EntityStore> store) {
        return getActiveInteraction(ref, store, InteractionType.Primary);
    }

    public static String getSecondaryInteraction(Ref<EntityStore> ref, Store<EntityStore> store) {
        return getActiveInteraction(ref, store, InteractionType.Secondary);
    }

    /**
     * Classifies an interaction ID string into an InteractionKind.
     */
    public static InteractionKind classifyInteraction(String interactionId) {
        if (interactionId == null) return InteractionKind.UNKNOWN;
        if (interactionId.contains("_Attack")) return InteractionKind.ATTACK;
        if (interactionId.contains("_Secondary_Guard") || interactionId.contains("Shield_Block")) return InteractionKind.BLOCK;
        if (interactionId.contains("Item_Throw")) return InteractionKind.ITEM_THROW;
        if (interactionId.contains("Consume_Potion")) return InteractionKind.POTION_CONSUME;
        return InteractionKind.UNKNOWN;
    }
}
