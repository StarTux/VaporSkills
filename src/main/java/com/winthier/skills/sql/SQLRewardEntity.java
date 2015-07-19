package com.winthier.skills.sql;

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
@Table(name = "reward_entities",
       uniqueConstraints = @UniqueConstraint(columnNames = {"skill_id", "entity_type"}))
@Getter
@Setter
public class SQLRewardEntity implements Reward
{
    // Cache
    @Value static class Key { String skill; String entityType; }
    final static Map<Key, SQLRewardEntity> cache = new HashMap<>();
    // Content
    @Id Integer id;
    @NotNull @ManyToOne SQLString skill;
    @NotNull @ManyToOne SQLString entityType;
    // Rewards
    @NotNull float skillPoints;
    @NotNull float money;
    @NotNull float exp;

    private SQLRewardEntity(SQLString skill, SQLString entityType)
    {
        setSkill(skill);
        setEntityType(entityType);
        setSkillPoints(0);
        setMoney(0.0);
        setExp(0);
    }

    public static SQLRewardEntity find(String skill, String entityType)
    {
        Key key = new Key(skill, entityType);
        SQLRewardEntity result = cache.get(key);
        if (result == null) {
            result = SQLDB.get().find(SQLRewardEntity.class).where()
                .eq("skill", SQLString.of(skill))
                .eq("entity_type", SQLString.of(entityType))
                .findUnique();
            cache.put(key, result);
        }
        return result;
    }
}
