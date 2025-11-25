package com.jakeberryman.svcdiscordintegration.voicechat;

import com.jakeberryman.svcdiscordintegration.SvcDiscordIntegration;
import com.jakeberryman.svcdiscordintegration.audio.AudioBridgeRegistry;
import com.jakeberryman.svcdiscordintegration.config.Config;
import com.jakeberryman.svcdiscordintegration.discord.BotInstance;
import de.maxhenkel.voicechat.api.*;
import de.maxhenkel.voicechat.api.events.*;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;


public class SvcPlugin implements VoicechatPlugin {

    @Nullable
    public static VoicechatServerApi SERVER_API;

    public static List<Group> groups = new ArrayList<>();

    public static BotInstance bot;

    public static VolumeCategory discordPlayer;

    private final SvcAudioListener audioListener = new SvcAudioListener();

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
        registration.registerEvent(JoinGroupEvent.class, this::onPlayerJoinedGroup);
        registration.registerEvent(LeaveGroupEvent.class, this::onPlayerLeftGroup);
        registration.registerEvent(MicrophonePacketEvent.class, audioListener::onMicrophonePacket);
    }

    private void onServerStarted(VoicechatServerStartedEvent event) {
        SERVER_API = event.getVoicechat();

        discordPlayer = SERVER_API.volumeCategoryBuilder()
                .setId("discord")
                .setName("Discord")
                .setNameTranslationKey("audioplayer.category.music_discs")
                .setDescription("The volume of all custom music discs")
                .setDescriptionTranslationKey("audioplayer.category.music_discs.description")
                //.setIcon()
                .build();

        bot = new BotInstance(1, Config.getToken());
        bot.startBot();
    }

    private void onGroupCreated(CreateGroupEvent event){
        groups.add(event.getGroup());
    }

    private void onGroupRemoved(RemoveGroupEvent event){
        groups.remove(event.getGroup());
        // Clean up any audio bridge for this group
        AudioBridgeRegistry.unregisterBridge(event.getGroup().getId());
    }

    private void onPlayerJoinedGroup(JoinGroupEvent event) {
        // Add player to audio bridge when they join a group
        AudioBridgeRegistry.getBridgeByGroup(event.getGroup()).ifPresent(bridge -> {
            bridge.addConnection(event.getConnection());
            SvcDiscordIntegration.LOGGER.info("Player {} joined bridged group {}",
                event.getConnection().getPlayer().getUuid(), event.getGroup().getName());
        });
    }

    private void onPlayerLeftGroup(LeaveGroupEvent event) {
        // Remove player from audio bridge when they leave a group
        AudioBridgeRegistry.getBridgeByGroup(event.getGroup()).ifPresent(bridge -> {
            bridge.removeConnection(event.getConnection());
            SvcDiscordIntegration.LOGGER.info("Player {} left bridged group {}",
                event.getConnection().getPlayer().getUuid(), event.getGroup().getName());
        });
    }

}
