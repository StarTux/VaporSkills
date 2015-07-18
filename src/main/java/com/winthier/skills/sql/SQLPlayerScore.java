package com.winthier.skills.sql;

import com.avaje.ebean.validation.Length;
import com.avaje.ebean.validation.NotEmpty;
import com.avaje.ebean.validation.NotNull;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.Value;

@Entity
@Table(name = "player_scores",
       uniqueConstraints = @UniqueConstraint(columnNames = {"player_id", "skill_id"}))
@Getter
@Setter
public class SQLPlayerScore
{
    // Cache
    @Value static class Key { UUID player; String skill; }
    final static Map<Key, SQLPlayerScore> cache = new HashMap<>();
    // Content
    @Id Integer id;
    @NotNull @ManyToOne SQLPlayer player;
    @NotNull @ManyToOne SQLString skill;
    @NotNull Integer points;
    @NotNull Integer level;

    private SQLPlayerScore(SQLPlayer player, SQLString skill)
    {
	setPlayer(player);
	setSkill(skill);
	setPoints(0);
	setLevel(0);
    }

    public static SQLPlayerScore of(UUID uuid, String skill)
    {
	Key key = new Key(uuid, skill);
	SQLPlayerScore result = cache.get(key);
	if (result == null) {
	    result = SQLDB.get().find(SQLPlayerScore.class).where()
		.eq("player", SQLPlayer.of(uuid))
		.eq("skill", SQLString.of(skill))
		.findUnique();
	    if (result == null) {
		result = new SQLPlayerScore(SQLPlayer.of(uuid), SQLString.of(skill));
		result.save();
	    }
	    cache.put(key, result);
	}
	return result;
    }

    public void save()
    {
	SQLDB.get().save(this);
    }
}
