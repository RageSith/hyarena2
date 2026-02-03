package de.ragesith.hyarena2.command.testing;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import fi.sulku.hytale.TinyMsg;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import de.ragesith.hyarena2.Permissions;
import de.ragesith.hyarena2.arena.Arena;
import de.ragesith.hyarena2.arena.ArenaConfig;
import de.ragesith.hyarena2.arena.Match;
import de.ragesith.hyarena2.arena.MatchManager;
import de.ragesith.hyarena2.bot.BotDifficulty;
import de.ragesith.hyarena2.bot.BotManager;
import de.ragesith.hyarena2.bot.BotParticipant;
import de.ragesith.hyarena2.config.Position;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * Spawns a bot in the player's current match.
 * Usage: /tbot [difficulty] [kitId]
 */
public class TestBotSpawnCommand extends AbstractPlayerCommand {
    private final MatchManager matchManager;
    private final BotManager botManager;

    private final OptionalArg<String> difficultyArg =
        withOptionalArg("difficulty", "Bot difficulty (EASY, MEDIUM, HARD)", ArgTypes.STRING);
    private final OptionalArg<String> kitIdArg =
        withOptionalArg("kitId", "Kit to apply to the bot", ArgTypes.STRING);

    public TestBotSpawnCommand(MatchManager matchManager, BotManager botManager) {
        super("tbot", "Spawn a bot in your current match");
        requirePermission(Permissions.ADMIN_BOT);
        this.matchManager = matchManager;
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

        // Get player's current match
        Match match = matchManager.getPlayerMatch(playerRef.getUuid());
        if (match == null) {
            player.sendMessage(TinyMsg.parse("<color:#e74c3c>You are not in a match. Join a match first with /tmcreate or /tmjoin.</color>"));
            return;
        }

        // Parse difficulty
        String difficultyStr = difficultyArg.get(context);
        BotDifficulty difficulty = BotDifficulty.MEDIUM;
        if (difficultyStr != null && !difficultyStr.isEmpty()) {
            difficulty = BotDifficulty.fromString(difficultyStr);
        } else {
            // Try to get from arena config
            String arenaDifficulty = match.getArena().getConfig().getBotDifficulty();
            if (arenaDifficulty != null && !arenaDifficulty.isEmpty()) {
                difficulty = BotDifficulty.fromString(arenaDifficulty);
            }
        }

        // Get kit
        String kitId = kitIdArg.get(context);

        // Get spawn position (use next available spawn point)
        Arena arena = match.getArena();
        List<ArenaConfig.SpawnPoint> spawnPoints = arena.getSpawnPoints();
        int spawnIndex = match.getParticipants().size();
        if (spawnIndex >= spawnPoints.size()) {
            player.sendMessage(TinyMsg.parse("<color:#e74c3c>Match is full, no spawn points available.</color>"));
            return;
        }

        ArenaConfig.SpawnPoint sp = spawnPoints.get(spawnIndex);
        Position spawnPos = new Position(sp.getX(), sp.getY(), sp.getZ(), sp.getYaw(), sp.getPitch());

        // Spawn bot
        BotParticipant bot = botManager.spawnBot(match, spawnPos, kitId, difficulty);
        if (bot == null) {
            player.sendMessage(TinyMsg.parse("<color:#e74c3c>Failed to spawn bot.</color>"));
            return;
        }

        // Add bot to match
        if (!match.addBot(bot)) {
            botManager.despawnBot(bot);
            player.sendMessage(TinyMsg.parse("<color:#e74c3c>Failed to add bot to match.</color>"));
            return;
        }

        player.sendMessage(TinyMsg.parse("<color:#2ecc71>Spawned bot </color><color:#f1c40f>" + bot.getName() +
            "</color><color:#2ecc71> (difficulty: " + difficulty + ") in match.</color>"));
    }
}
