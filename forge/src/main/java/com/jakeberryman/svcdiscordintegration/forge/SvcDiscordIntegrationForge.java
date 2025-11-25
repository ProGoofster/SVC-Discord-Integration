package com.jakeberryman.svcdiscordintegration.forge;

import com.jakeberryman.svcdiscordintegration.SvcDiscordIntegration;
import net.minecraftforge.fml.common.Mod;

@Mod(SvcDiscordIntegration.MOD_ID)
public final class SvcDiscordIntegrationForge {
    public SvcDiscordIntegrationForge() {
        // Run our common setup.
        SvcDiscordIntegration.init();
    }
}
