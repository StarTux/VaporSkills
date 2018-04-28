package com.winthier.skills;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@RequiredArgsConstructor
class SkillsCommand implements CommandExecutor {
    private final SkillsPlugin plugin;

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        final Player player = sender instanceof Player ? (Player)sender : null;
        if (player == null) {
            sender.sendMessage("Player expected");
            return true;
        }
        String cmd = args.length > 0 ? args[0].toLowerCase() : "";
        if (args.length == 0) {
            listSkills(player);
        } else if (args.length >= 1 && "progressbar".equals(cmd)) {
            String sub = args.length == 2 ? args[1].toLowerCase() : "";
            modifyProgressBar(player, sub);
        } else if (args.length == 1) {
            skillDetail(player, cmd);
        } else {
            return false;
        }
        return true;
    }

    void listSkills(Player player) {
        UUID uuid = player.getUniqueId();
        Msg.msg(player, "");
        Msg.msg(player, "&3&lSkills &7&o(Click for more info)");
        List<Object> message = new ArrayList<>();
        for (Skill skill : plugin.getSkills()) {
            if (!skill.isEnabled()) continue;
            int skillPoints = (int)plugin.getScore().getSkillPoints(uuid, skill.skillType);
            int skillLevel = plugin.getScore().getSkillLevel(uuid, skill.skillType);
            int pointsInLevel = Score.pointsInLevel(skillPoints);
            int pointsToLevelUp = Score.pointsToLevelUpTo(skillLevel + 1);
            if (!message.isEmpty()) message.add(" ");
            message.add(Msg.button(
                            ChatColor.AQUA,
                            "&b" + skill.getShorthand() + "&3(&f" + skillLevel + "&3)",
                            "/sk " + skill.skillType.key,
                            "&a/sk " + skill.skillType.key,
                            "&3&l" + skill.getDisplayName() + " " + Msg.progressBar(pointsInLevel, pointsToLevelUp),
                            "&3Skill Level: &b" + skillLevel,
                            "&3Skill Points: &f" + pointsInLevel + "&3/&f" + pointsToLevelUp,
                            "&r" + Msg.wrap(skill.getDescription(), 32),
                            "&7Click for more details"));
        }
        Msg.raw(player, message);
        Msg.raw(player,
                       Msg.format("&3Progress Bar: "),
                       Msg.button("&3[&fOn&3]", "/sk progressbar on", "&a/sk progressbar on", "&5&oEnable Progress Bar"),
                       " ",
                       Msg.button("&3[&fOff&3]", "/sk progressbar off", "&a/sk progressbar off", "&5&oDisable Progress Bar"));
        Msg.msg(player, "");
    }

    void skillDetail(Player player, String name) {
        Skill skill = plugin.skillByName(name);
        if (skill == null) {
            Msg.msg(player, "&cSkill not found: %s", name);
            return;
        }
        final UUID uuid = player.getUniqueId();
        int skillPoints = (int)plugin.getScore().getSkillPoints(uuid, skill.skillType);
        int skillLevel = plugin.getScore().getSkillLevel(uuid, skill.skillType);
        int pointsInLevel = Score.pointsInLevel(skillPoints);
        int pointsToLevelUp = Score.pointsToLevelUpTo(skillLevel + 1);
        Msg.msg(player, "");
        // Title
        Msg.msg(player, "&3&l%s &bLevel &f%d %s",
                       skill.getDisplayName(),
                       skillLevel,
                       Msg.progressBar(pointsInLevel, pointsToLevelUp));
        // Statistics
        Msg.raw(player,
                       Msg.format(" &3Skill Points: "),
                       Msg.tooltip(
                           Msg.format("&f%d&3/&f%d",
                                             pointsInLevel,
                                             pointsToLevelUp),
                           Msg.format("&3Total Skill Points: &f%d", skillPoints),
                           Msg.format("&3For Next Level: &f%d",
                                             Score.pointsForNextLevel(skillPoints))));
        // Bonus
        int level = plugin.getScore().getSkillLevel(player.getUniqueId(), skill.skillType);
        int bonusFactor = level / 10;
        int nextBonusLevel = ((level / 10) + 1) * 10;
        Msg.raw(player,
                Msg.format(" &3Money Bonus: "),
                Msg.tooltip(
                    Msg.format("%d%%", bonusFactor),
                    Msg.format("&3Next Bonus Level: &f%d", nextBonusLevel)));
        // Highscore
        Highscore hi = plugin.getScore().getHighscore(skill.skillType);
        int rank = hi.rankOfPlayer(uuid);
        String rankString = rank > 0 ? "#" + rank : "-";
        Msg.raw(player,
                       Msg.format(" &3Your rank: "),
                       Msg.button("&f" + rankString + " &3[&fHighscore&3]",
                                         "/hi " + skill.skillType.key,
                                         "&a/hi " + skill.skillType.key,
                                         "&3&l" + skill.getDisplayName() + " &f" + rankString,
                                         plugin.getHighscoreCommand().formatHighscoreAroundPlayer(hi, uuid),
                                         "&7Click for more details"));
        // Description
        Msg.msg(player, " &r%s", skill.getDescription());
        Msg.msg(player, "");
    }

    void modifyProgressBar(Player player, String arg) {
        if ("off".equals(arg)) {
            plugin.getSession(player).setProgressBarEnabled(false);
            Msg.msg(player, "&bProgress bar disabled");
        } else if ("on".equals(arg)) {
            plugin.getSession(player).setProgressBarEnabled(true);
            Msg.msg(player, "&bProgress bar enabled");
        }
    }
}
