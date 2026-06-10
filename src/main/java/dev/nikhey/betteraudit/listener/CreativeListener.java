package dev.nikhey.betteraudit.listener;

import dev.nikhey.betteraudit.Recorder;
import dev.nikhey.betteraudit.config.Settings;
import dev.nikhey.betteraudit.model.ActionType;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public final class CreativeListener implements Listener {

    private static final long DEDUPE_WINDOW_MS = 2000;

    private record LastTake(Material material, long time) {
    }

    private final Supplier<Settings> settings;
    private final Recorder recorder;
    private final Map<UUID, LastTake> lastTakes = new ConcurrentHashMap<>();

    public CreativeListener(Supplier<Settings> settings, Recorder recorder) {
        this.settings = settings;
        this.recorder = recorder;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCreativeClick(InventoryCreativeEvent event) {
        Settings s = settings.get();
        if (!s.creativeEnabled() || !(event.getWhoClicked() instanceof Player player) || !s.isTracked(player)) {
            return;
        }
        ItemStack cursor = event.getCursor();
        if (cursor.getType().isAir()) {
            return;
        }
        long now = System.currentTimeMillis();
        LastTake last = lastTakes.get(player.getUniqueId());
        if (last != null && last.material() == cursor.getType() && now - last.time() < DEDUPE_WINDOW_MS) {
            return;
        }
        lastTakes.put(player.getUniqueId(), new LastTake(cursor.getType(), now));
        recorder.record(player, ActionType.CREATIVE_TAKE,
                cursor.getAmount() + "x " + cursor.getType().name().toLowerCase(), 0);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        lastTakes.remove(event.getPlayer().getUniqueId());
    }
}
