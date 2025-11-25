package com.jakeberryman.svcdiscordintegration.discord;


import com.jakeberryman.svcdiscordintegration.SvcDiscordIntegration;
import com.jakeberryman.svcdiscordintegration.audio.AudioBridge;
import com.jakeberryman.svcdiscordintegration.audio.AudioBridgeRegistry;
import de.maxhenkel.voicechat.api.Group;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.managers.AudioManager;

public class BotInstance {

    private final int id;
    private final String token;
    private JDA api;
    private final CommandHandler commandHandler;
    private final DiscordEventHandler eventHandler;

    private volatile boolean busy = false;
    private volatile String currentActivity = "Waiting for bridge";

    public BotInstance(int id, String token) {
        this.id = id;
        this.token = token;
        this.commandHandler = new CommandHandler(this);
        this.eventHandler = new DiscordEventHandler();
    }

    public void startBot() {
        api = JDABuilder.createDefault(token)
                .setActivity(Activity.playing("hello from minecraft"))
                .addEventListeners(commandHandler, eventHandler)
                .build();

        commandHandler.registerCommands(api);
    }

    public AudioBridge createAudioBridge(Group svcGroup, VoiceChannel discordChannel) {
        if (AudioBridgeRegistry.hasBridge(svcGroup.getId())) {
            SvcDiscordIntegration.LOGGER.warn("Audio bridge already exists for group: {}", svcGroup.getName());
            return AudioBridgeRegistry.getBridge(svcGroup.getId()).orElse(null);
        }

        AudioManager audioManager = discordChannel.getGuild().getAudioManager();
        AudioBridge bridge = new AudioBridge(svcGroup, discordChannel, audioManager);

        AudioBridgeRegistry.registerBridge(bridge);
        bridge.start();

        SvcDiscordIntegration.LOGGER.info("Created audio bridge for group: {}", svcGroup.getName());
        return bridge;
    }

    public void destroyAudioBridge(Group svcGroup) {
        AudioBridgeRegistry.unregisterBridge(svcGroup.getId());
        SvcDiscordIntegration.LOGGER.info("Destroyed audio bridge for group: {}", svcGroup.getName());
    }

    public JDA getApi() {
        return api;
    }
}
