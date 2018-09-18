package com.winthier.skills;

import java.util.Date;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "perks",
       uniqueConstraints = @UniqueConstraint(columnNames = {"player", "perk"}))
@Getter
@Setter
@NoArgsConstructor
public final class SQLPerk {
    @Id private Integer id;
    @Column(nullable = false) private UUID player;
    @Column(nullable = false, length = 32) private String perk;
    @Column(nullable = false) private Date unlocked;

    public SQLPerk(UUID player, String perk) {
        setPlayer(player);
        setPerk(perk);
        setUnlocked(new Date());
    }
}
