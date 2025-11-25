package com.jakeberryman.svcdiscordintegration.forge;

import com.jakeberryman.svcdiscordintegration.config.Config;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

public class ForgeConfig implements Config.ConfigProvider {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    private static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.ConfigValue<String> TOKEN;

    static {
        BUILDER.push("Discord Integration");
        TOKEN = BUILDER.comment("Discord bot token")
                .define("token", "");
        BUILDER.pop();

        SPEC = BUILDER.build();
    }

    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, SPEC);
        Config.setProvider(new ForgeConfig());
    }

    @Override
    public String getToken() {
        return TOKEN.get();
    }

    @Override
    public void setToken(String token) {
        TOKEN.set(token);
    }
}
