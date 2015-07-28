package com.winthier.skills.bukkit;

import com.winthier.playercache.PlayerCache;
import com.winthier.skills.Highscore;
import com.winthier.skills.util.Strings;
import java.util.UUID;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

class BukkitCommandHighscore implements CommandExecutor
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
        final Player player = sender instanceof Player ? (Player)sender : null;
        if (args.length == 0) {
            if (player == null) {
                sender.sendMessage("Player expected");
                return true;
            }
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
        BukkitUtil.msg(sender, "&b&l%s", skill.getTitle());
        for (Highscore.Row row : getSkills().getScore().getHighscore(skill).getRows()) {
            String name = PlayerCache.nameForUuid(row.getPlayer());
            BukkitUtil.msg(sender, " &3%d &b%02d &3%s", row.getRank(), row.getSkillLevel(), name);
        }
        return true;
    }

    void listSkills(Player player)
    {
        UUID uuid = player.getUniqueId();
        StringBuilder sb = new StringBuilder("&b&lSkills&8:");
        for (BukkitSkill skill : getSkills().getSkills()) {
            int rank = getSkills().getScore().getHighscore(skill).rankOfPlayer(uuid);
            sb.append(" &3");
            sb.append(Strings.camelCase(skill.getVerb()));
            sb.append("&8(&b#").append(rank).append("&8)");
        }
        BukkitUtil.msg(player, sb.toString());
    }
}
