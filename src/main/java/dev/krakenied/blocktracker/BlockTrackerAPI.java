package dev.krakenied.blocktracker;

import dev.krakenied.blocktracker.event.BlockChangeEvent;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

@SuppressWarnings("unused")
public final class BlockTrackerAPI {

    @Getter
    private static BlockTrackerPlugin instance;
    private static final List<Consumer<BlockChangeEvent>> callbacks = new CopyOnWriteArrayList<>();

    static void setInstance(final @NotNull BlockTrackerPlugin instance) {
        BlockTrackerAPI.instance = instance;
    }

    public static boolean isTracked(final @NotNull Block block) {
        return BlockTrackerAPI.instance.getTrackingManager().isTrackedByBlock(block);
    }

    public static void registerBlockChangeCallback(final @NotNull Consumer<BlockChangeEvent> callback) {
        callbacks.add(callback);
    }

    public static void unregisterBlockChangeCallback(final @NotNull Consumer<BlockChangeEvent> callback) {
        callbacks.remove(callback);
    }

    /**
     * Fires the Bukkit event and then invokes the legacy callback API.
     * Both Bukkit listeners and callbacks may cancel the change.
     *
     * @return true when the change is allowed, false when it was cancelled
     */
    public static boolean callBlockChange(final @NotNull BlockChangeEvent event) {
        Bukkit.getPluginManager().callEvent(event);
        notifyBlockChange(event);
        return !event.isCancelled();
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
