package dev.nikhey.betteraudit.listener;

import dev.nikhey.betteraudit.Recorder;
import dev.nikhey.betteraudit.config.Settings;
import dev.nikhey.betteraudit.model.ActionType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.ServerCommandEvent;

import java.util.Locale;
import java.util.function.Supplier;

public final class CommandListener implements Listener {

    private final Supplier<Settings> settings;
    private final Recorder recorder;

    public CommandListener(Supplier<Settings> settings, Recorder recorder) {
        this.settings = settings;
        this.recorder = recorder;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        Settings s = settings.get();
        if (!s.isTracked(event.getPlayer())) {
            return;
        }
        String message = event.getMessage();
        String root = rootCommand(message);
        if (root.isEmpty() || s.isIgnoredCommand(root)) {
            return;
        }
        ActionType type = classify(s, root);
        if (!moduleEnabled(s, type)) {
            return;
        }
        recorder.record(event.getPlayer(), type, message, 0);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onConsoleCommand(ServerCommandEvent event) {
        Settings s = settings.get();
        String root = rootCommand(event.getCommand());
        if (root.isEmpty() || s.isIgnoredCommand(root)) {
            return;
        }
        ActionType type = classify(s, root);
        if (type == ActionType.COMMAND) {
            return;
        }
        if (!moduleEnabled(s, type)) {
            return;
        }
        recorder.recordConsole(type, event.getCommand());
    }

    private static ActionType classify(Settings s, String root) {
        if (root.equals("op") || root.equals("deop")) {
            return ActionType.OP_CHANGE;
        }
        if (s.isPunishmentCommand(root)) {
            return ActionType.PUNISHMENT;
        }
        if (s.isInspectionCommand(root)) {
            return ActionType.INSPECTION;
        }
        if (s.isEconomyCommand(root)) {
            return ActionType.ECONOMY;
        }
        return ActionType.COMMAND;
    }

    private static boolean moduleEnabled(Settings s, ActionType type) {
        return switch (type) {
            case OP_CHANGE -> s.opChangesEnabled();
            case PUNISHMENT -> s.punishmentsEnabled();
            case INSPECTION -> s.inspectionsEnabled();
            case ECONOMY -> s.economyEnabled();
            default -> s.commandsEnabled();
        };
    }

    /**
     * Extracts the bare command word: "/minecraft:ban Steve grief" -> "ban".
     */
    static String rootCommand(String message) {
        String trimmed = message.trim();
        if (trimmed.startsWith("/")) {
            trimmed = trimmed.substring(1);
        }
        int space = trimmed.indexOf(' ');
        String root = space == -1 ? trimmed : trimmed.substring(0, space);
        int colon = root.indexOf(':');
        if (colon != -1) {
            root = root.substring(colon + 1);
        }
        return root.toLowerCase(Locale.ROOT);
    }
}
