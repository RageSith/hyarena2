package de.ragesith.hyarena2.interaction;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import de.ragesith.hyarena2.HyArena2;
import de.ragesith.hyarena2.economy.EconomyManager;
import de.ragesith.hyarena2.economy.HonorManager;
import fi.sulku.hytale.TinyMsg;

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

        // Show AP balance and rank info (full shop page deferred to later)
        com.hypixel.hytale.server.core.universe.PlayerRef playerRef =
            store.getComponent(ref, com.hypixel.hytale.server.core.universe.PlayerRef.getComponentType());
        if (playerRef == null) return;

        java.util.UUID playerUuid = playerRef.getUuid();
        EconomyManager economyManager = pluginInstance.getEconomyManager();
        HonorManager honorManager = pluginInstance.getHonorManager();

        if (economyManager != null && honorManager != null) {
            int ap = economyManager.getArenaPoints(playerUuid);
            String rank = honorManager.getRankDisplayName(playerUuid);
            player.sendMessage(TinyMsg.parse(
                "<color:#f1c40f>Your Balance: " + ap + " AP</color>"
                + " <color:#7f8c8d>|</color> "
                + "<color:#3498db>Rank: " + rank + "</color>"
            ));
            player.sendMessage(TinyMsg.parse("<color:#f39c12>Shop page coming soon! Use /tshoplist and /tshopbuy for now.</color>"));
        } else {
            player.sendMessage(TinyMsg.parse("<color:#f39c12>Shop coming soon!</color>"));
        }
    }
}
