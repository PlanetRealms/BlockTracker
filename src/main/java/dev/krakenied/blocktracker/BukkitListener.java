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

    private final BukkitBlockTrackerPlugin plugin;
    private final BukkitTrackingManager trackingManager;

    public BukkitListener(final @NotNull BukkitBlockTrackerPlugin plugin) {
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

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(final @NotNull BlockPlaceEvent event) {
        final Block block = event.getBlock();
        this.trackingManager.trackByBlock(block);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockMultiPlace(final @NotNull BlockMultiPlaceEvent event) {
        final List<BlockState> states = event.getReplacedBlockStates();
        this.trackingManager.trackByStateIterable(states);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(final @NotNull BlockBreakEvent event) {
        final Block block = event.getBlock();
        final BlockData blockData = block.getBlockData();
        this.untrackCustom(block, blockData);
    }

    // Explosions

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockExplode(final @NotNull BlockExplodeEvent event) {
        final List<Block> blocks = event.blockList();
        this.trackingManager.untrackByBlockIterable(blocks);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityExplode(final @NotNull EntityExplodeEvent event) {
        final List<Block> blocks = event.blockList();
        this.trackingManager.untrackByBlockIterable(blocks);
    }

    // Burns

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBurn(final @NotNull BlockBurnEvent event) {
        final Block block = event.getBlock();
        this.trackingManager.untrackByBlock(block);
    }

    // Pistons

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPistonExtend(final @NotNull BlockPistonExtendEvent event) {
        final List<Block> blocks = event.getBlocks();
        final BlockFace direction = event.getDirection();
        this.trackingManager.shiftByBlockList(blocks, direction);

        final Block block = event.getBlock();
        final boolean pistonTracked = this.trackingManager.isTrackedByBlock(block);

        final Block pistonHeadBlock = block.getRelative(direction);
        if (pistonTracked) {
            this.trackingManager.trackByBlock(pistonHeadBlock);
        } else {
            this.trackingManager.untrackByBlock(pistonHeadBlock);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPistonRetract(final @NotNull BlockPistonRetractEvent event) {
        final Block block = event.getBlock();
        if (this.trackingManager.isTrackedByBlock(block)) {
            final BlockFace pistonHeadFace = event.getDirection().getOppositeFace();
            final Block pistonHeadBlock = block.getRelative(pistonHeadFace);
            this.trackingManager.untrackByBlock(pistonHeadBlock);
        }

        if (!event.isSticky()) {
            return;
        }

        final List<Block> blocks = event.getBlocks();
        final BlockFace direction = event.getDirection();
        this.trackingManager.shiftByBlockList(blocks, direction);
    }

    // Trees & Structure growth

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onStructureGrow(final @NotNull StructureGrowEvent event) {
        final List<BlockState> states = event.getBlocks();
        this.trackingManager.untrackByStateIterable(states);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockFertilize(final @NotNull BlockFertilizeEvent event) {
        final Block block = event.getBlock();
        final Material type = block.getType();
        final List<BlockState> destinations = event.getBlocks();

        if (type == Material.CRIMSON_FUNGUS || type == Material.WARPED_FUNGUS) {
            this.trackingManager.untrackByBlock(block);
            return;
        }

        this.trackingManager.trackByStateIterable(destinations);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onLeavesDecay(final @NotNull LeavesDecayEvent event) {
        final Block block = event.getBlock();
        this.trackingManager.untrackByBlock(block);
    }

    // Farms

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockGrow(final @NotNull BlockGrowEvent event) {
        final Block block = event.getBlock();
        this.trackingManager.untrackByBlock(block);
    }

    // Fluid flow block breaking

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreakBlock(final @NotNull BlockBreakBlockEvent event) {
        final Block source = event.getSource();
        final Material sourceType = source.getType();

        if (sourceType == Material.PISTON || sourceType == Material.STICKY_PISTON) {
            return;
        }

        final Block block = event.getBlock();
        this.trackingManager.untrackByBlock(block);
    }

    // Frost Walker

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityBlockForm(final @NotNull EntityBlockFormEvent event) {
        final Entity entity = event.getEntity();
        if (!(entity instanceof Player)) {
            return;
        }

        final Block block = event.getBlock();
        this.trackingManager.trackByBlock(block);
    }

    // Ice and other blocks fading

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockFade(final @NotNull BlockFadeEvent event) {
        final Block block = event.getBlock();
        this.trackingManager.untrackByBlock(block);
    }

    // Falling blocks

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntitySpawn(final @NotNull EntitySpawnEvent event) {
        final Entity entity = event.getEntity();
        if (!(entity instanceof FallingBlock)) {
            return;
        }

        final Block block = entity.getLocation().getBlock();
        if (!this.trackingManager.isTrackedByBlock(block)) {
            return;
        }

        entity.setMetadata("block_tracker", new FixedMetadataValue(plugin, true));
        this.trackingManager.untrackByBlock(block);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityChangeBlock(final @NotNull EntityChangeBlockEvent event) {
        final Entity entity = event.getEntity();
        if (!(entity instanceof FallingBlock)) {
            return;
        }

        if (!entity.hasMetadata("block_tracker")) {
            return;
        }

        final Block block = event.getBlock();
        this.trackingManager.trackByBlock(block);
    }

    // Untrack multi-block structures (beds, double plants, pistons)

    private void untrackCustom(final @NotNull Block block, final @NotNull BlockData blockData) {
        this.trackingManager.untrackByBlock(block);

        final Block secondBlock;

        if (blockData instanceof final Bisected bisected && !(blockData instanceof SmallDripleaf || blockData instanceof Stairs || blockData instanceof TrapDoor)) {
            final Bisected.Half half = bisected.getHalf();

            if (half == Bisected.Half.BOTTOM) {
                secondBlock = block.getRelative(BlockFace.UP);
            } else {
                secondBlock = block.getRelative(BlockFace.DOWN);
            }
        } else if (blockData instanceof final Bed bed) {
            final Bed.Part part = bed.getPart();
            final BlockFace facing = bed.getFacing();

            if (part == Bed.Part.FOOT) {
                secondBlock = block.getRelative(facing);
            } else {
                secondBlock = block.getRelative(facing.getOppositeFace());
            }
        } else if (blockData instanceof final Piston piston) {
            final BlockFace facing = piston.getFacing();

            if (piston.isExtended()) {
                secondBlock = block.getRelative(facing);
            } else {
                return;
            }
        } else if (blockData instanceof final PistonHead pistonHead) {
            final BlockFace facing = pistonHead.getFacing();

            secondBlock = block.getRelative(facing.getOppositeFace());
        } else {
            return;
        }

        this.trackingManager.untrackByBlock(secondBlock);
    }

    // Dragon Egg teleportation

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockFromTo(final @NotNull BlockFromToEvent event) {
        final Block from = event.getBlock();
        if (from.getType() != Material.DRAGON_EGG) {
            return;
        }

        final Block to = event.getToBlock();
        this.trackingManager.move(from, to);
    }

    // Emptying and filling buckets

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerBucketEmpty(final @NotNull PlayerBucketEmptyEvent event) {
        final Block block = event.getBlock();
        this.trackingManager.trackByBlock(block);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerBucketFill(final @NotNull PlayerBucketFillEvent event) {
        final Block block = event.getBlock();
        this.trackingManager.untrackByBlock(block);
    }

    // Block spreading (e.g., Mycelium, Sculk, Chorus, Fire)
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockSpread(final @NotNull BlockSpreadEvent event) {
        final Block source = event.getSource();
        final Block block = event.getBlock();

        final boolean sourceTracked = this.trackingManager.isTrackedByBlock(source);
        if (sourceTracked) {
            this.trackingManager.trackByBlock(block);
        }
    }
}
