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

    /**
     * Called before a tracked block change is committed.
     * These callbacks may cancel the change when cancellation is supported.
     */
    private static final List<Consumer<BlockChangeEvent>> blockChangeCallbacks = new CopyOnWriteArrayList<>();

    /**
     * Called only after a tracked block change has actually been committed.
     * These callbacks are notification-only; cancelling the event here has no effect.
     */
    private static final List<Consumer<BlockChangeEvent>> blockChangedCallbacks = new CopyOnWriteArrayList<>();

    private BlockTrackerAPI() {
    }

    static void setInstance(final @NotNull BlockTrackerPlugin instance) {
        BlockTrackerAPI.instance = instance;
    }

    public static boolean isTracked(final @NotNull Block block) {
        return BlockTrackerAPI.instance.getTrackingManager().isTrackedByBlock(block);
    }

    /**
     * Registers a callback that runs before a tracked block change is committed.
     * The callback may cancel the supplied event when
     * {@link BlockChangeEvent#isCancellationSupported()} returns true.
     */
    public static void registerBlockChangeCallback(final @NotNull Consumer<BlockChangeEvent> callback) {
        blockChangeCallbacks.add(callback);
    }

    public static void unregisterBlockChangeCallback(final @NotNull Consumer<BlockChangeEvent> callback) {
        blockChangeCallbacks.remove(callback);
    }

    /**
     * Registers a notification callback that runs after the tracker has committed
     * the change. This is the callback to use for synchronizing external state.
     */
    public static void registerBlockChangedCallback(final @NotNull Consumer<BlockChangeEvent> callback) {
        blockChangedCallbacks.add(callback);
    }

    public static void unregisterBlockChangedCallback(final @NotNull Consumer<BlockChangeEvent> callback) {
        blockChangedCallbacks.remove(callback);
    }

    /**
     * Fires the Bukkit event and then invokes the pre-change callback API.
     * Both Bukkit listeners and pre-change callbacks may cancel the change.
     *
     * @return true when the change is allowed, false when it was cancelled
     */
    public static boolean callBlockChange(final @NotNull BlockChangeEvent event) {
        Bukkit.getPluginManager().callEvent(event);
        notifyBlockChange(event);
        return !event.isCancelled();
    }

    /**
     * Invokes pre-change callbacks.
     */
    public static void notifyBlockChange(final @NotNull BlockChangeEvent event) {
        notifyCallbacks(blockChangeCallbacks, event, "BlockChangeCallback");
    }

    /**
     * Invokes post-change callbacks after the tracker has committed the change.
     */
    public static void notifyBlockChanged(final @NotNull BlockChangeEvent event) {
        notifyCallbacks(blockChangedCallbacks, event, "BlockChangedCallback");
    }

    private static void notifyCallbacks(
            final @NotNull List<Consumer<BlockChangeEvent>> callbacks,
            final @NotNull BlockChangeEvent event,
            final @NotNull String callbackName
    ) {
        for (final Consumer<BlockChangeEvent> callback : callbacks) {
            try {
                callback.accept(event);
            } catch (final Throwable t) {
                if (instance != null) {
                    instance.getLogger().log(
                            java.util.logging.Level.SEVERE,
                            "Error executing " + callbackName,
                            t
                    );
                }
            }
        }
    }
}
