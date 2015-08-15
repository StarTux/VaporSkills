package com.winthier.skills.bukkit;

import com.winthier.skills.util.Strings;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
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
        List<Object> message = new ArrayList<>();
        message.add(BukkitUtil.format("&3&lSkills:"));
        for (BukkitSkill skill : getSkills().getSkills()) {
            int skillPoints = (int)getSkills().getScore().getSkillPoints(uuid, skill);
            int skillLevel = getSkills().getScore().getSkillLevel(uuid, skill);
            int pointsInLevel = getSkills().getScore().pointsInLevel(skillPoints);
            int pointsToLevelUp = getSkills().getScore().pointsToLevelUpTo(skillLevel + 1);
            message.add(" ");
            message.add(BukkitUtil.button(
                            "&b" + Strings.camelCase(skill.getVerb()) + "&3(&f"+skillLevel+"&3)",
                            "/sk " + skill.getVerb(),
                            "&3&l" + skill.getTitle(),
                            "&3Skill Level: &b" + skillLevel,
                            "&3Skill Points: &f"+pointsInLevel+"&3/&f"+pointsToLevelUp,
                            "&7Click for more details"));
        }
        BukkitUtil.raw(player, message);
    }
    
    void skillDetail(Player player, String name)
    {
        BukkitSkill skill = getSkills().skillByName(name);
        if (skill == null) {
            BukkitUtil.msg(player, "&cSkill not found: %s", name);
            return;
        }
        final UUID uuid = player.getUniqueId();
        int skillPoints = (int)getSkills().getScore().getSkillPoints(uuid, skill);
        int skillLevel = getSkills().getScore().getSkillLevel(uuid, skill);
        BukkitUtil.msg(player, "&3&l%s &bLevel &f%d", skill.getTitle(), skillLevel);
        BukkitUtil.raw(player,
                       BukkitUtil.format(" &3Skill Points: "),
                       BukkitUtil.tooltip(
                           BukkitUtil.format("&f%d&3/&f%d",
                                             getSkills().getScore().pointsInLevel(skillPoints),
                                             getSkills().getScore().pointsToLevelUpTo(skillLevel + 1)),
                           BukkitUtil.format("&3Total Skill Points: &f%d", skillPoints),
                           BukkitUtil.format("&3For Next Level: &f%d",
                                             getSkills().getScore().pointsForNextLevel(skillPoints))));
        BukkitUtil.msg(player, " &r%s", skill.getDescription());
    }

    String progressBar()
    {
        return "|";
    }
}
