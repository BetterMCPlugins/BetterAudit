package dev.nikhey.betteraudit.hook;

import dev.nikhey.betteraudit.Recorder;
import dev.nikhey.betteraudit.config.Settings;
import dev.nikhey.betteraudit.model.ActionType;
import litebans.api.Entry;
import litebans.api.Events;
import org.bukkit.Bukkit;
import org.slf4j.Logger;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * LiteBans integration: records punishments through the LiteBans API, which
 * catches GUI-issued punishments and ones synced from other servers — both
 * invisible to command logging.
 */
public final class LiteBansHook {

    private LiteBansHook() {
    }

    public static void register(Supplier<Settings> settings, Recorder recorder, Logger logger) {
        Events.get().register(new Events.Listener() {
            @Override
            public void entryAdded(Entry entry) {
                if (!settings.get().punishmentsEnabled()) {
                    return;
                }
                try {
                    String executorName = entry.getExecutorName() == null ? "CONSOLE" : entry.getExecutorName();
                    UUID executorUuid = parseUuid(entry.getExecutorUUID());
                    recorder.recordOffline(executorUuid, executorName, ActionType.PUNISHMENT,
                            "[LiteBans] " + entry.getType() + " " + targetName(entry)
                                    + (entry.getReason() == null ? "" : ": " + entry.getReason())
                                    + (entry.isPermanent() ? " (permanent)" : ""));
                } catch (Throwable t) {
                    logger.warn("Failed to record a LiteBans punishment: {}", t.toString());
                }
            }
        });
    }

    private static UUID parseUuid(String raw) {
        if (raw != null) {
            try {
                return UUID.fromString(raw);
            } catch (IllegalArgumentException ignored) {
            }
        }
        return Recorder.CONSOLE_UUID;
    }

    private static String targetName(Entry entry) {
        if (entry.getUuid() != null) {
            try {
                String name = Bukkit.getOfflinePlayer(UUID.fromString(entry.getUuid())).getName();
                if (name != null) {
                    return name;
                }
            } catch (IllegalArgumentException ignored) {
            }
            return entry.getUuid();
        }
        return entry.getIp() != null ? "ip " + entry.getIp() : "unknown";
    }
}
