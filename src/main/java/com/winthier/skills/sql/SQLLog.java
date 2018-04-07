package com.winthier.skills.sql;

import com.winthier.skills.Reward;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.PersistenceException;
import javax.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "logs")
@Getter
@Setter
@NoArgsConstructor
public final class SQLLog implements Reward {
    @Getter private static final List<SQLLog> DIRTIES = new ArrayList<>();
    private static long lastSave = 0;
    // Key
    @Id private Integer id;
    @ManyToOne(optional = false) private SQLString skill;
    @ManyToOne(optional = false) private SQLString target;
    private Integer type;
    private Integer data;
    @ManyToOne(optional = true) private SQLString name;
    // Stats
    @ManyToOne(optional = false) private SQLPlayer player;
    @ManyToOne(optional = false) private Date time;
    // Reward
    @Column(nullable = false) private float skillPoints;
    @Column(nullable = false) private float money;
    @Column(nullable = false) private float exp;

    private SQLLog(SQLString skill, SQLString target, Integer type, Integer data, SQLString name, SQLPlayer player, Date time, float skillPoints, float money, float exp) {
        setSkill(skill);
        setTarget(target);
        setType(type);
        setData(data);
        setName(name);

        setPlayer(player);
        setTime(time);

        setSkillPoints(skillPoints);
        setMoney(money);
        setExp(exp);
    }

    public static void log(SQLReward reward, UUID player, Date time, Reward outcome) {
        SQLLog log = new SQLLog(
            reward.getSkill(), reward.getTarget(), reward.getType(), reward.getData(), reward.getName(),
            SQLPlayer.of(player), time,
            outcome.getSkillPoints(), outcome.getMoney(), outcome.getExp());
        DIRTIES.add(log);
    }

    public static void log(SQLReward reward, UUID player, Reward outcome) {
        log(reward, player, new Date(), outcome);
    }

    static void saveAll() {
        try {
            SQLDB.get().save(DIRTIES);
        } catch (PersistenceException pe) {
            System.err.println("SQLLog saveAll throws PersistenceException.");
            pe.printStackTrace();
        }
        DIRTIES.clear();
    }

    static void saveSome() {
        long now = System.currentTimeMillis();
        if (now - lastSave < 1000L * 60L) return;
        lastSave = now;
        saveAll();
    }
}
