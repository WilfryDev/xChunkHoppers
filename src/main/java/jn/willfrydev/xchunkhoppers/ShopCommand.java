/*
 * MIT License
 *
 * Copyright (c) 2025 xPlugins x WillfryDev
 */

package jn.willfrydev.xchunkhoppers;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ShopCommand extends Command {

    private final GUI gui;

    public ShopCommand(String name, List<String> aliases, GUI gui) {
        super(name);
        this.setAliases(aliases);
        this.setDescription("Abre la tienda de tolvas");
        this.setUsage("/" + name);
        this.gui = gui;
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
        if (sender instanceof Player p) {
            gui.openShopMenu(p);
            return true;
        }
        sender.sendMessage("Este comando solo es para jugadores.");
        return false;
    }
}