package dev.nikhey.betteraudit.hook;

import dev.nikhey.betteraudit.Recorder;
import dev.nikhey.betteraudit.config.Settings;
import dev.nikhey.betteraudit.model.ActionType;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.actionlog.Action;
import net.luckperms.api.event.log.LogPublishEvent;
import org.bukkit.plugin.Plugin;

import java.util.function.Supplier;

/**
 * LuckPerms integration: records every published action-log entry —
 * rank changes, permission grants, meta edits — with the staff member
 * who made them, including changes made through the web editor.
 */
public final class LuckPermsHook {

    private LuckPermsHook() {
    }

    public static void register(Plugin plugin, Supplier<Settings> settings, Recorder recorder) {
        LuckPerms luckPerms = LuckPermsProvider.get();
        luckPerms.getEventBus().subscribe(plugin, LogPublishEvent.class, event -> {
            if (!settings.get().permissionChangesEnabled()) {
                return;
            }
            Action entry = event.getEntry();
            recorder.recordOffline(
                    entry.getSource().getUniqueId(),
                    entry.getSource().getName(),
                    ActionType.PERMISSION_CHANGE,
                    entry.getTarget().getName() + ": " + entry.getDescription());
        });
    }
}
