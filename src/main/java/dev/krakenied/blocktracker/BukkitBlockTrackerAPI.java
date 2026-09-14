package dev.krakenied.blocktracker;

import dev.krakenied.blocktracker.event.BlockChangeEvent;
import lombok.Getter;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

@SuppressWarnings("unused")
public final class BukkitBlockTrackerAPI {

    @Getter
    private static BukkitBlockTrackerPlugin instance;
    private static final List<Consumer<BlockChangeEvent>> callbacks = new CopyOnWriteArrayList<>();

    static void setInstance(final @NotNull BukkitBlockTrackerPlugin instance) {
        BukkitBlockTrackerAPI.instance = instance;
    }

    public static boolean isTracked(final @NotNull Block block) {
        return BukkitBlockTrackerAPI.instance.getTrackingManager().isTrackedByBlock(block);
    }

    public static void registerBlockChangeCallback(final @NotNull Consumer<BlockChangeEvent> callback) {
        callbacks.add(callback);
    }

    public static void unregisterBlockChangeCallback(final @NotNull Consumer<BlockChangeEvent> callback) {
        callbacks.remove(callback);
    }

    public static void notifyBlockChange(final @NotNull BlockChangeEvent event) {
        for (final Consumer<BlockChangeEvent> callback : callbacks) {
            try {
                callback.accept(event);
            } catch (final Throwable t) {
                if (instance != null) {
                    instance.getLogger().log(
                            java.util.logging.Level.SEVERE,
                            "Error executing BlockChangeCallback",
                            t
                    );
                }
            }
        }
    }
}
