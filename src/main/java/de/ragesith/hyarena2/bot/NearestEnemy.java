package de.ragesith.hyarena2.bot;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import de.ragesith.hyarena2.config.Position;
import de.ragesith.hyarena2.participant.Participant;

/**
 * Holds info about the nearest enemy target found during scanning.
 * Top-level class used by BrainContext and BotBrain.
 */
public class NearestEnemy {
    public final Participant participant;
    public final Ref<EntityStore> entityRef;
    public final double distance;
    public final Position position;

    public NearestEnemy(Participant participant, Ref<EntityStore> entityRef, double distance, Position position) {
        this.participant = participant;
        this.entityRef = entityRef;
        this.distance = distance;
        this.position = position;
    }
}
