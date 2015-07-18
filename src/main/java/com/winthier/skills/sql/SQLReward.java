package com.winthier.skills.sql;

import com.avaje.ebean.validation.Length;
import com.avaje.ebean.validation.NotEmpty;
import com.avaje.ebean.validation.NotNull;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

@Entity
@Table(name = "rewards")
@Getter
@Setter
public class SQLReward
{
    // Content
    @Id Integer id;
    @NotNull Integer skillPoints;
    @NotNull Double money;
    @NotNull Integer exp;

    private SQLReward()
    {
        setSkillPoints(0);
        setMoney(0.0);
        setExp(0);
    }

    static SQLReward create()
    {
        SQLReward result = new SQLReward();
        SQLDB.get().save(result);
        return result;
    }
}
