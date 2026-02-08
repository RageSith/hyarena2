package de.ragesith.hyarena2;

/**
 * Centralized permission definitions for HyArena2.
 * All permission nodes should be defined here for easy management.
 */
public final class Permissions {

    private Permissions() {} // Prevent instantiation

    // ========== Player Permissions ==========

    /** Basic player access - can use /arena command */
    public static final String PLAYER = "hyarena.player";

    /** Can join arena queues */
    public static final String QUEUE = "hyarena.queue";

    /** Can use chat in matches */
    public static final String CHAT = "hyarena.chat";

    /** Can place and break blocks */
    public static final String BUILD = "hyarena.build";

    // ========== Bypass Permissions ==========

    /** Bypass hub boundary restrictions */
    public static final String BYPASS_BOUNDARY = "hyarena.bypass.boundary";

    /** Bypass queue cooldowns */
    public static final String BYPASS_COOLDOWN = "hyarena.bypass.cooldown";

    /** Bypass kit restrictions */
    public static final String BYPASS_KIT = "hyarena.bypass.kit";

    // ========== Admin Permissions ==========

    /** Access to admin commands */
    public static final String ADMIN = "hyarena.admin";

    /** Can create/edit arenas */
    public static final String ADMIN_ARENA = "hyarena.admin.arena";

    /** Can create/edit kits */
    public static final String ADMIN_KIT = "hyarena.admin.kit";

    /** Can edit hub settings */
    public static final String ADMIN_HUB = "hyarena.admin.hub";

    /** Can reload configuration */
    public static final String ADMIN_RELOAD = "hyarena.admin.reload";

    /** Can force-start/end matches */
    public static final String ADMIN_MATCH = "hyarena.admin.match";

    /** Can spawn/manage bots */
    public static final String ADMIN_BOT = "hyarena.admin.bot";

    // ========== Moderator Permissions ==========

    /** Can kick players from matches */
    public static final String MOD_KICK = "hyarena.mod.kick";

    /** Can ban players from arenas */
    public static final String MOD_BAN = "hyarena.mod.ban";

    /** Can view player stats */
    public static final String MOD_STATS = "hyarena.mod.stats";

    // ========== VIP Permissions ==========

    /** Priority queue access */
    public static final String VIP_PRIORITY = "hyarena.vip.priority";

    /** Access to exclusive arenas */
    public static final String VIP_ARENA = "hyarena.vip.arena";

    /** Access to exclusive kits */
    public static final String VIP_KIT = "hyarena.vip.kit";

    // ========== Debug Permissions ==========

    /** Can see debug information */
    public static final String DEBUG = "hyarena.debug";
}
