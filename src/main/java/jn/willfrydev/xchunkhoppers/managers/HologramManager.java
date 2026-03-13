package jn.willfrydev.xchunkhoppers.managers;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Display;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.TextDisplay;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.bukkit.ChatColor;
import me.clip.placeholderapi.PlaceholderAPI;

import jn.willfrydev.xchunkhoppers.xChunkHoppers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HologramManager {

    private final xChunkHoppers plugin;
    private final Map<Location, TextDisplay> holograms = new HashMap<>();
    private final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private final boolean hasPAPI;

    public HologramManager(xChunkHoppers plugin) {
        this.plugin = plugin;
        this.hasPAPI = Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;
        startHologramUpdater();
    }

    public void spawnHologram(Location loc, xChunkHoppers.HopperData data) {
        String path = "hopper-types." + data.getType() + ".hologram.";
        if (!plugin.getConfig().getBoolean(path + "enabled", true)) return;

        double heightOffset = plugin.getConfig().getDouble(path + "height", 1.5);
        Location spawnLoc = loc.clone().add(0.5, heightOffset, 0.5);

        TextDisplay display = (TextDisplay) loc.getWorld().spawnEntity(spawnLoc, EntityType.TEXT_DISPLAY);

        String billboardType = plugin.getConfig().getString(path + "billboard", "CENTER").toUpperCase();
        try { display.setBillboard(Display.Billboard.valueOf(billboardType)); } catch (Exception e) { display.setBillboard(Display.Billboard.CENTER); }

        display.setShadowed(plugin.getConfig().getBoolean(path + "shadow", true));
        display.setViewRange((float) plugin.getConfig().getDouble(path + "view-distance", 20.0));
        display.setBackgroundColor(parseColor(plugin.getConfig().getString(path + "background-color", "50,0,0,0")));

        try {
            float scale = (float) plugin.getConfig().getDouble(path + "scale", 1.0);
            Transformation transformation = display.getTransformation();
            transformation.getScale().set(scale);
            display.setTransformation(transformation);
        } catch (Exception ignored) {}

        updateHologramText(display, data);
        holograms.put(loc, display);
    }

    private Color parseColor(String colorStr) {
        if (colorStr.equalsIgnoreCase("transparent")) return Color.fromARGB(0, 0, 0, 0);
        switch (colorStr.toUpperCase()) {
            case "RED": return Color.fromARGB(100, 255, 0, 0);
            case "BLUE": return Color.fromARGB(100, 0, 0, 255);
            case "GREEN": return Color.fromARGB(100, 0, 255, 0);
            case "BLACK": return Color.fromARGB(100, 0, 0, 0);
            case "WHITE": return Color.fromARGB(100, 255, 255, 255);
            case "YELLOW": return Color.fromARGB(100, 255, 255, 0);
        }
        try {
            String[] rgba = colorStr.split(",");
            if (rgba.length == 4) return Color.fromARGB(Integer.parseInt(rgba[0].trim()), Integer.parseInt(rgba[1].trim()), Integer.parseInt(rgba[2].trim()), Integer.parseInt(rgba[3].trim()));
        } catch (Exception ignored) {}
        return Color.fromARGB(50, 0, 0, 0);
    }

    private void updateHologramText(TextDisplay display, xChunkHoppers.HopperData data) {
        List<String> lines = plugin.getConfig().getStringList("hopper-types." + data.getType() + ".hologram.lines");
        StringBuilder finalText = new StringBuilder();
        OfflinePlayer offlineOwner = data.getOwner() != null ? Bukkit.getOfflinePlayer(data.getOwner()) : null;

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).replace("%player%", data.getOwnerName() != null ? data.getOwnerName() : "Desconocido")
                    .replace("%items%", String.valueOf(data.getCollectedItems()))
                    .replace("%type%", data.getType().toUpperCase());
            if (hasPAPI) line = PlaceholderAPI.setPlaceholders(offlineOwner, line);
            finalText.append(colorize(line));
            if (i < lines.size() - 1) finalText.append("\n");
        }
        display.setText(finalText.toString());
    }

    private void startHologramUpdater() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (holograms.isEmpty()) return;
                holograms.entrySet().removeIf(entry -> {
                    Location loc = entry.getKey();
                    TextDisplay display = entry.getValue();
                    if (display == null || display.isDead()) return true;
                    xChunkHoppers.HopperData data = plugin.getHopperCache().get(loc); // YA NO DA ERROR
                    if (data != null) updateHologramText(display, data);
                    return false;
                });
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    public void removeHologram(Location loc) {
        TextDisplay display = holograms.remove(loc);
        if (display != null && !display.isDead()) display.remove();
    }

    public void removeAll() {
        for (TextDisplay display : holograms.values()) if (display != null && !display.isDead()) display.remove();
        holograms.clear();
    }

    private String colorize(String message) {
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
}