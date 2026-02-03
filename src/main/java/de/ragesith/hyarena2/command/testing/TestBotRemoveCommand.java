package de.ragesith.hyarena2.command.testing;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import fi.sulku.hytale.TinyMsg;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import de.ragesith.hyarena2.Permissions;
import de.ragesith.hyarena2.bot.BotManager;
import de.ragesith.hyarena2.bot.BotParticipant;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * Removes a specific bot.
 * Usage: /tbotremove <botName>
 */
public class TestBotRemoveCommand extends AbstractPlayerCommand {
    private final BotManager botManager;

    private final RequiredArg<String> botNameArg =
        withRequiredArg("botName", "The bot name to remove", ArgTypes.STRING);

    public TestBotRemoveCommand(BotManager botManager) {
        super("tbotremove", "Remove a bot");
        requirePermission(Permissions.ADMIN_BOT);
        this.botManager = botManager;
    }

    @Override
    protected void execute(
            @Nonnull CommandContext context,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull PlayerRef playerRef,
            @Nonnull World world
    ) {
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) return;

        String botName = botNameArg.get(context);

        // Find bot by name
        BotParticipant botToRemove = null;
        for (BotParticipant bot : botManager.getAllBots()) {
            if (bot.getName().equalsIgnoreCase(botName)) {
                botToRemove = bot;
                break;
            }
        }

        if (botToRemove == null) {
            player.sendMessage(TinyMsg.parse("<color:#e74c3c>Bot not found: " + botName + "</color>"));
            return;
        }

        // Remove from match participant list
        var match = botManager.getBotMatch(botToRemove.getUniqueId());
        if (match != null) {
            match.removeParticipant(botToRemove.getUniqueId(), "Removed by admin");
        }

        // Despawn
        botManager.despawnBot(botToRemove);

        player.sendMessage(TinyMsg.parse("<color:#2ecc71>Removed bot: </color><color:#f1c40f>" + botName + "</color>"));
    }
}
