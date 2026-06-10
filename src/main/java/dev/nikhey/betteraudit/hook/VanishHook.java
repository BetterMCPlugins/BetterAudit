package dev.nikhey.betteraudit.hook;

import de.myzelyam.api.vanish.PlayerHideEvent;
import de.myzelyam.api.vanish.PlayerShowEvent;
import dev.nikhey.betteraudit.Recorder;
import dev.nikhey.betteraudit.config.Settings;
import dev.nikhey.betteraudit.listener.SessionListener;
import dev.nikhey.betteraudit.model.ActionType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * SuperVanish / PremiumVanish integration (both expose the same API).
 * Records vanish toggles and the time spent vanished.
 */
public final class VanishHook implements Listener {

    private final Supplier<Settings> settings;
    private final Recorder recorder;
    private final Map<UUID, Long> vanishStarts = new ConcurrentHashMap<>();

    public VanishHook(Supplier<Settings> settings, Recorder recorder) {
        this.settings = settings;
        this.recorder = recorder;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHide(PlayerHideEvent event) {
        Settings s = settings.get();
        Player player = event.getPlayer();
        if (!s.vanishEnabled() || !s.isTracked(player)) {
            return;
        }
        vanishStarts.put(player.getUniqueId(), System.currentTimeMillis());
        recorder.record(player, ActionType.VANISH, "vanished", 0);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onShow(PlayerShowEvent event) {
        Long start = vanishStarts.remove(event.getPlayer().getUniqueId());
        Settings s = settings.get();
        if (start == null || !s.vanishEnabled()) {
            return;
        }
        long seconds = (System.currentTimeMillis() - start) / 1000;
        recorder.record(event.getPlayer(), ActionType.VANISH,
                "unvanished after " + SessionListener.formatDuration(seconds), seconds);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Long start = vanishStarts.remove(event.getPlayer().getUniqueId());
        if (start == null || !settings.get().vanishEnabled()) {
            return;
        }
        long seconds = (System.currentTimeMillis() - start) / 1000;
        recorder.record(event.getPlayer(), ActionType.VANISH,
                "logged out while vanished (" + SessionListener.formatDuration(seconds) + ")", seconds);
    }
}
