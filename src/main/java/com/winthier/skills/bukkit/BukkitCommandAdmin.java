package com.winthier.skills.bukkit;

import com.winthier.skills.sql.SQLDB;
import com.winthier.skills.sql.SQLReward;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
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
            if (cmd.equals("reward")) {
                return onCommandRewards(sender, Arrays.copyOfRange(args, 1, args.length));
            } else if (cmd.equals("reload")) {
                getPlugin().reloadAll();
                sender.sendMessage("[Skills] Configuration reloaded");
            } else if (cmd.equals("save")) {
                getPlugin().saveAll();
                sender.sendMessage("[Skills] All data saved to disk");
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
                sender.sendMessage("/skadmin reload");
                sender.sendMessage("/skadmin save");
                sender.sendMessage("/skadmin debug");
                sender.sendMessage("/skadmin reward");
            }
        } catch (RuntimeException re) {
            sender.sendMessage("Syntax error");
            re.printStackTrace();
        }
        return true;
    }

    boolean onCommandRewards(CommandSender sender, String[] args)
    {
        String cmd = args.length == 0 ? "" : args[0].toLowerCase();
        if (cmd.equals("list") && args.length == 2) {
            BukkitSkill skill = BukkitSkills.getInstance().skillByName(args[1]);
            if (skill == null) throw new IllegalArgumentException("Skill not found: " + args[1]);
            sender.sendMessage("Rewards of " + skill.getTitle() + ":");
            int count = 0;
            for (SQLReward sqlReward : SQLReward.findList(skill.getKey())) {
                sender.sendMessage(BukkitReward.of(sqlReward).toString());
                count++;
            }
            sender.sendMessage("end of list (" + count + ")");
        } else if (cmd.equals("set") && args.length == 9) {
            BukkitReward reward = BukkitReward.parse(Arrays.copyOfRange(args, 1, args.length));
            reward.store();
            sender.sendMessage("+" + reward);
        } else if (cmd.equals("import") && args.length == 1) {
            File file = new File(getPlugin().getDataFolder(), "rewards.txt");
            if (!file.exists()) {
                sender.sendMessage("rewards.txt not found");
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
                sender.sendMessage("Imported " + count + " rewards from rewards.txt.");
            } catch (IOException ioe) {
                sender.sendMessage("Error reading rewards.txt. See console.");
                ioe.printStackTrace();
            } catch (RuntimeException re) {
                sender.sendMessage("Error parsing rewards.txt, line " + linum + ". See console.");
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
            sender.sendMessage("/skadmin reward get <skill> <target> <type> <data> <name>");
            sender.sendMessage("/skadmin reward set <skill> <target> <type> <data> <name> <sp> <money> <exp>");
            sender.sendMessage("/skadmin reward import");
            sender.sendMessage("/skadmin reward clear");
        }
        return true;
    }
}
