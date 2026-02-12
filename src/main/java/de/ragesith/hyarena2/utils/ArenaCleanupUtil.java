package de.ragesith.hyarena2.utils;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import de.ragesith.hyarena2.arena.ArenaConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * Cleans up stale entities (NPCs, holograms) within arena bounds.
 * Used before match creation to recover from server crashes that leave
 * world-persisted entities behind.
 * All methods must be called on the target world's thread.
 */
public class ArenaCleanupUtil {

    private ArenaCleanupUtil() {}

    /**
     * Removes stale holograms and NPC entities within the given arena bounds.
     * Must be called on the world thread (inside world.execute()).
     */
    public static void cleanupArena(World world, ArenaConfig.Bounds bounds) {
        double loX = Math.min(bounds.getMinX(), bounds.getMaxX());
        double hiX = Math.max(bounds.getMinX(), bounds.getMaxX());
        double loZ = Math.min(bounds.getMinZ(), bounds.getMaxZ());
        double hiZ = Math.max(bounds.getMinZ(), bounds.getMaxZ());
        System.out.println("[ArenaCleanup] Scanning world '" + world.getName()
            + "' bounds XZ [" + loX + ", " + loZ + "] to [" + hiX + ", " + hiZ + "]");

        int holograms = HologramUtil.cleanupHologramsInBounds(world, bounds);
        int npcs = cleanupNPCsInBounds(world, bounds);

        System.out.println("[ArenaCleanup] Result for '" + world.getName()
            + "': removed " + holograms + " hologram(s), " + npcs + " NPC(s)");
    }

    /**
     * Scans a world for NPC entities within the given XZ bounds and removes them.
     * Must be called on the world thread (inside world.execute()).
     */
    private static int cleanupNPCsInBounds(World world, ArenaConfig.Bounds bounds) {
        try {
            Store<EntityStore> store = world.getEntityStore().getStore();
            List<Ref<EntityStore>> toRemove = new ArrayList<>();

            store.forEachChunk(NPCEntity.getComponentType(), (chunk, commandBuffer) -> {
                for (int i = 0; i < chunk.size(); i++) {
                    TransformComponent transform = chunk.getComponent(i, TransformComponent.getComponentType());
                    if (transform != null) {
                        Vector3d pos = transform.getPosition();
                        if (bounds.containsXZ(pos.getX(), pos.getZ())) {
                            toRemove.add(chunk.getReferenceTo(i));
                        }
                    }
                }
            });

            for (Ref<EntityStore> ref : toRemove) {
                if (ref != null && ref.isValid()) {
                    store.removeEntity(ref, RemoveReason.REMOVE);
                }
            }

            return toRemove.size();
        } catch (Exception e) {
            System.err.println("[ArenaCleanup] Failed to cleanup NPCs in bounds: " + e.getMessage());
            return 0;
        }
    }
}
