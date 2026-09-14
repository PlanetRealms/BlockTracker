package dev.krakenied.blocktracker.event;

import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class BlockChangeEvent {

    private final Block block;
    private final BlockChangeType changeType;
    private final Block destinationBlock;

    public BlockChangeEvent(final @NotNull Block block, final @NotNull BlockChangeType changeType) {
        this(block, changeType, null);
    }

    public BlockChangeEvent(final @NotNull Block block, final @NotNull BlockChangeType changeType, final @Nullable Block destinationBlock) {
        this.block = block;
        this.changeType = changeType;
        this.destinationBlock = destinationBlock;
    }

    public @NotNull Block getBlock() {
        return this.block;
    }

    public @NotNull BlockChangeType getChangeType() {
        return this.changeType;
    }

    public @Nullable Block getDestinationBlock() {
        return this.destinationBlock;
    }
}
