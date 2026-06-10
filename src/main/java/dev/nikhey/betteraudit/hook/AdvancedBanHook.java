package dev.nikhey.betteraudit.hook;

import dev.nikhey.betteraudit.Recorder;
import dev.nikhey.betteraudit.config.Settings;
import dev.nikhey.betteraudit.model.ActionType;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.slf4j.Logger;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * AdvancedBan integration. AdvancedBan publishes no API artifact that is
 * reliable to build against, so the event is registered by class name and
 * read reflectively — the classes are only resolved when the plugin is
 * actually installed.
 */
public final class AdvancedBanHook {

    private AdvancedBanHook() {
    }

    @SuppressWarnings("unchecked")
    public static boolean register(Plugin plugin, Supplier<Settings> settings, Recorder recorder, Logger logger) {
        Class<? extends Event> eventClass;
        try {
            eventClass = (Class<? extends Event>) Class.forName("me.leoko.advancedban.bukkit.event.PunishmentEvent");
        } catch (ClassNotFoundException | ClassCastException e) {
            logger.warn("AdvancedBan is installed but its PunishmentEvent was not found - hook disabled.");
            return false;
        }
        Bukkit.getPluginManager().registerEvent(eventClass, new Listener() {
        }, EventPriority.MONITOR, (listener, event) -> {
            if (!eventClass.isInstance(event) || !settings.get().punishmentsEnabled()) {
                return;
            }
            try {
                Object punishment = event.getClass().getMethod("getPunishment").invoke(event);
                String target = (String) punishment.getClass().getMethod("getName").invoke(punishment);
                String operator = (String) punishment.getClass().getMethod("getOperator").invoke(punishment);
                String reason = (String) punishment.getClass().getMethod("getReason").invoke(punishment);
                Object type = punishment.getClass().getMethod("getType").invoke(punishment);
                recorder.recordOffline(actorUuid(operator),
                        operator == null ? "CONSOLE" : operator,
                        ActionType.PUNISHMENT,
                        "[AdvancedBan] " + String.valueOf(type).toLowerCase() + " " + target
                                + (reason == null || reason.isBlank() ? "" : ": " + reason));
            } catch (Throwable t) {
                logger.warn("Failed to record an AdvancedBan punishment: {}", t.toString());
            }
        }, plugin, true);
        return true;
    }

    private static UUID actorUuid(String operator) {
        if (operator == null || operator.equalsIgnoreCase("CONSOLE")) {
            return Recorder.CONSOLE_UUID;
        }
        var online = Bukkit.getPlayerExact(operator);
        if (online != null) {
            return online.getUniqueId();
        }
        return UUID.nameUUIDFromBytes(("OfflinePlayer:" + operator).getBytes(StandardCharsets.UTF_8));
    }
}
