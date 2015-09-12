package com.winthier.skills.sql;

import com.avaje.ebean.validation.Length;
import com.avaje.ebean.validation.NotEmpty;
import com.avaje.ebean.validation.NotNull;
import com.winthier.skills.Skill;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.PersistenceException;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import lombok.Value;

@Entity
@Table(name = "scores",
       uniqueConstraints = @UniqueConstraint(columnNames = {"player_id", "skill_id"}))
@Getter
@Setter
@NoArgsConstructor
public class SQLScore
{
    static final float SAVING_THRESHOLD = 200.0f;
    // Cache
    @Value static class Key { UUID player; String skill; }
    final static Map<Key, SQLScore> cache = new HashMap<>();
    @Getter final static Set<SQLScore> dirties = new HashSet<>();
    // Content
    @Id Integer id;
    @NotNull @ManyToOne SQLPlayer player;
    @NotNull @ManyToOne SQLString skill;
    @NotNull float skillPoints;
    @NotNull int skillLevel;
    @Version Date version;
    transient float skillPointsAdded = 0;

    private SQLScore(SQLPlayer player, SQLString skill)
    {
	setPlayer(player);
	setSkill(skill);
	setSkillPoints(0);
	setSkillLevel(0);
    }

    public static SQLScore of(UUID uuid, Skill skill)
    {
	Key key = new Key(uuid, skill.getKey());
	SQLScore result = cache.get(key);
	if (result == null) {
	    result = SQLDB.get().find(SQLScore.class).where()
		.eq("player", SQLPlayer.of(uuid))
		.eq("skill", SQLString.of(skill.getKey()))
		.findUnique();
	    if (result == null) {
		result = new SQLScore(SQLPlayer.of(uuid), SQLString.of(skill.getKey()));
		result.setDirty();
	    }
	    cache.put(key, result);
	}
	return result;
    }

    public static List<SQLScore> rank(Skill skill)
    {
        return SQLDB.get().find(SQLScore.class).where()
            .eq("skill_id", SQLString.of(skill.getKey()).getId())
            .gt("skill_level", 0)
            .orderBy("skill_points DESC")
            .findList();
    }

    public void setDirty(float val)
    {
	dirties.add(this);
        skillPointsAdded += val;
    }

    public void setDirty()
    {
        setDirty(SAVING_THRESHOLD);
    }

    public static void saveAll()
    {
        try {
            SQLDB.get().save(dirties);
        } catch (PersistenceException pe) {
            System.err.println("SQLScore saveAll throws PersistenceException. Clearing cache");
            pe.printStackTrace();
            cache.clear();
        }
        dirties.clear();
    }

    public static void saveSome()
    {
        for (Iterator<SQLScore> iter = dirties.iterator(); iter.hasNext(); ) {
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
