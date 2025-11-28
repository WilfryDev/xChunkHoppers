<div align="center">

# 🚀 xChunkHoppers
### The Ultimate Chunk Collector Plugin

![Java](https://img.shields.io/badge/Java-17%2B-orange?style=for-the-badge&logo=java)
![Spigot](https://img.shields.io/badge/Spigot-1.16--1.21-yellow?style=for-the-badge&logo=spigot)
![License](https://img.shields.io/badge/License-MIT-blue?style=for-the-badge)
![Author](https://img.shields.io/badge/Author-xPlugins%20x%20WillfryDev-purple?style=for-the-badge)

</div>

---

## 📋 Description

**xChunkHoppers** is the ultimate solution for Survival, SkyBlock, and Factions servers. It allows you to create "Magic Hoppers" that automatically collect items within a specific radius or the entire Chunk without the need for water streams or lag.

Optimized for high performance, with support for **Hex Colors**, **Effects**, **GUI Menu**, and a **complete API** for developers.

## ✨ Key Features

* **🛡️ Extreme Performance:** Smart caching and `PersistentDataContainer` (PDC). Zero lag.
* **📡 Multi-Types:** Create infinite hopper types (VIP, GOD, BASIC) with configurable ranges (8x8, 64x64, Full Chunk).
* **🎨 Premium Design:** Full support for HEX colors (`&#RRGGBB`) and gradients.
* **🖥️ GUI Menu:** Visual admin panel to obtain items (`/xch menu`).
* **💾 Data Persistence:** `data.yml` system to save locations across restarts.
* **🎵 Visual Effects:** Fully configurable sounds and particles when placing blocks.
* **🔌 Developer API:** Simple API to integrate with other plugins.
* **⚙️ Filtering:** Whitelist or blacklist of materials (e.g., only Cactus and Iron).

---

## 📥 Installation

1.  Download `xChunkHoppers.jar` from the releases page.
2.  Place it in your server's `/plugins/` folder.
3.  (Optional) Install **PlaceholderAPI** to use variables.
4.  Restart the server.
5.  Configure the hopper types in `config.yml` and enjoy!

---

## 🎥 Showcase Video

[![xChunkHoppers Showcase](https://img.youtube.com/vi/-T7Z5SKMWpQ/0.jpg)](https://www.youtube.com/watch?v=-T7Z5SKMWpQ)
> *Click the image above to watch the showcase video.*

---

## 💻 Commands and Permissions

| Command | Description | Permission |
| :--- | :--- | :--- |
| `/xch give <player> <type> [amount]` | Gives a specific ChunkHopper to a player. | `xchunkhoppers.admin` |
| `/xch menu` | Opens the GUI menu with all hoppers. | `xchunkhoppers.admin` |
| `/xch reload` | Reloads the configuration and database. | `xchunkhoppers.admin` |

### Other Permissions
* `xchunkhoppers.place` » Allows placing Chunk Hoppers.
* `xchunkhoppers.break` » Allows breaking and retrieving Chunk Hoppers.

---

## 🛠️ Configuration

<details>
<summary>📄 Click to view the default config.yml</summary>

```yaml
#         ___ _                 _
#__  __ / __\ |__  _   _ _ __ | | __ /\  /\___  _ __  _ __   ___ _ __ ___
#\ \/ // /  | '_ \| | | | '_ \| |/ // /_/ / _ \| '_ \| '_ \ / _ \ '__/ __|
# >  </ /___| | | | |_| | | | |   </ __  / (_) | |_) | |_) |  __/ |  \__ \
#/_/\_\____/|_| |_|\__,_|_| |_|_|\_\/ /_/ \___/| .__/| .__/ \___|_|  |___/
#                                               |_|    |_|
#                 Web: [https://xplugin.es](https://xplugin.es)
#           Wiki: [https://xplugin.es/xchunkhoppers](https://xplugin.es/xchunkhoppers)
#             Discord: [https://discord.xplugin.es](https://discord.xplugin.es)

settings:
  # Enable or disable full plugin functionality
  enabled: true

  # If true: Only collects items in 'filter-list'.
  # If false: Collects everything EXCEPT items in 'filter-list'.
  use-whitelist: false

  # List of materials (Use Bukkit Material enum names)
  filter-list:
    - "COBBLESTONE"
    - "DIRT"
    - "ROTTEN_FLESH"

# GUI Menu
menu:
  title: "&#B3B3B3Admin hoppers menu"
  size: 27
  fill:
    enabled: true
    material: "BLACK_STAINED_GLASS_PANE" # Material to fill empty slots

# Hoppers
hopper-types:
  default:
    radius: -1
    name: "&#00ff00Chunk Hopper &7(16x16)"
    lore:
      - "&8 Special Item"
      - ""
      - "&f Place this block to"
      - "&f collect all items"
      - "&f in this chunk automatically."
      - ""
      - "&eUnique Chunk!"
      - ""
  vip:
    radius: 4
    name: "&#ffaa00Hopper &lVIP &7(8x8)"
    lore:
      - "&8 VIP Item"
      - ""
      - "&f Place this block to"
      - "&f collect all items"
      - "&f in this chunk automatically."
      - ""
      - "&eUnique Chunk!"
      - ""
  god:
    radius: 32
    name: "&#ff0000Hopper &lGOD &7(64x64)"
    lore:
      - "&8 GOD Item"
      - ""
      - "&f Place this block to"
      - "&f collect all items"
      - "&f in this chunk automatically."
      - ""
      - "&eUnique Chunk!"
      - ""

# Configuration of effects when placing the Hopper and using the Menu
effects:
  sound:
    enabled: true
    # Sound names: [https://hub.spigotmc.org/javadocs/spigot/org/bukkit/Sound.html](https://hub.spigotmc.org/javadocs/spigot/org/bukkit/Sound.html)
    type: "ENTITY_PLAYER_LEVELUP"
    volume: 1.0
    pitch: 1.0
  particles:
    enabled: true
    type: "VILLAGER_HAPPY"
    count: 15

permissions:
  admin: "xchunkhoppers.admin"
  place: "xchunkhoppers.place"
  break: "xchunkhoppers.break"

messages:
  prefix: "&8[&d&lxCH&8] "
  no-permission: "&cYou do not have permission to do this."
  reload: "&#00ff00Configuration and Data reloaded successfully."
  player-not-found: "&cPlayer not found."
  give-success: "&aYou gave &e%amount% &f%type% &a to &f%player%&a."
  received: "&aYou have received a &dChunk Hopper&a."
  placed: "&#00ff00Chunk Hopper placed! It will now collect items in this range."
  broken: "&eYou have removed the Chunk Hopper."
  already-exists: "&cA Chunk Hopper already exists here."
  type-not-found: "&cThat Hopper type does not exist in the config."

version: 1.0.0
```
</details>

---

## 🧩 Developer API

To use the API in your plugin, add `xChunkHoppers` as a dependency in your `plugin.yml`.

### Maven / Gradle
Add the jar as a local library to your project.

### Usage Example
```java
import jn.willfrydev.xchunkhoppers.xChunkHoppers;
import jn.willfrydev.xchunkhoppers.api.ChunkHopperAPI;
import org.bukkit.Chunk;
import org.bukkit.plugin.java.JavaPlugin;

public class YourPlugin extends JavaPlugin {
    
    public void checkHopper(Chunk chunk) {
        ChunkHopperAPI api = xChunkHoppers.getAPI();

        if (api.hasHopper(chunk)) {
            getLogger().info("There is a magic hopper in this chunk!");
            
            // Get location and perform actions
            Location loc = api.getHopperLocation(chunk);
        }
    }
}
```

---

<div align="center">
  
  Made with ❤️ by **WillfryDev** for the community.  
  _© 2025 xPlugins_

</div>
