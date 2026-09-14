package dev.krakenied.blocktracker.event;

import lombok.Getter;
import lombok.Setter;
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
    @Setter
    private boolean cancelled;

    public BlockChangeEvent(@NotNull Block block, @NotNull BlockChangeType changeType) {
        this(block, changeType, null);
    }

    public BlockChangeEvent(@NotNull Block block, @NotNull BlockChangeType changeType, @Nullable Block destinationBlock) {
        super(block);
        this.changeType = changeType;
        this.destinationBlock = destinationBlock;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }
}
