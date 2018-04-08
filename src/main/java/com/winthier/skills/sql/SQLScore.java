package com.winthier.skills.sql;

import com.winthier.skills.Skill;
import com.winthier.skills.SkillsPlugin;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.PersistenceException;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.Value;

@Entity
@Table(name = "scores",
       uniqueConstraints = @UniqueConstraint(columnNames = {"player_id", "skill_id"}))
@Getter
@Setter
@NoArgsConstructor
public final class SQLScore {
    private static final float SAVING_THRESHOLD = 200.0f;

    // Cache
    @Value public static class Entry {
        final UUID player;
        final double skillPoints;
        final int skillLevel;
    }

    @Value private static class Key {
        final UUID player;
        final String skill;
    }

    static final Map<Key, SQLScore> CACHE = new HashMap<>();
    @Getter private static final Set<SQLScore> DIRTIES = new HashSet<>();
    // Content
    @Id private Integer id;
    @Column(nullable = false) @ManyToOne private SQLPlayer player;
    @Column(nullable = false) @ManyToOne private SQLString skill;
    @Column(nullable = false) private float skillPoints;
    @Column(nullable = false) private int skillLevel;
    @Version private Date version;
    private transient float skillPointsAdded = 0;

    private SQLScore(SQLPlayer player, SQLString skill) {
        setPlayer(player);
        setSkill(skill);
        setSkillPoints(0);
        setSkillLevel(0);
    }

    public static SQLScore of(UUID uuid, Skill skill) {
        Key key = new Key(uuid, skill.getKey());
        SQLScore result = CACHE.get(key);
        if (result == null) {
            result = SQLDB.get().find(SQLScore.class).where()
                .eq("player", SQLPlayer.of(uuid))
                .eq("skill", SQLString.of(skill.getKey()))
                .findUnique();
            if (result == null) {
                result = new SQLScore(SQLPlayer.of(uuid), SQLString.of(skill.getKey()));
                result.setDirty();
            }
            CACHE.put(key, result);
        }
        return result;
    }

    public static List<Entry> rank(Skill skill) {
        if (skill == null) return rank();
        List<Entry> result = new ArrayList<>();
        for (SQLScore score : SQLDB.get().find(SQLScore.class).where()
                 .eq("skill", SQLString.of(skill.getKey()))
                 .gt("skill_level", 0)
                 .orderByDescending("skill_points")
                 .limit(100)
                 .findList()) {
            result.add(new Entry(score.getPlayer().getUuid(), score.getSkillPoints(), score.getSkillLevel()));
        }
        return result;
    }

    public static List<Entry> rank() {
        List<Integer> skillIds = new ArrayList<>();
        for (Skill skill : SkillsPlugin.getInstance().getSkills()) skillIds.add(SQLString.of(skill.getKey()).getId());
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT player_id, uuid, SUM(skill_points) AS skill_points FROM `")
            .append(SQLDB.get().getTable(SQLScore.class).getTableName())
            .append("` JOIN `")
            .append(SQLDB.get().getTable(SQLPlayer.class).getTableName())
            .append("` players ON player_id = players.id WHERE skill_id IN (")
            .append(skillIds.get(0));
        for (int i = 1; i < skillIds.size(); i += 1) sb.append(", ").append(skillIds.get(i));
        sb.append(") GROUP BY player_id ORDER BY skill_points DESC LIMIT 100");
        ResultSet row = SQLDB.get().executeQuery(sb.toString());
        List<Entry> result = new ArrayList<>();
        try {
            while (row.next()) {
                UUID uuid = UUID.fromString(row.getString("uuid"));
                double skillPoints = (double)row.getFloat("skill_points") / (double)skillIds.size();
                int skillLevel = SkillsPlugin.getInstance().getScore().levelForPoints(skillPoints);
                result.add(new Entry(uuid, skillPoints, skillLevel));
            }
        } catch (SQLException sqle) {
            sqle.printStackTrace();
        }
        return result;
    }

    public void setDirty(float val) {
        DIRTIES.add(this);
        skillPointsAdded += val;
    }

    public void setDirty() {
        setDirty(SAVING_THRESHOLD);
    }

    public static void saveAll() {
        try {
            SQLDB.get().save(DIRTIES);
        } catch (PersistenceException pe) {
            System.err.println("SQLScore saveAll throws PersistenceException. Clearing cache");
            pe.printStackTrace();
            CACHE.clear();
        }
        DIRTIES.clear();
    }

    public static void saveSome() {
        for (Iterator<SQLScore> iter = DIRTIES.iterator(); iter.hasNext();) {
            SQLScore score = iter.next();
            if (score.skillPointsAdded < SAVING_THRESHOLD) continue;
            score.skillPointsAdded = 0;
            try {
                SQLDB.get().save(score);
            } catch (PersistenceException pe) {
                System.err.println("SQLScore saveSome throws PersistenceException.");
            }
            iter.remove();
            return;
        }
    }
}
