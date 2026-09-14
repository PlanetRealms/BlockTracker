package dev.krakenied.blocktracker.bukkit;

import lombok.Getter;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public final class BukkitBlockTrackerPlugin extends JavaPlugin {

    @Getter
    private final BukkitTrackingManager trackingManager = new BukkitTrackingManager();

    @Override
    public void onEnable() {
        BukkitBlockTrackerAPI.setInstance(this);

        this.trackingManager.initializeLoadedWorlds();
        this.registerListeners();
    }

    @Override
    public void onDisable() {
        this.unregisterListeners();
        this.trackingManager.terminateLoadedWorlds();
    }

    private void registerListeners() {
        this.getServer().getPluginManager().registerEvents(new BukkitListener(this), this);
    }

    private void unregisterListeners() {
        HandlerList.unregisterAll(this);
    }

    @SuppressWarnings("unused")
    public static boolean isTracked(final @NotNull org.bukkit.block.Block block) {
        return BukkitBlockTrackerAPI.isTracked(block);
    }
}
