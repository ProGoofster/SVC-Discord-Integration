package com.jakeberryman.svcdiscordintegration.discord;

import com.jakeberryman.svcdiscordintegration.voicechat.SvcPlugin;
import de.maxhenkel.voicechat.api.Group;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.managers.AudioManager;

public class CommandHandler extends ListenerAdapter {

    public void registerCommands(JDA api) {
        api.updateCommands().addCommands(
                Commands.slash("list", "List available voice channels"),
                Commands.slash("join", "Join a voice channel")
                        .addOption(OptionType.STRING, "group", "The group to join", true)
                        .addOption(OptionType.STRING, "password", "password for the group", false),
                Commands.slash("leave", "Leave the current voice channel")
                        .addOption(OptionType.STRING, "group", "The group to leave", false)
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
        if (SvcPlugin.groups.isEmpty()) {
            event.reply("No voice chat groups are currently active.").setEphemeral(true).queue();
            return;
        }

        StringBuilder response = new StringBuilder("**Available Voice Chat Groups:**\n");
        for (int i = 0; i < SvcPlugin.groups.size(); i++) {
            response.append((i + 1)).append(". ").append(SvcPlugin.groups.get(i).getName()).append("\n");
        }

        event.reply(response.toString()).setEphemeral(true).queue();
    }

    private void handleJoinCommand(SlashCommandInteractionEvent event) {
        String groupName = event.getOption("group").getAsString();

        // Get the member who executed the command
        Member member = event.getMember();
        if (member == null) {
            event.reply("Could not identify user.").setEphemeral(true).queue();
            return;
        }

        // Check if the user is in a Discord voice channel
        if (member.getVoiceState() == null || !member.getVoiceState().inAudioChannel()) {
            event.reply("You must be in a Discord voice channel to use this command.").setEphemeral(true).queue();
            return;
        }

        VoiceChannel voiceChannel = member.getVoiceState().getChannel().asVoiceChannel();

        Group group = SvcPlugin.groups.stream()
                .filter(g -> g.getName().equals(groupName))
                .findFirst()
                .orElse(null);

        if (group == null) {
            event.reply("Group not found: " + groupName).setEphemeral(true).queue();
            return;
        }

        AudioManager manager = event.getGuild().getAudioManager();

        //manager.setSendingHandler(new MySendHandler());
        manager.openAudioConnection(voiceChannel);

        // Connect Discord to the voice chat group
        SvcPlugin.joinDiscordToGroup(group, voiceChannel.getIdLong());

        event.reply("Discord bot joined voice channel: " + voiceChannel.getName() + " and connected to group: " + group.getName()).setEphemeral(true).queue();
    }

    private void handleLeaveCommand(SlashCommandInteractionEvent event) {
        String group = event.getOption("group") != null
            ? event.getOption("group").getAsString()
            : null;

        if (group == null) {
            event.reply("Leave command received (not yet implemented)").setEphemeral(true).queue();
        } else {
            event.reply("Leave command received for group: " + group + " (not yet implemented)").setEphemeral(true).queue();
        }
    }
}
