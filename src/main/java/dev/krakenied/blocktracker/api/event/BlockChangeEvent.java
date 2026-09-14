package dev.krakenied.blocktracker.api.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Getter
@AllArgsConstructor
public final class BlockChangeEvent {

    private final @NotNull Block block;
    private final @NotNull BlockChangeType changeType;
    private final @Nullable Block destinationBlock;

    public BlockChangeEvent(final @NotNull Block block, final @NotNull BlockChangeType changeType) {
        this(block, changeType, null);
    }
}
