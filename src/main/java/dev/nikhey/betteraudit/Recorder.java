package dev.nikhey.betteraudit;

import dev.nikhey.betteraudit.alert.AlertSink;
import dev.nikhey.betteraudit.config.Settings;
import dev.nikhey.betteraudit.model.ActionType;
import dev.nikhey.betteraudit.storage.AuditStore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

/**
 * Central sink for audit events: persists the entry, then fans out
 * in-game and Discord alerts for the configured action types.
 */
public final class Recorder {

    public static final UUID CONSOLE_UUID = new UUID(0, 0);
    public static final String CONSOLE_NAME = "CONSOLE";

    private final Supplier<Settings> settings;
    private final AuditStore store;
    private final List<AlertSink> sinks = new CopyOnWriteArrayList<>();

    public Recorder(Supplier<Settings> settings, AuditStore store, AlertSink... alertSinks) {
        this.settings = settings;
        this.store = store;
        sinks.addAll(List.of(alertSinks));
    }

    public void addSink(AlertSink sink) {
        sinks.add(sink);
    }

    public void record(Player actor, ActionType type, String detail, long durationSeconds) {
        Location loc = actor.getLocation();
        record(actor.getUniqueId(), actor.getName(), type, detail,
                loc.getWorld() == null ? null : loc.getWorld().getName(),
                loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), durationSeconds);
    }

    public void recordConsole(ActionType type, String detail) {
        record(CONSOLE_UUID, CONSOLE_NAME, type, detail, null, 0, 0, 0, 0);
    }

    /** For hooks whose actor may be offline (e.g. LuckPerms changes from a web editor). */
    public void recordOffline(UUID uuid, String name, ActionType type, String detail) {
        record(uuid, name, type, detail, null, 0, 0, 0, 0);
    }

    private void record(UUID uuid, String name, ActionType type, String detail,
                        String world, int x, int y, int z, long durationSeconds) {
        store.insert(System.currentTimeMillis(), uuid, name, type, detail, world, x, y, z, durationSeconds);

        Settings s = settings.get();
        if (!s.alertsFor(type)) {
            return;
        }
        if (s.notifyIngame()) {
            Component alert = Component.text()
                    .append(Component.text("[Audit] ", NamedTextColor.GOLD))
                    .append(Component.text(name, NamedTextColor.YELLOW))
                    .append(Component.text(" · " + type.display() + " · ", NamedTextColor.GRAY))
                    .append(Component.text(detail, NamedTextColor.WHITE))
                    .build();
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (online.hasPermission(Settings.PERM_NOTIFY) && !online.getUniqueId().equals(uuid)) {
                    online.sendMessage(alert);
                }
            }
        }
        for (AlertSink sink : sinks) {
            sink.send(type, name, detail);
        }
    }
}
