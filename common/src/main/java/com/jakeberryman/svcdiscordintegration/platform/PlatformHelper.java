package com.jakeberryman.svcdiscordintegration.platform;

import de.maxhenkel.voicechat.api.VoicechatConnection;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Platform-specific helper methods.
 * Implemented differently for Forge and Fabric.
 */
public interface PlatformHelper {

    /**
     * Creates a fake player entity and returns its voice chat connection.
     * This allows the Discord bot to appear in voice chat groups.
     *
     * @param uuid The UUID for the fake player
     * @param name The display name for the fake player
     * @return The VoicechatConnection for the fake player, or null if creation failed
     */
    @Nullable
    VoicechatConnection createFakePlayer(UUID uuid, String name);

    /**
     * Removes a fake player entity.
     *
     * @param uuid The UUID of the fake player to remove
     */
    void removeFakePlayer(UUID uuid);
}
