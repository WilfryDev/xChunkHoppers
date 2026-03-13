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
import java.util.UUID;
import java.util.logging.Level;

public class DataManager {

    private final xChunkHoppers plugin;
    private File file;
    private FileConfiguration dataConfig;

    public DataManager(xChunkHoppers plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "data.yml");
        if (!file.exists()) {
            try { file.createNewFile(); } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "No se pudo crear data.yml", e);
            }
        }
        this.dataConfig = YamlConfiguration.loadConfiguration(file);
        migrateOldData();
    }

    public void saveData() {
        try { dataConfig.save(file); } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "No se pudo guardar data.yml", e);
        }
    }

    public void addHopper(Location loc, xChunkHoppers.HopperData data) {
        String locStr = locToString(loc);
        dataConfig.set("hoppers." + locStr + ".type", data.getType());
        dataConfig.set("hoppers." + locStr + ".owner", data.getOwner() != null ? data.getOwner().toString() : null);
        dataConfig.set("hoppers." + locStr + ".ownerName", data.getOwnerName());
        dataConfig.set("hoppers." + locStr + ".placedTime", data.getPlacedTime());
        dataConfig.set("hoppers." + locStr + ".collectedItems", data.getCollectedItems());
        saveData();
    }

    public void removeHopper(Location loc) {
        String locStr = locToString(loc);
        dataConfig.set("hoppers." + locStr, null);
        saveData();
    }

    public Map<Location, xChunkHoppers.HopperData> loadHoppers() {
        Map<Location, xChunkHoppers.HopperData> loaded = new HashMap<>();
        if (dataConfig.getConfigurationSection("hoppers") == null) return loaded;

        for (String key : dataConfig.getConfigurationSection("hoppers").getKeys(false)) {
            Location loc = stringToLoc(key);
            if (loc == null) continue;

            String type = dataConfig.getString("hoppers." + key + ".type");
            String uuidStr = dataConfig.getString("hoppers." + key + ".owner");
            UUID owner = null;
            if (uuidStr != null) {
                try { owner = UUID.fromString(uuidStr); } catch (Exception ignored) {}
            }
            String ownerName = dataConfig.getString("hoppers." + key + ".ownerName", "Desconocido");
            long placedTime = dataConfig.getLong("hoppers." + key + ".placedTime", System.currentTimeMillis());
            int collectedItems = dataConfig.getInt("hoppers." + key + ".collectedItems", 0);

            if (type != null) {
                loaded.put(loc, new xChunkHoppers.HopperData(type, owner, ownerName, placedTime, collectedItems));
            }
        }
        return loaded;
    }

    private void migrateOldData() {
        if (dataConfig.getConfigurationSection("hoppers") == null) return;
        boolean changed = false;
        for (String key : dataConfig.getConfigurationSection("hoppers").getKeys(false)) {
            if (dataConfig.isString("hoppers." + key)) {
                String type = dataConfig.getString("hoppers." + key);
                dataConfig.set("hoppers." + key, null);
                dataConfig.set("hoppers." + key + ".type", type);
                dataConfig.set("hoppers." + key + ".ownerName", "Dueño Antiguo");
                dataConfig.set("hoppers." + key + ".placedTime", System.currentTimeMillis());
                dataConfig.set("hoppers." + key + ".collectedItems", 0);
                changed = true;
            }
        }
        if (changed) saveData();
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
        return new Location(w, Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3]));
    }
}