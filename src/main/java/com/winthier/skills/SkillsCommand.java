package com.winthier.skills;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
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
        Msg.msg(player, "&9&lSkills &7&o(Click for more info)");
        List<Object> message = new ArrayList<>();
        for (Skill skill : plugin.getSkills()) {
            int skillPoints = (int)plugin.getScore().getSkillPoints(uuid, skill.skillType);
            int skillLevel = plugin.getScore().getSkillLevel(uuid, skill.skillType);
            int pointsInLevel = Score.pointsInLevel(skillPoints);
            int pointsToLevelUp = Score.pointsToLevelUpTo(skillLevel + 1);
            if (!message.isEmpty()) message.add(" ");
            message.add(Msg.button(
                            org.bukkit.ChatColor.GRAY,
                            "&7" + skill.getShorthand() + "&9(&f" + skillLevel + "&9)",
                            "/sk " + skill.skillType.key,
                            "&a/sk " + skill.skillType.key,
                            "&9&l" + skill.getDisplayName() + " " + Msg.progressBar(pointsInLevel, pointsToLevelUp),
                            "&9Skill Level: &7" + skillLevel,
                            "&9Skill Points: &f" + pointsInLevel + "&9/&f" + pointsToLevelUp,
                            "&r" + Msg.wrap(skill.getDescription(), 32),
                            "&7Click for more details"));
        }
        Msg.raw(player, message);
        Msg.raw(player,
                       Msg.format("&9Progress Bar: "),
                       Msg.button("&9[&fOn&9]", "/sk progressbar on", "&a/sk progressbar on", "&5&oEnable Progress Bar"),
                       " ",
                       Msg.button("&9[&fOff&9]", "/sk progressbar off", "&a/sk progressbar off", "&5&oDisable Progress Bar"));
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
        Msg.msg(player, "&9&l%s &7Level &f%d %s",
                       skill.getDisplayName(),
                       skillLevel,
                       Msg.progressBar(pointsInLevel, pointsToLevelUp));
        // Description
        Msg.msg(player, " &r%s", skill.getDescription());
        // Statistics
        Msg.raw(player,
                       Msg.format(" &9Skill Points: "),
                       Msg.tooltip(
                           Msg.format("&f%d&9/&f%d",
                                             pointsInLevel,
                                             pointsToLevelUp),
                           Msg.format("&9Total Skill Points: &f%d", skillPoints),
                           Msg.format("&9For Next Level: &f%d",
                                             Score.pointsForNextLevel(skillPoints))));
        // Highscore
        Highscore hi = plugin.getScore().getHighscore(skill.skillType);
        int rank = hi.rankOfPlayer(uuid);
        String rankString = rank > 0 ? "#" + rank : "-";
        Msg.raw(player,
                       Msg.format(" &9Your rank: "),
                       Msg.button("&f" + rankString + " &9[&fHighscore&9]",
                                         "/hi " + skill.skillType.key,
                                         "&a/hi " + skill.skillType.key,
                                         "&9&l" + skill.getDisplayName() + " &f" + rankString,
                                         plugin.getHighscoreCommand().formatHighscoreAroundPlayer(hi, uuid),
                                         "&7Click for more details"));
        // Perks
        ComponentBuilder cb = new ComponentBuilder("");
        for (Perk perk: Perk.values()) {
            if (perk.skillType != skill.skillType) continue;
            cb.append(" ");
            ConfigurationSection section = plugin.getPerksConfig().getConfigurationSection(perk.key);
            if (section == null) section = plugin.getPerksConfig().createSection("tmp");
            String title = section.getString("title");
            String description = section.getString("description");
            if (title == null) title = Msg.niceEnumName(perk);
            if (description == null) description = title;
            cb.append("§9[§f" + title + "§9]").color(ChatColor.WHITE);
            cb.event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText("§9§l" + title + "\n§r§d§o" + description)));
        }
        player.spigot().sendMessage(cb.create());
        Msg.msg(player, "");
    }

    void modifyProgressBar(Player player, String arg) {
        if ("off".equals(arg)) {
            plugin.getSession(player).setProgressBarEnabled(false);
            Msg.msg(player, "&7Progress bar disabled");
        } else if ("on".equals(arg)) {
            plugin.getSession(player).setProgressBarEnabled(true);
            Msg.msg(player, "&7Progress bar enabled");
        }
    }
}
