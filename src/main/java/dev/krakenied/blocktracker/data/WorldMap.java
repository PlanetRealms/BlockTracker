package dev.krakenied.blocktracker.data;

import dev.krakenied.blocktracker.BukkitTrackedWorld;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import java.util.UUID;

public final class WorldMap extends Object2ObjectOpenHashMap<UUID, BukkitTrackedWorld> {

    public WorldMap() {
        super();
    }
}
