/*
 * MIT License
 *
 * Copyright (c) 2025 xPlugins x WillfryDev
 */

package jn.willfrydev.xchunkhoppers;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Hopper;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;
import org.bukkit.Location;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class GUI implements Listener {

    private final xChunkHoppers plugin;
    private final NamespacedKey typeKey;

    public GUI(xChunkHoppers plugin) {
        this.plugin = plugin;
        this.typeKey = new NamespacedKey(plugin, "hopper_type");
    }

    // Holders personalizados para identificar cada menú de forma segura
    private static class ShopHolder implements InventoryHolder { @Override public Inventory getInventory() { return null; } }
    private static class InfoHolder implements InventoryHolder { @Override public Inventory getInventory() { return null; } }
    private static class AdminHolder implements InventoryHolder { @Override public Inventory getInventory() { return null; } }

    public void openShopMenu(Player player) {
        ConfigurationSection config = plugin.getConfig().getConfigurationSection("shop-menu");
        if (config == null) return;

        int size = config.getInt("size", 36);
        Inventory inv = Bukkit.createInventory(new ShopHolder(), size, plugin.colorize(config.getString("title", "Tienda")));

        fillBackground(inv, "shop-menu.fill", size);

        // Cargar Botón de Cerrar/Volver
        if (config.getBoolean("close-button.enabled", true)) {
            Material mat = Material.matchMaterial(config.getString("close-button.material", "BARRIER"));
            if (mat == null) mat = Material.BARRIER;

            ItemStack close = new ItemStack(mat);
            ItemMeta m = close.getItemMeta();
            if (m != null) {
                m.setDisplayName(plugin.colorize(config.getString("close-button.name", "&cCerrar")));
                m.setLore(config.getStringList("close-button.lore").stream().map(plugin::colorize).collect(Collectors.toList()));
                close.setItemMeta(m);
            }
            inv.setItem(config.getInt("close-button.slot", 31), close);
        }

        // Cargar items de venta
        ConfigurationSection items = config.getConfigurationSection("items");
        if (items != null) {
            for (String key : items.getKeys(false)) {
                String type = items.getString(key + ".type");
                double price = items.getDouble(key + ".price");
                int slot = items.getInt(key + ".slot");

                // Obtenemos el item base configurado en hopper-types
                ItemStack hopper = plugin.getHopperItem(type, 1);

                // Si el usuario especificó un material distinto para mostrar en la tienda, lo aplicamos
                if (items.contains(key + ".material")) {
                    Material displayMat = Material.matchMaterial(items.getString(key + ".material"));
                    if (displayMat != null) {
                        hopper.setType(displayMat);
                    }
                }

                ItemMeta meta = hopper.getItemMeta();
                if (meta != null) {
                    // Creamos una lista NUEVA y vacía para IGNORAR el lore original
                    List<String> shopLore = new ArrayList<>();

                    // Solo añadimos el formato de precio de la tienda
                    for (String line : config.getStringList("price-format")) {
                        shopLore.add(plugin.colorize(line.replace("%price%", String.format("%.2f", price))));
                    }

                    // Sobrescribimos el lore del item solo para la GUI
                    meta.setLore(shopLore);
                    hopper.setItemMeta(meta);
                }
                inv.setItem(slot, hopper);
            }
        }
        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 1f, 1f);
    }

    public void openInfoMenu(Player player, Location loc, xChunkHoppers.HopperData data) {
        ConfigurationSection config = plugin.getConfig().getConfigurationSection("info-menu");
        if (config == null) return;

        int size = config.getInt("size", 27);
        Inventory inv = Bukkit.createInventory(new InfoHolder(), size, plugin.colorize(config.getString("title", "Info")));
        fillBackground(inv, "info-menu.fill", size);

        Material mat = Material.matchMaterial(config.getString("info-item.material", "PAPER"));
        if (mat == null) mat = Material.PAPER;

        ItemStack info = new ItemStack(mat);
        ItemMeta m = info.getItemMeta();
        if (m != null) {
            m.setDisplayName(plugin.colorize(config.getString("info-item.name", "&aInfo")));
            SimpleDateFormat sdf = new SimpleDateFormat(config.getString("date-format", "dd/MM/yyyy HH:mm"));
            String dateStr = sdf.format(new Date(data.getPlacedTime()));
            m.setLore(config.getStringList("info-item.lore").stream()
                    .map(l -> plugin.colorize(l.replace("%owner%", data.getOwnerName() != null ? data.getOwnerName() : "Desconocido")
                            .replace("%type%", data.getType())
                            .replace("%date%", dateStr)
                            .replace("%items%", String.valueOf(data.getCollectedItems()))))
                    .collect(Collectors.toList()));
            info.setItemMeta(m);
        }
        inv.setItem(config.getInt("info-item.slot", 13), info);

        // Preview del inventario real de la tolva
        if (loc.getBlock().getState() instanceof Hopper hopper) {
            ItemStack[] contents = hopper.getInventory().getContents();
            List<Integer> slots = config.getIntegerList("preview-slots");
            for (int i = 0; i < contents.length && i < slots.size(); i++) {
                if (contents[i] != null) {
                    inv.setItem(slots.get(i), contents[i]);
                }
            }
        }
        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1f, 1f);
    }

    public void openAdminMenu(Player player) {
        ConfigurationSection config = plugin.getConfig().getConfigurationSection("menu");
        if (config == null) return;

        int size = config.getInt("size", 27);
        Inventory inv = Bukkit.createInventory(new AdminHolder(), size, plugin.colorize(config.getString("title", "Admin Menu")));
        fillBackground(inv, "menu.fill", size);

        ConfigurationSection types = plugin.getConfig().getConfigurationSection("hopper-types");
        if (types != null) {
            int slot = 10;
            for (String key : types.getKeys(false)) {
                if (slot >= size) break; // Evitar OutOfBounds si hay muchos tipos
                inv.setItem(slot++, plugin.getHopperItem(key, 1));
            }
        }
        player.openInventory(inv);
    }

    private void fillBackground(Inventory inv, String path, int size) {
        if (plugin.getConfig().getBoolean(path + ".enabled", true)) {
            Material mat = Material.matchMaterial(plugin.getConfig().getString(path + ".material", "BLACK_STAINED_GLASS_PANE"));
            if (mat == null) mat = Material.BLACK_STAINED_GLASS_PANE;

            ItemStack glass = new ItemStack(mat);
            ItemMeta m = glass.getItemMeta();
            if (m != null) {
                m.setDisplayName(" ");
                glass.setItemMeta(m);
            }
            for (int i = 0; i < size; i++) {
                if (inv.getItem(i) == null) {
                    inv.setItem(i, glass);
                }
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getClickedInventory() == null) return;

        InventoryHolder holder = event.getInventory().getHolder();
        // Solo cancelamos e interactuamos si es uno de nuestros menús
        if (!(holder instanceof ShopHolder || holder instanceof InfoHolder || holder instanceof AdminHolder)) return;

        event.setCancelled(true);
        Player p = (Player) event.getWhoClicked();

        // Lógica de la Tienda
        if (holder instanceof ShopHolder) {
            // Evitar clics en el inventario del jugador mientras la tienda está abierta
            if (event.getClickedInventory().equals(p.getInventory())) return;

            int slot = event.getSlot();
            if (slot == plugin.getConfig().getInt("shop-menu.close-button.slot", 31)) {
                p.closeInventory();
                return;
            }

            ConfigurationSection items = plugin.getConfig().getConfigurationSection("shop-menu.items");
            if (items == null) return;

            for (String key : items.getKeys(false)) {
                if (items.getInt(key + ".slot") == slot) {
                    double price = items.getDouble(key + ".price");
                    String type = items.getString(key + ".type");
                    Economy econ = xChunkHoppers.getEconomy();

                    if (econ == null) {
                        p.sendMessage(plugin.colorize("&cError: Vault/Economía no detectada en el servidor."));
                        return;
                    }

                    if (econ.getBalance(p) >= price) {
                        if (p.getInventory().firstEmpty() == -1) {
                            p.sendMessage(plugin.getMsg("shop-inventory-full"));
                            return;
                        }

                        econ.withdrawPlayer(p, price);

                        // Le damos el item REAL de la tolva, independientemente del material que se muestre en la tienda
                        p.getInventory().addItem(plugin.getHopperItem(type, 1));

                        p.sendMessage(plugin.getMsg("shop-buy-success")
                                .replace("%price%", String.format("%.2f", price))
                                .replace("%type%", type));
                        p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
                    } else {
                        double missing = price - econ.getBalance(p);
                        p.sendMessage(plugin.getMsg("shop-insufficient-funds")
                                .replace("%missing%", String.format("%.2f", missing)));
                        p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                    }
                    return;
                }
            }
        }
        // Lógica del Menú de Información
        else if (holder instanceof InfoHolder) {
            // En el menú de información solo cancelamos el evento para que no puedan sacar los items de la preview
            return;
        }
        // Lógica del Menú de Administradores
        else if (holder instanceof AdminHolder) {
            if (event.getClickedInventory().equals(p.getInventory())) return;

            ItemStack item = event.getCurrentItem();
            if (item != null && item.hasItemMeta()) {
                String type = item.getItemMeta().getPersistentDataContainer().get(typeKey, PersistentDataType.STRING);
                if (type != null) {
                    p.getInventory().addItem(plugin.getHopperItem(type, 1));
                    p.playSound(p.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1f, 1f);
                }
            }
        }
    }
}