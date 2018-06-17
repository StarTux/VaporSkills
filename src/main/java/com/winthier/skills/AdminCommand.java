package com.winthier.skills;

import com.winthier.playercache.PlayerCache;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.json.simple.JSONValue;

@RequiredArgsConstructor
class AdminCommand implements CommandExecutor {
    private final SkillsPlugin plugin;

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String cmd = args.length == 0 ? "" : args[0].toLowerCase();
        final Player player = sender instanceof Player ? (Player)sender : null;
        try {
            if (cmd.equals("config")) {
                return onCommandConfig(sender, Arrays.copyOfRange(args, 1, args.length));
            } else if (cmd.equals("reward")) {
                return onCommandReward(sender, Arrays.copyOfRange(args, 1, args.length));
            } else if (cmd.equals("score")) {
                return onCommandScore(sender, Arrays.copyOfRange(args, 1, args.length));
            } else if (cmd.equals("test")) {
                return onCommandTest(sender, Arrays.copyOfRange(args, 1, args.length));
            } else if (cmd.equals("perk")) {
                return onCommandPerks(sender, Arrays.copyOfRange(args, 1, args.length));
            } else if (cmd.equals("debug")) {
                if (player == null) {
                    sender.sendMessage("Player expected");
                    return true;
                }
                if (plugin.hasDebugMode(player)) {
                    plugin.setDebugMode(player, false);
                    player.sendMessage("Debug mode disabled");
                } else {
                    plugin.setDebugMode(player, true);
                    player.sendMessage("Debug mode enabled");
                }
            } else if (cmd.equals("info")) {
                sender.sendMessage("Debug info");
                sender.sendMessage("Dirty scores: " + plugin.getScore().dirtyScores.size());
            } else {
                sender.sendMessage("/skadmin info");
                sender.sendMessage("/skadmin debug");
                sender.sendMessage("/skadmin config");
                sender.sendMessage("/skadmin reward");
                sender.sendMessage("/skadmin score");
                sender.sendMessage("/skadmin perks");
                sender.sendMessage("/skadmin test");
            }
        } catch (RuntimeException re) {
            sender.sendMessage("Syntax error");
            re.printStackTrace();
        }
        return true;
    }

    boolean onCommandTest(CommandSender sender, String[] args) {
        String cmd = args.length == 0 ? "" : args[0].toLowerCase();
        if (cmd.equals("levelup")) {
            Player player = plugin.getServer().getPlayer(args[1]);
            if (player == null) {
                sender.sendMessage("Player not found: " + args[1]);
                return true;
            }
            SkillType skillType;
            if ("total".equals(args[2])) {
                skillType = SkillType.TOTAL;
            } else {
                Skill skill = plugin.skillByName(args[2]);
                if (skill == null) {
                    sender.sendMessage("Skill not found: " + args[2]);
                    return true;
                } else {
                    skillType = skill.getSkillType();
                }
            }
            int skillLevel = 0;
            try {
                skillLevel = Integer.parseInt(args[3]);
            } catch (NumberFormatException nfe) { }
            if (skillLevel < 0) {
                sender.sendMessage("Invalid level: " + args[3]);
                return true;
            }
            LevelUpEffect.launch(plugin, player, skillType, skillLevel);
        } else if (cmd.equals("cat")) {
            Player player = (Player)sender;
            org.bukkit.entity.Ocelot cat = player.getWorld().spawn(player.getLocation(), org.bukkit.entity.Ocelot.class);
            cat.setCatType(org.bukkit.entity.Ocelot.Type.BLACK_CAT);
            cat.setTamed(true);
            cat.setRemoveWhenFarAway(false);
            cat.setOwner(player);
        } else if (cmd.equals("horse")) {
            Player player = (Player)sender;
            org.bukkit.entity.Horse horse = player.getWorld().spawn(player.getLocation(), org.bukkit.entity.Horse.class);
            horse.setTamed(true);
            horse.setRemoveWhenFarAway(false);
            horse.setOwner(player);
        } else if (cmd.equals("inv")) {
            Player player = (Player)sender;
            org.bukkit.inventory.Inventory inv = plugin.getServer().createInventory(player, org.bukkit.event.inventory.InventoryType.ANVIL, "Hello World");
            player.openInventory(inv);
        } else if (cmd.equals("perks")) {
            Map<SkillType, Integer> map = new EnumMap<>(SkillType.class);
            for (SkillType skillType: SkillType.values()) {
                map.put(skillType, 0);
            }
            for (Perk perk: Perk.values()) {
                map.put(perk.skillType, map.get(perk.skillType) + 1);
            }
            for (SkillType skillType: SkillType.values()) {
                sender.sendMessage(map.get(skillType) + " " + skillType.key);
            }
        } else {
            sender.sendMessage("skadmin test levelup <player> <skill> <level>");
        }
        return true;
    }

    boolean onCommandConfig(CommandSender sender, String[] args) {
        String cmd = args.length == 0 ? "" : args[0].toLowerCase();
        if (cmd.equals("reload")) {
            plugin.reloadAll();
            sender.sendMessage("[Skills] Configuration reloaded");
        } else if (cmd.equals("save")) {
            plugin.writeDefaultFiles(false);
            sender.sendMessage("[Skills] Default files saved to disk");
        } else if (cmd.equals("overwrite")) {
            plugin.writeDefaultFiles(true);
            sender.sendMessage("[Skills] All config files overwritten with plugin defaults");
        } else {
            sender.sendMessage("/skadmin config reload");
            sender.sendMessage("/skadmin config save");
            sender.sendMessage("/skadmin config overwrite");
        }
        return true;
    }

    boolean onCommandReward(CommandSender sender, String[] args) {
        String cmd = args.length == 0 ? "" : args[0].toLowerCase();
        if (cmd.equals("list") && args.length == 2) {
            Skill skill = plugin.skillByName(args[1]);
            if (skill == null) throw new IllegalArgumentException("Skill not found: " + args[1]);
            SkillType skillType = skill.getSkillType();
            sender.sendMessage("Rewards of " + skill.getDisplayName() + ":");
            int count = 0;
            for (Reward reward: plugin.getScore().getRewards().values()) {
                if (reward.key.getSkill() == skillType) {
                    sender.sendMessage(reward.toString());
                    count++;
                }
            }
            sender.sendMessage("end of list (" + count + ")");
        } else if (cmd.equals("reload") && args.length == 1) {
            plugin.importRewards();
            sender.sendMessage("Rewards imported");
        } else {
            sender.sendMessage("/skadmin reward list <skill>");
            sender.sendMessage("/skadmin reward reload");
        }
        return true;
    }

    boolean onCommandScore(CommandSender sender, String[] args) {
        String cmd = args.length == 0 ? "" : args[0].toLowerCase();
        if (cmd.equals("list") && args.length == 2) {
            UUID uuid = PlayerCache.uuidForName(args[1]);
            if (uuid == null) {
                sender.sendMessage("Player not found: " + args[1]);
                return true;
            }
            sender.sendMessage("Scores of " + PlayerCache.nameForUuid(uuid) + ":");
            for (Skill skill : plugin.getSkills()) {
                int lvl = plugin.getScore().getSkillLevel(uuid, skill.skillType);
                int sp = (int)plugin.getScore().getSkillPoints(uuid, skill.skillType);
                int pil = Score.pointsInLevel(sp);
                int ptlut = Score.pointsToLevelUpTo(lvl + 1);
                sender.sendMessage(String.format(" lvl:%d %s sp:%d (%d/%d)", lvl, skill.getShorthand(), sp, pil, ptlut));
            }
        } else if (cmd.equals("reset") && (args.length == 2 || args.length == 3)) {
            UUID uuid = PlayerCache.uuidForName(args[1]);
            if (uuid == null) {
                sender.sendMessage("Player not found: " + args[1]);
                return true;
            }
            if (args.length >= 3) {
                Skill skill = plugin.skillByName(args[2]);
                if (skill == null) {
                    sender.sendMessage("Skill not found: " + args[2]);
                    return true;
                }
                plugin.getScore().setSkillLevel(uuid, skill.skillType, 0);
                sender.sendMessage("Score reset: " + PlayerCache.nameForUuid(uuid) + ", " + skill.getDisplayName());
            } else {
                for (Skill skill : plugin.getSkills()) {
                    plugin.getScore().setSkillLevel(uuid, skill.skillType, 0);
                }
                sender.sendMessage("Scores reset: " + PlayerCache.nameForUuid(uuid));
            }
        } else if (cmd.equals("setlevel") && args.length == 4) {
            UUID uuid = PlayerCache.uuidForName(args[1]);
            if (uuid == null) {
                sender.sendMessage("Player not found: " + args[1]);
                return true;
            }
            Skill skill = plugin.skillByName(args[2]);
            if (skill == null) {
                sender.sendMessage("Skill not found: " + args[2]);
                return true;
            }
            int skillLevel = 0;
            try {
                skillLevel = Integer.parseInt(args[3]);
            } catch (NumberFormatException nfe) { }
            if (skillLevel < 0) {
                sender.sendMessage("Invalid level: " + args[3]);
                return true;
            }
            plugin.getScore().setSkillLevel(uuid, skill.skillType, skillLevel);
            sender.sendMessage("Skill level set: " + PlayerCache.nameForUuid(uuid) + ", " + skill.getDisplayName() + ": " + skillLevel);
        } else if (cmd.equals("givepoints") && args.length == 4) {
            UUID uuid = PlayerCache.uuidForName(args[1]);
            if (uuid == null) {
                sender.sendMessage("Player not found: " + args[1]);
                return true;
            }
            Skill skill = plugin.skillByName(args[2]);
            if (skill == null) {
                sender.sendMessage("Skill not found: " + args[2]);
                return true;
            }
            int skillPoints = 0;
            try {
                skillPoints = Integer.parseInt(args[3]);
            } catch (NumberFormatException nfe) { }
            if (skillPoints <= 0) {
                sender.sendMessage("Invalid points: " + args[3]);
                return true;
            }
            plugin.getScore().giveSkillPoints(uuid, skill.skillType, skillPoints);
            sender.sendMessage("Points given: " + PlayerCache.nameForUuid(uuid) + ", " + skill.getDisplayName() + ": " + skillPoints);
        } else {
            sender.sendMessage("/skadmin score list <player>");
            sender.sendMessage("/skadmin score reset <player> [skill]");
            sender.sendMessage("/skadmin score setlevel <player> <skill> <level>");
            sender.sendMessage("/skadmin score givepoints <player> <skill> <points>");
        }
        return true;
    }

    private void makeAdvancement(File root, String perkName, String depends) throws IOException {
        File file = new File(root, perkName + ".json");
        Map<String, Object> map = new HashMap<>();
        Map<String, Object> displayMap = new HashMap<>();
        map.put("display", displayMap);
        Map<String, Object> iconMap = new HashMap<>();
        displayMap.put("icon", iconMap);
        ConfigurationSection config = plugin.getPerksConfig().getConfigurationSection("perks." + perkName);
        iconMap.put("item", config.getString("Icon.Item", "minecraft:diamond"));
        if (config.isSet("Icon.Data")) iconMap.put("data", config.getInt("Icon.Data"));
        displayMap.put("title", config.getString("DisplayName", perkName));
        displayMap.put("description", config.getString("Description", perkName));
        if (perkName.equals("root")) displayMap.put("background", "minecraft:textures/gui/advancements/backgrounds/stone.png");
        displayMap.put("hidden", false);
        displayMap.put("announce_to_chat", false);
        displayMap.put("show_toast", false);
        // displayMap.put("frame", "goal");
        if (depends != null) {
            map.put("parent", "winthier:skills/" + depends);
        }
        Map<String, Object> criteriaMap = new HashMap<>();
        map.put("criteria", criteriaMap);
        Map<String, Object> impossibleMap = new HashMap<>();
        criteriaMap.put("impossible", impossibleMap);
        impossibleMap.put("trigger", "minecraft:location");
        //impossibleMap.put("trigger", "minecraft:impossible");
        FileWriter writer = new FileWriter(file);
        JSONValue.writeJSONString(map, writer);
        writer.close();
    }

    boolean onCommandPerks(CommandSender sender, String[] args) {
        if (args.length == 1 && args[0].equals("adv")) {
            File root = plugin.getDataFolder();
            root = new File(root, "advancements");
            root = new File(root, "winthier");
            root = new File(root, "skills");
            root.mkdirs();
            try {
                makeAdvancement(root, "root", null);
                for (Perk perk: Perk.values()) makeAdvancement(root, perk.name().toLowerCase(), perk.depends == null ? "root" : perk.depends.name().toLowerCase());
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
            sender.sendMessage("" + Perk.values().length + " advancements created");
        } else {
            sender.sendMessage("/skadmin perk adv");
        }
        return true;
    }
}
