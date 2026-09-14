package dev.krakenied.blocktracker.event;

import lombok.Getter;
import org.bukkit.block.Block;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.block.BlockEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Getter
public final class BlockChangeEvent extends BlockEvent implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();

    private final @NotNull BlockChangeType changeType;
    private final @Nullable Block destinationBlock;
    private final boolean cancellationSupported;
    private boolean cancelled;

    public BlockChangeEvent(final @NotNull Block block, final @NotNull BlockChangeType changeType) {
        this(block, changeType, null, true);
    }

    public BlockChangeEvent(final @NotNull Block block, final @NotNull BlockChangeType changeType, final boolean cancellationSupported) {
        this(block, changeType, null, cancellationSupported);
    }

    public BlockChangeEvent(final @NotNull Block block, final @NotNull BlockChangeType changeType, final @Nullable Block destinationBlock) {
        this(block, changeType, destinationBlock, true);
    }

    public BlockChangeEvent(
            final @NotNull Block block,
            final @NotNull BlockChangeType changeType,
            final @Nullable Block destinationBlock,
            final boolean cancellationSupported
    ) {
        super(block);
        this.changeType = changeType;
        this.destinationBlock = destinationBlock;
        this.cancellationSupported = cancellationSupported;
    }

    @Override
    public void setCancelled(final boolean cancelled) {
        if (cancelled && !this.cancellationSupported) {
            return;
        }

        this.cancelled = cancelled;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }
}
