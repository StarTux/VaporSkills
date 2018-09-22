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
import lombok.Setter;

@Entity
@Table(name = "perk_progress",
       uniqueConstraints = @UniqueConstraint(columnNames = {"player", "skill"}))
@Getter
@Setter
public final class SQLPerkProgress {
    @Id private Integer id;
    @Column(nullable = false) private UUID player;
    @Column(nullable = false, length = 32) private String skill;
    @Column(nullable = false) private Integer perkPoints;
    @Column(nullable = false) private Integer perks;
    @Version private Date version;

    public SQLPerkProgress() { }

    public SQLPerkProgress(UUID player, String skill) {
        this.player = player;
        this.skill = skill;
        this.perkPoints = 0;
        this.perks = 0;
    }
}
