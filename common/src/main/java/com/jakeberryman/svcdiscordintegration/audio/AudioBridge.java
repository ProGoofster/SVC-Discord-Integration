package com.jakeberryman.svcdiscordintegration.audio;

import com.jakeberryman.svcdiscordintegration.SvcDiscordIntegration;
import com.jakeberryman.svcdiscordintegration.discord.DiscordAudioReceiveHandler;
import com.jakeberryman.svcdiscordintegration.discord.DiscordAudioSendHandler;
import com.jakeberryman.svcdiscordintegration.voicechat.SvcPlugin;
import de.maxhenkel.voicechat.api.Group;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.audiochannel.StaticAudioChannel;
import net.dv8tion.jda.api.entities.User;
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
    private final Set<VoicechatConnection> groupConnections = ConcurrentHashMap.newKeySet();
    private volatile boolean active = false;

    public AudioBridge(Group svcGroup, VoiceChannel discordChannel, AudioManager discordAudioManager) {
        this.svcGroup = svcGroup;
        this.discordChannel = discordChannel;
        this.discordAudioManager = discordAudioManager;

        this.sendHandler = new DiscordAudioSendHandler();
        this.receiveHandler = new DiscordAudioReceiveHandler(
            discordChannel.getGuild().getJDA(),
            this::onDiscordUserAudioReceived
        );
    }

    public void start() {
        if (active) return;

        SvcDiscordIntegration.LOGGER.info("Starting audio bridge between Discord channel '{}' and SVC group '{}'",
            discordChannel.getName(), svcGroup.getName());

        // Create a static audio channel for broadcasting Discord audio to SVC players
        UUID channelId = UUID.randomUUID();
        svcAudioChannel = SvcPlugin.SERVER_API.createStaticAudioChannel(channelId);

        if (svcAudioChannel != null) {
            svcAudioChannel.setCategory(SvcPlugin.discordPlayer.getId());
            SvcDiscordIntegration.LOGGER.info("Set audio channel category to: {}", SvcPlugin.discordPlayer.getId());
        } else {
            SvcDiscordIntegration.LOGGER.error("Failed to create static audio channel!");
        }

        discordAudioManager.setReceivingHandler(receiveHandler);
        discordAudioManager.setSendingHandler(sendHandler);

        receiveHandler.setActive(true);
        sendHandler.setActive(true);
        active = true;

        SvcDiscordIntegration.LOGGER.info("Audio bridge started - forwarding raw Opus audio between Discord and SVC");
    }

    public void stop() {
        if (!active) return;

        SvcDiscordIntegration.LOGGER.info("Stopping audio bridge");

        receiveHandler.setActive(false);
        sendHandler.setActive(false);

        if (svcAudioChannel != null) {
            svcAudioChannel.clearTargets();
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

    private void onDiscordUserAudioReceived(User user, byte[] opusData) {
        if (!active || svcAudioChannel == null) return;

        try {
            // Discord sends raw Opus-encoded audio per user (48kHz mono, 20ms frames)
            // Forward directly to SVC without decoding - both systems use Opus
            svcAudioChannel.send(opusData);

            SvcDiscordIntegration.LOGGER.info("Forwarded {} bytes of Opus audio from Discord user {} to SVC ({} targets)",
                opusData.length, user.getName(), groupConnections.size());

        } catch (Exception e) {
            SvcDiscordIntegration.LOGGER.error("Error forwarding Discord user audio to SVC", e);
        }
    }

    public void onSvcAudioReceived(UUID playerUuid, byte[] opusData) {
        if (!active) return;

        try {
            // SVC provides raw Opus-encoded data, forward directly to Discord without encoding
            sendHandler.queueAudio(opusData);
        } catch (Exception e) {
            SvcDiscordIntegration.LOGGER.error("Error forwarding SVC audio to Discord", e);
        }
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
