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
        double skillPoints;
        int skillLevel;
    }

    final long timestamp = System.currentTimeMillis();
    final List<Row> rows = new ArrayList<>();

    private Highscore() {}

    static Highscore create(Skill skill)
    {
        Highscore result = new Highscore();
        int rank = 1;
        for (SQLScore score : SQLScore.rank(skill)) {
            UUID player = score.getPlayer().getUuid();
            double skillPoints = score.getSkillPoints();
            int skillLevel = score.getSkillLevel();
            result.rows.add(new Row(rank++, player, skillPoints, skillLevel));
        }
        return result;
    }

    public int rankOfPlayer(UUID player)
    {
        for (Row row : rows) {
            if (row.player.equals(player)) return row.rank;
        }
        return rows.size() + 1;
    }

    public long ageInSeconds()
    {
        return (System.currentTimeMillis() - timestamp) / 1000;
    }
}
