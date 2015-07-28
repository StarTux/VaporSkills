package com.winthier.skills.bukkit;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
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

    static String format(String msg, Object... args)
    {
        msg = ChatColor.translateAlternateColorCodes('&', msg);
        if (args.length > 0) msg = String.format(msg, args);
        return msg;
    }

    static void msg(CommandSender sender, String msg, Object... args)
    {
        sender.sendMessage(format(msg, args));
    }

    static void announce(String msg, Object... args)
    {
        msg = format(msg, args);
        for (Player player : Bukkit.getServer().getOnlinePlayers()) {
            player.sendMessage(msg);
        }
    }
}
