package dev.nikhey.betteraudit.hook;

import dev.nikhey.betteraudit.alert.AlertSink;
import dev.nikhey.betteraudit.config.Settings;
import dev.nikhey.betteraudit.model.ActionType;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.jda.api.entities.TextChannel;
import github.scarsz.discordsrv.util.DiscordUtil;

import java.util.function.Supplier;

/**
 * Sends alerts through DiscordSRV instead of (or alongside) the raw webhook,
 * so servers that already run DiscordSRV need zero extra setup. The target is
 * the in-game channel named in discord.discordsrv.channel, falling back to
 * DiscordSRV's main channel.
 */
public final class DiscordSrvSink implements AlertSink {

    private final Supplier<Settings> settings;

    public DiscordSrvSink(Supplier<Settings> settings) {
        this.settings = settings;
    }

    @Override
    public void send(ActionType type, String actorName, String detail) {
        Settings s = settings.get();
        if (!s.discordSrvEnabled()) {
            return;
        }
        DiscordSRV srv = DiscordSRV.getPlugin();
        String channelName = s.discordSrvChannel();
        TextChannel channel = channelName.isBlank()
                ? srv.getMainTextChannel()
                : srv.getDestinationTextChannelForGameChannelName(channelName);
        if (channel == null) {
            return;
        }
        // Zero-width space after @ blocks accidental/abusive pings from detail text.
        String safeDetail = detail.replace("@", "@​");
        DiscordUtil.queueMessage(channel,
                "**" + type.display() + " — " + actorName + "**: " + safeDetail);
    }
}
