package dev.nikhey.betteraudit.command;

import dev.nikhey.betteraudit.BetterAuditPlugin;
import dev.nikhey.betteraudit.listener.SessionListener;
import dev.nikhey.betteraudit.menu.AuditMenu;
import dev.nikhey.betteraudit.model.ActionType;
import dev.nikhey.betteraudit.model.AuditEntry;
import dev.nikhey.betteraudit.storage.AuditStore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class AuditCommand implements TabExecutor {

    private static final int PAGE_SIZE = 8;
    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("MMM d HH:mm").withZone(ZoneId.systemDefault());
    private static final List<String> SUBCOMMANDS =
            List.of("menu", "recent", "player", "type", "stats", "purge", "reload");

    private final BetterAuditPlugin plugin;
    private final AuditStore store;
    private final AuditMenu menu;

    public AuditCommand(BetterAuditPlugin plugin, AuditStore store, AuditMenu menu) {
        this.plugin = plugin;
        this.store = store;
        this.menu = menu;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "menu" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(Component.text("The menu is only available in-game.", NamedTextColor.RED));
                    return true;
                }
                menu.open(player);
            }
            case "recent" -> {
                int page = parsePage(args, 1);
                store.recent(PAGE_SIZE, (page - 1) * PAGE_SIZE)
                        .whenComplete((entries, error) ->
                                sendPage(sender, "Recent activity", "/audit recent", entries, error, page));
            }
            case "player" -> {
                if (args.length < 2) {
                    sender.sendMessage(Component.text("Usage: /audit player <name> [page]", NamedTextColor.RED));
                    return true;
                }
                String name = args[1];
                int page = parsePage(args, 2);
                store.byActor(name, PAGE_SIZE, (page - 1) * PAGE_SIZE)
                        .whenComplete((entries, error) ->
                                sendPage(sender, "Activity of " + name, "/audit player " + name, entries, error, page));
            }
            case "type" -> {
                if (args.length < 2) {
                    sender.sendMessage(Component.text("Usage: /audit type <type> [page]", NamedTextColor.RED));
                    return true;
                }
                ActionType type;
                try {
                    type = ActionType.valueOf(args[1].toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException e) {
                    sender.sendMessage(Component.text("Unknown type. Use the tab completions.", NamedTextColor.RED));
                    return true;
                }
                int page = parsePage(args, 2);
                store.byType(type, PAGE_SIZE, (page - 1) * PAGE_SIZE)
                        .whenComplete((entries, error) ->
                                sendPage(sender, type.display() + " entries", "/audit type " + args[1], entries, error, page));
            }
            case "stats" -> {
                if (args.length < 2) {
                    sender.sendMessage(Component.text("Usage: /audit stats <name>", NamedTextColor.RED));
                    return true;
                }
                String name = args[1];
                store.stats(name).whenComplete((stats, error) -> {
                    if (error != null || stats == null) {
                        sender.sendMessage(Component.text("Failed to load stats.", NamedTextColor.RED));
                        return;
                    }
                    sendStats(sender, name, stats);
                });
            }
            case "purge" -> {
                if (!sender.hasPermission("betteraudit.admin")) {
                    sender.sendMessage(Component.text("No permission.", NamedTextColor.RED));
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(Component.text("Usage: /audit purge <days>", NamedTextColor.RED));
                    return true;
                }
                int days;
                try {
                    days = Integer.parseInt(args[1]);
                } catch (NumberFormatException e) {
                    sender.sendMessage(Component.text("Days must be a number.", NamedTextColor.RED));
                    return true;
                }
                if (days < 1) {
                    sender.sendMessage(Component.text("Days must be at least 1.", NamedTextColor.RED));
                    return true;
                }
                store.purgeOlderThan(days).whenComplete((deleted, error) -> {
                    if (error != null || deleted == null) {
                        sender.sendMessage(Component.text("Purge failed.", NamedTextColor.RED));
                        return;
                    }
                    sender.sendMessage(Component.text("Deleted " + deleted + " entries older than " + days + " days.",
                            NamedTextColor.GREEN));
                });
            }
            case "reload" -> {
                if (!sender.hasPermission("betteraudit.admin")) {
                    sender.sendMessage(Component.text("No permission.", NamedTextColor.RED));
                    return true;
                }
                plugin.reloadSettings();
                sender.sendMessage(Component.text("BetterAudit configuration reloaded.", NamedTextColor.GREEN));
            }
            default -> sendHelp(sender);
        }
        return true;
    }

    private static int parsePage(String[] args, int index) {
        if (args.length > index) {
            try {
                return Math.max(1, Integer.parseInt(args[index]));
            } catch (NumberFormatException ignored) {
            }
        }
        return 1;
    }

    private void sendPage(CommandSender sender, String title, String baseCommand,
                          List<AuditEntry> entries, Throwable error, int page) {
        if (error != null || entries == null) {
            sender.sendMessage(Component.text("Failed to query the audit log.", NamedTextColor.RED));
            return;
        }
        if (entries.isEmpty()) {
            sender.sendMessage(Component.text(page == 1
                    ? "No audit entries found."
                    : "No more entries.", NamedTextColor.GRAY));
            return;
        }
        sender.sendMessage(Component.text()
                .append(Component.text("--- ", NamedTextColor.DARK_GRAY))
                .append(Component.text(title + " (page " + page + ")", NamedTextColor.GOLD))
                .append(Component.text(" ---", NamedTextColor.DARK_GRAY))
                .build());
        for (AuditEntry entry : entries) {
            sender.sendMessage(formatEntry(entry));
        }
        if (entries.size() == PAGE_SIZE) {
            sender.sendMessage(Component.text("[Next page »]", NamedTextColor.AQUA)
                    .clickEvent(ClickEvent.runCommand(baseCommand + " " + (page + 1)))
                    .hoverEvent(HoverEvent.showText(Component.text("Show page " + (page + 1)))));
        }
    }

    private Component formatEntry(AuditEntry entry) {
        Component line = Component.text()
                .append(Component.text(TIME_FORMAT.format(Instant.ofEpochMilli(entry.time())) + " ",
                        NamedTextColor.DARK_GRAY))
                .append(Component.text(entry.actorName(), NamedTextColor.YELLOW))
                .append(Component.text(" · " + entry.type().display() + " · ", NamedTextColor.GRAY))
                .append(Component.text(entry.detail(), NamedTextColor.WHITE))
                .build();
        if (entry.world() != null) {
            line = line.hoverEvent(HoverEvent.showText(Component.text(
                    entry.world() + " " + entry.x() + ", " + entry.y() + ", " + entry.z())));
        }
        return line;
    }

    private void sendStats(CommandSender sender, String name, AuditStore.Stats stats) {
        if (stats.counts().isEmpty()) {
            sender.sendMessage(Component.text("No data recorded for " + name + ".", NamedTextColor.GRAY));
            return;
        }
        sender.sendMessage(Component.text()
                .append(Component.text("--- ", NamedTextColor.DARK_GRAY))
                .append(Component.text("Stats for " + name, NamedTextColor.GOLD))
                .append(Component.text(" ---", NamedTextColor.DARK_GRAY))
                .build());
        for (ActionType type : ActionType.values()) {
            Integer count = stats.counts().get(type);
            if (count == null) {
                continue;
            }
            sender.sendMessage(Component.text()
                    .append(Component.text("  " + type.display() + ": ", NamedTextColor.GRAY))
                    .append(Component.text(String.valueOf(count), NamedTextColor.WHITE))
                    .build());
        }
        sender.sendMessage(Component.text()
                .append(Component.text("  Recorded playtime: ", NamedTextColor.GRAY))
                .append(Component.text(SessionListener.formatDuration(stats.totalSessionSeconds()),
                        NamedTextColor.WHITE))
                .build());
        if (stats.lastSeen() > 0) {
            sender.sendMessage(Component.text()
                    .append(Component.text("  First / last entry: ", NamedTextColor.GRAY))
                    .append(Component.text(TIME_FORMAT.format(Instant.ofEpochMilli(stats.firstSeen()))
                            + " / " + TIME_FORMAT.format(Instant.ofEpochMilli(stats.lastSeen())),
                            NamedTextColor.WHITE))
                    .build());
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Component.text("BetterAudit — staff activity log", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("  /audit menu", NamedTextColor.GRAY)
                .append(Component.text(" — staff overview (click a head)", NamedTextColor.DARK_GRAY)));
        sender.sendMessage(Component.text("  /audit recent [page]", NamedTextColor.GRAY)
                .append(Component.text(" — latest entries", NamedTextColor.DARK_GRAY)));
        sender.sendMessage(Component.text("  /audit player <name> [page]", NamedTextColor.GRAY)
                .append(Component.text(" — one player's timeline", NamedTextColor.DARK_GRAY)));
        sender.sendMessage(Component.text("  /audit type <type> [page]", NamedTextColor.GRAY)
                .append(Component.text(" — filter by action type", NamedTextColor.DARK_GRAY)));
        sender.sendMessage(Component.text("  /audit stats <name>", NamedTextColor.GRAY)
                .append(Component.text(" — per-staff statistics", NamedTextColor.DARK_GRAY)));
        sender.sendMessage(Component.text("  /audit purge <days>", NamedTextColor.GRAY)
                .append(Component.text(" — delete old entries", NamedTextColor.DARK_GRAY)));
        sender.sendMessage(Component.text("  /audit reload", NamedTextColor.GRAY)
                .append(Component.text(" — reload the config", NamedTextColor.DARK_GRAY)));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            return filter(SUBCOMMANDS, args[0]);
        }
        if (args.length == 2) {
            switch (args[0].toLowerCase(Locale.ROOT)) {
                case "player", "stats" -> {
                    List<String> names = new ArrayList<>();
                    for (Player online : Bukkit.getOnlinePlayers()) {
                        names.add(online.getName());
                    }
                    return filter(names, args[1]);
                }
                case "type" -> {
                    List<String> types = new ArrayList<>();
                    for (ActionType type : ActionType.values()) {
                        types.add(type.name().toLowerCase(Locale.ROOT));
                    }
                    return filter(types, args[1]);
                }
                default -> {
                    return List.of();
                }
            }
        }
        return List.of();
    }

    private static List<String> filter(List<String> options, String prefix) {
        String lower = prefix.toLowerCase(Locale.ROOT);
        List<String> result = new ArrayList<>();
        for (String option : options) {
            if (option.toLowerCase(Locale.ROOT).startsWith(lower)) {
                result.add(option);
            }
        }
        return result;
    }
}
