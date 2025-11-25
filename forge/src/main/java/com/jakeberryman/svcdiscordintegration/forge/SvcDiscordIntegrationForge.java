package com.jakeberryman.svcdiscordintegration.forge;

import com.jakeberryman.svcdiscordintegration.SvcDiscordIntegration;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(SvcDiscordIntegration.MOD_ID)
public final class SvcDiscordIntegrationForge {
    public SvcDiscordIntegrationForge() {
        // Register config
        ForgeConfig.register();

        // Register setup event
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::commonSetup);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        // Run our common setup after config is loaded
        SvcDiscordIntegration.init();
    }
}
