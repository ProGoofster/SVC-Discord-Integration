package com.jakeberryman.svcdiscordintegration.discord;


import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;

public class BotInstance {

    private final int id;
    private final String token;
    private JDA api;
    private final CommandHandler commandHandler;

    private volatile boolean busy = false;
    private volatile String currentActivity = "Waiting for bridge";

    public BotInstance(int id, String token) {
        this.id = id;
        this.token = token;
        this.commandHandler = new CommandHandler();
    }

    public void startBot() {
        api = JDABuilder.createDefault(token)
                .setActivity(Activity.playing("hello from minecraft"))
                .addEventListeners(commandHandler)
                .build();

        commandHandler.registerCommands(api);
    }
}
