package com.winthier.skills.sql;

import com.winthier.skills.Reward;
import com.winthier.skills.SkillType;
import com.winthier.sql.SQLTable;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.Column;
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
@Table(name = "rewards",
       uniqueConstraints = @UniqueConstraint(columnNames = {"skill_id", "target_id", "type", "data", "name_id"}))
@Getter
@Setter
@NoArgsConstructor
public final class SQLReward implements Reward {
    public enum Target {
        NAME, BLOCK, ITEM, ENTITY, ENCHANTMENT, POTION_EFFECT;
    }
    // Cache
    @Value static class Key {
        @NonNull final String skill;
        @NonNull final Target target;
        final Integer type;
        final Integer data;
        final String name;
    }

    static final Map<Key, SQLReward> CACHE = new HashMap<>();
    static final Map<Key, List<SQLReward>> LIST_CACHE = new HashMap<>();
    // Content
    @Id private Integer id;
    @Column(nullable = false) @ManyToOne private SQLString skill;
    @Column(nullable = false) @ManyToOne private SQLString target;
    private Integer type;
    private Integer data;
    @ManyToOne(optional = true) private SQLString name;
    // Reward
    @Column(nullable = false) private float skillPoints;
    @Column(nullable = false) private float money;
    @Column(nullable = false) private float exp;
    // Version
    @Version private Date version;

    private SQLReward(SQLString skill, SQLString target, Integer type, Integer data, SQLString name) {
        setSkill(skill);
        setTarget(target);
        setType(type);
        setData(data);
        setName(name);
        // Init reward
        setSkillPoints(0);
        setMoney(0.0f);
        setExp(0);
    }

    private SQLReward(Key key) {
        this(SQLString.of(key.skill), SQLString.of(key.target.name()), key.type, key.data, SQLString.of(key.name));
    }

    private static SQLReward find(Key key) {
        if (CACHE.containsKey(key)) return CACHE.get(key);
        SQLTable<SQLReward>.Finder expr = SQLDB.get().find(SQLReward.class).where();
        expr = expr.eq("skill", SQLString.of(key.skill));
        expr = expr.eq("target", SQLString.of(key.target.name()));
        expr = key.type != null ? expr.eq("type", key.type)               : expr.isNull("type");
        expr = key.data != null ? expr.eq("data", key.data)               : expr.isNull("data");
        expr = key.name != null ? expr.eq("name", SQLString.of(key.name)) : expr.isNull("name");
        SQLReward result = SQLDB.unique(expr.findList());
        CACHE.put(key, result); // may insert null
        return result;
    }

    private static List<SQLReward> findList(Key key) {
        if (LIST_CACHE.containsKey(key)) return LIST_CACHE.get(key);
        SQLTable<SQLReward>.Finder expr = SQLDB.get().find(SQLReward.class).where();
        expr = expr.eq("skill", SQLString.of(key.skill));
        expr = expr.eq("target", SQLString.of(key.target.name()));
        if (key.type != null) expr = expr.eq("type", key.type);
        if (key.data != null) expr = expr.eq("data", key.data);
        if (key.name != null) expr = expr.eq("name", SQLString.of(key.name));
        List<SQLReward> result = expr.findList();
        LIST_CACHE.put(key, result);
        return result;
    }

    private static SQLReward of(Key key) {
        SQLReward result = find(key);
        if (result == null) {
            result = new SQLReward(key);
            SQLDB.get().save(result);
            CACHE.put(key, result);
        }
        return result;
    }

    public static SQLReward find(@NonNull SkillType skill, @NonNull Target target, Integer type, Integer data, String name) {
        return find(new Key(skill.key, target, type, data, name));
    }

    public static List<SQLReward> findList(@NonNull SkillType skill, @NonNull Target target, Integer type, Integer data, String name) {
        return findList(new Key(skill.key, target, type, data, name));
    }

    public static SQLReward of(@NonNull SkillType skill, @NonNull Target target, Integer type, Integer data, String name) {
        return of(new Key(skill.key, target, type, data, name));
    }

    public static List<SQLReward> findList(@NonNull SkillType skill) {
        return SQLDB.get().find(SQLReward.class).where().eq("skill", SQLString.of(skill.key)).findList();
    }

    public void save() {
        SQLDB.get().save(this);
    }

    public static void deleteAll() {
        CACHE.clear();
        LIST_CACHE.clear();
        SQLDB.get().executeUpdate("DELETE FROM rewards");
    }
}
