package com.jakeberryman.svcdiscordintegration.discord;

import com.jakeberryman.svcdiscordintegration.voicechat.SvcPlugin;
import de.maxhenkel.voicechat.api.Group;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.unions.AudioChannelUnion;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.managers.AudioManager;

public class DiscordEventHandler extends ListenerAdapter {

    @Override
    public void onGuildVoiceUpdate(GuildVoiceUpdateEvent event) {
        AudioChannelUnion channelLeft = event.getChannelLeft();

        if (channelLeft == null) return;
        AudioManager audioManager = event.getGuild().getAudioManager();

        if (!audioManager.isConnected()) return;

        AudioChannelUnion botChannel = audioManager.getConnectedChannel();
        if (botChannel == null || !botChannel.equals(channelLeft)) return;

        // Count non-bot members in the channel
        long humanCount = channelLeft.getMembers().stream()
                .filter(member -> !member.getUser().isBot())
                .count();

        // If no humans left, disconnect the bot
        if (humanCount == 0) {
            long channelId = channelLeft.getIdLong();

            // Find and disconnect from any associated voice chat group
            for (Group group : SvcPlugin.groups) {
                if (SvcPlugin.isDiscordConnected(group)) {
                    SvcPlugin.leaveDiscordFromGroup(group);
                    break;
                }
            }

            audioManager.closeAudioConnection();
            System.out.println("Bot left voice channel '" + channelLeft.getName() + "' because it was empty.");
        }
    }
}