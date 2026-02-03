package de.ragesith.hyarena2.command.testing;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import fi.sulku.hytale.TinyMsg;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import de.ragesith.hyarena2.Permissions;
import de.ragesith.hyarena2.arena.Match;
import de.ragesith.hyarena2.bot.BotManager;
import de.ragesith.hyarena2.bot.BotParticipant;

import javax.annotation.Nonnull;
import java.util.Collection;

/**
 * Lists all active bots.
 * Usage: /tbotlist
 */
public class TestBotListCommand extends AbstractPlayerCommand {
    private final BotManager botManager;

    public TestBotListCommand(BotManager botManager) {
        super("tbotlist", "List all active bots");
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

        Collection<BotParticipant> bots = botManager.getAllBots();

        if (bots.isEmpty()) {
            player.sendMessage(TinyMsg.parse("<color:#7f8c8d>No active bots.</color>"));
            return;
        }

        player.sendMessage(TinyMsg.parse("<color:#3498db>Active Bots (" + bots.size() + "):</color>"));

        for (BotParticipant bot : bots) {
            Match match = botManager.getBotMatch(bot.getUniqueId());
            String matchInfo = match != null ? match.getMatchId().toString().substring(0, 8) + "..." : "unknown";
            String healthInfo = String.format("%.0f/%.0f", bot.getHealth(), bot.getMaxHealth());
            String status = bot.isAlive() ? "<color:#2ecc71>alive</color>" : "<color:#e74c3c>dead</color>";

            player.sendMessage(TinyMsg.parse(
                "  <color:#f1c40f>" + bot.getName() + "</color> " +
                "[" + bot.getDifficulty() + "] " +
                "HP: " + healthInfo + " " +
                status + " " +
                "<color:#7f8c8d>match: " + matchInfo + "</color>"
            ));
        }
    }
}
