package de.ragesith.hyarena2.utils;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.ProjectileComponent;
import com.hypixel.hytale.server.core.entity.nameplate.Nameplate;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.tracker.NetworkId;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.ArrayList;
import java.util.List;

/**
 * Reusable utility for spawning and despawning floating text holograms.
 * Creates invisible projectile entities with a Nameplate component.
 * All methods must be called on the target world's thread.
 */
public class HologramUtil {

    private HologramUtil() {}

    /**
     * Spawns a hologram entity at the given position with the given text.
     * Must be called on the world thread (inside world.execute()).
     *
     * @return the entity ref, or null on failure
     */
    public static Ref<EntityStore> spawnHologram(World world, double x, double y, double z, String text) {
        try {
            Holder<EntityStore> holder = EntityStore.REGISTRY.newHolder();

            ProjectileComponent projectile = new ProjectileComponent("Projectile");
            holder.putComponent(ProjectileComponent.getComponentType(), projectile);
            holder.putComponent(TransformComponent.getComponentType(),
                new TransformComponent(new Vector3d(x, y, z), new Vector3f(0, 0, 0)));
            holder.ensureComponent(UUIDComponent.getComponentType());

            projectile.initialize();

            holder.addComponent(NetworkId.getComponentType(),
                new NetworkId(world.getEntityStore().getStore()
                    .getExternalData().takeNextNetworkId()));

            holder.addComponent(Nameplate.getComponentType(),
                new Nameplate(text));

            return world.getEntityStore().getStore()
                .addEntity(holder, AddReason.SPAWN);
        } catch (Exception e) {
            System.err.println("[HologramUtil] Failed to spawn hologram: " + e.getMessage());
            return null;
        }
    }

    /**
     * Despawns a hologram entity.
     * Must be called on the world thread (inside world.execute()).
     */
    public static void despawnHologram(Ref<EntityStore> ref) {
        try {
            if (ref != null && ref.isValid()) {
                Store<EntityStore> store = ref.getStore();
                if (store != null) {
                    store.removeEntity(ref, RemoveReason.REMOVE);
                }
            }
        } catch (Exception e) {
            System.err.println("[HologramUtil] Failed to despawn hologram: " + e.getMessage());
        }
    }

    /**
     * Scans a world for all hologram entities (ProjectileComponent + Nameplate) and removes them.
     * Used on startup to clean up persisted holograms from previous sessions or crashes.
     * Must be called on the world thread (inside world.execute()).
     */
    public static int cleanupAllHolograms(World world) {
        try {
            Store<EntityStore> store = world.getEntityStore().getStore();
            List<Ref<EntityStore>> toRemove = new ArrayList<>();

            store.forEachChunk(ProjectileComponent.getComponentType(), (chunk, commandBuffer) -> {
                for (int i = 0; i < chunk.size(); i++) {
                    Nameplate nameplate = chunk.getComponent(i, Nameplate.getComponentType());
                    if (nameplate != null) {
                        Ref<EntityStore> ref = chunk.getReferenceTo(i);
                        toRemove.add(ref);
                    }
                }
            });

            for (Ref<EntityStore> ref : toRemove) {
                despawnHologram(ref);
            }

            return toRemove.size();
        } catch (Exception e) {
            System.err.println("[HologramUtil] Failed to cleanup holograms: " + e.getMessage());
            return 0;
        }
    }
}
