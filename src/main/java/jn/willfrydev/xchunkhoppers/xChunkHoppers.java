/*
 * MIT License
 *
 * Copyright (c) 2025 xPlugins
 */

package jn.willfrydev.xchunkhoppers;

import jn.willfrydev.xchunkhoppers.api.ChunkHopperAPI;
import jn.willfrydev.xchunkhoppers.managers.HologramManager;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.milkbowl.vault.economy.Economy;
import org.bstats.bukkit.Metrics;
import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.command.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class xChunkHoppers extends JavaPlugin implements Listener, CommandExecutor, TabCompleter, ChunkHopperAPI {

    private static xChunkHoppers instance;
    private NamespacedKey typeKey;
    private final Map<Location, HopperData> hopperCache = new HashMap<>();
    private final Map<String, Integer> typeRadiusMap = new HashMap<>();
    private static Economy econ = null;

    private DataManager dataManager;
    private HologramManager hologramManager;
    private GUI menuGUI;
    private final Map<String, org.bukkit.command.Command> dynamicCommands = new HashMap<>();

    private List<String> filterList;
    private boolean useWhitelist;
    private boolean isEnabled;

    private final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

    public static ChunkHopperAPI getAPI() { return instance; }

    public Map<Location, HopperData> getHopperCache() {
        return hopperCache;
    }

    public static class HopperData {
        private final String type;
        private final UUID owner;
        private final String ownerName;
        private final long placedTime;
        private int collectedItems;

        public HopperData(String type, UUID owner, String ownerName, long placedTime, int collectedItems) {
            this.type = type;
            this.owner = owner;
            this.ownerName = ownerName;
            this.placedTime = placedTime;
            this.collectedItems = collectedItems;
        }

        public String getType() { return type; }
        public UUID getOwner() { return owner; }
        public String getOwnerName() { return ownerName; }
        public long getPlacedTime() { return placedTime; }
        public int getCollectedItems() { return collectedItems; }
        public void addCollectedItems(int amount) { this.collectedItems += amount; }
    }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        if (!setupEconomy()) {
            getLogger().warning("Vault no detectado. La tienda no funcionara sin economia.");
        }

        dataManager = new DataManager(this);
        hologramManager = new HologramManager(this);
        typeKey = new NamespacedKey(this, "hopper_type");

        menuGUI = new GUI(this);
        getServer().getPluginManager().registerEvents(menuGUI, this);

        loadConfiguration();

        getServer().getPluginManager().registerEvents(this, this);

        registerCommand("xchunkhopper");

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new HopperExpansion(this).register();
        }

        try { new Metrics(this, 30087); } catch (Exception ignored) {}

        for (Map.Entry<Location, HopperData> entry : hopperCache.entrySet()) {
            hologramManager.spawnHologram(entry.getKey(), entry.getValue());
        }

        log(colorize("&a&lxChunkHoppers &bActivado (v" + getDescription().getVersion() + ") &fby WillfryDev"));
    }

    public void log(String msg) {
        Bukkit.getConsoleSender().sendMessage(colorize("&8[&d&lxCH&8] " + msg));
    }

    private void registerCommand(String name) {

        PluginCommand cmd = getCommand(name);
        if (cmd != null) {
            cmd.setExecutor(this);
            cmd.setTabCompleter(this);
        }
    }

    private boolean setupEconomy() {

        if (getServer().getPluginManager().getPlugin("Vault") == null) return false;
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) return false;
        econ = rsp.getProvider();
        return econ != null;
    }

    public static Economy getEconomy() { return econ; }

    @Override
    public void onDisable() {
        unregisterDynamicCommands();
        if (hologramManager != null) hologramManager.removeAll();
        if (dataManager != null) dataManager.saveData();
        log(colorize("&c&lxChunkHoppers Desactivado."));
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
                typeRadiusMap.put(key, section.getInt(key + ".radius", -1));
            }
        }
        hopperCache.clear();
        hopperCache.putAll(dataManager.loadHoppers());

        registerDynamicCommands();
    }

    private void registerDynamicCommands() {
        unregisterDynamicCommands();
        List<String> commands = getConfig().getStringList("shop-menu.commands");
        if (commands.isEmpty()) return;

        try {
            final java.lang.reflect.Field bukkitCommandMap = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            bukkitCommandMap.setAccessible(true);
            org.bukkit.command.CommandMap commandMap = (org.bukkit.command.CommandMap) bukkitCommandMap.get(Bukkit.getServer());

            for (String cmdName : commands) {
                DynamicCommand cmd = new DynamicCommand(cmdName, menuGUI);
                commandMap.register(getName().toLowerCase(), cmd);
                dynamicCommands.put(cmdName.toLowerCase(), cmd);
            }
            log("&aComandos de tienda registrados: " + commands);
        } catch (Exception e) {
            getLogger().severe("No se pudo registrar comandos dinámicos usando Reflection.");
        }
    }

    private void unregisterDynamicCommands() {
        if (dynamicCommands.isEmpty()) return;
        try {
            final java.lang.reflect.Field bukkitCommandMap = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            bukkitCommandMap.setAccessible(true);
            org.bukkit.command.CommandMap commandMap = (org.bukkit.command.CommandMap) bukkitCommandMap.get(Bukkit.getServer());

            final java.lang.reflect.Field knownCommandsField = commandMap.getClass().getDeclaredField("knownCommands");
            knownCommandsField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, org.bukkit.command.Command> knownCommands = (Map<String, org.bukkit.command.Command>) knownCommandsField.get(commandMap);

            for (Map.Entry<String, org.bukkit.command.Command> entry : dynamicCommands.entrySet()) {
                knownCommands.remove(entry.getKey());
                entry.getValue().unregister(commandMap);
            }
            dynamicCommands.clear();
        } catch (Exception e) {
            getLogger().warning("No se pudo desregistrar comandos dinámicos antiguos.");
        }
    }

    public String colorize(String message) {
        if (message == null) return "";
        Matcher matcher = HEX_PATTERN.matcher(message);
        StringBuilder buffer = new StringBuilder();
        while (matcher.find()) {
            try {
                String hexCode = matcher.group(1);
                matcher.appendReplacement(buffer, net.md_5.bungee.api.ChatColor.of("#" + hexCode).toString());
            } catch (Exception ignored) {}
        }
        matcher.appendTail(buffer);
        return ChatColor.translateAlternateColorCodes('&', buffer.toString());
    }

    public String getMsg(String key) {
        String msg = getConfig().getString("messages." + key);
        String prefix = getConfig().getString("messages.prefix", "");
        return colorize(prefix + (msg != null ? msg : key));
    }

    @Override public boolean isChunkHopper(Block block) { return hopperCache.containsKey(block.getLocation()); }
    @Override public Location getHopperLocation(Chunk chunk) { return null; }
    @Override public boolean hasHopper(Chunk chunk) { return false; }

    @Override
    public ItemStack getHopperItem(String type, int amount) {
        if (!typeRadiusMap.containsKey(type)) return new ItemStack(Material.HOPPER, amount);
        ItemStack item = new ItemStack(Material.HOPPER, amount);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            ConfigurationSection s = getConfig().getConfigurationSection("hopper-types." + type);
            if (s != null) {
                meta.setDisplayName(colorize(s.getString("name", "&dHopper")));
                meta.setLore(s.getStringList("lore").stream().map(this::colorize).collect(Collectors.toList()));
                meta.getPersistentDataContainer().set(typeKey, PersistentDataType.STRING, type);
                item.setItemMeta(meta);
            }
        }
        return item;
    }
    @Override public ItemStack getHopperItem(int amount) { return getHopperItem("default", amount); }
    @Override public Collection<String> getFilterList() { return Collections.unmodifiableList(filterList); }
    @Override public String getHopperType(Location loc) { return hopperCache.containsKey(loc) ? hopperCache.get(loc).getType() : null; }
    @Override public UUID getHopperOwner(Location loc) { return hopperCache.containsKey(loc) ? hopperCache.get(loc).getOwner() : null; }

    @Override
    public void createHopper(Location loc, String type, UUID owner, String ownerName) {
        HopperData data = new HopperData(type, owner, ownerName, System.currentTimeMillis(), 0);
        dataManager.addHopper(loc, data);
        hopperCache.put(loc, data);
        hologramManager.spawnHologram(loc, data);
    }

    @Override
    public void deleteHopper(Location loc) {
        dataManager.removeHopper(loc);
        hopperCache.remove(loc);
        hologramManager.removeHologram(loc);
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {

        if (!isEnabled) return;
        ItemStack item = event.getItemInHand();
        if (item.getType() == Material.HOPPER && item.hasItemMeta()) {
            String type = item.getItemMeta().getPersistentDataContainer().get(typeKey, PersistentDataType.STRING);
            if (type != null) {
                createHopper(event.getBlock().getLocation(), type, event.getPlayer().getUniqueId(), event.getPlayer().getName());
                event.getPlayer().sendMessage(getMsg("placed"));
            }
        }
    }

    @EventHandler

    public void onBlockBreak(BlockBreakEvent event) {

        Location loc = event.getBlock().getLocation();
        if (hopperCache.containsKey(loc)) {
            String type = getHopperType(loc);
            deleteHopper(loc);
            event.setDropItems(false);
            loc.getWorld().dropItemNaturally(loc, getHopperItem(type, 1));
            event.getPlayer().sendMessage(getMsg("broken"));
        }
    }

    @EventHandler

    public void onItemSpawn(ItemSpawnEvent event) {
        if (!isEnabled) return;
        Location itemLoc = event.getLocation();
        for (Map.Entry<Location, HopperData> entry : hopperCache.entrySet()) {
            Location hLoc = entry.getKey();
            if (!hLoc.getWorld().equals(itemLoc.getWorld())) continue;

            int radius = typeRadiusMap.getOrDefault(entry.getValue().getType(), -1);
            boolean collect;

            if (radius == -1) {
                collect = hLoc.getChunk().equals(itemLoc.getChunk());
            } else {

                double dx = Math.abs(hLoc.getX() - itemLoc.getX());
                double dz = Math.abs(hLoc.getZ() - itemLoc.getZ());
                collect = dx <= radius && dz <= radius;
            }

            if (collect && hLoc.getBlock().getState() instanceof Hopper h) {
                ItemStack stack = event.getEntity().getItemStack();
                if ((useWhitelist && !filterList.contains(stack.getType().name())) || (!useWhitelist && filterList.contains(stack.getType().name()))) return;

                event.setCancelled(true);
                h.getInventory().addItem(stack);
                entry.getValue().addCollectedItems(stack.getAmount());
                return;
            }
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null && event.getClickedBlock().getType() == Material.HOPPER) {
            Location loc = event.getClickedBlock().getLocation();
            if (hopperCache.containsKey(loc) && event.getPlayer().isSneaking()) {
                event.setCancelled(true);
                menuGUI.openInfoMenu(event.getPlayer(), loc, hopperCache.get(loc));
            }
        }
    }
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sender.sendMessage(colorize("&#fb00ff&lxChunkHoppers &7v" + getDescription().getVersion()));
            sender.sendMessage(colorize("&7/xch menu &8- &fMenu Admin"));
            if (sender.hasPermission("xchunkhoppers.admin")) {
                sender.sendMessage(colorize("&7/xch reload &8- &fRecargar plugin"));
            }
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload" -> {
                if (sender.hasPermission("xchunkhoppers.admin")) {
                    loadConfiguration();
                    sender.sendMessage(getMsg("reload"));
                } else {
                    sender.sendMessage(getMsg("no-permission"));
                }
            }
            case "menu" -> {
                if (sender instanceof Player p && p.hasPermission("xchunkhoppers.admin")) {
                    menuGUI.openAdminMenu(p);
                } else if (!sender.hasPermission("xchunkhoppers.admin")) {
                    sender.sendMessage(getMsg("no-permission"));
                }
            }
            case "give" -> {
                if (sender.hasPermission("xchunkhoppers.admin") && args.length >= 3) {
                    Player t = Bukkit.getPlayer(args[1]);
                    if (t != null) {
                        int amount = args.length > 3 ? Integer.parseInt(args[3]) : 1;
                        t.getInventory().addItem(getHopperItem(args[2], amount));

                        // Mensaje al admin
                        sender.sendMessage(getMsg("give-success")
                                .replace("%player%", t.getName())
                                .replace("%type%", args[2])
                                .replace("%amount%", String.valueOf(amount)));

                        // Mensaje al jugador
                        t.sendMessage(getMsg("received"));
                    } else {
                        sender.sendMessage(getMsg("player-not-found"));
                    }
                } else if (!sender.hasPermission("xchunkhoppers.admin")) {
                    sender.sendMessage(getMsg("no-permission"));
                } else {
                    sender.sendMessage(colorize("&cUso: /xch give <jugador> <tipo> [cantidad]"));
                }
            }
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            if (sender.hasPermission("xchunkhoppers.admin")) {
                completions.addAll(Arrays.asList("reload", "menu", "give"));
            }
            List<String> shopCommands = getConfig().getStringList("shop-menu.commands");
            if (!shopCommands.isEmpty()) completions.add(shopCommands.get(0));
        } else if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            return null;
        } else if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            completions.addAll(typeRadiusMap.keySet());
        }
        return completions.stream().filter(s -> s.startsWith(args[args.length-1].toLowerCase())).collect(Collectors.toList());
    }

    public static class HopperExpansion extends PlaceholderExpansion {
        private final xChunkHoppers plugin;
        public HopperExpansion(xChunkHoppers plugin) { this.plugin = plugin; }
        @Override public @NotNull String getIdentifier() { return "xchunkhopper"; }
        @Override public @NotNull String getAuthor() { return "WillfryDev"; }
        @Override public @NotNull String getVersion() { return "1.0.3"; }
        @Override public boolean persist() { return true; }
        @Override public String onPlaceholderRequest(Player player, @NotNull String params) {
            return params.equalsIgnoreCase("count") ? String.valueOf(plugin.hopperCache.size()) : null;
        }
    }

    private class DynamicCommand extends org.bukkit.command.Command {
        private final GUI gui;
        protected DynamicCommand(@NotNull String name, GUI gui) {
            super(name);
            this.setAliases(new ArrayList<>());
            this.setDescription("Abre la tienda de tolvas");
            this.setUsage("/" + name);
            this.gui = gui;
        }
        @Override
        public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
            if (sender instanceof Player p) { gui.openShopMenu(p); return true; }
            sender.sendMessage("Este comando solo es para jugadores."); return false;
        }
        @Override
        public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) throws IllegalArgumentException {
            return new ArrayList<>();
        }
    }
}