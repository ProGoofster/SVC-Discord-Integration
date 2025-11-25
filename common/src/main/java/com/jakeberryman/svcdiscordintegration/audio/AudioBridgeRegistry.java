package com.jakeberryman.svcdiscordintegration.audio;

import de.maxhenkel.voicechat.api.Group;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AudioBridgeRegistry {

    private static final Map<UUID, AudioBridge> bridges = new ConcurrentHashMap<>();

    public static void registerBridge(AudioBridge bridge) {
        bridges.put(bridge.getSvcGroup().getId(), bridge);
    }

    public static void unregisterBridge(UUID groupId) {
        AudioBridge bridge = bridges.remove(groupId);
        if (bridge != null) {
            bridge.stop();
        }
    }

    public static Optional<AudioBridge> getBridge(UUID groupId) {
        return Optional.ofNullable(bridges.get(groupId));
    }

    public static Optional<AudioBridge> getBridgeByGroup(Group group) {
        return getBridge(group.getId());
    }

    public static void stopAll() {
        bridges.values().forEach(AudioBridge::stop);
        bridges.clear();
    }

    public static boolean hasBridge(UUID groupId) {
        return bridges.containsKey(groupId);
    }
}
