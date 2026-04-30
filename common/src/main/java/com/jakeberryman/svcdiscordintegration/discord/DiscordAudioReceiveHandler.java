package com.jakeberryman.svcdiscordintegration.discord;

import com.jakeberryman.svcdiscordintegration.SvcDiscordIntegration;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.audio.AudioReceiveHandler;
import net.dv8tion.jda.api.audio.OpusPacket;
import net.dv8tion.jda.api.entities.User;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiConsumer;

public class DiscordAudioReceiveHandler implements AudioReceiveHandler {

    private final JDA jda;
    private final BiConsumer<User, byte[]> opusConsumer;
    private volatile boolean active = true;

    public DiscordAudioReceiveHandler(JDA jda, BiConsumer<User, byte[]> opusConsumer) {
        this.jda = jda;
        this.opusConsumer = opusConsumer;
    }

    @Override
    public boolean canReceiveEncoded() {
        return true;
    }

    @Override
    public boolean canReceiveUser() {
        return false;
    }

    @Override
    public boolean canReceiveCombined() {
        return false;
    }

    @Override
    public void handleEncodedAudio(@NotNull OpusPacket packet) {
        if (!active) return;

        try {
            // Get raw Opus bytes without decoding
            byte[] opusAudio = packet.getOpusAudio();
            if (opusAudio != null && opusAudio.length > 0) {
                // Look up the User from the userId
                User user = jda.getUserById(packet.getUserId());
                if (user != null) {
                    opusConsumer.accept(user, opusAudio);
                }
            }
        } catch (Exception e) {
            SvcDiscordIntegration.LOGGER.error("Error handling Discord encoded audio", e);
        }
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isActive() {
        return active;
    }
}
