package de.ragesith.hyarena2.interaction;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import de.ragesith.hyarena2.HyArena2;
import de.ragesith.hyarena2.ui.page.ShopPage;

import javax.annotation.Nonnull;

/**
 * Custom interaction for opening the shop UI when using the statue.
 */
public class OpenShopInteraction extends SimpleInstantInteraction {

    public static final BuilderCodec<OpenShopInteraction> CODEC =
        BuilderCodec.builder(OpenShopInteraction.class,
            OpenShopInteraction::new, SimpleInstantInteraction.CODEC).build();

    private static HyArena2 pluginInstance;

    /**
     * Sets the plugin instance for accessing the UI methods.
     */
    public static void setPluginInstance(HyArena2 plugin) {
        pluginInstance = plugin;
    }

    @Override
    protected void firstRun(@Nonnull InteractionType type, @Nonnull InteractionContext context, @Nonnull CooldownHandler handler) {
        if (pluginInstance == null) {
            System.err.println("[OpenShopInteraction] Plugin instance not set!");
            return;
        }

        // Get player from context
        Ref<EntityStore> ref = context.getOwningEntity();
        if (ref == null) {
            return;
        }

        Store<EntityStore> store = ref.getStore();
        if (store == null) {
            return;
        }

        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            return;
        }

        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        if (playerRef == null) {
            return;
        }

        java.util.UUID playerUuid = playerRef.getUuid();

        // Check if player is in a match
        if (pluginInstance.getMatchManager().isPlayerInMatch(playerUuid)) {
            player.sendMessage(Message.raw("<color:#e74c3c>You cannot open the shop while in a match.</color>"));
            return;
        }

        // Close any existing page first
        pluginInstance.getHudManager().closeActivePage(playerUuid);

        // Open the shop page
        try {
            ShopPage page = new ShopPage(
                playerRef, playerUuid,
                pluginInstance.getShopManager(),
                pluginInstance.getEconomyManager(),
                pluginInstance.getHonorManager(),
                pluginInstance.getHudManager(),
                pluginInstance.getScheduler()
            );
            player.getPageManager().openCustomPage(ref, store, page);
        } catch (Exception e) {
            System.err.println("[OpenShopInteraction] Failed to open ShopPage: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
