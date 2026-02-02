package de.ragesith.hyarena2.event;

import java.util.UUID;

/**
 * Published when a player joins or returns to the hub.
 */
public record PlayerJoinedHubEvent(
    UUID playerId,
    String playerName,
    boolean isFirstJoin
) implements Event {
}
