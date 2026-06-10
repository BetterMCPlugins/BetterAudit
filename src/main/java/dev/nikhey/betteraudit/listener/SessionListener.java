package dev.nikhey.betteraudit.listener;

import dev.nikhey.betteraudit.Recorder;
import dev.nikhey.betteraudit.config.Settings;
import dev.nikhey.betteraudit.model.ActionType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public final class SessionListener implements Listener {

    private final Supplier<Settings> settings;
    private final Recorder recorder;
    private final Map<UUID, Long> sessionStarts = new ConcurrentHashMap<>();

    public SessionListener(Supplier<Settings> settings, Recorder recorder) {
        this.settings = settings;
        this.recorder = recorder;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Settings s = settings.get();
        if (!s.sessionsEnabled() || !s.isTracked(event.getPlayer())) {
            return;
        }
        sessionStarts.put(event.getPlayer().getUniqueId(), System.currentTimeMillis());
        recorder.record(event.getPlayer(), ActionType.SESSION_START, "joined the server", 0);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        Long start = sessionStarts.remove(event.getPlayer().getUniqueId());
        Settings s = settings.get();
        if (start == null || !s.sessionsEnabled()) {
            return;
        }
        long seconds = (System.currentTimeMillis() - start) / 1000;
        recorder.record(event.getPlayer(), ActionType.SESSION_END,
                "session lasted " + formatDuration(seconds), seconds);
    }

    public static String formatDuration(long seconds) {
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        if (hours > 0) {
            return hours + "h " + minutes + "m";
        }
        if (minutes > 0) {
            return minutes + "m " + (seconds % 60) + "s";
        }
        return seconds + "s";
    }
}
