package dev.krakenied.blocktracker.bukkit;

import dev.krakenied.blocktracker.api.data.ChunkMap;
import dev.krakenied.blocktracker.api.object.TrackedChunk;
import org.bukkit.Chunk;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

public final class BukkitTrackedWorld {

    private final ChunkMap chunkMap = new ChunkMap();

    public void initializeChunk(final @NotNull Chunk chunk) {
        final PersistentDataContainer pdc = chunk.getPersistentDataContainer();
        final int[] data = pdc.get(BukkitConstants.DATA_KEY, PersistentDataType.INTEGER_ARRAY);
        this.chunkMap.put(chunk.getX(), chunk.getZ(), new TrackedChunk(data));
    }

    public void terminateChunk(final @NotNull Chunk chunk) {
        final TrackedChunk trackedChunk = this.chunkMap.get(chunk.getX(), chunk.getZ());
        if (trackedChunk == null) {
            return;
        }

        final PersistentDataContainer pdc = chunk.getPersistentDataContainer();
        if (trackedChunk.isEmpty()) {
            pdc.remove(BukkitConstants.DATA_KEY);
        } else {
            pdc.set(BukkitConstants.DATA_KEY, PersistentDataType.INTEGER_ARRAY, trackedChunk.toIntArray());
        }
    }

    public boolean isTrackedByBlock(final @NotNull Block block) {
        final TrackedChunk chunk = this.getTrackedChunkByBlock(block);
        return chunk != null && chunk.isTracked(block.getX(), block.getY(), block.getZ());
    }

    public boolean trackByBlock(final @NotNull Block block) {
        final TrackedChunk chunk = this.getTrackedChunkByBlock(block);
        return chunk != null && chunk.track(block.getX(), block.getY(), block.getZ());
    }

    public boolean trackByState(final @NotNull BlockState state) {
        final TrackedChunk chunk = this.getTrackedChunkByState(state);
        return chunk != null && chunk.track(state.getX(), state.getY(), state.getZ());
    }

    public boolean untrackByBlock(final @NotNull Block block) {
        final TrackedChunk chunk = this.getTrackedChunkByBlock(block);
        return chunk != null && chunk.untrack(block.getX(), block.getY(), block.getZ());
    }

    public boolean untrackByState(final @NotNull BlockState state) {
        final TrackedChunk chunk = this.getTrackedChunkByState(state);
        return chunk != null && chunk.untrack(state.getX(), state.getY(), state.getZ());
    }

    private TrackedChunk getTrackedChunkByBlock(final @NotNull Block block) {
        return this.chunkMap.getByBlock(block.getX(), block.getZ());
    }

    private TrackedChunk getTrackedChunkByState(final @NotNull BlockState state) {
        return this.chunkMap.getByBlock(state.getX(), state.getZ());
    }
}
