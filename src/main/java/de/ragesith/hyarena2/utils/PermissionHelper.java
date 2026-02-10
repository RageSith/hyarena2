package de.ragesith.hyarena2.utils;

import com.hypixel.hytale.server.core.command.system.CommandManager;
import com.hypixel.hytale.server.core.console.ConsoleSender;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;

import java.util.UUID;

/**
 * Static utility class that wraps EtherealPerms console commands.
 * No compile-time dependency on EtherealPerms — dispatches via CommandManager.
 */
public final class PermissionHelper {

    private PermissionHelper() {}

    /**
     * Grants a permission to a player.
     * Dispatches: ep user permission set <name> <permission>
     */
    public static void grantPermission(UUID playerUuid, String permission) {
        String name = getPlayerName(playerUuid);
        if (name == null) return;

        dispatchCommand("ep user permission set " + name + " " + permission);
    }

    /**
     * Revokes a permission from a player.
     * Dispatches: ep user permission unset <name> <permission>
     */
    public static void revokePermission(UUID playerUuid, String permission) {
        String name = getPlayerName(playerUuid);
        if (name == null) return;

        dispatchCommand("ep user permission unset " + name + " " + permission);
    }

    /**
     * Adds a player to a permission group.
     * Dispatches: ep user group add <name> <group>
     */
    public static void addToGroup(UUID playerUuid, String group) {
        String name = getPlayerName(playerUuid);
        if (name == null) return;

        dispatchCommand("ep user group add " + name + " " + group);
    }

    /**
     * Removes a player from a permission group.
     * Dispatches: ep user group remove <name> <group>
     */
    public static void removeFromGroup(UUID playerUuid, String group) {
        String name = getPlayerName(playerUuid);
        if (name == null) return;

        dispatchCommand("ep user group remove " + name + " " + group);
    }

    /**
     * Resolves a player's name from their UUID.
     */
    private static String getPlayerName(UUID playerUuid) {
        try {
            PlayerRef ref = Universe.get().getPlayer(playerUuid);
            if (ref != null) {
                return ref.getUsername();
            }
        } catch (Exception e) {
            System.err.println("[PermissionHelper] Failed to resolve player name for " + playerUuid + ": " + e.getMessage());
        }
        return null;
    }

    /**
     * Dispatches a command via the console sender.
     */
    private static void dispatchCommand(String command) {
        try {
            CommandManager.get().handleCommand(ConsoleSender.INSTANCE, command);
        } catch (Exception e) {
            System.err.println("[PermissionHelper] Failed to dispatch command '" + command + "': " + e.getMessage());
        }
    }
}
