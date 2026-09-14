package dev.krakenied.blocktracker;

import dev.krakenied.blocktracker.data.WorldMap;
import dev.krakenied.blocktracker.event.BlockChangeEvent;
import dev.krakenied.blocktracker.event.BlockChangeType;
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@SuppressWarnings("UnusedReturnValue")
public final class BukkitTrackingManager {

    public enum ChangeResult {
        CHANGED,
        UNCHANGED,
        CANCELLED;

        public boolean isCancelled() {
            return this == CANCELLED;
        }
    }

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
        return this.trackByBlockResult(block) == ChangeResult.CHANGED;
    }

    public @NotNull ChangeResult trackByBlockResult(final @NotNull Block block) {
        if (this.isTrackedByBlock(block)) {
            return ChangeResult.UNCHANGED;
        }

        if (!this.fireChange(new BlockChangeEvent(block, BlockChangeType.CREATED))) {
            return ChangeResult.CANCELLED;
        }

        return this.rawTrackByBlock(block) ? ChangeResult.CHANGED : ChangeResult.UNCHANGED;
    }

    public boolean trackByState(final @NotNull BlockState state) {
        return this.trackByStateResult(state) == ChangeResult.CHANGED;
    }

    public @NotNull ChangeResult trackByStateResult(final @NotNull BlockState state) {
        final Block block = state.getBlock();
        if (this.isTrackedByBlock(block)) {
            return ChangeResult.UNCHANGED;
        }

        if (!this.fireChange(new BlockChangeEvent(block, BlockChangeType.CREATED))) {
            return ChangeResult.CANCELLED;
        }

        return this.rawTrackByState(state) ? ChangeResult.CHANGED : ChangeResult.UNCHANGED;
    }

    public boolean trackByStateIterable(final @NotNull Iterable<BlockState> states) {
        return this.trackByStateIterableResult(states) == ChangeResult.CHANGED;
    }

    public @NotNull ChangeResult trackByStateIterableResult(final @NotNull Iterable<BlockState> states) {
        final List<BlockState> pending = new ArrayList<>();
        final List<BlockChangeEvent> events = new ArrayList<>();

        for (final BlockState state : states) {
            if (!this.isTrackedByBlock(state.getBlock())) {
                pending.add(state);
                events.add(new BlockChangeEvent(state.getBlock(), BlockChangeType.CREATED));
            }
        }

        if (pending.isEmpty()) {
            return ChangeResult.UNCHANGED;
        }

        if (!this.fireChanges(events)) {
            return ChangeResult.CANCELLED;
        }

        boolean changed = false;
        for (final BlockState state : pending) {
            changed |= this.rawTrackByState(state);
        }
        return changed ? ChangeResult.CHANGED : ChangeResult.UNCHANGED;
    }

    public boolean untrackByBlock(final @NotNull Block block) {
        return this.untrackByBlockResult(block) == ChangeResult.CHANGED;
    }

    public @NotNull ChangeResult untrackByBlockResult(final @NotNull Block block) {
        return this.untrackByBlockResult(block, true);
    }

    public @NotNull ChangeResult untrackByBlockResult(final @NotNull Block block, final boolean cancellationSupported) {
        if (!this.isTrackedByBlock(block)) {
            return ChangeResult.UNCHANGED;
        }

        if (!this.fireChange(new BlockChangeEvent(block, BlockChangeType.REMOVED, cancellationSupported))) {
            return ChangeResult.CANCELLED;
        }

        return this.rawUntrackByBlock(block) ? ChangeResult.CHANGED : ChangeResult.UNCHANGED;
    }

    public boolean untrackByState(final @NotNull BlockState state) {
        return this.untrackByStateResult(state) == ChangeResult.CHANGED;
    }

    public @NotNull ChangeResult untrackByStateResult(final @NotNull BlockState state) {
        final Block block = state.getBlock();
        if (!this.isTrackedByBlock(block)) {
            return ChangeResult.UNCHANGED;
        }

        if (!this.fireChange(new BlockChangeEvent(block, BlockChangeType.REMOVED))) {
            return ChangeResult.CANCELLED;
        }

        return this.rawUntrackByState(state) ? ChangeResult.CHANGED : ChangeResult.UNCHANGED;
    }

    public boolean untrackByBlockIterable(final @NotNull Iterable<Block> blocks) {
        return this.untrackByBlockIterableResult(blocks) == ChangeResult.CHANGED;
    }

    public @NotNull ChangeResult untrackByBlockIterableResult(final @NotNull Iterable<Block> blocks) {
        final List<Block> pending = new ArrayList<>();
        final List<BlockChangeEvent> events = new ArrayList<>();

        for (final Block block : blocks) {
            if (this.isTrackedByBlock(block)) {
                pending.add(block);
                events.add(new BlockChangeEvent(block, BlockChangeType.REMOVED));
            }
        }

        if (pending.isEmpty()) {
            return ChangeResult.UNCHANGED;
        }

        if (!this.fireChanges(events)) {
            return ChangeResult.CANCELLED;
        }

        boolean changed = false;
        for (final Block block : pending) {
            changed |= this.rawUntrackByBlock(block);
        }
        return changed ? ChangeResult.CHANGED : ChangeResult.UNCHANGED;
    }

    public boolean untrackByStateIterable(final @NotNull Iterable<BlockState> states) {
        return this.untrackByStateIterableResult(states) == ChangeResult.CHANGED;
    }

    public @NotNull ChangeResult untrackByStateIterableResult(final @NotNull Iterable<BlockState> states) {
        final List<BlockState> pending = new ArrayList<>();
        final List<BlockChangeEvent> events = new ArrayList<>();

        for (final BlockState state : states) {
            if (this.isTrackedByBlock(state.getBlock())) {
                pending.add(state);
                events.add(new BlockChangeEvent(state.getBlock(), BlockChangeType.REMOVED));
            }
        }

        if (pending.isEmpty()) {
            return ChangeResult.UNCHANGED;
        }

        if (!this.fireChanges(events)) {
            return ChangeResult.CANCELLED;
        }

        boolean changed = false;
        for (final BlockState state : pending) {
            changed |= this.rawUntrackByState(state);
        }
        return changed ? ChangeResult.CHANGED : ChangeResult.UNCHANGED;
    }

    public boolean move(final @NotNull Block from, final @NotNull Block to) {
        return this.moveResult(from, to) == ChangeResult.CHANGED;
    }

    public @NotNull ChangeResult moveResult(final @NotNull Block from, final @NotNull Block to) {
        if (!this.isTrackedByBlock(from)) {
            return ChangeResult.UNCHANGED;
        }

        if (!this.fireChange(new BlockChangeEvent(from, BlockChangeType.MOVED, to))) {
            return ChangeResult.CANCELLED;
        }

        if (!this.rawUntrackByBlock(from)) {
            return ChangeResult.UNCHANGED;
        }

        this.rawTrackByBlock(to);
        return ChangeResult.CHANGED;
    }

    public boolean shiftByBlockList(final @NotNull List<Block> blocks, final @NotNull BlockFace direction) {
        return this.shiftByBlockListResult(blocks, direction) == ChangeResult.CHANGED;
    }

    public @NotNull ChangeResult shiftByBlockListResult(final @NotNull List<Block> blocks, final @NotNull BlockFace direction) {
        return this.shiftByBlockListResult(blocks, direction, List.of(), null);
    }

    @NotNull ChangeResult handlePistonExtend(final @NotNull Block piston, final @NotNull List<Block> blocks, final @NotNull BlockFace direction) {
        final Block pistonHead = piston.getRelative(direction);
        final boolean pistonTracked = this.isTrackedByBlock(piston);
        final BlockChangeEvent headEvent = this.createSetTrackedEvent(pistonHead, pistonTracked);
        return this.shiftByBlockListResult(blocks, direction, headEvent == null ? List.of() : List.of(headEvent), () -> {
            if (pistonTracked) {
                this.rawTrackByBlock(pistonHead);
            } else {
                this.rawUntrackByBlock(pistonHead);
            }
        });
    }

    @NotNull ChangeResult handlePistonRetract(final @NotNull Block piston, final boolean sticky, final @NotNull List<Block> blocks, final @NotNull BlockFace direction) {
        final List<BlockChangeEvent> extraEvents = new ArrayList<>();
        final Block pistonHead = piston.getRelative(direction.getOppositeFace());
        final boolean removeHead = this.isTrackedByBlock(piston) && this.isTrackedByBlock(pistonHead);
        if (removeHead) {
            extraEvents.add(new BlockChangeEvent(pistonHead, BlockChangeType.REMOVED));
        }

        final Runnable extraMutation = removeHead ? () -> this.rawUntrackByBlock(pistonHead) : null;
        if (!sticky) {
            if (extraEvents.isEmpty()) {
                return ChangeResult.UNCHANGED;
            }
            if (!this.fireChanges(extraEvents)) {
                return ChangeResult.CANCELLED;
            }
            extraMutation.run();
            return ChangeResult.CHANGED;
        }

        return this.shiftByBlockListResult(blocks, direction, extraEvents, extraMutation);
    }

    private @NotNull ChangeResult shiftByBlockListResult(
            final @NotNull List<Block> blocks,
            final @NotNull BlockFace direction,
            final @NotNull List<BlockChangeEvent> extraEvents,
            final @Nullable Runnable extraMutation
    ) {
        final ObjectList<Block> originalBlocks = new ObjectArrayList<>(blocks);
        final int size = originalBlocks.size();
        final boolean[] wasTracked = new boolean[size];
        final ObjectList<Block> targetBlocks = new ObjectArrayList<>(size);
        final Set<Block> originalSet = new HashSet<>(originalBlocks);
        final List<BlockChangeEvent> events = new ArrayList<>();
        boolean hasMutation = false;

        for (int i = 0; i < size; i++) {
            final Block block = originalBlocks.get(i);
            final Block targetBlock = block.getRelative(direction);
            final boolean tracked = this.isTrackedByBlock(block);

            wasTracked[i] = tracked;
            targetBlocks.add(targetBlock);

            if (tracked) {
                events.add(new BlockChangeEvent(block, BlockChangeType.MOVED, targetBlock));
                hasMutation = true;
            } else if (!originalSet.contains(targetBlock) && this.isTrackedByBlock(targetBlock)) {
                events.add(new BlockChangeEvent(targetBlock, BlockChangeType.REMOVED));
                hasMutation = true;
            }
        }

        events.addAll(extraEvents);
        hasMutation |= !extraEvents.isEmpty();

        if (!hasMutation) {
            return ChangeResult.UNCHANGED;
        }

        if (!this.fireChanges(events)) {
            return ChangeResult.CANCELLED;
        }

        for (final Block block : originalBlocks) {
            this.rawUntrackByBlock(block);
        }

        for (int i = 0; i < size; i++) {
            final Block targetBlock = targetBlocks.get(i);
            if (wasTracked[i]) {
                this.rawTrackByBlock(targetBlock);
            } else {
                this.rawUntrackByBlock(targetBlock);
            }
        }

        if (extraMutation != null) {
            extraMutation.run();
        }

        return ChangeResult.CHANGED;
    }

    private @Nullable BlockChangeEvent createSetTrackedEvent(final @NotNull Block block, final boolean tracked) {
        final boolean currentlyTracked = this.isTrackedByBlock(block);
        if (currentlyTracked == tracked) {
            return null;
        }
        return new BlockChangeEvent(block, tracked ? BlockChangeType.CREATED : BlockChangeType.REMOVED);
    }

    private boolean fireChange(final @NotNull BlockChangeEvent event) {
        return BlockTrackerAPI.callBlockChange(event);
    }

    private boolean fireChanges(final @NotNull Iterable<BlockChangeEvent> events) {
        for (final BlockChangeEvent event : events) {
            if (!this.fireChange(event)) {
                return false;
            }
        }
        return true;
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
