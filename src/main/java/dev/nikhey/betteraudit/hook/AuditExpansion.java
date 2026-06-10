package dev.nikhey.betteraudit.hook;

import dev.nikhey.betteraudit.listener.SessionListener;
import dev.nikhey.betteraudit.model.ActionType;
import dev.nikhey.betteraudit.storage.AuditStore;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * PlaceholderAPI expansion. Placeholders:
 *   %betteraudit_entries%      total audit entries for the player
 *   %betteraudit_punishments%  punishment commands issued
 *   %betteraudit_playtime%     recorded session time, formatted
 *
 * PlaceholderAPI calls are synchronous while the store is async, so values
 * come from a per-player cache that refreshes in the background.
 */
public final class AuditExpansion extends PlaceholderExpansion {

    private static final long CACHE_TTL_MS = 60_000;

    private record Cached(AuditStore.Stats stats, long at) {
    }

    private final Plugin plugin;
    private final AuditStore store;
    private final Map<UUID, Cached> cache = new ConcurrentHashMap<>();

    public AuditExpansion(Plugin plugin, AuditStore store) {
        this.plugin = plugin;
        this.store = store;
    }

    @Override
    public String getIdentifier() {
        return "betteraudit";
    }

    @Override
    public String getAuthor() {
        return "Nikhey";
    }

    @Override
    public String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        if (player == null || player.getName() == null) {
            return "";
        }
        UUID id = player.getUniqueId();
        Cached cached = cache.get(id);
        long now = System.currentTimeMillis();
        if (cached == null || now - cached.at() > CACHE_TTL_MS) {
            String name = player.getName();
            store.stats(name).thenAccept(stats -> cache.put(id, new Cached(stats, now)));
        }
        if (cached == null) {
            return "0";
        }
        AuditStore.Stats stats = cached.stats();
        return switch (params.toLowerCase()) {
            case "entries" -> String.valueOf(
                    stats.counts().values().stream().mapToInt(Integer::intValue).sum());
            case "punishments" -> String.valueOf(
                    stats.counts().getOrDefault(ActionType.PUNISHMENT, 0));
            case "playtime" -> SessionListener.formatDuration(stats.totalSessionSeconds());
            default -> null;
        };
    }
}
