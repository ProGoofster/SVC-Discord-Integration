package com.jakeberryman.svcdiscordintegration.fabric;

import com.jakeberryman.svcdiscordintegration.SvcDiscordIntegration;
import net.fabricmc.api.ModInitializer;

public final class SvcDiscordIntegrationFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        // This code runs as soon as Minecraft is in a mod-load-ready state.
        // However, some things (like resources) may still be uninitialized.
        // Proceed with mild caution.

        // Register config
        FabricConfig.register();

        // Run our common setup.
        SvcDiscordIntegration.init();
    }
}
