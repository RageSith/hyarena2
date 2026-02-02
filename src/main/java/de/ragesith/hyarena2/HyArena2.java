package de.ragesith.hyarena2;

import com.hypixel.hytale.server.core.plugin.ServerPlugin;

/**
 * HyArena2 - PvP Arena Plugin
 * Main plugin entry point.
 */
public class HyArena2 extends ServerPlugin {

    private static HyArena2 instance;

    @Override
    public void onEnable() {
        instance = this;
        System.out.println("[HyArena2] Plugin enabled!");
    }

    @Override
    public void onDisable() {
        System.out.println("[HyArena2] Plugin disabled!");
        instance = null;
    }

    public static HyArena2 getInstance() {
        return instance;
    }
}
