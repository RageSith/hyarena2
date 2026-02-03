package de.ragesith.hyarena2.kit;

/**
 * Helper class that pairs a kit with its access status for a player.
 * Used for UI display purposes.
 */
public class KitAccessInfo {
    private final KitConfig kit;
    private final boolean unlocked;

    public KitAccessInfo(KitConfig kit, boolean unlocked) {
        this.kit = kit;
        this.unlocked = unlocked;
    }

    /**
     * Gets the kit configuration.
     */
    public KitConfig getKit() {
        return kit;
    }

    /**
     * Checks if the player has unlocked/can use this kit.
     */
    public boolean isUnlocked() {
        return unlocked;
    }

    /**
     * Gets the kit ID.
     */
    public String getKitId() {
        return kit.getId();
    }

    /**
     * Gets the kit display name.
     */
    public String getDisplayName() {
        return kit.getDisplayName();
    }

    /**
     * Gets the kit description.
     */
    public String getDescription() {
        return kit.getDescription();
    }
}
