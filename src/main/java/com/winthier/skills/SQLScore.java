package com.winthier.skills;

import java.util.Date;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "scores",
       uniqueConstraints = @UniqueConstraint(columnNames = {"player", "skill"}))
@Getter
@Setter
@NoArgsConstructor
public final class SQLScore {
    @Id private Integer id;
    @Column(nullable = false) private UUID player;
    @Column(nullable = false, length = 16) private String skill;
    @Column(nullable = false) private double skillPoints;
    @Column(nullable = false) private int skillLevel;
    @Version private Date version;

    public SQLScore(UUID player, String skill) {
        setPlayer(player);
        setSkill(skill);
        setSkillPoints(0);
        setSkillLevel(0);
    }
}
