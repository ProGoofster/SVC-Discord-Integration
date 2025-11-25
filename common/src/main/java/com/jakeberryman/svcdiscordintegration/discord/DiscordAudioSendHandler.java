package com.jakeberryman.svcdiscordintegration.discord;

import com.jakeberryman.svcdiscordintegration.SvcDiscordIntegration;
import net.dv8tion.jda.api.audio.AudioSendHandler;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentLinkedQueue;

public class DiscordAudioSendHandler implements AudioSendHandler {

    private final ConcurrentLinkedQueue<byte[]> audioQueue = new ConcurrentLinkedQueue<>();
    private volatile boolean active = true;
    private static final int MAX_QUEUE_SIZE = 100;

    public void queueAudio(byte[] audioData) {
        if (!active) return;

        if (audioQueue.size() >= MAX_QUEUE_SIZE) {
            audioQueue.poll();
        }
        audioQueue.offer(audioData);
    }

    @Override
    public boolean canProvide() {
        return active && !audioQueue.isEmpty();
    }

    @Nullable
    @Override
    public ByteBuffer provide20MsAudio() {
        byte[] data = audioQueue.poll();
        if (data == null) {
            return null;
        }
        return ByteBuffer.wrap(data);
    }

    @Override
    public boolean isOpus() {
        return true; // We're sending Opus-encoded data from SVC
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isActive() {
        return active;
    }

    public void clearQueue() {
        audioQueue.clear();
    }
}
