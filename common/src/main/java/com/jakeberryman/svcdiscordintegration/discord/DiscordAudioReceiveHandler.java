package com.jakeberryman.svcdiscordintegration.discord;

import com.jakeberryman.svcdiscordintegration.SvcDiscordIntegration;
import net.dv8tion.jda.api.audio.AudioReceiveHandler;
import net.dv8tion.jda.api.audio.CombinedAudio;
import net.dv8tion.jda.api.audio.UserAudio;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public class DiscordAudioReceiveHandler implements AudioReceiveHandler {

    private final Consumer<byte[]> audioConsumer;
    private volatile boolean active = true;

    public DiscordAudioReceiveHandler(Consumer<byte[]> audioConsumer) {
        this.audioConsumer = audioConsumer;
    }

    @Override
    public boolean canReceiveCombined() {
        return true;
    }

    @Override
    public void handleCombinedAudio(@NotNull CombinedAudio combinedAudio) {
        if (!active) return;

        try {
            byte[] audioData = combinedAudio.getAudioData(1.0f);
            if (audioData != null && audioData.length > 0) {
                audioConsumer.accept(audioData);
            }
        } catch (Exception e) {
            SvcDiscordIntegration.LOGGER.error("Error handling Discord audio", e);
        }
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isActive() {
        return active;
    }
}
