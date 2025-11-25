package com.jakeberryman.svcdiscordintegration.voicechat;

import com.jakeberryman.svcdiscordintegration.SvcDiscordIntegration;
import com.jakeberryman.svcdiscordintegration.config.Config;
import com.jakeberryman.svcdiscordintegration.discord.BotInstance;
import de.maxhenkel.voicechat.api.*;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.CreateGroupEvent;
import de.maxhenkel.voicechat.api.events.RemoveGroupEvent;
import de.maxhenkel.voicechat.api.events.VoicechatServerStartedEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;


public class SvcPlugin implements VoicechatPlugin {

    @Nullable
    public static VoicechatServerApi SERVER_API;

    public static List<Group> groups = new ArrayList<>();

    private BotInstance bot;

    public static VolumeCategory discordPlayer;

    @Override
    public String getPluginId() {
        return SvcDiscordIntegration.MOD_ID;
    }

    @Override
    public void initialize(VoicechatApi api) {
        SvcDiscordIntegration.LOGGER.info("SVC discord plugin initialized!");
    }

    @Override
    public void registerEvents(EventRegistration registration) {
        registration.registerEvent(VoicechatServerStartedEvent.class, this::onServerStarted);
        registration.registerEvent(CreateGroupEvent.class, this::onGroupCreated);
        registration.registerEvent(RemoveGroupEvent.class, this::onGroupRemoved);
    }

    private void onServerStarted(VoicechatServerStartedEvent event) {
        discordPlayer = SERVER_API.volumeCategoryBuilder()
                .setId("DISCORD")
                .setName("Discord")
                .setNameTranslationKey("audioplayer.category.music_discs")
                .setDescription("The volume of all custom music discs")
                .setDescriptionTranslationKey("audioplayer.category.music_discs.description")
                //.setIcon()
                .build();


        SERVER_API = event.getVoicechat();
        bot = new BotInstance(1, Config.getToken());
        bot.startBot();
    }

    private void onGroupCreated(CreateGroupEvent event){
        groups.add(event.getGroup());
    }

    private void onGroupRemoved(RemoveGroupEvent event){
        groups.remove(event.getGroup());
    }

}
