package com.jakeberryman.svcdiscordintegration.voicechat;

import com.jakeberryman.svcdiscordintegration.SvcDiscordIntegration;
import com.jakeberryman.svcdiscordintegration.audio.AudioBridge;
import com.jakeberryman.svcdiscordintegration.audio.AudioBridgeRegistry;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;

import java.util.UUID;

public class SvcAudioListener {

    public void onMicrophonePacket(MicrophonePacketEvent event) {
        try {
            if (event.getSenderConnection() == null) {
                return;
            }

            UUID playerUuid = event.getSenderConnection().getPlayer().getUuid();

            if (event.getSenderConnection().getGroup() == null) {
                return;
            }

            UUID groupId = event.getSenderConnection().getGroup().getId();

            AudioBridgeRegistry.getBridge(groupId).ifPresent(bridge -> {
                if (bridge.isActive()) {
                    byte[] audioData = event.getPacket().getOpusEncodedData();
                    if (audioData != null && audioData.length > 0) {
                        byte[] pcmData = audioData;
                        bridge.onSvcAudioReceived(playerUuid, pcmData);
                    }
                }
            });
        } catch (Exception e) {
            SvcDiscordIntegration.LOGGER.error("Error handling SVC microphone packet", e);
        }
    }
}
