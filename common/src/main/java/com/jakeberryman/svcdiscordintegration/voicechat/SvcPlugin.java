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

    // Voice chat client for the Discord bot
    private static VoiceChatClient voiceChatClient;

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

        // Connect the voice chat client if not already connected
        if (SERVER_API != null) {
            try {
                // Create fake player first (if not already created)
                if (platformHelper != null && getFakePlayer() == null) {
                    SvcDiscordIntegration.LOGGER.info("Creating fake player for Discord bot...");
                    platformHelper.createFakePlayer(DISCORD_BOT_UUID, "Discord");
                }

                if (voiceChatClient == null || !voiceChatClient.isConnected()) {
                    // Get the voice chat server configuration
                    String host = "localhost"; // Server is on the same machine
                    int port = 24454; // Default voice chat port (TODO: get from config)

                    // Generate a secret for the bot
                    // Since we're on the server side, we can generate and register the secret directly
                    byte[] secret = generateBotSecret();

                    // Create and connect the voice chat client
                    voiceChatClient = new VoiceChatClient(DISCORD_BOT_UUID, host, port, secret);
                    voiceChatClient.connect();

                    SvcDiscordIntegration.LOGGER.info("Discord bot connected to voice chat server");
                }

                // Wait for the server to register the connection (try a few times)
                VoicechatConnection botConnection = null;
                for (int i = 0; i < 10; i++) {
                    // Try to get the connection through the API
                    botConnection = SERVER_API.getConnectionOf(DISCORD_BOT_UUID);
                    if (botConnection != null) {
                        break;
                    }

                    // If API doesn't have it, try accessing through reflection
                    if (i == 5) {
                        botConnection = getConnectionThroughReflection();
                        if (botConnection != null) {
                            SvcDiscordIntegration.LOGGER.info("Got connection through reflection, breaking loop");
                            break;
                        } else {
                            SvcDiscordIntegration.LOGGER.warn("Reflection returned null connection");
                        }
                    }

                    // Wait 100ms before retrying
                    Thread.sleep(100);
                }

                SvcDiscordIntegration.LOGGER.info("After retry loop, botConnection is: {}", botConnection != null ? "NOT NULL" : "NULL");

                if (botConnection != null) {
                    // Add bot to the group
                    botConnection.setGroup(group);
                    SvcDiscordIntegration.LOGGER.info("Discord bot joined voice chat group: " + group.getName());
                } else {
                    SvcDiscordIntegration.LOGGER.warn("Voice chat client connected but connection not found after retries. Bot may appear shortly.");
                }
            } catch (Exception e) {
                SvcDiscordIntegration.LOGGER.error("Failed to connect Discord bot to voice chat", e);
            }
        }

        SvcDiscordIntegration.LOGGER.info("Discord channel " + discordChannelId + " connected to voice chat group: " + group.getName());
    }

    /**
     * Tries to get the bot's VoicechatConnection by creating it from the PlayerState.
     */
    private static VoicechatConnection getConnectionThroughReflection() {
        try {
            // Access the voice chat server through reflection
            Class<?> voicechatClass = Class.forName("de.maxhenkel.voicechat.Voicechat");
            Object serverVoiceEvents = voicechatClass.getField("SERVER").get(null);

            if (serverVoiceEvents != null) {
                Object server = serverVoiceEvents.getClass().getMethod("getServer").invoke(serverVoiceEvents);

                if (server != null) {
                    // Get the PlayerStateManager
                    Object playerStateManager = server.getClass().getMethod("getPlayerStateManager").invoke(server);

                    // Get the PlayerState for our bot
                    Object playerState = playerStateManager.getClass().getMethod("getState", UUID.class).invoke(playerStateManager, DISCORD_BOT_UUID);

                    if (playerState != null) {
                        SvcDiscordIntegration.LOGGER.info("Found PlayerState for bot");

                        // Try to get the fake player we created
                        Object fakePlayer = getFakePlayer();

                        if (fakePlayer != null) {
                            // Create VoicechatConnectionImpl
                            Class<?> connImplClass = Class.forName("de.maxhenkel.voicechat.plugins.impl.VoicechatConnectionImpl");
                            Object connection = connImplClass.getDeclaredConstructor(
                                    net.minecraft.server.level.ServerPlayer.class,
                                    playerState.getClass()
                            ).newInstance(fakePlayer, playerState);

                            SvcDiscordIntegration.LOGGER.info("Created VoicechatConnection for bot");
                            return (VoicechatConnection) connection;
                        } else {
                            SvcDiscordIntegration.LOGGER.warn("Fake player not found");
                        }
                    } else {
                        SvcDiscordIntegration.LOGGER.warn("PlayerState not found for bot UUID");
                    }
                }
            }
        } catch (Exception e) {
            SvcDiscordIntegration.LOGGER.error("Failed to create connection through reflection", e);
        }
        return null;
    }

    /**
     * Gets the fake player entity we created.
     */
    private static Object getFakePlayer() {
        if (platformHelper != null) {
            try {
                Class<?> helperClass = platformHelper.getClass();
                java.lang.reflect.Field fakePlayersField = helperClass.getDeclaredField("fakePlayers");
                fakePlayersField.setAccessible(true);
                @SuppressWarnings("unchecked")
                java.util.Map<UUID, Object> fakePlayers = (java.util.Map<UUID, Object>) fakePlayersField.get(platformHelper);
                return fakePlayers.get(DISCORD_BOT_UUID);
            } catch (Exception e) {
                SvcDiscordIntegration.LOGGER.debug("Failed to get fake player", e);
            }
        }
        return null;
    }

    /**
     * Gets or generates a secret for the Discord bot from the voice chat server.
     */
    private static byte[] generateBotSecret() {
        try {
            // Access the voice chat server through reflection
            Class<?> voicechatClass = Class.forName("de.maxhenkel.voicechat.Voicechat");
            Object serverVoiceEvents = voicechatClass.getField("SERVER").get(null);

            if (serverVoiceEvents != null) {
                Object server = serverVoiceEvents.getClass().getMethod("getServer").invoke(serverVoiceEvents);

                if (server != null) {
                    // Call getSecret() which will generate and register a new secret if needed
                    Object secretObj = server.getClass().getMethod("getSecret", UUID.class).invoke(server, DISCORD_BOT_UUID);
                    byte[] secret = (byte[]) secretObj.getClass().getMethod("getSecret").invoke(secretObj);

                    SvcDiscordIntegration.LOGGER.info("Retrieved secret for Discord bot from voice chat server");
                    return secret;
                }
            }
        } catch (Exception e) {
            SvcDiscordIntegration.LOGGER.error("Failed to get secret from voice chat server", e);
        }

        // Fallback: generate a random secret (won't work for authentication)
        SvcDiscordIntegration.LOGGER.warn("Could not get secret from server, generating random secret (authentication will likely fail)");
        byte[] secret = new byte[16];
        new java.security.SecureRandom().nextBytes(secret);
        return secret;
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

            // If no more groups are connected, disconnect the voice chat client
            if (groupToDiscordChannel.isEmpty() && voiceChatClient != null) {
                voiceChatClient.disconnect();
                voiceChatClient = null;
                SvcDiscordIntegration.LOGGER.info("Disconnected Discord bot from voice chat server");
            }

            SvcDiscordIntegration.LOGGER.info("Discord channel " + channelId + " disconnected from voice chat group: " + group.getName());
        }
    }

    public static boolean isDiscordConnected(Group group) {
        return groupToDiscordChannel.containsKey(group.getId());
    }

}
