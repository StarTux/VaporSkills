package com.winthier.skills.bukkit;

import com.winthier.playercache.PlayerCache;
import com.winthier.skills.sql.SQLDB;
import com.winthier.skills.sql.SQLLog;
import com.winthier.skills.sql.SQLReward;
import com.winthier.skills.sql.SQLScore;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

class BukkitCommandAdmin implements CommandExecutor
{
    BukkitSkills getSkills()
    {
        return BukkitSkills.getInstance();
    }

    BukkitSkillsPlugin getPlugin()
    {
        return BukkitSkillsPlugin.getInstance();
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
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
            } else if (cmd.equals("debug")) {
                if (player == null) {
                    sender.sendMessage("Player expected");
                    return true;
                }
                if (getSkills().hasDebugMode(player)) {
                    getSkills().setDebugMode(player, false);
                    player.sendMessage("Debug mode disabled");
                } else {
                    getSkills().setDebugMode(player, true);
                    player.sendMessage("Debug mode enabled");
                }
            } else {
                sender.sendMessage("/skadmin debug");
                sender.sendMessage("/skadmin config");
                sender.sendMessage("/skadmin reward");
                sender.sendMessage("/skadmin score");
                sender.sendMessage("/skadmin test");
            }
        } catch (RuntimeException re) {
            sender.sendMessage("Syntax error");
            re.printStackTrace();
        }
        return true;
    }

    boolean onCommandTest(CommandSender sender, String[] args)
    {
        String cmd = args.length == 0 ? "" : args[0].toLowerCase();
        if (cmd.equals("levelup")) {
            Player player = getPlugin().getServer().getPlayer(args[1]);
            if (player == null) {
                sender.sendMessage("Player not found: " + args[1]);
                return true;
            }
            BukkitSkill skill = getSkills().skillByName(args[2]);
            if (skill == null) {
                sender.sendMessage("Skill not found: " + args[2]);
                return true;
            }
            int skillLevel = 0;
            try {
                skillLevel = Integer.parseInt(args[3]);
            } catch (NumberFormatException nfe) {}
            if (skillLevel < 0) {
                sender.sendMessage("Invalid level: " + args[3]);
                return true;
            }
            BukkitLevelUpEffect.launch(player, skill, skillLevel);
        } else if (cmd.equals("backlog")) {
            sender.sendMessage("Moneys: " + getSkills().getMoneys().size());
            sender.sendMessage("Score: " + SQLScore.getDirties().size());
            sender.sendMessage("Logs: " + SQLLog.getDirties().size());
            sender.sendMessage("Drops: " + ((BukkitSkillSacrifice)getSkills().skillByType(BukkitSkillType.SACRIFICE)).dropped.size());
        } else {
            sender.sendMessage("skadmin test levelup <player> <skill> <level>");
            sender.sendMessage("skadmin test backlog");
        }
        return true;
    }

    boolean onCommandConfig(CommandSender sender, String[] args)
    {
        String cmd = args.length == 0 ? "" : args[0].toLowerCase();
        if (cmd.equals("reload")) {
            getPlugin().reloadAll();
            sender.sendMessage("[Skills] Configuration reloaded");
        } else if (cmd.equals("save")) {
            getPlugin().writeDefaultFiles(false);
            getPlugin().saveAll();
            sender.sendMessage("[Skills] All data saved to disk");
        } else if (cmd.equals("overwrite")) {
            getPlugin().writeDefaultFiles(true);
            sender.sendMessage("[Skills] All config files overwritten with plugin defaults");
        } else {
            sender.sendMessage("/skadmin config reload");
            sender.sendMessage("/skadmin config save");
            sender.sendMessage("/skadmin config overwrite");
        }
        return true;
    }

    boolean onCommandReward(CommandSender sender, String[] args)
    {
        String cmd = args.length == 0 ? "" : args[0].toLowerCase();
        if (cmd.equals("list") && args.length == 2) {
            BukkitSkill skill = BukkitSkills.getInstance().skillByName(args[1]);
            if (skill == null) throw new IllegalArgumentException("Skill not found: " + args[1]);
            sender.sendMessage("Rewards of " + skill.getDisplayName() + ":");
            int count = 0;
            for (SQLReward sqlReward : SQLReward.findList(skill)) {
                sender.sendMessage(BukkitReward.of(sqlReward).toString());
                count++;
            }
            sender.sendMessage("end of list (" + count + ")");
        } else if (cmd.equals("flush") && args.length == 1) {
            SQLDB.saveAll();
            SQLDB.clearAllCaches();
            sender.sendMessage("Databases flushed");
        } else if (cmd.equals("clear") && args.length == 1) {
            SQLDB.saveAll();
            SQLDB.clearAllCaches();
            SQLReward.deleteAll();
            sender.sendMessage("All rewards cleared");
        } else if (cmd.equals("import") && args.length == 1) {
            SQLDB.saveAll();
            SQLDB.clearAllCaches();
            File file = new File(getPlugin().getDataFolder(), BukkitSkillsPlugin.REWARDS_TXT);
            if (!file.exists()) {
                sender.sendMessage(BukkitSkillsPlugin.REWARDS_TXT + " not found");
                return true;
            }
            int linum = 0;
            int count = 0;
            BufferedReader in = null;
            BukkitReward reward = null;
            try {
                Set<BukkitReward.Key> keys = new HashSet<>();
                in = new BufferedReader(new FileReader(file));
                String line = null;
                while (null != (line = in.readLine())) {
                    linum++;
                    line = line.split("#")[0];
                    if (line.isEmpty()) continue;
                    String[] tokens = line.split("\\s+");
                    reward = BukkitReward.parse(tokens);
                    if (keys.contains(reward.key)) sender.sendMessage("Warning: Duplicate key '" + reward.key + "' in line " + linum);
                    reward.store();
                    sender.sendMessage("" + reward);
                    count++;
                }
                sender.sendMessage("Imported " + count + " rewards from " + BukkitSkillsPlugin.REWARDS_TXT);
            } catch (IOException ioe) {
                sender.sendMessage("Error reading " + BukkitSkillsPlugin.REWARDS_TXT + ". See console.");
                ioe.printStackTrace();
            } catch (RuntimeException re) {
                sender.sendMessage("Error parsing " + BukkitSkillsPlugin.REWARDS_TXT + ", line " + linum + ". See console.");
                sender.sendMessage("" + reward);
                re.printStackTrace();
            } finally {
                try {
                    in.close();
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
            }
        } else {
            sender.sendMessage("/skadmin reward list <skill>");
            sender.sendMessage("/skadmin reward flush");
            sender.sendMessage("/skadmin reward clear");
            sender.sendMessage("/skadmin reward import");
        }
        return true;
    }

    boolean onCommandScore(CommandSender sender, String[] args)
    {
        String cmd = args.length == 0 ? "" : args[0].toLowerCase();
        if (cmd.equals("list") && args.length == 2) {
            UUID uuid = PlayerCache.uuidForName(args[1]);
            if (uuid == null) {
                sender.sendMessage("Player not found: " + args[1]);
                return true;
            }
            sender.sendMessage("Scores of " + PlayerCache.nameForUuid(uuid) + ":");
            for (BukkitSkill skill : getSkills().getSkills()) {
                int lvl = getSkills().getScore().getSkillLevel(uuid, skill);
                int sp = (int)getSkills().getScore().getSkillPoints(uuid, skill);
                int pil = getSkills().getScore().pointsInLevel(sp);
                int ptlut = getSkills().getScore().pointsToLevelUpTo(lvl + 1);
                sender.sendMessage(String.format(" lvl:%d %s sp:%d (%d/%d)", lvl, skill.getShorthand(), sp, pil, ptlut));
                                                 
            }
        } else if (cmd.equals("reset") && (args.length == 2 || args.length == 3)) {
            UUID uuid = PlayerCache.uuidForName(args[1]);
            if (uuid == null) {
                sender.sendMessage("Player not found: " + args[1]);
                return true;
            }
            if (args.length >= 3) {
                BukkitSkill skill = getSkills().skillByName(args[2]);
                if (skill == null) {
                    sender.sendMessage("Skill not found: " + args[2]);
                    return true;
                }
                getSkills().getScore().setSkillLevel(uuid, skill, 0);
                sender.sendMessage("Score reset: " + PlayerCache.nameForUuid(uuid) + ", " + skill.getDisplayName());
            } else {
                for (BukkitSkill skill : getSkills().getSkills()) {
                    getSkills().getScore().setSkillLevel(uuid, skill, 0);
                }
                sender.sendMessage("Scores reset: " + PlayerCache.nameForUuid(uuid));
            }
        } else if (cmd.equals("setlevel") && args.length == 4) {
            UUID uuid = PlayerCache.uuidForName(args[1]);
            if (uuid == null) {
                sender.sendMessage("Player not found: " + args[1]);
                return true;
            }
            BukkitSkill skill = getSkills().skillByName(args[2]);
            if (skill == null) {
                sender.sendMessage("Skill not found: " + args[2]);
                return true;
            }
            int skillLevel = 0;
            try {
                skillLevel = Integer.parseInt(args[3]);
            } catch (NumberFormatException nfe) {}
            if (skillLevel < 0) {
                sender.sendMessage("Invalid level: " + args[3]);
                return true;
            }
            getSkills().getScore().setSkillLevel(uuid, skill, skillLevel);
            sender.sendMessage("Skill level set: " + PlayerCache.nameForUuid(uuid) + ", " + skill.getDisplayName() + ": " + skillLevel);
        } else if (cmd.equals("givepoints") && args.length == 4) {
            UUID uuid = PlayerCache.uuidForName(args[1]);
            if (uuid == null) {
                sender.sendMessage("Player not found: " + args[1]);
                return true;
            }
            BukkitSkill skill = getSkills().skillByName(args[2]);
            if (skill == null) {
                sender.sendMessage("Skill not found: " + args[2]);
                return true;
            }
            int skillPoints = 0;
            try {
                skillPoints = Integer.parseInt(args[3]);
            } catch (NumberFormatException nfe) {}
            if (skillPoints <= 0) {
                sender.sendMessage("Invalid points: " + args[3]);
                return true;
            }
            getSkills().getScore().giveSkillPoints(uuid, skill, skillPoints);
            sender.sendMessage("Points given: " + PlayerCache.nameForUuid(uuid) + ", " + skill.getDisplayName() + ": " + skillPoints);
        } else {
            sender.sendMessage("/skadmin score list <player>");
            sender.sendMessage("/skadmin score reset <player> [skill]");
            sender.sendMessage("/skadmin score setlevel <player> <skill> <level>");
            sender.sendMessage("/skadmin score givepoints <player> <skill> <points>");
        }
        return true;
    }
}
