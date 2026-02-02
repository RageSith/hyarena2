package de.ragesith.hyarena2;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

/**
 * HyArena2 - PvP Arena Plugin
 * Main plugin entry point.
 */
public class HyArena2 extends JavaPlugin {

    private static HyArena2 instance;

    public HyArena2(@NonNullDecl JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        super.setup();
        instance = this;
        System.out.println("[HyArena2] Plugin initialized!");
    }

    public static HyArena2 getInstance() {
        return instance;
    }
}
