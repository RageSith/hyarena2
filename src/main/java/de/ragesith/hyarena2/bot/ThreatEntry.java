package de.ragesith.hyarena2.bot;

import java.util.UUID;

/**
 * Tracks a single attacker's threat data against a bot.
 * Replaces the old Map<UUID, Long> with richer per-threat information.
 */
public class ThreatEntry {
    private final UUID attackerUuid;
    private long lastHitTick;
    private ThreatType lastHitType;
    private double totalDamage;
    private int hitCount;

    public ThreatEntry(UUID attackerUuid, long tick, ThreatType type, double damage) {
        this.attackerUuid = attackerUuid;
        this.lastHitTick = tick;
        this.lastHitType = type;
        this.totalDamage = damage;
        this.hitCount = 1;
    }

    public void update(long tick, ThreatType type, double damage) {
        this.lastHitTick = tick;
        this.lastHitType = type;
        this.totalDamage += damage;
        this.hitCount++;
    }

    public UUID getAttackerUuid() { return attackerUuid; }
    public long getLastHitTick() { return lastHitTick; }
    public ThreatType getLastHitType() { return lastHitType; }
    public double getTotalDamage() { return totalDamage; }
    public int getHitCount() { return hitCount; }
}
