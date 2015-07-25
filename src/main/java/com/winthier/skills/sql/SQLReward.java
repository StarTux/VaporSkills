package com.winthier.skills.sql;

import com.avaje.ebean.ExpressionList;
import com.avaje.ebean.validation.Length;
import com.avaje.ebean.validation.NotEmpty;
import com.avaje.ebean.validation.NotNull;
import com.winthier.skills.Reward;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import lombok.Value;

@Entity
@Table(name = "rewards",
       uniqueConstraints = @UniqueConstraint(columnNames = {"skill_id", "type", "data", "name_id"}))
@Getter
@Setter
@NoArgsConstructor
public class SQLReward implements Reward
{
    // Cache
    @Value static class Key { @NonNull String skill; Integer type; Integer data; String name; }
    final static Map<Key, SQLReward> cache = new HashMap<>();
    // Content
    @Id Integer id;
    @NotNull @ManyToOne SQLString skill;
    Integer type;
    Integer data;
    @ManyToOne(optional=true) SQLString name;
    // Reward
    @NotNull float skillPoints;
    @NotNull float money;
    @NotNull float exp;


    private SQLReward(SQLString skill, Integer type, Integer data, SQLString name)
    {
        setSkill(skill);
        setType(type);
        setData(data);
        setName(name);
        // Init reward
        setSkillPoints(0);
        setMoney(0.0f);
        setExp(0);
    }

    private SQLReward(Key key) {
        this(SQLString.of(key.skill), key.type, key.data, SQLString.of(key.name));
    }

    private static SQLReward find(Key key)
    {
        SQLReward result = cache.get(key);
        if (key == null) {
            ExpressionList<SQLReward> expr = SQLDB.get().find(SQLReward.class).where();
            expr = expr.eq("skill", SQLString.of(key.skill));
            expr = key.type != null ? expr.eq("type", key.type)               : expr.isNull("type");
            expr = key.data != null ? expr.eq("data", key.data)               : expr.isNull("data");
            expr = key.name != null ? expr.eq("name", SQLString.of(key.name)) : expr.isNull("name");
            result = SQLDB.unique(expr.findList());
            cache.put(key, result);
        }
        return result;
    }

    private static SQLReward of(Key key)
    {
        SQLReward result = find(key);
        if (result == null) {
            result = new SQLReward(key);
            SQLDB.get().save(result);
            cache.put(key, result);
        }
        return result;
    }

    public static SQLReward find(@NonNull String skill, Integer type, Integer data, String name)
    {
        return find(new Key(skill, type, data, name));
    }

    public static SQLReward of(@NonNull String skill, Integer type, Integer data, String name)
    {
        return of(new Key(skill, type, data, name));
    }
}
