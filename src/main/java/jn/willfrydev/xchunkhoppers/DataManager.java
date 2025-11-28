package jn.willfrydev.xchunkhoppers;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class DataManager {

    private final xChunkHoppers plugin;
    private File file;
    private FileConfiguration dataConfig;

    public DataManager(xChunkHoppers plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "data.yml");
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "No se pudo crear data.yml", e);
            }
        }
        this.dataConfig = YamlConfiguration.loadConfiguration(file);
    }

    public void saveData() {
        try {
            dataConfig.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "No se pudo guardar data.yml", e);
        }
    }

    public void addHopper(Location loc, String type) {
        String locStr = locToString(loc);
        dataConfig.set("hoppers." + locStr, type);
        saveData();
    }

    public void removeHopper(Location loc) {
        String locStr = locToString(loc);
        dataConfig.set("hoppers." + locStr, null);
        saveData();
    }

    public Map<Location, String> loadHoppers() {
        Map<Location, String> loaded = new HashMap<>();
        if (dataConfig.getConfigurationSection("hoppers") == null) return loaded;

        for (String key : dataConfig.getConfigurationSection("hoppers").getKeys(false)) {
            String type = dataConfig.getString("hoppers." + key);
            Location loc = stringToLoc(key);
            if (loc != null && type != null) {
                loaded.put(loc, type);
            }
        }
        return loaded;
    }

    private String locToString(Location loc) {
        if (loc.getWorld() == null) return "";
        return loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
    }

    private Location stringToLoc(String str) {
        String[] parts = str.split(",");
        if (parts.length != 4) return null;
        World w = Bukkit.getWorld(parts[0]);
        if (w == null) return null;
        int x = Integer.parseInt(parts[1]);
        int y = Integer.parseInt(parts[2]);
        int z = Integer.parseInt(parts[3]);
        return new Location(w, x, y, z);
    }
}