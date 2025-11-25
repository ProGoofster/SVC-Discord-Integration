package com.jakeberryman.svcdiscordintegration.voicechat;

import com.jakeberryman.svcdiscordintegration.SvcDiscordIntegration;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * A UDP client that connects to the Simple Voice Chat server to make the Discord bot appear in voice chat groups.
 */
public class VoiceChatClient {

    private static final byte MAGIC_BYTE = (byte) 0b11111111;
    private static final byte AUTHENTICATE_PACKET_ID = 0x5;
    private static final byte KEEP_ALIVE_PACKET_ID = 0x8;
    private static final byte CONNECTION_CHECK_PACKET_ID = 0x9;

    private final UUID playerUuid;
    private final String host;
    private final int port;
    private byte[] secret;

    private DatagramSocket socket;
    private ScheduledExecutorService keepAliveExecutor;
    private boolean connected = false;

    public VoiceChatClient(UUID playerUuid, String host, int port, byte[] secret) {
        this.playerUuid = playerUuid;
        this.host = host;
        this.port = port;
        this.secret = secret;
    }

    /**
     * Connects to the voice chat server and authenticates.
     */
    public void connect() throws IOException {
        if (connected) {
            SvcDiscordIntegration.LOGGER.warn("Voice chat client already connected");
            return;
        }

        try {
            socket = new DatagramSocket();
            socket.connect(InetAddress.getByName(host), port);

            SvcDiscordIntegration.LOGGER.info("Connecting to voice chat server at {}:{}", host, port);

            // Send authentication packet
            sendAuthenticatePacket();

            // Wait a moment for the server to process authentication
            Thread.sleep(100);

            // Send connection check packet to complete the connection
            sendConnectionCheckPacket();

            // Start keep-alive thread
            startKeepAlive();

            connected = true;
            SvcDiscordIntegration.LOGGER.info("Successfully connected to voice chat server");
        } catch (Exception e) {
            SvcDiscordIntegration.LOGGER.error("Failed to connect to voice chat server", e);
            disconnect();
            throw new IOException("Failed to connect to voice chat server", e);
        }
    }

    /**
     * Sends the authentication packet to the server.
     */
    private void sendAuthenticatePacket() throws Exception {
        FriendlyByteBuf payloadBuffer = new FriendlyByteBuf(Unpooled.buffer());

        // Write packet type
        payloadBuffer.writeByte(AUTHENTICATE_PACKET_ID);

        // Write player UUID
        payloadBuffer.writeUUID(playerUuid);

        // Write secret (16 bytes)
        payloadBuffer.writeBytes(secret);

        // Read payload bytes
        byte[] payloadBytes = new byte[payloadBuffer.readableBytes()];
        payloadBuffer.readBytes(payloadBytes);

        // Encrypt the payload
        byte[] encryptedPayload = encryptPayload(payloadBytes);

        // Create the full packet
        FriendlyByteBuf packetBuffer = new FriendlyByteBuf(Unpooled.buffer());
        packetBuffer.writeByte(MAGIC_BYTE);
        packetBuffer.writeUUID(playerUuid);
        packetBuffer.writeByteArray(encryptedPayload);

        // Read packet bytes
        byte[] packetBytes = new byte[packetBuffer.readableBytes()];
        packetBuffer.readBytes(packetBytes);

        // Send the packet
        DatagramPacket packet = new DatagramPacket(packetBytes, packetBytes.length);
        socket.send(packet);

        SvcDiscordIntegration.LOGGER.info("Sent authentication packet");
    }

    /**
     * Sends the connection check packet to complete the connection.
     */
    private void sendConnectionCheckPacket() throws Exception {
        FriendlyByteBuf payloadBuffer = new FriendlyByteBuf(Unpooled.buffer());

        // Write packet type (ConnectionCheckPacket has no data)
        payloadBuffer.writeByte(CONNECTION_CHECK_PACKET_ID);

        // Read payload bytes
        byte[] payloadBytes = new byte[payloadBuffer.readableBytes()];
        payloadBuffer.readBytes(payloadBytes);

        // Encrypt the payload
        byte[] encryptedPayload = encryptPayload(payloadBytes);

        // Create the full packet
        FriendlyByteBuf packetBuffer = new FriendlyByteBuf(Unpooled.buffer());
        packetBuffer.writeByte(MAGIC_BYTE);
        packetBuffer.writeUUID(playerUuid);
        packetBuffer.writeByteArray(encryptedPayload);

        // Read packet bytes
        byte[] packetBytes = new byte[packetBuffer.readableBytes()];
        packetBuffer.readBytes(packetBytes);

        // Send the packet
        DatagramPacket packet = new DatagramPacket(packetBytes, packetBytes.length);
        socket.send(packet);

        SvcDiscordIntegration.LOGGER.info("Sent connection check packet");
    }

    /**
     * Encrypts the payload using AES/GCM/NoPadding.
     */
    private byte[] encryptPayload(byte[] data) throws Exception {
        javax.crypto.spec.SecretKeySpec keySpec = new javax.crypto.spec.SecretKeySpec(secret, "AES");

        // Generate IV
        byte[] iv = new byte[12];
        new java.security.SecureRandom().nextBytes(iv);

        // Encrypt
        javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, keySpec, new javax.crypto.spec.GCMParameterSpec(128, iv));
        byte[] encrypted = cipher.doFinal(data);

        // Combine IV + encrypted data
        byte[] payload = new byte[iv.length + encrypted.length];
        System.arraycopy(iv, 0, payload, 0, iv.length);
        System.arraycopy(encrypted, 0, payload, iv.length, encrypted.length);

        return payload;
    }

    /**
     * Starts sending keep-alive packets every 5 seconds.
     */
    private void startKeepAlive() {
        keepAliveExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "VoiceChat-KeepAlive");
            thread.setDaemon(true);
            return thread;
        });

        keepAliveExecutor.scheduleAtFixedRate(() -> {
            try {
                sendKeepAlivePacket();
            } catch (Exception e) {
                SvcDiscordIntegration.LOGGER.error("Failed to send keep-alive packet", e);
            }
        }, 5, 5, TimeUnit.SECONDS);
    }

    /**
     * Sends a keep-alive packet to maintain the connection.
     */
    private void sendKeepAlivePacket() throws Exception {
        FriendlyByteBuf payloadBuffer = new FriendlyByteBuf(Unpooled.buffer());

        // Write packet type
        payloadBuffer.writeByte(KEEP_ALIVE_PACKET_ID);

        // Read payload bytes
        byte[] payloadBytes = new byte[payloadBuffer.readableBytes()];
        payloadBuffer.readBytes(payloadBytes);

        // Encrypt the payload
        byte[] encryptedPayload = encryptPayload(payloadBytes);

        // Create the full packet
        FriendlyByteBuf packetBuffer = new FriendlyByteBuf(Unpooled.buffer());
        packetBuffer.writeByte(MAGIC_BYTE);
        packetBuffer.writeUUID(playerUuid);
        packetBuffer.writeByteArray(encryptedPayload);

        // Read packet bytes
        byte[] packetBytes = new byte[packetBuffer.readableBytes()];
        packetBuffer.readBytes(packetBytes);

        // Send the packet
        DatagramPacket packet = new DatagramPacket(packetBytes, packetBytes.length);
        socket.send(packet);
    }

    /**
     * Disconnects from the voice chat server.
     */
    public void disconnect() {
        connected = false;

        if (keepAliveExecutor != null) {
            keepAliveExecutor.shutdownNow();
            keepAliveExecutor = null;
        }

        if (socket != null && !socket.isClosed()) {
            socket.close();
            socket = null;
        }

        SvcDiscordIntegration.LOGGER.info("Disconnected from voice chat server");
    }

    public boolean isConnected() {
        return connected;
    }
}
