package dev.nikhey.betteraudit.hook;

import dev.nikhey.betteraudit.Recorder;
import dev.nikhey.betteraudit.config.Settings;
import dev.nikhey.betteraudit.listener.SessionListener;
import dev.nikhey.betteraudit.model.ActionType;
import net.ess3.api.events.UserBalanceUpdateEvent;
import net.ess3.api.events.VanishStatusChangeEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * EssentialsX integration:
 *  - balance updates on tracked players, with old/new amount and cause
 *  - Essentials' own /vanish, which the SuperVanish hook can't see
 */
public final class EssentialsHook implements Listener {

    private final Supplier<Settings> settings;
    private final Recorder recorder;
    private final Map<UUID, Long> vanishStarts = new ConcurrentHashMap<>();

    public EssentialsHook(Supplier<Settings> settings, Recorder recorder) {
        this.settings = settings;
        this.recorder = recorder;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBalanceUpdate(UserBalanceUpdateEvent event) {
        Settings s = settings.get();
        Player player = event.getPlayer();
        if (!s.economyEnabled() || !s.isTracked(player)) {
            return;
        }
        recorder.record(player, ActionType.ECONOMY,
                "balance " + money(event.getOldBalance()) + " -> " + money(event.getNewBalance())
                        + " (" + event.getCause().name().toLowerCase() + ")", 0);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onVanishChange(VanishStatusChangeEvent event) {
        Settings s = settings.get();
        Player player = event.getAffected().getBase();
        if (player == null || !s.vanishEnabled() || !s.isTracked(player)) {
            return;
        }
        if (event.getValue()) {
            vanishStarts.put(player.getUniqueId(), System.currentTimeMillis());
            recorder.record(player, ActionType.VANISH, "vanished (Essentials)", 0);
        } else {
            Long start = vanishStarts.remove(player.getUniqueId());
            long seconds = start == null ? 0 : (System.currentTimeMillis() - start) / 1000;
            recorder.record(player, ActionType.VANISH,
                    "unvanished after " + SessionListener.formatDuration(seconds), seconds);
        }
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

    private static String money(BigDecimal value) {
        return value.setScale(2, java.math.RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }
}
