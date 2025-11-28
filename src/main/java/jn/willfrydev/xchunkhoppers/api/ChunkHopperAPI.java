/*
 * MIT License
 *
 * Copyright (c) 2025 xPlugins x WillfryDev
 */
package jn.willfrydev.xchunkhoppers.api;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import java.util.Collection;

public interface ChunkHopperAPI {


    boolean isChunkHopper(Block block);
    Location getHopperLocation(Chunk chunk);
    boolean hasHopper(Chunk chunk);
    ItemStack getHopperItem(int amount);
    Collection<String> getFilterList();






}