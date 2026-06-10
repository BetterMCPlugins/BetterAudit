package dev.nikhey.betteraudit.alert;

import dev.nikhey.betteraudit.model.ActionType;

/** A destination for audit alerts (Discord webhook, DiscordSRV channel, ...). */
public interface AlertSink {

    void send(ActionType type, String actorName, String detail);
}
