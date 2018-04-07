package com.winthier.skills.bukkit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.json.simple.JSONValue;

final class BukkitUtil {
    private BukkitUtil() { }

    static void consoleCommand(String cmd, Object... args) {
        if (args.length > 0) cmd = String.format(cmd, args);
        // BukkitSkillsPlugin.getInstance().getLogger().info("Running console command: " + cmd);
        Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), cmd);
    }

    static String format(String msg, Object... args) {
        msg = ChatColor.translateAlternateColorCodes('&', msg);
        if (args.length > 0) msg = String.format(msg, args);
        return msg;
    }

    static void msg(CommandSender sender, String msg, Object... args) {
        sender.sendMessage(format(msg, args));
    }

    static void announce(String msg, Object... args) {
        msg = format(msg, args);
        for (Player player : Bukkit.getServer().getOnlinePlayers()) {
            player.sendMessage(msg);
        }
    }

    static void title(Player player, String title, String subtitle) {
        player.sendTitle(format(title), format(subtitle));
    }

    static void raw(Player player, Object... obj) {
        if (obj.length == 0) return;
        if (obj.length == 1) {
            consoleCommand("minecraft:tellraw %s %s", player.getName(), JSONValue.toJSONString(obj[0]));
        } else {
            consoleCommand("minecraft:tellraw %s %s", player.getName(), JSONValue.toJSONString(Arrays.asList(obj)));
        }
    }

    static void announceRaw(Object... obj) {
        for (Player player : Bukkit.getServer().getOnlinePlayers()) {
            raw(player, obj);
        }
    }

    static Object button(ChatColor color, String chat, String command, String... tooltip) {
        Map<String, Object> map = new HashMap<>();
        if (color != null) {
            map.put("color", color.name().toLowerCase());
        }
        map.put("text", format(chat));
        if (command != null) {
            Map<String, Object> clickEvent = new HashMap<>();
            map.put("clickEvent", clickEvent);
            clickEvent.put("action", "run_command");
            clickEvent.put("value", command);
        }
        Map<String, Object> hoverEvent = new HashMap<>();
        map.put("hoverEvent", hoverEvent);
        hoverEvent.put("action", "show_text");
        List<String> lines = new ArrayList<>();
        for (String line : tooltip) {
            if (!lines.isEmpty()) lines.add("\n");
            lines.add(format(line));
        }
        hoverEvent.put("value", lines);
        return map;
    }

    static Object button(String chat, String command, String... tooltip) {
        return button(null, chat, command, tooltip);
    }

    static Object tooltip(String chat, String... tooltip) {
        Map<String, Object> map = new HashMap<>();
        map.put("text", format(chat));
        Map<String, Object> map2;
        map2 = new HashMap<>();
        map.put("hoverEvent", map2);
        map2.put("action", "show_text");
        List<String> lines = new ArrayList<>();
        for (String line : tooltip) {
            if (!lines.isEmpty()) lines.add("\n");
            lines.add(format(line));
        }
        map2.put("value", lines);
        return map;
    }

    static String progressBar(int has, int needs) {
        final int len = 20;
        double percentage = Math.min(100.0, (double)has / (double)needs);
        has = (int)(percentage * (double)len);
        StringBuilder sb = new StringBuilder();
        sb.append("&3[&f");
        for (int i = 0; i < len; ++i) {
            if (has == i) sb.append("&8");
            sb.append("|");
        }
        sb.append("&3]");
        return format(sb.toString());
    }
}
