package com.jakeberryman.svcdiscordintegration;

import com.jakeberryman.svcdiscordintegration.config.Config;
import com.jakeberryman.svcdiscordintegration.discord.BotInstance;

public final class SvcDiscordIntegration {
    public static final String MOD_ID = "svcdiscordintegration";

    public static void init() {
        BotInstance bot = new BotInstance(1, Config.getToken());
        bot.startBot();
    }
}
