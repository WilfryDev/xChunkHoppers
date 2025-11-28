package jn.willfrydev.xchunkhoppers;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GUI implements Listener {

    private final xChunkHoppers plugin;
    private final NamespacedKey typeKey;
    private final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

    public GUI(xChunkHoppers plugin) {
        this.plugin = plugin;
        this.typeKey = new NamespacedKey(plugin, "hopper_type");
    }

    public void openMenu(Player player) {
        String title = colorize(plugin.getConfig().getString("menu.title", "&8Hoppers Menu"));
        int size = plugin.getConfig().getInt("menu.size", 27);

        Inventory inv = Bukkit.createInventory(null, size, title);

        if (plugin.getConfig().getBoolean("menu.fill.enabled")) {
            ItemStack filler = new ItemStack(Material.valueOf(plugin.getConfig().getString("menu.fill.material", "BLACK_STAINED_GLASS_PANE")));
            ItemMeta meta = filler.getItemMeta();
            meta.setDisplayName(" ");
            filler.setItemMeta(meta);
            for (int i = 0; i < size; i++) {
                inv.setItem(i, filler);
            }
        }

        ConfigurationSection section = plugin.getConfig().getConfigurationSection("hopper-types");
        if (section != null) {
            int slotIndex = 0;
            for (String key : section.getKeys(false)) {
                if (slotIndex >= size) break;
                ItemStack hopperItem = plugin.getHopperItem(key, 1);

                inv.setItem(slotIndex + 10, hopperItem);
                slotIndex++;
            }
        }

        player.openInventory(inv);
        playSound(player);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = colorize(plugin.getConfig().getString("menu.title", "&8Hoppers Menu"));
        if (!event.getView().getTitle().equals(title)) return;

        event.setCancelled(true);

        if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) return;

        Player player = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();
        ItemMeta meta = clickedItem.getItemMeta();

        if (meta != null && meta.getPersistentDataContainer().has(typeKey, PersistentDataType.STRING)) {
            String type = meta.getPersistentDataContainer().get(typeKey, PersistentDataType.STRING);

            player.getInventory().addItem(plugin.getHopperItem(type, 1));
            player.sendMessage(colorize(plugin.getConfig().getString("messages.received")));
            playSound(player);
        }
    }

    private void playSound(Player player) {
        if (plugin.getConfig().getBoolean("effects.sound.enabled")) {
            try {
                String s = plugin.getConfig().getString("effects.sound.type", "UI_BUTTON_CLICK");
                player.playSound(player.getLocation(), Sound.valueOf(s), 1f, 1f);
            } catch (Exception ignored) {}
        }
    }

    private String colorize(String message) {
        if (message == null) return "";
        Matcher matcher = HEX_PATTERN.matcher(message);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            try {
                String hexCode = matcher.group(1);
                matcher.appendReplacement(buffer, net.md_5.bungee.api.ChatColor.of("#" + hexCode).toString());
            } catch (Exception ignored) {}
        }
        matcher.appendTail(buffer);
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', buffer.toString());
    }
}