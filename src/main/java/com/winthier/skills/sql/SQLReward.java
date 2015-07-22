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
import lombok.NonNull;
import lombok.Setter;
import lombok.Value;

@Entity
@Table(name = "rewards",
       uniqueConstraints = @UniqueConstraint(columnNames = {"skill_id", "material", "data", "type_id"}))
@Getter
@Setter
public class SQLReward implements Reward
{
    // Cache
    @Value static class Key { @NonNull String skill; Integer material; Integer data; String type; }
    final static Map<Key, SQLReward> cache = new HashMap<>();
    // Content
    @Id Integer id;
    @NotNull @ManyToOne SQLString skill;
    Integer material;
    Integer data;
    @ManyToOne(optional=true) SQLString type;
    // Reward
    @NotNull float skillPoints;
    @NotNull float money;
    @NotNull float exp;


    private SQLReward(SQLString skill, Integer material, Integer data, SQLString type)
    {
        setSkill(skill);
        setMaterial(material);
        setData(data);
        setType(type);
        // Init reward
        setSkillPoints(0);
        setMoney(0.0f);
        setExp(0);
    }

    private SQLReward(Key key) {
        this(SQLString.of(key.skill), key.material, key.data, SQLString.of(key.type));
    }

    private static SQLReward find(Key key)
    {
        SQLReward result = cache.get(key);
        if (key == null) {
            ExpressionList<SQLReward> expr = SQLDB.get().find(SQLReward.class).where();
            expr = expr.eq("skill", SQLString.of(key.skill));
            expr = key.material != null ? expr.eq("material", key.material)           : expr.isNull("material");
            expr = key.data     != null ? expr.eq("data",     key.data)               : expr.isNull("data");
            expr = key.type     != null ? expr.eq("type",     SQLString.of(key.type)) : expr.isNull("type");
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

    public static SQLReward find(@NonNull String skill, Integer material, Integer data, String type)
    {
        return find(new Key(skill, material, data, type));
    }

    public static SQLReward of(@NonNull String skill, Integer material, Integer data, String type)
    {
        return of(new Key(skill, material, data, type));
    }
}
