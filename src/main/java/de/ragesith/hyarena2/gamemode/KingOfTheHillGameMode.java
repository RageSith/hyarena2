package de.ragesith.hyarena2.gamemode;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.modules.entity.EntityModule;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import de.ragesith.hyarena2.arena.ArenaConfig;
import de.ragesith.hyarena2.bot.BotParticipant;
import de.ragesith.hyarena2.config.Position;
import de.ragesith.hyarena2.participant.Participant;
import de.ragesith.hyarena2.participant.ParticipantType;

import java.util.*;
import java.util.stream.Collectors;

/**
 * King of the Hill game mode.
 * Players fight over a capture zone — standing alone in it earns points over time.
 * First to the score target wins. Multiple zones rotate on a timer.
 */
public class KingOfTheHillGameMode implements GameMode {
    private static final String ID = "koth";
    private static final String DISPLAY_NAME = "King of the Hill";

    private final Map<UUID, Integer> controlTicks = new HashMap<>();
    private int activeZoneIndex = 0;
    private UUID currentController = null;
    private boolean contested = false;

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getDisplayName() {
        return DISPLAY_NAME;
    }

    @Override
    public String getDescription() {
        return "Group { LayoutMode: Top;"
            + " Label { Text: \"King of the Hill\"; Anchor: (Height: 28); Style: (FontSize: 18, TextColor: #e8c872, RenderBold: true); }"
            + " Label { Text: \"Fight over a capture zone. Stand alone on the hill to earn points. First to the score target wins.\"; Anchor: (Height: 40, Top: 4); Style: (FontSize: 13, TextColor: #b7cedd, Wrap: true); }"
            + " Label { Text: \"Rules\"; Anchor: (Height: 24, Top: 12); Style: (FontSize: 15, TextColor: #e8c872, RenderBold: true); }"
            + " Label { Text: \"\u2022 Stand alone in the capture zone to earn points\"; Anchor: (Height: 18, Top: 4); Style: (FontSize: 12, TextColor: #96a9be); }"
            + " Label { Text: \"\u2022 If multiple players are in the zone, it is contested\"; Anchor: (Height: 18, Top: 2); Style: (FontSize: 12, TextColor: #96a9be); }"
            + " Label { Text: \"\u2022 First player to reach the score target wins\"; Anchor: (Height: 18, Top: 2); Style: (FontSize: 12, TextColor: #96a9be); }"
            + " Label { Text: \"\u2022 Players respawn after death\"; Anchor: (Height: 18, Top: 2); Style: (FontSize: 12, TextColor: #96a9be); }"
            + " Label { Text: \"\u2022 Multiple zones may rotate on a timer\"; Anchor: (Height: 18, Top: 2); Style: (FontSize: 12, TextColor: #96a9be); }"
            + " Label { Text: \"Tips\"; Anchor: (Height: 24, Top: 12); Style: (FontSize: 15, TextColor: #e8c872, RenderBold: true); }"
            + " Label { Text: \"\u2022 Control the hill \u2014 kills alone won't win the game\"; Anchor: (Height: 18, Top: 4); Style: (FontSize: 12, TextColor: #96a9be); }"
            + " Label { Text: \"\u2022 Push enemies off the zone to stop their scoring\"; Anchor: (Height: 18, Top: 2); Style: (FontSize: 12, TextColor: #96a9be); }"
            + " Label { Text: \"\u2022 Watch for zone rotations and reposition quickly\"; Anchor: (Height: 18, Top: 2); Style: (FontSize: 12, TextColor: #96a9be); }"
            + " }";
    }

    @Override
    public void onMatchStart(ArenaConfig config, List<Participant> participants) {
        controlTicks.clear();
        activeZoneIndex = 0;
        currentController = null;
        contested = false;

        for (Participant p : participants) {
            p.setAlive(true);
            controlTicks.put(p.getUniqueId(), 0);
        }
    }

    @Override
    public void onGameplayBegin(ArenaConfig config, List<Participant> participants) {
        List<ArenaConfig.CaptureZone> zones = config.getCaptureZones();
        String zoneName = (zones != null && !zones.isEmpty()) ? zones.get(0).getDisplayName() : "the hill";

        for (Participant p : participants) {
            p.sendMessage("<gradient:#2ecc71:#27ae60><b>FIGHT!</b></gradient>");
            p.sendMessage("<color:#f1c40f>First to " + config.getScoreTarget() + " score wins!</color>");
            if (zones != null && zones.size() > 1) {
                p.sendMessage("<color:#e8c872>Active zone: " + zoneName + "</color>");
            }
        }
    }

    @Override
    public void onTick(ArenaConfig config, List<Participant> participants, int tickCount) {
        List<ArenaConfig.CaptureZone> zones = config.getCaptureZones();
        if (zones == null || zones.isEmpty()) {
            return;
        }

        // Zone rotation
        if (zones.size() > 1 && tickCount > 0 && tickCount % (config.getZoneRotationSeconds() * 20) == 0) {
            activeZoneIndex = (activeZoneIndex + 1) % zones.size();
            String newZoneName = zones.get(activeZoneIndex).getDisplayName();
            for (Participant p : participants) {
                p.sendMessage("<color:#e8c872><b>Zone rotated!</b> Active zone: " + newZoneName + "</color>");
            }
            // Reset controller state on rotation
            currentController = null;
            contested = false;
        }

        ArenaConfig.CaptureZone activeZone = zones.get(activeZoneIndex);

        // Scan positions of all alive participants (players and bots)
        List<UUID> participantsInZone = new ArrayList<>();
        for (Participant p : participants) {
            if (!p.isAlive()) {
                continue;
            }

            if (p.getType() == ParticipantType.BOT) {
                BotParticipant bot = (BotParticipant) p;
                Position botPos = bot.getCurrentPosition();
                if (botPos != null && activeZone.contains(botPos.getX(), botPos.getY(), botPos.getZ())) {
                    participantsInZone.add(p.getUniqueId());
                }
            } else {
                Vector3d pos = getPlayerPosition(p.getUniqueId());
                if (pos != null && activeZone.contains(pos.getX(), pos.getY(), pos.getZ())) {
                    participantsInZone.add(p.getUniqueId());
                }
            }
        }

        // Scoring logic
        UUID previousController = currentController;
        boolean previousContested = contested;

        if (participantsInZone.isEmpty()) {
            currentController = null;
            contested = false;
        } else if (participantsInZone.size() == 1) {
            currentController = participantsInZone.get(0);
            contested = false;
            controlTicks.merge(currentController, 1, Integer::sum);
        } else {
            currentController = null;
            contested = true;
        }

        // Feedback every second (20 ticks)
        if (tickCount % 20 == 0) {
            // Controller changed
            if (!Objects.equals(previousController, currentController) || previousContested != contested) {
                if (contested) {
                    for (Participant p : participants) {
                        p.sendMessage("<color:#e74c3c>Contested!</color> <color:#95a5a6>Multiple players on the hill</color>");
                    }
                } else if (currentController != null) {
                    Participant controller = findParticipant(participants, currentController);
                    if (controller != null) {
                        for (Participant p : participants) {
                            p.sendMessage("<color:#3498db>" + controller.getName() + " is capturing the hill!</color>");
                        }
                    }
                } else if (previousController != null || previousContested) {
                    for (Participant p : participants) {
                        p.sendMessage("<color:#95a5a6>The hill is empty</color>");
                    }
                }
            }

            // Score milestone to controller
            if (currentController != null) {
                int ticks = controlTicks.getOrDefault(currentController, 0);
                int score = ticks / 20;
                Participant controller = findParticipant(participants, currentController);
                if (controller != null) {
                    controller.sendMessage("<color:#2ecc71>Score: " + score + "/" + config.getScoreTarget() + "</color>");
                }
            }
        }
    }

    @Override
    public boolean onParticipantKilled(ArenaConfig config, Participant victim, Participant killer, List<Participant> participants) {
        victim.setAlive(false);
        victim.addDeath();

        if (killer != null) {
            killer.addKill();
        }

        // Broadcast kill
        String killMsg;
        if (killer != null) {
            killMsg = "<color:#e74c3c>" + victim.getName() + "</color> <color:#7f8c8d>was killed by</color> <color:#2ecc71>" + killer.getName() + "</color>";
        } else {
            killMsg = "<color:#e74c3c>" + victim.getName() + "</color> <color:#7f8c8d>died</color>";
        }
        for (Participant p : participants) {
            p.sendMessage(killMsg);
        }

        // Kills don't end the match in KOTH
        return false;
    }

    @Override
    public void onParticipantDamaged(ArenaConfig config, Participant victim, Participant attacker, double damage) {
        victim.addDamageTaken(damage);
        if (attacker != null) {
            attacker.addDamageDealt(damage);
        }
    }

    @Override
    public boolean shouldMatchEnd(ArenaConfig config, List<Participant> participants) {
        if (config.getScoreTarget() <= 0) {
            return false; // No score target — match ends by time only
        }
        int targetTicks = config.getScoreTarget() * 20;
        return controlTicks.values().stream().anyMatch(t -> t >= targetTicks);
    }

    @Override
    public int getParticipantScore(UUID participantId) {
        return controlTicks.getOrDefault(participantId, 0) / 20;
    }

    @Override
    public int getScoreTarget(ArenaConfig config) {
        return config.getScoreTarget();
    }

    @Override
    public String getScoreLabel() {
        return "Pts";
    }

    @Override
    public boolean shouldRespawn(ArenaConfig config, Participant participant) {
        return true;
    }

    @Override
    public int getRespawnDelayTicks(ArenaConfig config) {
        return config.getRespawnDelaySeconds() * 20;
    }

    @Override
    public List<UUID> getWinners(ArenaConfig config, List<Participant> participants) {
        // 1. Most zone control points
        int maxTicks = controlTicks.values().stream()
                .mapToInt(Integer::intValue)
                .max()
                .orElse(0);

        if (maxTicks == 0) {
            return new ArrayList<>();
        }

        List<UUID> topControllers = controlTicks.entrySet().stream()
                .filter(e -> e.getValue() == maxTicks)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        if (topControllers.size() == 1) {
            return topControllers;
        }

        // 2. Tiebreaker: most kills
        int maxKills = -1;
        List<UUID> killLeaders = new ArrayList<>();
        for (UUID uuid : topControllers) {
            Participant p = findParticipant(participants, uuid);
            if (p == null) continue;
            int kills = p.getKills();
            if (kills > maxKills) {
                maxKills = kills;
                killLeaders.clear();
                killLeaders.add(uuid);
            } else if (kills == maxKills) {
                killLeaders.add(uuid);
            }
        }

        if (killLeaders.size() == 1) {
            return killLeaders;
        }

        // 3. Still tied — draw
        return new ArrayList<>();
    }

    @Override
    public String getVictoryMessage(ArenaConfig config, List<Participant> winners) {
        if (winners.isEmpty()) {
            return "<color:#f39c12>No one controlled the hill!</color>";
        }

        Participant winner = winners.get(0);
        int ticks = controlTicks.getOrDefault(winner.getUniqueId(), 0);
        int score = ticks / 20;
        return "<gradient:#f1c40f:#f39c12><b>" + winner.getName() + "</b></gradient> <color:#f1c40f>controls the hill with " + score + " score!</color>";
    }

    /**
     * Gets the current position of a player from the world thread.
     * This method is called from onTick which already runs on the world thread.
     */
    private Vector3d getPlayerPosition(UUID playerUuid) {
        try {
            PlayerRef playerRef = Universe.get().getPlayer(playerUuid);
            if (playerRef == null) return null;

            Ref<EntityStore> ref = playerRef.getReference();
            if (ref == null) return null;

            Store<EntityStore> store = ref.getStore();
            if (store == null) return null;

            TransformComponent transform = store.getComponent(ref,
                EntityModule.get().getTransformComponentType());
            if (transform == null) return null;

            return transform.getPosition();
        } catch (Exception e) {
            return null;
        }
    }

    private Participant findParticipant(List<Participant> participants, UUID uuid) {
        for (Participant p : participants) {
            if (p.getUniqueId().equals(uuid)) {
                return p;
            }
        }
        return null;
    }
}
