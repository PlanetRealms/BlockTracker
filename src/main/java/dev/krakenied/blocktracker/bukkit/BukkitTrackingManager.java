package dev.krakenied.blocktracker.bukkit;

import dev.krakenied.blocktracker.api.data.WorldMap;
import dev.krakenied.blocktracker.api.event.BlockChangeEvent;
import dev.krakenied.blocktracker.api.event.BlockChangeType;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

@SuppressWarnings("UnusedReturnValue")
public final class BukkitTrackingManager {

    private final WorldMap worldMap = new WorldMap();

    public void initializeWorld(final @NotNull World world) {
        final BukkitTrackedWorld trackedWorld = new BukkitTrackedWorld();

        for (final Chunk chunk : world.getLoadedChunks()) {
            trackedWorld.initializeChunk(chunk);
        }

        this.worldMap.put(world.getUID(), trackedWorld);
    }

    public void terminateWorld(final @NotNull World world) {
        final BukkitTrackedWorld trackedWorld = this.worldMap.remove(world.getUID());
        if (trackedWorld == null) {
            return;
        }

        for (final Chunk chunk : world.getLoadedChunks()) {
            trackedWorld.terminateChunk(chunk);
        }
    }

    public void initializeChunk(final @NotNull Chunk chunk) {
        final BukkitTrackedWorld trackedWorld = this.getTrackedWorldByChunk(chunk);
        if (trackedWorld != null) {
            trackedWorld.initializeChunk(chunk);
        }
    }

    public void terminateChunk(final @NotNull Chunk chunk) {
        final BukkitTrackedWorld trackedWorld = this.getTrackedWorldByChunk(chunk);
        if (trackedWorld != null) {
            trackedWorld.terminateChunk(chunk);
        }
    }

    public void initializeLoadedWorlds() {
        Bukkit.getWorlds().forEach(this::initializeWorld);
    }

    public void terminateLoadedWorlds() {
        Bukkit.getWorlds().forEach(this::terminateWorld);
    }

    public boolean isTrackedByBlock(final @NotNull Block block) {
        final BukkitTrackedWorld trackedWorld = this.getTrackedWorldByBlock(block);
        return trackedWorld != null && trackedWorld.isTrackedByBlock(block);
    }

    public boolean trackByBlock(final @NotNull Block block) {
        final boolean tracked = this.rawTrackByBlock(block);
        if (tracked) {
            BukkitBlockTrackerAPI.notifyBlockChange(new BlockChangeEvent(block, BlockChangeType.TRACK));
        }
        return tracked;
    }

    public boolean trackByState(final @NotNull BlockState state) {
        final boolean tracked = this.rawTrackByState(state);
        if (tracked) {
            BukkitBlockTrackerAPI.notifyBlockChange(new BlockChangeEvent(state.getBlock(), BlockChangeType.TRACK));
        }
        return tracked;
    }

    public boolean trackByStateIterable(final @NotNull Iterable<BlockState> states) {
        boolean ret = false;
        for (final BlockState state : states) {
            if (this.trackByState(state) && !ret) {
                ret = true;
            }
        }
        return ret;
    }

    public boolean untrackByBlock(final @NotNull Block block) {
        final boolean untracked = this.rawUntrackByBlock(block);
        if (untracked) {
            BukkitBlockTrackerAPI.notifyBlockChange(new BlockChangeEvent(block, BlockChangeType.UNTRACK));
        }
        return untracked;
    }

    public boolean untrackByState(final @NotNull BlockState state) {
        final boolean untracked = this.rawUntrackByState(state);
        if (untracked) {
            BukkitBlockTrackerAPI.notifyBlockChange(new BlockChangeEvent(state.getBlock(), BlockChangeType.UNTRACK));
        }
        return untracked;
    }

    public boolean untrackByBlockIterable(final @NotNull Iterable<Block> blocks) {
        boolean ret = false;
        for (final Block block : blocks) {
            if (this.untrackByBlock(block) && !ret) {
                ret = true;
            }
        }
        return ret;
    }

    public boolean untrackByStateIterable(final @NotNull Iterable<BlockState> states) {
        boolean ret = false;
        for (final BlockState state : states) {
            if (this.untrackByState(state) && !ret) {
                ret = true;
            }
        }
        return ret;
    }

    public void move(final @NotNull Block from, final @NotNull Block to) {
        if (this.rawUntrackByBlock(from)) {
            this.rawTrackByBlock(to);
            BukkitBlockTrackerAPI.notifyBlockChange(new BlockChangeEvent(from, BlockChangeType.MOVE, to));
        }
    }

    public void shiftByBlockList(final @NotNull List<Block> blocks, final @NotNull BlockFace direction) {
        final ObjectList<Block> originalBlocks = new ObjectArrayList<>(blocks);
        final int size = originalBlocks.size();
        final boolean[] wasTracked = new boolean[size];
        final ObjectList<Block> targetBlocks = new ObjectArrayList<>(size);

        for (int i = 0; i < size; i++) {
            final Block block = originalBlocks.get(i);
            wasTracked[i] = this.rawUntrackByBlock(block);
            targetBlocks.add(block.getRelative(direction));
        }

        for (int i = 0; i < size; i++) {
            final Block targetBlock = targetBlocks.get(i);
            if (wasTracked[i]) {
                this.rawTrackByBlock(targetBlock);
                BukkitBlockTrackerAPI.notifyBlockChange(new BlockChangeEvent(originalBlocks.get(i), BlockChangeType.MOVE, targetBlock));
            } else {
                this.untrackByBlock(targetBlock);
            }
        }
    }

    private boolean rawTrackByBlock(final @NotNull Block block) {
        final BukkitTrackedWorld trackedWorld = this.getTrackedWorldByBlock(block);
        return trackedWorld != null && trackedWorld.trackByBlock(block);
    }

    private boolean rawTrackByState(final @NotNull BlockState state) {
        final BukkitTrackedWorld trackedWorld = this.getTrackedWorldByState(state);
        return trackedWorld != null && trackedWorld.trackByState(state);
    }

    private boolean rawUntrackByBlock(final @NotNull Block block) {
        final BukkitTrackedWorld trackedWorld = this.getTrackedWorldByBlock(block);
        return trackedWorld != null && trackedWorld.untrackByBlock(block);
    }

    private boolean rawUntrackByState(final @NotNull BlockState state) {
        final BukkitTrackedWorld trackedWorld = this.getTrackedWorldByState(state);
        return trackedWorld != null && trackedWorld.untrackByState(state);
    }

    private @Nullable BukkitTrackedWorld getTrackedWorldByWorld(final @NotNull World world) {
        final UUID uniqueId = world.getUID();
        return this.worldMap.get(uniqueId);
    }

    private @Nullable BukkitTrackedWorld getTrackedWorldByChunk(final @NotNull Chunk chunk) {
        return this.getTrackedWorldByWorld(chunk.getWorld());
    }

    private @Nullable BukkitTrackedWorld getTrackedWorldByBlock(final @NotNull Block block) {
        return this.getTrackedWorldByWorld(block.getWorld());
    }

    private @Nullable BukkitTrackedWorld getTrackedWorldByState(final @NotNull BlockState state) {
        return this.getTrackedWorldByWorld(state.getWorld());
    }
}
