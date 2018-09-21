package com.winthier.skills;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.json.simple.JSONValue;

final class Msg {
    private Msg() { }

    static void consoleCommand(String cmd, Object... args) {
        if (args.length > 0) cmd = String.format(cmd, args);
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

    @SuppressWarnings("deprecation")
    static void title(Player player, String title, String subtitle) {
        player.sendTitle(format(title), format(subtitle));
    }

    static void entityData(Entity e, String key, Object value) {
        Map<String, Object> map = new HashMap<>();
        map.put(key, value);
        String uuid = e.getUniqueId().toString();
        consoleCommand("minecraft:execute %s ~ ~ ~ minecraft:entitydata %s %s", uuid, uuid, JSONValue.toJSONString(map));
    }

    static void actionBar(Player player, String text, Object... args) {
        Map<String, Object> map = new HashMap<>();
        map.put("text", format(text, args));
        consoleCommand("minecraft:title %s actionbar %s", player.getName(), JSONValue.toJSONString(map));
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
        sb.append("&7[&9");
        for (int i = 0; i < len; ++i) {
            if (has == i) sb.append("&8");
            sb.append("|");
        }
        sb.append("&7]");
        return format(sb.toString());
    }


    private static List<String> wrapInternal(String what, int maxLineLength) {
        String[] words = what.split("\\s+");
        List<String> lines = new ArrayList<>();
        if (words.length == 0) return lines;
        StringBuilder line = new StringBuilder(words[0]);
        int lineLength = ChatColor.stripColor(words[0]).length();
        for (int i = 1; i < words.length; ++i) {
            String word = words[i];
            int wordLength = ChatColor.stripColor(word).length();
            if (lineLength + wordLength + 1 > maxLineLength) {
                lines.add(line.toString());
                line = new StringBuilder(word);
                lineLength = wordLength;
            } else {
                line.append(" ");
                line.append(word);
                lineLength += wordLength + 1;
            }
        }
        if (line.length() > 0) lines.add(line.toString());
        return lines;
    }

    public static List<String> wrap(String what, int maxLineLength) {
        List<String> lines = new ArrayList<>();
        for (String string: what.split("\n")) {
            if (string.isEmpty()) {
                lines.add("");
            } else {
                lines.addAll(wrapInternal(string, maxLineLength));
            }
        }
        return lines;
    }

    public static String wrap(String what, int maxLineLength, String endl) {
        List<String> lines = wrap(what, maxLineLength);
        return fold(lines, endl);
    }

    public static String fold(List<String> ls, String glue) {
        if (ls.isEmpty()) return "";
        StringBuilder sb = new StringBuilder(ls.get(0));
        for (int i = 1; i < ls.size(); ++i) sb.append(glue).append(ls.get(i));
        return sb.toString();
    }

    public static String niceEnumName(Enum en) {
        return en.name().toLowerCase().replace("_", " ");
    }

    public static String capitalEnumName(Enum en) {
        String[] t = en.name().split("_");
        StringBuilder sb = new StringBuilder(capitalize(t[0]));
        for (int i = 1; i < t.length; i += 1) sb.append(" ").append(capitalize(t[i]));
        return sb.toString();
    }

    public static String capitalize(String input) {
        return input.substring(0, 1).toUpperCase() + input.substring(1, input.length()).toLowerCase();
    }

    public static String toJSONString(Object o) {
        return JSONValue.toJSONString(o);
    }

    public static Object fromJSONString(String s) {
        return JSONValue.parse(s);
    }

    public static String roman(int i) {
        switch (i) {
        case 1: return "I";
        case 2: return "II";
        case 3: return "III";
        case 4: return "IV";
        case 5: return "V";
        case 6: return "VI";
        case 7: return "VII";
        case 8: return "VIII";
        case 9: return "IX";
        case 10: return "X";
        default: return "" + i;
        }
    }
}
