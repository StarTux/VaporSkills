package com.winthier.skills;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
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
        } else if (args.length == 2 && args[0].startsWith("perk")) {
            skillPerks(player, args[1]);
        } else if (args.length == 2 && args[0].equals("unlock")) {
            unlockPerk(player, args[1]);
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
        ComponentBuilder cb = new ComponentBuilder(" ");
        cb.append("Perks").color(ChatColor.BLUE);
        int perks = plugin.getScore().getPerks(uuid, skill.skillType);
        if (perks > 0) {
            if (perks > 1) {
                cb.append(" " + perks + " Points ");
            } else {
                cb.append(" " + perks + " Point ");
            }
            cb.color(ChatColor.WHITE);
            cb.append("§9[§fSpend§9]").color(ChatColor.WHITE)
                .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText("§9View and unlock perks")))
                .event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/sk perk " + skill.skillType.key));
        } else {
            int perkPoints = plugin.getScore().getPerkPoints(uuid, skill.skillType);
            int left = Score.perkPointsPerPerk() - perkPoints;
            if (left > 1) {
                cb.append(" " + left + " more levels required. ").color(ChatColor.WHITE);
            } else {
                cb.append(" one more level required. ").color(ChatColor.WHITE);
            }
            cb.append("§9[§fView§9]").color(ChatColor.WHITE)
                .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText("§9View perks")))
                .event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/sk perk " + skill.skillType.key));
        }
        player.spigot().sendMessage(cb.create());
        Msg.msg(player, "");
    }

    void skillPerks(Player player, String name) {
        Skill skill = plugin.skillByName(name);
        if (skill == null) {
            Msg.msg(player, "&cSkill not found: %s", name);
            return;
        }
        skillPerks(player, skill);
    }

    void skillPerks(Player player, Skill skill) {
        final UUID uuid = player.getUniqueId();
        // Title
        Msg.msg(player, "&9&l%s &7Perks", skill.getDisplayName());
        // Perks
        Set<Perk> playerPerks = plugin.getScore().getPerks(player.getUniqueId());
        ComponentBuilder cb = new ComponentBuilder("");
        Perk pre = null;
        for (Perk perk: Perk.values()) {
            if (perk.skillType != skill.skillType) continue;
            boolean hasPerk = playerPerks.contains(perk);
            boolean canSeePerk = hasPerk || perk.depends == null || playerPerks.contains(perk.depends);
            if (!canSeePerk) continue;
            if (pre != null && pre == perk.depends) {
                cb.append("  ").color(ChatColor.BLUE).strikethrough(true);
                cb.append("").strikethrough(false);
            } else {
                cb.append("  ");
            }
            pre = perk;
            String title = plugin.getPerkInfo(perk.key).title;
            String description = plugin.getPerkInfo(perk.key).description;
            if (hasPerk) {
                // Has perk
                cb.append("§9[§6" + title + "§9]").color(ChatColor.GOLD);
                cb.event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText("§9" + title + "\n§r§d§o" + description)));
            } else if (canSeePerk) {
                // Can see perk
                cb.append("§9[§8" + title + "§9]").color(ChatColor.DARK_GRAY);
                cb.event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText("§9" + title + "\n§4LOCKED\n§eUnlock for 1 Perk.\n§r§d§o" + description)));
                cb.event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/sk unlock " + perk.key));
            }
        }
        player.spigot().sendMessage(cb.create());
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

    boolean unlockPerk(Player player, String arg) {
        Perk perk;
        try {
            perk = Perk.valueOf(arg.toUpperCase());
        } catch (IllegalArgumentException iae) {
            return true;
        }
        UUID uuid = player.getUniqueId();
        if (plugin.getScore().hasPerk(uuid, perk)) return true;
        if (perk.depends != null && !plugin.getScore().hasPerk(uuid, perk.depends)) return true;
        if (plugin.getScore().getPerks(uuid, perk.skillType) < 1) {
            int left = Score.perkPointsPerPerk() - plugin.getScore().getPerkPoints(uuid, perk.skillType);
            if (left > 1) {
                Msg.msg(player, "&cYou need to gain " + left + " more levels to unlock another perk!");
            } else {
                Msg.msg(player, "&cYou need to gain one more level to unlock another perk!");
            }
            return true;
        }
        plugin.getScore().givePerks(uuid, perk.skillType, -1);
        plugin.getScore().unlockPerk(uuid, perk);
        Msg.msg(player, "&6Perk %s unlocked!", plugin.getPerkInfo(perk.key).title);
        Msg.title(player, "&6" + plugin.getPerkInfo(perk.key).title, "&6Perk Unlocked!");
        skillPerks(player, plugin.getSkill(perk.skillType));
        Msg.consoleCommand("minecraft:advancement grant " + player.getName() + " until " + "skills:" + perk.skillType.key + "/" + perk.key);
        return true;
    }
}
