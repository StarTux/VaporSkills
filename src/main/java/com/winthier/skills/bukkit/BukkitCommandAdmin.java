package com.winthier.skills.bukkit;

import com.winthier.skills.sql.SQLDB;
import com.winthier.skills.sql.SQLReward;
import java.util.Arrays;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

class BukkitCommandAdmin implements CommandExecutor
{
    BukkitSkills getSkills()
    {
        return BukkitSkills.getInstance();
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        if (args.length == 0) {
            sender.sendMessage("Usage: /skadmin reward");
            return true;
        }
        String cmd = args[0].toLowerCase();
        try {
            if (cmd.equals("reward")) return onCommandRewards(sender, Arrays.copyOfRange(args, 1, args.length));
        } catch (RuntimeException re) {
            sender.sendMessage("Syntax error");
            re.printStackTrace();
        }
        return false;
    }

    boolean onCommandRewards(CommandSender sender, String[] args)
    {
        if (args.length == 0) {
            sender.sendMessage("/skadmin reward list <skill>");
            sender.sendMessage("/skadmin reward get <skill> <target> <type> <data> <name>");
            sender.sendMessage("/skadmin reward set <skill> <target> <type> <data> <name> <sp> <money> <exp>");
            sender.sendMessage("/skadmin reward import");
            sender.sendMessage("/skadmin reward clear");
            return true;
        }
        String cmd = args[0].toLowerCase();
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
            return true;
        }
        if (cmd.equals("set") && args.length == 9) {
            BukkitReward reward = BukkitReward.parse(Arrays.copyOfRange(args, 1, args.length));
            reward.store();
            sender.sendMessage("+" + reward);
            return true;
        }
        return false;
    }
}
