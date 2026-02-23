package de.ragesith.hyarena2.protection;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.RootDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.DamageBlockEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import de.ragesith.hyarena2.Permissions;
import de.ragesith.hyarena2.arena.Match;
import de.ragesith.hyarena2.arena.MatchManager;
import de.ragesith.hyarena2.arena.MatchState;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;

/**
 * Prevents block breaking unless the player has the build or admin permission,
 * or the player is in an active match whose game mode allows the break (e.g. Spleef).
 */
public class BlockBreakProtectionSystem extends EntityEventSystem<EntityStore, DamageBlockEvent> {

    private static volatile MatchManager matchManager;

    public BlockBreakProtectionSystem() {
        super(DamageBlockEvent.class);
    }

    /**
     * Sets the match manager for game-mode-specific block break checks.
     * Called once during plugin setup.
     */
    public static void setMatchManager(MatchManager mm) {
        matchManager = mm;
    }

    @Override
    public Query<EntityStore> getQuery() {
        return Query.any();
    }

    @Nonnull
    @Override
    public Set<Dependency<EntityStore>> getDependencies() {
        return Collections.singleton(RootDependency.first());
    }

    @Override
    public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> chunk,
                       @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer,
                       @Nonnull DamageBlockEvent event) {
        Ref<EntityStore> ref = chunk.getReferenceTo(index);
        Player player = store.getComponent(ref, Player.getComponentType());

        if (player == null) {
            event.setCancelled(true);
            return;
        }

        // Check if game mode allows this block break (e.g. Spleef floors)
        if (matchManager != null) {
            PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
            if (playerRef != null) {
                UUID playerUuid = playerRef.getUuid();
                Match match = matchManager.getPlayerMatch(playerUuid);
                if (match != null && match.getState() == MatchState.IN_PROGRESS) {
                    Vector3i block = event.getTargetBlock();
                    String blockId = event.getBlockType().getId();
                    if (match.getGameMode().shouldAllowBlockBreak(
                            match.getArena().getConfig(), playerUuid,
                            block.getX(), block.getY(), block.getZ(), blockId)) {
                        return; // Allow the break
                    }
                }
            }
        }

        // Default: require build or admin permission
        if (!player.hasPermission(Permissions.BUILD) && !player.hasPermission(Permissions.ADMIN)) {
            event.setCancelled(true);
        }
    }
}
