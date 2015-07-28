package com.winthier.skills.sql;

import com.avaje.ebean.validation.Length;
import com.avaje.ebean.validation.NotEmpty;
import com.avaje.ebean.validation.NotNull;
import com.winthier.skills.Skill;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
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
    // Cache
    @Value static class Key { UUID player; String skill; }
    final static Map<Key, SQLScore> cache = new HashMap<>();
    final static Set<SQLScore> dirties = new HashSet<>();
    // Content
    @Id Integer id;
    @NotNull @ManyToOne SQLPlayer player;
    @NotNull @ManyToOne SQLString skill;
    @NotNull float skillPoints;
    @NotNull int skillLevel;
    @Version Date version;

    private SQLScore(SQLPlayer player, SQLString skill)
    {
	setPlayer(player);
	setSkill(skill);
	setSkillPoints(0);
	setSkillLevel(0);
    }

    public static SQLScore of(UUID uuid, String skill)
    {
	Key key = new Key(uuid, skill);
	SQLScore result = cache.get(key);
	if (result == null) {
	    result = SQLDB.get().find(SQLScore.class).where()
		.eq("player", SQLPlayer.of(uuid))
		.eq("skill", SQLString.of(skill))
		.findUnique();
	    if (result == null) {
		result = new SQLScore(SQLPlayer.of(uuid), SQLString.of(skill));
		result.save();
	    }
	    cache.put(key, result);
	}
	return result;
    }

    public static List<SQLScore> rank(Skill skill)
    {
        return SQLDB.get().find(SQLScore.class).where()
            .eq("skill_id", SQLString.of(skill).getId())
            .gt("skill_level", 0)
            .orderBy("skill_points DESC")
            .findList();
    }

    public void save()
    {
	SQLDB.get().save(this);
    }

    public void setDirty()
    {
	dirties.add(this);
    }

    public static void saveAll()
    {
	SQLDB.get().save(dirties);
	dirties.clear();
    }
}
