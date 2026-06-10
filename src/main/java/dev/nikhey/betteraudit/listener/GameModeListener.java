package dev.nikhey.betteraudit.listener;

import dev.nikhey.betteraudit.Recorder;
import dev.nikhey.betteraudit.config.Settings;
import dev.nikhey.betteraudit.model.ActionType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerGameModeChangeEvent;

import java.util.function.Supplier;

public final class GameModeListener implements Listener {

    private final Supplier<Settings> settings;
    private final Recorder recorder;

    public GameModeListener(Supplier<Settings> settings, Recorder recorder) {
        this.settings = settings;
        this.recorder = recorder;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onGameModeChange(PlayerGameModeChangeEvent event) {
        Settings s = settings.get();
        if (!s.gamemodeEnabled() || !s.isTracked(event.getPlayer())) {
            return;
        }
        recorder.record(event.getPlayer(), ActionType.GAMEMODE,
                event.getPlayer().getGameMode() + " -> " + event.getNewGameMode(), 0);
    }
}
