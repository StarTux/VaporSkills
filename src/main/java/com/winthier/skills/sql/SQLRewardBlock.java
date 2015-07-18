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
@Table(name = "reward_blocks",
       uniqueConstraints = @UniqueConstraint(columnNames = {"skill_id", "block_type", "block_data"}))
@Getter
@Setter
public class SQLRewardBlock
{
    // Cache
    @Value static class Key { String skill; int blockType; int blockData; }
    final static Map<Key, SQLRewardBlock> cache = new HashMap<>();
    // Content
    @Id Integer id;
    @NotNull @ManyToOne SQLString skill;
    @NotNull Integer blockType;
    Integer blockData = null;
    @NotNull @ManyToOne SQLReward reward;

    private SQLRewardBlock(SQLString skill, int blockType)
    {
        setSkill(skill);
        setBlockType(blockType);
        reward = SQLReward.create();
    }

    private SQLRewardBlock(SQLString skill, int blockType, int blockData)
    {
        this(skill, blockType);
        setBlockData(blockData);
    }

    private static SQLRewardBlock findPrivate(String skill, int blockType, Integer blockData)
    {
        Key key = new Key(skill, blockType, blockData != null ? blockData : -1);
        if (cache.containsKey(key)) {
            return cache.get(key);
        } else {
            SQLRewardBlock result = SQLDB.get().find(SQLRewardBlock.class).where()
                .eq("skill", SQLString.of(skill))
                .eq("block_type", blockType)
                .eq("block_data", blockData)
                .findUnique();
            cache.put(key, result);
            return result;
        }
    }

    public static SQLRewardBlock find(String skill, int blockType)
    {
        return findPrivate(skill, blockType, null);
    }

    public static SQLRewardBlock find(String skill, int blockType, int blockData)
    {
        return findPrivate(skill, blockType, blockData);
    }
}
