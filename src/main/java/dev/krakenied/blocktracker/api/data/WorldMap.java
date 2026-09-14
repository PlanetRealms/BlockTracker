package dev.krakenied.blocktracker.api.data;

import dev.krakenied.blocktracker.bukkit.BukkitTrackedWorld;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import java.util.UUID;

public final class WorldMap extends Object2ObjectOpenHashMap<UUID, BukkitTrackedWorld> {

    public WorldMap() {
        super();
    }
}
