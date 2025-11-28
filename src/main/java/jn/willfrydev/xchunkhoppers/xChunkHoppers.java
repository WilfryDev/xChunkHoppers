/*
 * MIT License
 *
 * Copyright (c) 2025 xPlugins x WillfryDev
 */

package jn.willfrydev.xchunkhoppers;

import jn.willfrydev.xchunkhoppers.api.ChunkHopperAPI;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.command.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * xChunkHoppers Core Logic - Menu Edition
 * Author: xPlugins x WillfryDev
 */
public class xChunkHoppers extends JavaPlugin implements Listener, CommandExecutor, TabCompleter, ChunkHopperAPI {

    private static xChunkHoppers instance;
    private NamespacedKey typeKey;
    private final Map<Location, String> hopperCache = new HashMap<>();
    private final Map<String, Integer> typeRadiusMap = new HashMap<>();

    private DataManager dataManager;
    private GUI menuGUI; // Instancia del menú

    private List<String> filterList;
    private boolean useWhitelist;
    private boolean isEnabled;

    private final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

    public static ChunkHopperAPI getAPI() { return instance; }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        dataManager = new DataManager(this);
        typeKey = new NamespacedKey(this, "hopper_type");

        loadConfiguration();

        // Registrar Eventos
        getServer().getPluginManager().registerEvents(this, this);

        // Registrar GUI
        menuGUI = new GUI(this);
        getServer().getPluginManager().registerEvents(menuGUI, this);

        PluginCommand cmd = getCommand("xchunkhopper");
        if (cmd != null) {
            cmd.setExecutor(this);
            cmd.setTabCompleter(this);
        }

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new HopperExpansion(this).register();
        }

        log(colorize("&a&lxChunkHoppers &7(v" + getDescription().getVersion() + ") &bActivado."));
    }

    @Override
    public void onDisable() {
        dataManager.saveData();
        hopperCache.clear();
        log(colorize("&c&lxChunkHoppers Desactivado."));
    }

    private void log(String msg) {
        Bukkit.getConsoleSender().sendMessage("[xChunkHoppers] " + msg);
    }

    public void loadConfiguration() {
        reloadConfig();
        FileConfiguration config = getConfig();
        isEnabled = config.getBoolean("settings.enabled", true);
        useWhitelist = config.getBoolean("settings.use-whitelist", false);
        filterList = config.getStringList("settings.filter-list");

        typeRadiusMap.clear();
        ConfigurationSection section = config.getConfigurationSection("hopper-types");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                int radius = section.getInt(key + ".radius", -1);
                typeRadiusMap.put(key, radius);
            }
        }

        hopperCache.clear();
        hopperCache.putAll(dataManager.loadHoppers());
    }

    // --- UTILS ---
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
        return ChatColor.translateAlternateColorCodes('&', buffer.toString());
    }

    // --- API IMPL ---
    @Override
    public boolean isChunkHopper(Block block) { return hopperCache.containsKey(block.getLocation()); }

    @Override
    public Location getHopperLocation(Chunk chunk) {
        for (Map.Entry<Location, String> entry : hopperCache.entrySet()) {
            if (entry.getKey().getChunk().equals(chunk)) return entry.getKey();
        }
        return null;
    }

    @Override
    public boolean hasHopper(Chunk chunk) { return getHopperLocation(chunk) != null; }

    @Override
    public ItemStack getHopperItem(int amount) { return getHopperItem("default", amount); }

    public ItemStack getHopperItem(String type, int amount) {
        if (!typeRadiusMap.containsKey(type)) return new ItemStack(Material.HOPPER, amount);

        ItemStack item = new ItemStack(Material.HOPPER, amount);
        ItemMeta meta = item.getItemMeta();
        FileConfiguration config = getConfig();
        String path = "hopper-types." + type;

        if (meta != null) {
            String name = colorize(config.getString(path + ".name", "&dHopper"));
            meta.setDisplayName(name);
            List<String> lore = config.getStringList(path + ".lore").stream()
                    .map(this::colorize)
                    .collect(Collectors.toList());
            meta.setLore(lore);
            meta.getPersistentDataContainer().set(typeKey, PersistentDataType.STRING, type);
            item.setItemMeta(meta);
        }
        return item;
    }

    @Override
    public Collection<String> getFilterList() { return Collections.unmodifiableList(filterList); }

    private String getMsg(String key) {
        String msg = getConfig().getString("messages." + key);
        if (msg == null) return key;
        String prefix = getConfig().getString("messages.prefix", "");
        return colorize(prefix + msg);
    }

    // --- EVENTOS ---
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!isEnabled) return;
        ItemStack item = event.getItemInHand();
        if (item.getType() == Material.HOPPER && item.getItemMeta() != null) {
            PersistentDataContainer container = item.getItemMeta().getPersistentDataContainer();
            if (container.has(typeKey, PersistentDataType.STRING)) {
                String type = container.get(typeKey, PersistentDataType.STRING);
                if (!event.getPlayer().hasPermission(getConfig().getString("permissions.place"))) {
                    event.getPlayer().sendMessage(getMsg("no-permission"));
                    event.setCancelled(true);
                    return;
                }
                Location loc = event.getBlock().getLocation();
                dataManager.addHopper(loc, type);
                hopperCache.put(loc, type);
                String typeName = getConfig().getString("hopper-types." + type + ".name", type);
                event.getPlayer().sendMessage(getMsg("placed").replace("%type%", colorize(typeName)));
                playEffects(loc);
            }
        }
    }

    private void playEffects(Location location) {
        FileConfiguration config = getConfig();
        Location center = location.clone().add(0.5, 1.0, 0.5);
        if (config.getBoolean("effects.sound.enabled")) {
            try {
                String s = config.getString("effects.sound.type");
                location.getWorld().playSound(location, Sound.valueOf(s), 1f, 1f);
            } catch (Exception e) {}
        }
        if (config.getBoolean("effects.particles.enabled")) {
            try {
                String p = config.getString("effects.particles.type");
                location.getWorld().spawnParticle(Particle.valueOf(p), center, config.getInt("effects.particles.count"));
            } catch (Exception e) {}
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (!isEnabled) return;
        if (hopperCache.containsKey(event.getBlock().getLocation())) {
            if (!event.getPlayer().hasPermission(getConfig().getString("permissions.break"))) {
                event.getPlayer().sendMessage(getMsg("no-permission"));
                event.setCancelled(true);
                return;
            }
            String type = hopperCache.get(event.getBlock().getLocation());
            dataManager.removeHopper(event.getBlock().getLocation());
            hopperCache.remove(event.getBlock().getLocation());
            event.setDropItems(false);
            event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), getHopperItem(type, 1));
            event.getPlayer().sendMessage(getMsg("broken"));
        }
    }

    @EventHandler
    public void onItemSpawn(ItemSpawnEvent event) {
        if (!isEnabled) return;
        Location itemLoc = event.getLocation();
        for (Map.Entry<Location, String> entry : hopperCache.entrySet()) {
            Location hopperLoc = entry.getKey();
            if (!hopperLoc.getWorld().equals(itemLoc.getWorld())) continue;

            String type = entry.getValue();
            int radius = typeRadiusMap.getOrDefault(type, -1);
            boolean shouldCollect = false;

            if (radius == -1) {
                if (hopperLoc.getChunk().equals(itemLoc.getChunk())) shouldCollect = true;
            } else {
                if (hopperLoc.distance(itemLoc) <= radius) shouldCollect = true;
            }

            if (shouldCollect) {
                Block block = hopperLoc.getBlock();
                if (block.getState() instanceof Hopper) {
                    Hopper hopper = (Hopper) block.getState();
                    ItemStack item = event.getEntity().getItemStack();
                    boolean inList = filterList.contains(item.getType().toString());
                    if ((useWhitelist && !inList) || (!useWhitelist && inList)) return;
                    event.setCancelled(true);
                    HashMap<Integer, ItemStack> leftover = hopper.getInventory().addItem(item);
                    if (!leftover.isEmpty()) {
                        for (ItemStack drop : leftover.values()) {
                            block.getWorld().dropItemNaturally(hopperLoc.add(0.5, 1.2, 0.5), drop);
                        }
                    }
                    return;
                } else {
                    dataManager.removeHopper(hopperLoc);
                    hopperCache.remove(hopperLoc);
                }
            }
        }
    }

    // --- COMANDOS ---
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission(getConfig().getString("permissions.admin"))) {
            sender.sendMessage(getMsg("no-permission"));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(colorize("&#fb00ff&lxChunkHoppers &7v" + getDescription().getVersion()));
            sender.sendMessage(colorize("&7/xch menu &8- &fAbrir menú"));
            sender.sendMessage(colorize("&7/xch give <player> <type> &8- &fDar item"));
            sender.sendMessage(colorize("&7/xch reload"));
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            loadConfiguration();
            sender.sendMessage(getMsg("reload"));
            return true;
        }

        if (args[0].equalsIgnoreCase("menu")) {
            if (sender instanceof Player) {
                menuGUI.openMenu((Player) sender);
            } else {
                sender.sendMessage(ChatColor.RED + "Solo para jugadores.");
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("give")) {
            if (args.length < 3) {
                sender.sendMessage(colorize("&cUso: /xch give <player> <type> [amount]"));
                return true;
            }
            Player target = Bukkit.getPlayer(args[1]);
            String type = args[2].toLowerCase();
            if (target == null) {
                sender.sendMessage(getMsg("player-not-found"));
                return true;
            }
            if (!typeRadiusMap.containsKey(type)) {
                sender.sendMessage(getMsg("type-not-found"));
                return true;
            }
            int amount = 1;
            if (args.length >= 4) {
                try { amount = Integer.parseInt(args[3]); } catch (NumberFormatException ignored) {}
            }
            target.getInventory().addItem(getHopperItem(type, amount));
            sender.sendMessage(getMsg("give-success").replace("%amount%", String.valueOf(amount)).replace("%type%", type).replace("%player%", target.getName()));
            return true;
        }
        return false;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (!sender.hasPermission("xchunkhoppers.admin")) return Collections.emptyList();
        if (args.length == 1) return Arrays.asList("give", "menu", "reload");
        if (args.length == 2 && args[0].equalsIgnoreCase("give")) return null;
        if (args.length == 3 && args[0].equalsIgnoreCase("give")) return new ArrayList<>(typeRadiusMap.keySet());
        return Collections.emptyList();
    }

    public static class HopperExpansion extends PlaceholderExpansion {
        private final xChunkHoppers plugin;
        public HopperExpansion(xChunkHoppers plugin) { this.plugin = plugin; }
        @Override public @NotNull String getIdentifier() { return "xchunkhopper"; }
        @Override public @NotNull String getAuthor() { return "WillfryDev"; }
        @Override public @NotNull String getVersion() { return plugin.getDescription().getVersion(); }
        @Override public boolean persist() { return true; }
        @Override public String onPlaceholderRequest(Player player, @NotNull String params) {
            if (params.equalsIgnoreCase("count")) return String.valueOf(plugin.hopperCache.size());
            return null;
        }
    }
}