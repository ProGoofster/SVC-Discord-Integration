package com.jakeberryman.svcdiscordintegration.voicechat;

import de.maxhenkel.voicechat.api.Group;
import de.maxhenkel.voicechat.api.ServerPlayer;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;

/**
 * Wrapper around ClientConnection to work around class loader issues.
 * Uses reflection to call methods on the underlying ClientConnection object.
 */
public class ClientConnectionWrapper implements VoicechatConnection {

    private final Object clientConnection;
    private final Method getGroupMethod;
    private final Method setGroupMethod;
    private final Method isInGroupMethod;
    private final Method isConnectedMethod;
    private final Method setConnectedMethod;
    private final Method isDisabledMethod;
    private final Method setDisabledMethod;
    private final Method isInstalledMethod;
    private final Method getPlayerMethod;

    public ClientConnectionWrapper(Object clientConnection) throws Exception {
        this.clientConnection = clientConnection;
        Class<?> clazz = clientConnection.getClass();

        // Cache all the methods we need
        this.getGroupMethod = clazz.getMethod("getGroup");
        this.setGroupMethod = clazz.getMethod("setGroup", Group.class);
        this.isInGroupMethod = clazz.getMethod("isInGroup");
        this.isConnectedMethod = clazz.getMethod("isConnected");
        this.setConnectedMethod = clazz.getMethod("setConnected", boolean.class);
        this.isDisabledMethod = clazz.getMethod("isDisabled");
        this.setDisabledMethod = clazz.getMethod("setDisabled", boolean.class);
        this.isInstalledMethod = clazz.getMethod("isInstalled");
        this.getPlayerMethod = clazz.getMethod("getPlayer");
    }

    @Nullable
    @Override
    public Group getGroup() {
        try {
            return (Group) getGroupMethod.invoke(clientConnection);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get group", e);
        }
    }

    @Override
    public boolean isInGroup() {
        try {
            return (boolean) isInGroupMethod.invoke(clientConnection);
        } catch (Exception e) {
            throw new RuntimeException("Failed to check if in group", e);
        }
    }

    @Override
    public void setGroup(@Nullable Group group) {
        try {
            setGroupMethod.invoke(clientConnection, group);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set group", e);
        }
    }

    @Override
    public boolean isConnected() {
        try {
            return (boolean) isConnectedMethod.invoke(clientConnection);
        } catch (Exception e) {
            throw new RuntimeException("Failed to check if connected", e);
        }
    }

    @Override
    public void setConnected(boolean connected) {
        try {
            setConnectedMethod.invoke(clientConnection, connected);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set connected", e);
        }
    }

    @Override
    public boolean isDisabled() {
        try {
            return (boolean) isDisabledMethod.invoke(clientConnection);
        } catch (Exception e) {
            throw new RuntimeException("Failed to check if disabled", e);
        }
    }

    @Override
    public void setDisabled(boolean disabled) {
        try {
            setDisabledMethod.invoke(clientConnection, disabled);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set disabled", e);
        }
    }

    @Override
    public boolean isInstalled() {
        try {
            return (boolean) isInstalledMethod.invoke(clientConnection);
        } catch (Exception e) {
            throw new RuntimeException("Failed to check if installed", e);
        }
    }

    @Override
    public ServerPlayer getPlayer() {
        try {
            return (ServerPlayer) getPlayerMethod.invoke(clientConnection);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get player", e);
        }
    }
}
