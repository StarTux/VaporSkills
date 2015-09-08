package com.winthier.skills.sql;

import com.avaje.ebean.ExpressionList;
import com.avaje.ebean.validation.Length;
import com.avaje.ebean.validation.NotEmpty;
import com.avaje.ebean.validation.NotNull;
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
@Table(name = "stats",
       uniqueConstraints = @UniqueConstraint(columnNames = {"reward_id", "player_id"}))
@Getter
@Setter
@NoArgsConstructor
public class SQLStat
{
    // Cache
    @Value static class Key { @NonNull int rewardId; @NonNull UUID player; }
    final static Map<Key, SQLStat> cache = new HashMap<>();
    final static Set<SQLStat> dirties = new HashSet<>();
    // Key
    @Id Integer id;
    @ManyToOne(optional=true) SQLReward reward;
    @ManyToOne(optional=false) SQLPlayer player;
    // Data
    @NotNull float skillPoints;
    @NotNull float money;
    @NotNull float exp;
    // Version
    @Version Date version;

    private SQLStat(SQLReward reward, SQLPlayer player)
    {
        setReward(reward);
        setPlayer(player);
        setSkillPoints(0);
        setMoney(0);
        setExp(0);
    }

    static SQLStat of(SQLReward reward, UUID player)
    {
        Key key = new Key(reward.getId(), player);
        SQLStat result = cache.get(key);
        if (result != null) return result;
        result = SQLDB.get().find(SQLStat.class).where()
            .eq("reward", reward)
            .eq("player", SQLPlayer.of(player)).findUnique();
        if (result == null) {
            result = new SQLStat(reward, SQLPlayer.of(player));
            result.setDirty();
        }
        cache.put(key, result);
        return result;
    }

    void setDirty()
    {
        dirties.add(this);
    }

    static void saveAll()
    {
        try {
            SQLDB.get().save(dirties);
        } catch (PersistenceException pe) {
            System.err.println("SQLStat saveAll throws PersistenceException. Clearing cache");
            pe.printStackTrace();
            cache.clear();
        }
        dirties.clear();
    }

    void add(float skillPoints, float money, float exp)
    {
        setSkillPoints(getSkillPoints() + skillPoints);
        setMoney(getMoney() + money);
        setExp(getExp() + exp);
        setDirty();
    }

    public static void log(SQLReward reward, UUID player, float skillPoints, float money, float exp)
    {
        of(reward, player).add(skillPoints, money, exp);
    }
}
