package com.winthier.skills;

import com.winthier.skills.sql.SQLScore;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.Value;

@Getter
public class Highscore
{
    @Value
    public static class Row {
        int rank;
        UUID player;
        int skillPoints;
        int skillLevel;
    }

    final long timestamp = System.currentTimeMillis();
    final List<Row> rows = new ArrayList<>();

    private Highscore() {}

    static Highscore create(Skill skill)
    {
        Highscore result = new Highscore();
        int lastLevel = -1;
        int rank = 0;
        for (SQLScore score : SQLScore.rank(skill)) {
            UUID player = score.getPlayer().getUuid();
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

    public int rankOfPlayer(UUID player)
    {
        if (rows.isEmpty()) return 1;
        for (Row row : rows) {
            if (row.player.equals(player)) return row.rank;
        }
        return -1;
    }

    public int indexOfPlayer(UUID player)
    {
        if (rows.isEmpty()) return 0;
        int i = 0;
        for (Row row : rows) {
            if (row.player.equals(player)) break;
            i += 1;
        }
        return -1;
    }

    public Row rowAt(int index)
    {
        return rows.get(index);
    }

    public int size()
    {
        return rows.size();
    }

    public long ageInSeconds()
    {
        return (System.currentTimeMillis() - timestamp) / 1000;
    }
}
