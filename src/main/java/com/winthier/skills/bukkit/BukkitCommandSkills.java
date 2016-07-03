package com.winthier.skills.bukkit;

import com.winthier.skills.Highscore;
import com.winthier.skills.Reward;
import com.winthier.skills.util.Strings;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.apache.commons.lang3.text.WordUtils;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

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
        } else if (args.length == 1 && "checkitem".equals(cmd)) {
            checkItem(player);
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
        BukkitUtil.msg(player, "&3&lSkills &7&o(Click for more info)");
        List<Object> message = new ArrayList<>();
        for (BukkitSkill skill : getSkills().getSkills()) {
            if (!skill.isEnabled()) continue;
            int skillPoints = (int)getSkills().getScore().getSkillPoints(uuid, skill);
            int skillLevel = getSkills().getScore().getSkillLevel(uuid, skill);
            int pointsInLevel = getSkills().getScore().pointsInLevel(skillPoints);
            int pointsToLevelUp = getSkills().getScore().pointsToLevelUpTo(skillLevel + 1);
            if (!message.isEmpty()) message.add(" ");
            message.add(BukkitUtil.button(
                            ChatColor.AQUA,
                            "&b" + skill.getShorthand() + "&3(&f"+skillLevel+"&3)",
                            "/sk " + skill.getKey(),
                            "&a/sk " + skill.getKey(),
                            "&3&l" + skill.getDisplayName() + " " + BukkitUtil.progressBar(pointsInLevel, pointsToLevelUp),
                            "&3Skill Level: &b" + skillLevel,
                            "&3Skill Points: &f"+pointsInLevel+"&3/&f"+pointsToLevelUp,
                            "&r" + WordUtils.wrap(skill.getDescription(), 32),
                            "&7Click for more details"));
        }
        BukkitUtil.raw(player, message);
        BukkitUtil.raw(player,
                       BukkitUtil.format(" &3Sidebar: "),
                       BukkitUtil.button("&3[&fReset&3]", "/sk sidebar reset", "&a/sk sidebar reset", "&5&oReset the sidebar"),
                       " ",
                       BukkitUtil.button("&3[&fOn&3]", "/sk sidebar on", "&a/sk sidebar on", "&5&oEnable sidebar"),
                       " ", 
                       BukkitUtil.button("&3[&fOff&3]", "/sk sidebar off", "&a/sk sidebar off", "&5&oDisable sidebar"));
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
        // Title
        BukkitUtil.msg(player, "&3&l%s &bLevel &f%d %s",
                       skill.getDisplayName(),
                       skillLevel,
                       BukkitUtil.progressBar(pointsInLevel, pointsToLevelUp));
        // Statistics
        BukkitUtil.raw(player,
                       BukkitUtil.format(" &3Skill Points: "),
                       BukkitUtil.tooltip(
                           BukkitUtil.format("&f%d&3/&f%d",
                                             pointsInLevel,
                                             pointsToLevelUp),
                           BukkitUtil.format("&3Total Skill Points: &f%d", skillPoints),
                           BukkitUtil.format("&3For Next Level: &f%d",
                                             getSkills().getScore().pointsForNextLevel(skillPoints))));
        // Bonus
        int level = getSkills().getScore().getSkillLevel(player.getUniqueId(), skill);
        int bonusFactor = level / 10;
        int nextBonusLevel = ((level / 10) + 1) * 10;
        BukkitUtil.raw(player,
                       BukkitUtil.format(" &3Money Bonus: "),
                       BukkitUtil.tooltip(
                           BukkitUtil.format("%d%%", bonusFactor),
                           BukkitUtil.format("&3Next Bonus Level: &f%d", nextBonusLevel)));
        // Highscore
        Highscore hi = getSkills().getScore().getHighscore(skill);
        int rank = hi.rankOfPlayer(uuid);
        String rankString = rank > 0 ? "#" + rank : "-";
        BukkitUtil.raw(player,
                       BukkitUtil.format(" &3Your rank: "),
                       BukkitUtil.button("&f"+rankString+" &3[&fHighscore&3]",
                                         "/hi " + skill.getKey(),
                                         "&a/hi " + skill.getKey(),
                                         "&3&l" + skill.getDisplayName() + " &f" + rankString,
                                         getPlugin().getHighscoreCommand().formatHighscoreAroundPlayer(hi, uuid),
                                         "&7Click for more details"));
        // Sidebar
        BukkitUtil.raw(player,
                       BukkitUtil.format(" &3Sidebar: "),
                       BukkitUtil.button("&3[&fFocus&3]", "/sk sidebar "+skill.getKey(), "&a/sk sidebar "+skill.getKey(), "&5&oFocus "+skill.getDisplayName()+" in the sidebar"),
                       " ",
                       BukkitUtil.button("&3[&fReset&3]", "/sk sidebar reset", "&a/sk sidebar reset", "&5&oReset the sidebar"),
                       " ",
                       BukkitUtil.button("&3[&fOn&3]", "/sk sidebar on", "&a/sk sidebar on", "&5&oTurn sidebar on"),
                       " ", 
                       BukkitUtil.button("&3[&fOff&3]", "/sk sidebar off", "&a/sk sidebar off", "&5&oTurn sidebar off"));
        // Sacrifice special
        if (skill.getSkillType() == BukkitSkillType.SACRIFICE) {
            BukkitUtil.raw(player,
                           BukkitUtil.format(" &3Check item in hand: "),
                           BukkitUtil.button("&3[&fCheck&3]", "/sk checkitem",
                                             "&a/sk checkitem",
                                             "&5&oAsk the Creeper Overlord",
                                             "&5&oabout the value of the item",
                                             "&5&oin your hand"));
        }
        // Description
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
            BukkitUtil.msg(player, "&bDisplaying %s", skill.getDisplayName());
        }
    }

    void checkItem(Player player)
    {
        ItemStack item = player.getItemInHand();
        final String prefix = BukkitUtil.format("&r[&2Creeper Overlord&r] ");
        if (item == null || item.getType() == Material.AIR) {
            BukkitUtil.msg(player, prefix + "&cThere is no item in your hand");
            return;
        }
        Reward reward = ((BukkitSkillSacrifice)getSkills().skillByType(BukkitSkillType.SACRIFICE)).fullRewardForItem(item);
        double money = (double)reward.getMoney() * (double)item.getAmount();
        BukkitUtil.msg(player, prefix + "Sacrificing the %d item%s in your hand will grant you &a%s&r.", item.getAmount(), item.getAmount() == 1 ? "" : "s", BukkitSkillsPlugin.getInstance().getEconomy().format(money));
    }
}
