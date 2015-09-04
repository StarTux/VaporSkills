package com.winthier.skills.bukkit;

import com.winthier.playercache.PlayerCache;
import com.winthier.skills.Highscore;
import com.winthier.skills.util.Strings;
import java.util.ArrayList;
import java.util.List;
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
        if (player == null) {
            sender.sendMessage("Player expected");
            return true;
        }
        if (args.length == 0) {
            listSkills(player);
        } else if (args.length == 1) {
            skillDetail(player, args[0].toLowerCase());
        } else {
            return false;
        }
        return true;
    }

    void listSkills(Player player)
    {
        UUID uuid = player.getUniqueId();
        BukkitUtil.msg(player, "");
        List<Object> message = new ArrayList<>();
        message.add(BukkitUtil.format("&3&lHighscores:"));
        for (BukkitSkill skill : getSkills().getSkills()) {
            Highscore hi = getSkills().getScore().getHighscore(skill);
            int rank = hi.rankOfPlayer(uuid);
            int index = hi.indexOfPlayer(uuid);
            StringBuilder sb = new StringBuilder();
            for (int i = Math.max(0, index - 2); i <= Math.min(hi.size() - 1, index + 2); ++i) {
                if (sb.length() > 0) sb.append("\n");
                Highscore.Row row = hi.rowAt(i);
                if (uuid.equals(row.getPlayer())) {
                    sb.append(BukkitUtil.format("&3#%02d &blvl &f%d &b%s", row.getRank(), row.getSkillLevel(), PlayerCache.nameForUuid(row.getPlayer())));
                } else {
                    sb.append(BukkitUtil.format("&3#%02d &blvl &f%d &3%s", row.getRank(), row.getSkillLevel(), PlayerCache.nameForUuid(row.getPlayer())));
                }
            }
            message.add(" ");
            message.add(BukkitUtil.button(
                            "&b" + skill.getShorthand() + "&3(&f#"+rank+"&3)",
                            "/hi " + skill.getKey(),
                            "&3&l" + skill.getDisplayName() + " &f#" + rank,
                            sb.toString(),
                            "&7Click for more details"));
        }
        BukkitUtil.raw(player, message);
        BukkitUtil.msg(player, "");
    }

    // TODO page numbers
    void skillDetail(Player player, String name)
    {
        BukkitSkill skill = getSkills().skillByName(name);
        if (skill == null) {
            player.sendMessage("Skill not found: " + name);
            return;
        }
        final UUID uuid = player.getUniqueId();
        BukkitUtil.msg(player, "");
        BukkitUtil.msg(player, "&b&l%s", skill.getDisplayName());
        for (Highscore.Row row : getSkills().getScore().getHighscore(skill).getRows()) {
            BukkitUtil.msg(player, " &3#%d &f%02d &3%s", row.getRank(), row.getSkillLevel(), PlayerCache.nameForUuid(row.getPlayer()));
        }
        BukkitUtil.msg(player, "");
    }
}
