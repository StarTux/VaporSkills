package com.winthier.skills.bukkit;

import com.winthier.skills.util.Strings;
import java.util.UUID;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

class BukkitCommandSkills implements CommandExecutor
{
    BukkitSkills getSkills()
    {
        return BukkitSkills.getInstance();
    }

    BukkitSkillsPlugin getPlugin()
    {
        return BukkitSkillsPlugin.getInstance();
    }

    void msg(CommandSender sender, String msg, Object... args)
    {
        msg = ChatColor.translateAlternateColorCodes('&', msg);
        if (args.length > 0) msg = String.format(msg, args);
        sender.sendMessage(msg);
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        final Player player = sender instanceof Player ? (Player)sender : null;
        if (player == null) {
            sender.sendMessage("Player expected");
            return true;
        }
        if (args.length == 0) {
            listSkills(player);
            return true;
        }
        String cmd = args.length == 0 ? "" : args[0].toLowerCase();
        BukkitSkill skill = getSkills().skillByName(cmd);
        if (skill == null) {
            sender.sendMessage("Skill not found: " + cmd);
            return true;
        }
        final UUID uuid = player.getUniqueId();
        int skillPoints = (int)getSkills().getScore().getSkillPoints(uuid, skill);
        int skillLevel = getSkills().getScore().getSkillLevel(uuid, skill);
        int forNextLevel = getSkills().getScore().pointsForLevel(skillLevel + 1) - skillPoints;
        msg(sender, "&b&l%s", skill.getTitle());
        msg(sender, " &3Your Skill Level: &b%d", skillLevel);
        msg(sender, " &3Your Skill Points: &b%d", skillPoints);
        msg(sender, " &3For Next Level: &b%d", forNextLevel);
        return true;
    }

    void listSkills(Player player)
    {
        UUID uuid = player.getUniqueId();
        StringBuilder sb = new StringBuilder("&b&lSkills&8:");
        for (BukkitSkill skill : getSkills().getSkills()) {
            sb.append(" &3");
            sb.append(Strings.camelCase(skill.getVerb()));
            sb.append("&8(&b").append(getSkills().getScore().getSkillLevel(uuid, skill)).append("&8)");
        }
        msg(player, sb.toString());
    }
}
