package com.jakeberryman.svcdiscordintegration.forge;

import com.jakeberryman.svcdiscordintegration.SvcDiscordIntegration;
import com.jakeberryman.svcdiscordintegration.platform.PlatformHelper;
import com.jakeberryman.svcdiscordintegration.voicechat.SvcPlugin;
import com.mojang.authlib.GameProfile;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ForgePlatformHelper implements PlatformHelper {

    private final Map<UUID, FakePlayer> fakePlayers = new HashMap<>();

    @Override
    @Nullable
    public VoicechatConnection createFakePlayer(UUID uuid, String name) {
        try {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server == null) {
                SvcDiscordIntegration.LOGGER.error("Cannot create fake player: server is null");
                return null;
            }

            ServerLevel overworld = server.overworld();
            if (overworld == null) {
                SvcDiscordIntegration.LOGGER.error("Cannot create fake player: overworld is null");
                return null;
            }

            // Check if player already exists
            if (fakePlayers.containsKey(uuid)) {
                SvcDiscordIntegration.LOGGER.info("Fake player already exists for UUID: " + uuid);
                if (SvcPlugin.SERVER_API != null) {
                    return SvcPlugin.SERVER_API.getConnectionOf(uuid);
                }
                return null;
            }

            // Create a fake player with the specified UUID and name
            GameProfile profile = new GameProfile(uuid, name);
            FakePlayer fakePlayer = new FakePlayer(overworld, profile);

            // Store the fake player
            fakePlayers.put(uuid, fakePlayer);

            SvcDiscordIntegration.LOGGER.info("Created fake player: " + name + " with UUID: " + uuid);

            // Try to trigger voice chat detection by getting the player from the API
            // The voice chat mod listens to player join events, but since this is a fake player,
            // we need an alternative approach

            // Wait a moment for the voice chat to detect the player, then get the connection
            // Note: This may still return null if the voice chat mod doesn't automatically
            // create connections for fake players. In that case, we'd need to manually
            // connect to the voice chat server or trigger the connection event.
            if (SvcPlugin.SERVER_API != null) {
                VoicechatConnection connection = SvcPlugin.SERVER_API.getConnectionOf(uuid);
                if (connection == null) {
                    SvcDiscordIntegration.LOGGER.warn("Voice chat connection not created for fake player. The bot may not appear in groups.");
                }
                return connection;
            }

            return null;
        } catch (Exception e) {
            SvcDiscordIntegration.LOGGER.error("Failed to create fake player", e);
            return null;
        }
    }

    @Override
    public void removeFakePlayer(UUID uuid) {
        FakePlayer fakePlayer = fakePlayers.remove(uuid);
        if (fakePlayer != null) {
            SvcDiscordIntegration.LOGGER.info("Removed fake player with UUID: " + uuid);
        }
    }
}
