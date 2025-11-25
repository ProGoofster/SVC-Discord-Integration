package com.jakeberryman.svcdiscordintegration.discord;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

public class CommandHandler extends ListenerAdapter {

    public void registerCommands(JDA api) {
        api.updateCommands().addCommands(
                Commands.slash("list", "List available voice channels"),
                Commands.slash("join", "Join a voice channel")
                        .addOption(OptionType.STRING, "channel", "The channel to join", true),
                Commands.slash("leave", "Leave the current voice channel")
                        .addOption(OptionType.STRING, "channel", "The channel to leave", true)
        ).queue();
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        switch (event.getName()) {
            case "list":
                handleListCommand(event);
                break;
            case "join":
                handleJoinCommand(event);
                break;
            case "leave":
                handleLeaveCommand(event);
                break;
        }
    }

    private void handleListCommand(SlashCommandInteractionEvent event) {
        event.reply("List command received (not yet implemented)").setEphemeral(true).queue();
    }

    private void handleJoinCommand(SlashCommandInteractionEvent event) {
        String channel = event.getOption("channel").getAsString();
        event.reply("Join command received for channel: " + channel + " (not yet implemented)").setEphemeral(true).queue();
    }

    private void handleLeaveCommand(SlashCommandInteractionEvent event) {
        String channel = event.getOption("channel").getAsString();
        event.reply("Leave command received for channel: " + channel + " (not yet implemented)").setEphemeral(true).queue();
    }
}
