package dev.krakenied.blocktracker;

import io.papermc.paper.event.block.BlockBreakBlockEvent;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Bed;
import org.bukkit.block.data.type.Piston;
import org.bukkit.block.data.type.PistonHead;
import org.bukkit.block.data.type.SmallDripleaf;
import org.bukkit.block.data.type.Stairs;
import org.bukkit.block.data.type.TrapDoor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockFertilizeEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.block.BlockMultiPlaceEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.block.EntityBlockFormEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class BukkitListener implements Listener {

    private final BlockTrackerPlugin plugin;
    private final BukkitTrackingManager trackingManager;

    public BukkitListener(final @NotNull BlockTrackerPlugin plugin) {
        this.plugin = plugin;
        this.trackingManager = plugin.getTrackingManager();
    }

    // Worlds and chunks

    @EventHandler
    public void onWorldLoad(final @NotNull WorldLoadEvent event) {
        final World world = event.getWorld();
        this.trackingManager.initializeWorld(world);
    }

    @EventHandler
    public void onWorldUnload(final @NotNull WorldUnloadEvent event) {
        final World world = event.getWorld();
        this.trackingManager.terminateWorld(world);
    }

    @EventHandler
    public void onChunkLoad(final @NotNull ChunkLoadEvent event) {
        final Chunk chunk = event.getChunk();
        this.trackingManager.initializeChunk(chunk);
    }

    @EventHandler
    public void onChunkUnload(final @NotNull ChunkUnloadEvent event) {
        final Chunk chunk = event.getChunk();
        this.trackingManager.terminateChunk(chunk);
    }

    // Direct block placements and breaks

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(final @NotNull BlockPlaceEvent event) {
        final Block block = event.getBlock();
        this.cancelIfNeeded(event, this.trackingManager.trackByBlockResult(block));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockMultiPlace(final @NotNull BlockMultiPlaceEvent event) {
        final List<BlockState> states = event.getReplacedBlockStates();
        this.cancelIfNeeded(event, this.trackingManager.trackByStateIterableResult(states));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(final @NotNull BlockBreakEvent event) {
        final Block block = event.getBlock();
        final BlockData blockData = block.getBlockData();
        this.cancelIfNeeded(event, this.untrackCustom(block, blockData));
    }

    // Explosions

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockExplode(final @NotNull BlockExplodeEvent event) {
        final List<Block> blocks = event.blockList();
        this.cancelIfNeeded(event, this.trackingManager.untrackByBlockIterableResult(blocks));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityExplode(final @NotNull EntityExplodeEvent event) {
        final List<Block> blocks = event.blockList();
        this.cancelIfNeeded(event, this.trackingManager.untrackByBlockIterableResult(blocks));
    }

    // Burns

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBurn(final @NotNull BlockBurnEvent event) {
        final Block block = event.getBlock();
        this.cancelIfNeeded(event, this.trackingManager.untrackByBlockResult(block));
    }

    // Pistons

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPistonExtend(final @NotNull BlockPistonExtendEvent event) {
        final Block block = event.getBlock();
        final List<Block> blocks = event.getBlocks();
        final BlockFace direction = event.getDirection();
        this.cancelIfNeeded(event, this.trackingManager.handlePistonExtend(block, blocks, direction));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPistonRetract(final @NotNull BlockPistonRetractEvent event) {
        final Block block = event.getBlock();
        final List<Block> blocks = event.getBlocks();
        final BlockFace direction = event.getDirection();
        this.cancelIfNeeded(event, this.trackingManager.handlePistonRetract(block, event.isSticky(), blocks, direction));
    }

    // Trees & Structure growth

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onStructureGrow(final @NotNull StructureGrowEvent event) {
        final List<BlockState> states = event.getBlocks();
        this.cancelIfNeeded(event, this.trackingManager.untrackByStateIterableResult(states));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockFertilize(final @NotNull BlockFertilizeEvent event) {
        final Block block = event.getBlock();
        final Material type = block.getType();
        final List<BlockState> destinations = event.getBlocks();

        if (type == Material.CRIMSON_FUNGUS || type == Material.WARPED_FUNGUS) {
            this.cancelIfNeeded(event, this.trackingManager.untrackByBlockResult(block));
            return;
        }

        this.cancelIfNeeded(event, this.trackingManager.trackByStateIterableResult(destinations));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onLeavesDecay(final @NotNull LeavesDecayEvent event) {
        final Block block = event.getBlock();
        this.cancelIfNeeded(event, this.trackingManager.untrackByBlockResult(block));
    }

    // Farms

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockGrow(final @NotNull BlockGrowEvent event) {
        final Block block = event.getBlock();
        this.cancelIfNeeded(event, this.trackingManager.untrackByBlockResult(block));
    }

    // Fluid flow block breaking

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreakBlock(final @NotNull BlockBreakBlockEvent event) {
        final Block source = event.getSource();
        final Material sourceType = source.getType();

        if (sourceType == Material.PISTON || sourceType == Material.STICKY_PISTON) {
            return;
        }

        final Block block = event.getBlock();
        this.trackingManager.untrackByBlockResult(block, false);
    }

    // Frost Walker

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityBlockForm(final @NotNull EntityBlockFormEvent event) {
        final Entity entity = event.getEntity();
        if (!(entity instanceof Player)) {
            return;
        }

        final Block block = event.getBlock();
        this.cancelIfNeeded(event, this.trackingManager.trackByBlockResult(block));
    }

    // Ice and other blocks fading

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockFade(final @NotNull BlockFadeEvent event) {
        final Block block = event.getBlock();
        this.cancelIfNeeded(event, this.trackingManager.untrackByBlockResult(block));
    }

    // Falling blocks

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntitySpawn(final @NotNull EntitySpawnEvent event) {
        final Entity entity = event.getEntity();
        if (!(entity instanceof FallingBlock)) {
            return;
        }

        final Block block = entity.getLocation().getBlock();
        if (!this.trackingManager.isTrackedByBlock(block)) {
            return;
        }

        final BukkitTrackingManager.ChangeResult result = this.trackingManager.untrackByBlockResult(block);
        if (result.isCancelled()) {
            event.setCancelled(true);
            return;
        }

        if (result == BukkitTrackingManager.ChangeResult.CHANGED) {
            entity.setMetadata("block_tracker", new FixedMetadataValue(plugin, true));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityChangeBlock(final @NotNull EntityChangeBlockEvent event) {
        final Entity entity = event.getEntity();
        if (!(entity instanceof FallingBlock)) {
            return;
        }

        if (!entity.hasMetadata("block_tracker")) {
            return;
        }

        final Block block = event.getBlock();
        this.cancelIfNeeded(event, this.trackingManager.trackByBlockResult(block));
    }

    // Untrack multi-block structures (beds, double plants, pistons)

    private @NotNull BukkitTrackingManager.ChangeResult untrackCustom(final @NotNull Block block, final @NotNull BlockData blockData) {
        final List<Block> blocks = new java.util.ArrayList<>();
        blocks.add(block);

        final Block secondBlock;

        switch (blockData) {
            case final Bisected bisected when !(blockData instanceof SmallDripleaf || blockData instanceof Stairs || blockData instanceof TrapDoor) -> {
                final Bisected.Half half = bisected.getHalf();
                secondBlock = block.getRelative(half == Bisected.Half.BOTTOM ? BlockFace.UP : BlockFace.DOWN);
            }
            case final Bed bed -> {
                final Bed.Part part = bed.getPart();
                final BlockFace facing = bed.getFacing();
                secondBlock = block.getRelative(part == Bed.Part.FOOT ? facing : facing.getOppositeFace());
            }
            case final Piston piston -> {
                if (!piston.isExtended()) {
                    return this.trackingManager.untrackByBlockIterableResult(blocks);
                }
                secondBlock = block.getRelative(piston.getFacing());
            }
            case final PistonHead pistonHead ->
                    secondBlock = block.getRelative(pistonHead.getFacing().getOppositeFace());
            default -> {
                return this.trackingManager.untrackByBlockIterableResult(blocks);
            }
        }

        blocks.add(secondBlock);
        return this.trackingManager.untrackByBlockIterableResult(blocks);
    }

    // Dragon Egg teleportation

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockFromTo(final @NotNull BlockFromToEvent event) {
        final Block from = event.getBlock();
        if (from.getType() != Material.DRAGON_EGG) {
            return;
        }

        final Block to = event.getToBlock();
        this.cancelIfNeeded(event, this.trackingManager.moveResult(from, to));
    }

    // Emptying and filling buckets

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerBucketEmpty(final @NotNull PlayerBucketEmptyEvent event) {
        final Block block = event.getBlock();
        this.cancelIfNeeded(event, this.trackingManager.trackByBlockResult(block));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerBucketFill(final @NotNull PlayerBucketFillEvent event) {
        final Block block = event.getBlock();
        this.cancelIfNeeded(event, this.trackingManager.untrackByBlockResult(block));
    }

    // Block spreading (e.g., Mycelium, Sculk, Chorus, Fire)
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockSpread(final @NotNull BlockSpreadEvent event) {
        final Block source = event.getSource();
        final Block block = event.getBlock();

        final boolean sourceTracked = this.trackingManager.isTrackedByBlock(source);
        if (sourceTracked) {
            this.cancelIfNeeded(event, this.trackingManager.trackByBlockResult(block));
        }
    }

    private void cancelIfNeeded(final @NotNull Cancellable event, final @NotNull BukkitTrackingManager.ChangeResult result) {
        if (result.isCancelled()) {
            event.setCancelled(true);
        }
    }

}
