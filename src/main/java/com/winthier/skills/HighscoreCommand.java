package com.winthier.skills;

import com.winthier.playercache.PlayerCache;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.apache.commons.lang3.text.WordUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

class HighscoreCommand implements CommandExecutor {
    SkillsPlugin getSkills() {
        return SkillsPlugin.getInstance();
    }

    SkillsPlugin getPlugin() {
        return SkillsPlugin.getInstance();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
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

    String formatHighscoreAroundPlayer(Highscore hi, UUID uuid) {
        int rank = hi.rankOfPlayer(uuid);
        int index = hi.indexOfPlayer(uuid);
        if (index < 0) index = 2;
        StringBuilder sb = new StringBuilder();
        for (int i = Math.max(0, index - 2); i <= Math.min(hi.size() - 1, index + 2); ++i) {
            if (sb.length() > 0) sb.append("\n");
            Highscore.Row row = hi.rowAt(i);
            if (uuid.equals(row.getPlayer())) {
                sb.append(Msg.format("&3#%02d &blvl &f%d &b%s", row.getRank(), row.getSkillLevel(), PlayerCache.nameForUuid(row.getPlayer())));
            } else {
                sb.append(Msg.format("&3#%02d &blvl &f%d &3%s", row.getRank(), row.getSkillLevel(), PlayerCache.nameForUuid(row.getPlayer())));
            }
        }
        return sb.toString();
    }

    void listSkills(Player player) {
        UUID uuid = player.getUniqueId();
        Msg.msg(player, "");
        Msg.msg(player, "&3&lHighscore &7&o(Click for more info)");
        List<Object> message = new ArrayList<>();
        Highscore hi = getSkills().getScore().getHighscore(null);
        int rank = hi.rankOfPlayer(uuid);
        String rankString = rank > 0 ? "#" + rank : "-";
        message.add(Msg.button(
                        "&bTotal&3(&f" + rankString + "&3)",
                        "/hi total",
                        "&a/hi total",
                        "&3&lTotal &f" + rankString,
                        formatHighscoreAroundPlayer(hi, uuid),
                        "&r" + WordUtils.wrap(getPlugin().getConfig().getString("total.Description", ""), 32),
                        "&7Click for more details"));
        for (Skill skill : getSkills().getSkills()) {
            if (!skill.isEnabled()) continue;
            hi = getSkills().getScore().getHighscore(skill.skillType);
            rank = hi.rankOfPlayer(uuid);
            rankString = rank > 0 ? "#" + rank : "-";
            message.add(" ");
            message.add(Msg.button(
                            "&b" + skill.getShorthand() + "&3(&f" + rankString + "&3)",
                            "/hi " + skill.skillType.key,
                            "&a/hi " + skill.skillType.key,
                            "&3&l" + skill.getDisplayName() + " &f" + rankString,
                            formatHighscoreAroundPlayer(hi, uuid),
                            "&r" + WordUtils.wrap(skill.getDescription(), 32),
                            "&7Click for more details"));
        }
        Msg.raw(player, message);
        Msg.msg(player, "");
    }

    // TODO page numbers
    void skillDetail(Player player, String name) {
        Highscore hi;
        String displayName;
        if ("total".equals(name)) {
            hi = getSkills().getScore().getHighscore(null);
            displayName = "Total";
        } else {
            Skill skill = getSkills().skillByName(name);
            if (skill == null) {
                player.sendMessage("Skill not found: " + name);
                return;
            }
            hi = getSkills().getScore().getHighscore(skill.skillType);
            displayName = skill.getDisplayName();
        }
        final UUID uuid = player.getUniqueId();
        Msg.msg(player, "");
        int rank = hi.rankOfPlayer(uuid);
        String rankString = rank > 0 ? "#" + rank : "-";
        Msg.msg(player, "&3&l%s &bHighscore &3(Rank &f%s&3)", displayName, rankString);
        int size = Math.min(10, hi.size());
        for (int i = 0; i < size; ++i) {
            Highscore.Row row = hi.rowAt(i);
            Msg.msg(player, " &3#%d &f%02d &3%s", row.getRank(), row.getSkillLevel(), PlayerCache.nameForUuid(row.getPlayer()));
        }
        Msg.msg(player, "");
    }
}
