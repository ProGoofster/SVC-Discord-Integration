package com.jakeberryman.svcdiscordintegration.audio;

import com.jakeberryman.svcdiscordintegration.SvcDiscordIntegration;
import com.jakeberryman.svcdiscordintegration.discord.DiscordAudioReceiveHandler;
import com.jakeberryman.svcdiscordintegration.discord.DiscordAudioSendHandler;
import com.jakeberryman.svcdiscordintegration.voicechat.SvcPlugin;
import de.maxhenkel.voicechat.api.Group;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.audiochannel.StaticAudioChannel;
import de.maxhenkel.voicechat.api.opus.OpusEncoder;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.managers.AudioManager;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AudioBridge {

    private final Group svcGroup;
    private final VoiceChannel discordChannel;
    private final AudioManager discordAudioManager;

    private final DiscordAudioReceiveHandler receiveHandler;
    private final DiscordAudioSendHandler sendHandler;

    private StaticAudioChannel svcAudioChannel;
    private OpusEncoder opusEncoder;
    private final Set<VoicechatConnection> groupConnections = ConcurrentHashMap.newKeySet();
    private volatile boolean active = false;

    public AudioBridge(Group svcGroup, VoiceChannel discordChannel, AudioManager discordAudioManager) {
        this.svcGroup = svcGroup;
        this.discordChannel = discordChannel;
        this.discordAudioManager = discordAudioManager;

        this.sendHandler = new DiscordAudioSendHandler();
        this.receiveHandler = new DiscordAudioReceiveHandler(this::onDiscordAudioReceived);
    }

    public void start() {
        if (active) return;

        SvcDiscordIntegration.LOGGER.info("Starting audio bridge between Discord channel '{}' and SVC group '{}'",
            discordChannel.getName(), svcGroup.getName());

        // Create a static audio channel for broadcasting Discord audio to SVC players
        UUID channelId = UUID.randomUUID();
        svcAudioChannel = SvcPlugin.SERVER_API.createStaticAudioChannel(channelId);

        // Create an Opus encoder for encoding Discord PCM audio
        opusEncoder = SvcPlugin.SERVER_API.createEncoder();

        discordAudioManager.setReceivingHandler(receiveHandler);
        discordAudioManager.setSendingHandler(sendHandler);

        receiveHandler.setActive(true);
        sendHandler.setActive(true);
        active = true;

        SvcDiscordIntegration.LOGGER.info("Audio bridge started successfully - connections will be added as players join the group");
    }

    public void stop() {
        if (!active) return;

        SvcDiscordIntegration.LOGGER.info("Stopping audio bridge");

        receiveHandler.setActive(false);
        sendHandler.setActive(false);

        if (svcAudioChannel != null) {
            svcAudioChannel.clearTargets();
        }

        if (opusEncoder != null && !opusEncoder.isClosed()) {
            opusEncoder.close();
        }

        sendHandler.clearQueue();
        active = false;

        SvcDiscordIntegration.LOGGER.info("Audio bridge stopped");
    }

    public void addConnection(VoicechatConnection connection) {
        if (svcAudioChannel == null || !active) return;

        groupConnections.add(connection);
        svcAudioChannel.addTarget(connection);
        SvcDiscordIntegration.LOGGER.debug("Added player UUID {} as target for Discord audio",
            connection.getPlayer().getUuid());
    }

    public void removeConnection(VoicechatConnection connection) {
        if (svcAudioChannel == null) return;

        groupConnections.remove(connection);
        svcAudioChannel.removeTarget(connection);
        SvcDiscordIntegration.LOGGER.debug("Removed player UUID {} from Discord audio targets",
            connection.getPlayer().getUuid());
    }

    public void updateGroupTargets() {
        if (svcAudioChannel == null) return;

        // Refresh targets from our tracked connections
        svcAudioChannel.clearTargets();
        groupConnections.forEach(svcAudioChannel::addTarget);
    }

    private void onDiscordAudioReceived(byte[] audioData) {
        if (!active || svcAudioChannel == null || opusEncoder == null) return;

        try {
            // Discord provides 48kHz 16-bit stereo PCM (BigEndian)
            // Convert to short array and downmix stereo to mono
            short[] pcmStereo = bytesToShorts(audioData);
            short[] pcmMono = stereoToMono(pcmStereo);

            // Encode to Opus
            byte[] opusData = opusEncoder.encode(pcmMono);

            // Send Opus-encoded audio to all group members
            svcAudioChannel.send(opusData);

        } catch (Exception e) {
            SvcDiscordIntegration.LOGGER.error("Error processing Discord audio for SVC", e);
        }
    }

    public void onSvcAudioReceived(UUID playerUuid, byte[] opusData) {
        if (!active) return;

        try {
            // SVC provides Opus-encoded data, send directly to Discord
            sendHandler.queueAudio(opusData);
        } catch (Exception e) {
            SvcDiscordIntegration.LOGGER.error("Error processing SVC audio for Discord", e);
        }
    }

    private short[] bytesToShorts(byte[] bytes) {
        short[] shorts = new short[bytes.length / 2];
        for (int i = 0; i < shorts.length; i++) {
            // Discord uses BigEndian format
            shorts[i] = (short) ((bytes[i * 2] << 8) | (bytes[i * 2 + 1] & 0xFF));
        }
        return shorts;
    }

    private short[] stereoToMono(short[] stereo) {
        short[] mono = new short[stereo.length / 2];
        for (int i = 0; i < mono.length; i++) {
            // Average left and right channels
            int left = stereo[i * 2];
            int right = stereo[i * 2 + 1];
            mono[i] = (short) ((left + right) / 2);
        }
        return mono;
    }

    public boolean isActive() {
        return active;
    }

    public Group getSvcGroup() {
        return svcGroup;
    }

    public VoiceChannel getDiscordChannel() {
        return discordChannel;
    }
}
