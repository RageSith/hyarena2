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
import de.ragesith.hyarena2.bot.BotManager;

import javax.annotation.Nonnull;

/**
 * Toggles between BotBrain (new) and legacy bot AI.
 * Usage: /tbotai
 */
public class TestBotAIToggleCommand extends AbstractPlayerCommand {
    private final BotManager botManager;

    public TestBotAIToggleCommand(BotManager botManager) {
        super("tbotai", "Toggle between Brain AI and legacy bot AI");
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

        boolean newValue = !botManager.isUsingBrainAI();
        botManager.setUseBrainAI(newValue);

        String mode = newValue ? "BotBrain (new)" : "Legacy (old)";
        player.sendMessage(TinyMsg.parse("<color:#2ecc71>Bot AI switched to: " + mode + "</color>"));
        System.out.println("[BotManager] Bot AI toggled to: " + mode);
    }
}
