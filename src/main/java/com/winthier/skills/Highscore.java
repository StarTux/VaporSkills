package com.winthier.skills;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Value;

@Getter @RequiredArgsConstructor
public final class Highscore {
    private final SkillsPlugin plugin;
    final List<Row> rows = new ArrayList<>();

    @Value
    public static class Row {
        final int rank;
        final UUID player;
        final int skillPoints;
        final int skillLevel;
    }

    static Highscore create(SkillsPlugin plugin, SkillType skill) {
        Highscore result = new Highscore(plugin);
        int lastLevel = -1;
        int rank = 0;
        for (SQLScore score : plugin.getDb().find(SQLScore.class).where().eq("skill", skill.key).orderByDescending("skillLevel").findList()) {
            UUID player = score.getPlayer();
            int skillPoints = (int)score.getSkillPoints();
            int skillLevel = score.getSkillLevel();
            if (skillLevel != lastLevel) {
                rank += 1;
                lastLevel = skillLevel;
            }
            result.rows.add(new Row(rank, player, skillPoints, skillLevel));
        }
        return result;
    }

    public int rankOfPlayer(UUID player) {
        if (rows.isEmpty()) return -1;
        for (Row row : rows) {
            if (row.player.equals(player)) return row.rank;
        }
        return -1;
    }

    public int indexOfPlayer(UUID player) {
        if (rows.isEmpty()) return -1;
        int i = 0;
        for (Row row : rows) {
            if (row.player.equals(player)) return i;
            i += 1;
        }
        return -1;
    }

    public Row rowAt(int index) {
        return rows.get(index);
    }

    public int size() {
        return rows.size();
    }
}
