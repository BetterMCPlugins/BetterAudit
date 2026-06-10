package dev.nikhey.betteraudit.config;

import dev.nikhey.betteraudit.model.ActionType;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public final class Settings {

    public static final String PERM_TRACKED = "betteraudit.tracked";
    public static final String PERM_EXEMPT = "betteraudit.exempt";
    public static final String PERM_NOTIFY = "betteraudit.notify";

    private final boolean trackAll;
    private final boolean modCommands;
    private final boolean modGamemode;
    private final boolean modCreative;
    private final boolean modOpChanges;
    private final boolean modSessions;
    private final boolean modPunishments;
    private final boolean modInspections;
    private final boolean modEconomy;
    private final boolean modVanish;
    private final boolean modPermissionChanges;
    private final Set<String> ignoredCommands;
    private final Set<String> punishmentCommands;
    private final Set<String> inspectionCommands;
    private final Set<String> economyCommands;
    private final Set<ActionType> alerts;
    private final boolean notifyIngame;
    private final boolean discordEnabled;
    private final String webhookUrl;
    private final int retentionDays;

    private Settings(FileConfiguration c) {
        this.trackAll = c.getString("tracking.mode", "STAFF").equalsIgnoreCase("ALL");
        this.modCommands = c.getBoolean("modules.commands", true);
        this.modGamemode = c.getBoolean("modules.gamemode", true);
        this.modCreative = c.getBoolean("modules.creative-items", true);
        this.modOpChanges = c.getBoolean("modules.op-changes", true);
        this.modSessions = c.getBoolean("modules.sessions", true);
        this.modPunishments = c.getBoolean("modules.punishments", true);
        this.modInspections = c.getBoolean("modules.inspections", true);
        this.modEconomy = c.getBoolean("modules.economy", true);
        this.modVanish = c.getBoolean("modules.vanish", true);
        this.modPermissionChanges = c.getBoolean("modules.permission-changes", true);
        this.ignoredCommands = lowered(c.getStringList("ignored-commands"));
        this.punishmentCommands = lowered(c.getStringList("punishment-commands"));
        this.inspectionCommands = lowered(c.getStringList("inspection-commands"));
        this.economyCommands = lowered(c.getStringList("economy-commands"));
        this.alerts = EnumSet.noneOf(ActionType.class);
        for (String name : c.getStringList("alerts")) {
            try {
                this.alerts.add(ActionType.valueOf(name.toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException ignored) {
            }
        }
        this.notifyIngame = c.getBoolean("notify.ingame", true);
        this.discordEnabled = c.getBoolean("discord.enabled", false);
        this.webhookUrl = c.getString("discord.webhook-url", "");
        this.retentionDays = c.getInt("retention-days", 90);
    }

    public static Settings load(FileConfiguration config) {
        return new Settings(config);
    }

    private static Set<String> lowered(Iterable<String> values) {
        Set<String> set = new HashSet<>();
        for (String v : values) {
            set.add(v.toLowerCase(Locale.ROOT));
        }
        return set;
    }

    public boolean isTracked(Player player) {
        if (player.hasPermission(PERM_EXEMPT)) {
            return false;
        }
        return trackAll || player.isOp() || player.hasPermission(PERM_TRACKED);
    }

    public boolean commandsEnabled() {
        return modCommands;
    }

    public boolean gamemodeEnabled() {
        return modGamemode;
    }

    public boolean creativeEnabled() {
        return modCreative;
    }

    public boolean opChangesEnabled() {
        return modOpChanges;
    }

    public boolean sessionsEnabled() {
        return modSessions;
    }

    public boolean punishmentsEnabled() {
        return modPunishments;
    }

    public boolean inspectionsEnabled() {
        return modInspections;
    }

    public boolean economyEnabled() {
        return modEconomy;
    }

    public boolean vanishEnabled() {
        return modVanish;
    }

    public boolean permissionChangesEnabled() {
        return modPermissionChanges;
    }

    public boolean isIgnoredCommand(String root) {
        return ignoredCommands.contains(root);
    }

    public boolean isPunishmentCommand(String root) {
        return punishmentCommands.contains(root);
    }

    public boolean isInspectionCommand(String root) {
        return inspectionCommands.contains(root);
    }

    public boolean isEconomyCommand(String root) {
        return economyCommands.contains(root);
    }

    public boolean alertsFor(ActionType type) {
        return alerts.contains(type);
    }

    public boolean notifyIngame() {
        return notifyIngame;
    }

    public boolean discordEnabled() {
        return discordEnabled && webhookUrl != null && !webhookUrl.isBlank();
    }

    public String webhookUrl() {
        return webhookUrl;
    }

    public int retentionDays() {
        return retentionDays;
    }
}
