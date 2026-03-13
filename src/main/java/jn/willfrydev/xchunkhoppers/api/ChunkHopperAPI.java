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
import java.util.UUID;

public interface ChunkHopperAPI {

    // Consultas básicas
    boolean isChunkHopper(Block block);
    Location getHopperLocation(Chunk chunk);
    boolean hasHopper(Chunk chunk);
    ItemStack getHopperItem(int amount);
    ItemStack getHopperItem(String type, int amount);
    Collection<String> getFilterList();

    // NUEVO: Métodos avanzados de la API
    String getHopperType(Location location);
    UUID getHopperOwner(Location location);

    /**
     * Crea un ChunkHopper programáticamente.
     * @param location Ubicación del bloque (Debe ser un bloque de Hopper colocado)
     * @param type Tipo de hopper (ej. "default", "vip")
     * @param owner UUID del dueño
     * @param ownerName Nombre del dueño
     */
    void createHopper(Location location, String type, UUID owner, String ownerName);

    /**
     * Elimina un ChunkHopper y su holograma programáticamente.
     * No dropea el ítem (ideal para limpiezas).
     */
    void deleteHopper(Location location);
}