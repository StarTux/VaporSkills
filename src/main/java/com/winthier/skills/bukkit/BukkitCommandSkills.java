package com.winthier.skills.bukkit;

import com.winthier.skills.util.Strings;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.apache.commons.lang3.text.WordUtils;
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
        String cmd = args.length > 0 ? args[0].toLowerCase() : "";
        if (args.length == 0) {
            listSkills(player);
        } else if (args.length >= 1 && "sidebar".equals(cmd)) {
            String sub = args.length == 2 ? args[1].toLowerCase() : "";
            modifySidebar(player, sub);
        } else if (args.length == 1) {
            skillDetail(player, cmd);
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
        message.add(BukkitUtil.format("&3&lSkills:"));
        for (BukkitSkill skill : getSkills().getSkills()) {
            int skillPoints = (int)getSkills().getScore().getSkillPoints(uuid, skill);
            int skillLevel = getSkills().getScore().getSkillLevel(uuid, skill);
            int pointsInLevel = getSkills().getScore().pointsInLevel(skillPoints);
            int pointsToLevelUp = getSkills().getScore().pointsToLevelUpTo(skillLevel + 1);
            message.add(" ");
            message.add(BukkitUtil.button(
                            "&b" + Strings.camelCase(skill.getVerb()) + "&3(&f"+skillLevel+"&3)",
                            "/sk " + skill.getKey(),
                            "&3&l" + skill.getTitle() + " " + BukkitUtil.progressBar(pointsInLevel, pointsToLevelUp),
                            "&3Skill Level: &b" + skillLevel,
                            "&3Skill Points: &f"+pointsInLevel+"&3/&f"+pointsToLevelUp,
                            "&r" + WordUtils.wrap(skill.getDescription(), 32),
                            "&7Click for more details"));
        }
        BukkitUtil.raw(player, message);
        BukkitUtil.raw(player,
                       BukkitUtil.format(" &3Sidebar: "),
                       BukkitUtil.button("&3[&fReset&3]", "/sk sidebar reset", "&7Reset the sidebar"), " ",
                       BukkitUtil.button("&3[&fOn&3]", "/sk sidebar on", "&7Enable sidebar"), " ", 
                       BukkitUtil.button("&3[&fOff&3]", "/sk sidebar off", "&7Disable sidebar"));
        BukkitUtil.msg(player, "");
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
        int pointsInLevel = getSkills().getScore().pointsInLevel(skillPoints);
        int pointsToLevelUp = getSkills().getScore().pointsToLevelUpTo(skillLevel + 1);
        BukkitUtil.msg(player, "");
        BukkitUtil.msg(player, "&3&l%s &bLevel &f%d %s",
                       skill.getTitle(),
                       skillLevel,
                       BukkitUtil.progressBar(pointsInLevel, pointsToLevelUp));
        BukkitUtil.raw(player,
                       BukkitUtil.format(" &3Skill Points: "),
                       BukkitUtil.tooltip(
                           BukkitUtil.format("&f%d&3/&f%d",
                                             pointsInLevel,
                                             pointsToLevelUp),
                           BukkitUtil.format("&3Total Skill Points: &f%d", skillPoints),
                           BukkitUtil.format("&3For Next Level: &f%d",
                                             getSkills().getScore().pointsForNextLevel(skillPoints))));
        BukkitUtil.raw(player,
                       BukkitUtil.format(" &3Sidebar: "),
                       BukkitUtil.button("&3[&fFocus&3]", "/sk sidebar "+skill.getKey(), "&7Focus "+skill.getTitle()+" in the sidebar"), " ",
                       BukkitUtil.button("&3[&fReset&3]", "/sk sidebar reset", "&7Reset the sidebar"), " ",
                       BukkitUtil.button("&3[&fOn&3]", "/sk sidebar on", "&7Turn sidebar on"), " ", 
                       BukkitUtil.button("&3[&fOff&3]", "/sk sidebar off", "&7Turn sidebar off"));
        BukkitUtil.msg(player, " &r%s", skill.getDescription());
        BukkitUtil.msg(player, "");
    }

    void modifySidebar(Player player, String arg)
    {
        if ("reset".equals(arg)) {
            BukkitPlayer.of(player).setSidebarEnabled(player, false);
            BukkitPlayer.of(player).setSidebarEnabled(player, true);
            BukkitUtil.msg(player, "&bSidebar reset");
        } else if ("off".equals(arg)) {
            BukkitPlayer.of(player).setSidebarEnabled(player, false);
            BukkitUtil.msg(player, "&bSidebar disabled");
        } else if ("on".equals(arg)) {
            BukkitPlayer.of(player).setSidebarEnabled(player, true);
            BukkitUtil.msg(player, "&bSidebar enabled");
        } else {
            BukkitSkill skill = getSkills().skillByName(arg);
            if (skill == null) return;
            BukkitPlayer.of(player).setSidebarEnabled(player, true);
            BukkitPlayer.of(player).setForcedSkill(skill);
            BukkitUtil.msg(player, "&bDisplaying %s", skill.getTitle());
        }
    }
}
