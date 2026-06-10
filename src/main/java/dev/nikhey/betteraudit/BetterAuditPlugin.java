package dev.nikhey.betteraudit;

import dev.nikhey.betteraudit.alert.DiscordAlerter;
import dev.nikhey.betteraudit.command.AuditCommand;
import dev.nikhey.betteraudit.config.Settings;
import dev.nikhey.betteraudit.listener.CommandListener;
import dev.nikhey.betteraudit.listener.CreativeListener;
import dev.nikhey.betteraudit.listener.GameModeListener;
import dev.nikhey.betteraudit.listener.SessionListener;
import dev.nikhey.betteraudit.menu.AuditMenu;
import dev.nikhey.betteraudit.storage.AuditStore;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class BetterAuditPlugin extends JavaPlugin {

    private volatile Settings settings;
    private AuditStore store;
    private ScheduledExecutorService maintenance;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        settings = Settings.load(getConfig());

        store = new AuditStore(new File(getDataFolder(), "audit.db"), getSLF4JLogger());
        try {
            store.init();
        } catch (Exception e) {
            getSLF4JLogger().error("Could not open the audit database, disabling", e);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        DiscordAlerter discord = new DiscordAlerter(this::settings, getSLF4JLogger());
        Recorder recorder = new Recorder(this::settings, store, discord);

        getServer().getPluginManager().registerEvents(new CommandListener(this::settings, recorder), this);
        getServer().getPluginManager().registerEvents(new GameModeListener(this::settings, recorder), this);
        getServer().getPluginManager().registerEvents(new CreativeListener(this::settings, recorder), this);
        getServer().getPluginManager().registerEvents(new SessionListener(this::settings, recorder), this);

        AuditMenu menu = new AuditMenu(this, store);
        getServer().getPluginManager().registerEvents(menu, this);

        registerHooks(recorder);

        PluginCommand command = getCommand("audit");
        if (command != null) {
            AuditCommand executor = new AuditCommand(this, store, menu);
            command.setExecutor(executor);
            command.setTabCompleter(executor);
        }

        // Plain JDK scheduler keeps maintenance off server threads and Folia-safe.
        maintenance = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "BetterAudit-Maintenance");
            t.setDaemon(true);
            return t;
        });
        maintenance.scheduleAtFixedRate(this::runRetention, 1, 60 * 12, TimeUnit.MINUTES);

        getSLF4JLogger().info("BetterAudit enabled - your staff black box is recording.");
    }

    /**
     * Optional integrations. Each hook class is only loaded (and its plugin
     * classes only touched) when the target plugin is actually installed.
     */
    private void registerHooks(Recorder recorder) {
        var pm = getServer().getPluginManager();
        if (pm.getPlugin("SuperVanish") != null || pm.getPlugin("PremiumVanish") != null) {
            pm.registerEvents(new dev.nikhey.betteraudit.hook.VanishHook(this::settings, recorder), this);
            getSLF4JLogger().info("Hooked into SuperVanish/PremiumVanish - tracking vanish time.");
        }
        if (pm.getPlugin("LuckPerms") != null) {
            dev.nikhey.betteraudit.hook.LuckPermsHook.register(this, this::settings, recorder);
            getSLF4JLogger().info("Hooked into LuckPerms - tracking permission changes.");
        }
        if (pm.getPlugin("PlaceholderAPI") != null) {
            new dev.nikhey.betteraudit.hook.AuditExpansion(this, store).register();
            getSLF4JLogger().info("Registered PlaceholderAPI expansion (%betteraudit_*%).");
        }
    }

    private void runRetention() {
        int days = settings.retentionDays();
        if (days <= 0) {
            return;
        }
        store.purgeOlderThan(days).whenComplete((deleted, error) -> {
            if (error != null) {
                getSLF4JLogger().warn("Retention purge failed", error);
            } else if (deleted != null && deleted > 0) {
                getSLF4JLogger().info("Retention: removed {} audit entries older than {} days", deleted, days);
            }
        });
    }

    public Settings settings() {
        return settings;
    }

    public void reloadSettings() {
        reloadConfig();
        settings = Settings.load(getConfig());
    }

    @Override
    public void onDisable() {
        if (maintenance != null) {
            maintenance.shutdownNow();
        }
        if (store != null) {
            store.close();
        }
    }
}
