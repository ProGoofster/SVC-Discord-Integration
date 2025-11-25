package com.jakeberryman.svcdiscordintegration.voicechat;

import com.jakeberryman.svcdiscordintegration.SvcDiscordIntegration;
import com.jakeberryman.svcdiscordintegration.config.Config;
import com.jakeberryman.svcdiscordintegration.discord.BotInstance;
import com.jakeberryman.svcdiscordintegration.platform.PlatformHelper;
import de.maxhenkel.voicechat.api.*;
import de.maxhenkel.voicechat.api.audiosender.AudioSender;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.CreateGroupEvent;
import de.maxhenkel.voicechat.api.events.PlayerConnectedEvent;
import de.maxhenkel.voicechat.api.events.RemoveGroupEvent;
import de.maxhenkel.voicechat.api.events.VoicechatServerStartedEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;


public class SvcPlugin implements VoicechatPlugin {

    @Nullable
    public static VoicechatServerApi SERVER_API;

    public static List<Group> groups = new ArrayList<>();

    // Map of group UUID to Discord voice channel ID (for tracking which Discord channels are connected to which groups)
    private static Map<UUID, Long> groupToDiscordChannel = new HashMap<>();

    // UUID for the virtual Discord bot player
    private static final UUID DISCORD_BOT_UUID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    // Platform helper for creating fake players (set by platform-specific implementations)
    protected static PlatformHelper platformHelper;

    private BotInstance bot;

    public static VolumeCategory discordPlayer;

    /**
     * Sets the platform helper. Called by platform-specific implementations.
     */
    public static void setPlatformHelper(PlatformHelper helper) {
        platformHelper = helper;
    }

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
        registration.registerEvent(PlayerConnectedEvent.class, this::onPlayerConnected);
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

        // Clean up Discord connection if exists
        UUID groupId = event.getGroup().getId();
        if (groupToDiscordChannel.containsKey(groupId)) {
            groupToDiscordChannel.remove(groupId);
        }
    }

    private void onPlayerConnected(PlayerConnectedEvent event) {
        // When a real player connects to voice chat, try to create the fake player connection
        // This ensures the voice chat server is running and can handle connections
        if (platformHelper != null && SERVER_API != null) {
            VoicechatConnection botConnection = SERVER_API.getConnectionOf(DISCORD_BOT_UUID);
            if (botConnection == null) {
                SvcDiscordIntegration.LOGGER.info("Attempting to create Discord bot connection after player joined voice chat");
                platformHelper.createFakePlayer(DISCORD_BOT_UUID, "Discord");
            }
        }
    }

    public static void joinDiscordToGroup(Group group, long discordChannelId) {
        groupToDiscordChannel.put(group.getId(), discordChannelId);

        // Try to get or create a connection for the Discord bot
        if (SERVER_API != null && platformHelper != null) {
            VoicechatConnection botConnection = SERVER_API.getConnectionOf(DISCORD_BOT_UUID);

            if (botConnection == null) {
                // Connection doesn't exist, create a fake player
                SvcDiscordIntegration.LOGGER.info("Creating Discord bot player...");
                botConnection = platformHelper.createFakePlayer(DISCORD_BOT_UUID, "Discord");
            }

            if (botConnection != null) {
                // Bot connection exists, add it to the group
                botConnection.setGroup(group);
                botConnection.setConnected(true);
                SvcDiscordIntegration.LOGGER.info("Discord bot joined voice chat group: " + group.getName());
            } else {
                SvcDiscordIntegration.LOGGER.warn("Failed to create Discord bot player. Bot will not appear in group.");
            }
        }

        SvcDiscordIntegration.LOGGER.info("Discord channel " + discordChannelId + " connected to voice chat group: " + group.getName());
    }

    public static void leaveDiscordFromGroup(Group group) {
        UUID groupId = group.getId();
        if (groupToDiscordChannel.containsKey(groupId)) {
            Long channelId = groupToDiscordChannel.remove(groupId);

            // Remove the bot from the group
            if (SERVER_API != null) {
                VoicechatConnection botConnection = SERVER_API.getConnectionOf(DISCORD_BOT_UUID);
                if (botConnection != null) {
                    botConnection.setGroup(null);
                }
            }

            SvcDiscordIntegration.LOGGER.info("Discord channel " + channelId + " disconnected from voice chat group: " + group.getName());
        }
    }

    public static boolean isDiscordConnected(Group group) {
        return groupToDiscordChannel.containsKey(group.getId());
    }

}
