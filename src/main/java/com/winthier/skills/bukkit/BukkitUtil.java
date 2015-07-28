package com.winthier.skills.bukkit;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

class BukkitUtil
{
    static void consoleCommand(String cmd)
    {
        Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), cmd);
    }

    static void title(Player player, String title, String subtitle)
    {
        consoleCommand(String.format("minecraft:title %s subtitle %s", player.getName(), subtitle));
        consoleCommand(String.format("minecraft:title %s title %s", player.getName(), title));
    }
}
