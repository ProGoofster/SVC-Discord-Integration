package com.jakeberryman.svcdiscordintegration.discord;

import com.jakeberryman.svcdiscordintegration.SvcDiscordIntegration;
import com.jakeberryman.svcdiscordintegration.audio.AudioBridge;
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

    private final BotInstance botInstance;

    public CommandHandler(BotInstance botInstance) {
        this.botInstance = botInstance;
    }

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
        if (event.getOption("group") == null) {
            event.reply("Group name is required.").setEphemeral(true).queue();
            return;
        }

        String groupName = event.getOption("group").getAsString();

        Member member = event.getMember();
        if (member == null) {
            event.reply("Could not identify user.").setEphemeral(true).queue();
            return;
        }

        if (member.getVoiceState() == null || !member.getVoiceState().inAudioChannel()) {
            event.reply("You must be in a Discord voice channel to use this command.").setEphemeral(true).queue();
            return;
        }

        if (member.getVoiceState().getChannel() == null) {
            event.reply("Could not find your voice channel.").setEphemeral(true).queue();
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

        if (event.getGuild() == null) {
            event.reply("This command must be used in a server.").setEphemeral(true).queue();
            return;
        }

        AudioManager manager = event.getGuild().getAudioManager();
        manager.openAudioConnection(voiceChannel);

        try {
            AudioBridge bridge = botInstance.createAudioBridge(group, voiceChannel);
            if (bridge != null) {
                SvcDiscordIntegration.LOGGER.info("Successfully bridged Discord channel '{}' with SVC group '{}'",
                    voiceChannel.getName(), group.getName());
                event.reply("Successfully joined! Audio is now bridged between Discord channel '" +
                    voiceChannel.getName() + "' and SVC group '" + group.getName() + "'").queue();
            } else {
                event.reply("Failed to create audio bridge. Check server logs.").setEphemeral(true).queue();
            }
        } catch (Exception e) {
            SvcDiscordIntegration.LOGGER.error("Error creating audio bridge", e);
            event.reply("Error creating audio bridge: " + e.getMessage()).setEphemeral(true).queue();
        }
    }

    private void handleLeaveCommand(SlashCommandInteractionEvent event) {
        String groupName = event.getOption("group") != null
            ? event.getOption("group").getAsString()
            : null;

        if (groupName == null) {
            event.reply("Group name is required.").setEphemeral(true).queue();
            return;
        }

        Group group = SvcPlugin.groups.stream()
                .filter(g -> g.getName().equals(groupName))
                .findFirst()
                .orElse(null);

        if (group == null) {
            event.reply("Group not found: " + groupName).setEphemeral(true).queue();
            return;
        }

        try {
            botInstance.destroyAudioBridge(group);

            if (event.getGuild() != null) {
                event.getGuild().getAudioManager().closeAudioConnection();
            }

            SvcDiscordIntegration.LOGGER.info("Disconnected audio bridge for group '{}'", group.getName());
            event.reply("Successfully disconnected from SVC group '" + group.getName() + "'").queue();
        } catch (Exception e) {
            SvcDiscordIntegration.LOGGER.error("Error destroying audio bridge", e);
            event.reply("Error disconnecting: " + e.getMessage()).setEphemeral(true).queue();
        }
    }
}
