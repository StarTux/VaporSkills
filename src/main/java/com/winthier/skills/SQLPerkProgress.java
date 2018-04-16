package com.winthier.skills;

import java.util.Date;
import java.util.List;
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
@Table(name = "perks",
       uniqueConstraints = @UniqueConstraint(columnNames = {"player", "perk"}))
@Getter
@Setter
@NoArgsConstructor
public final class SQLPerkProgress {
    @Id private Integer id;
    @Column(nullable = false) private UUID player;
    @Column(nullable = false) private String skill;
    @Column(nullable = false) private Integer perkPoints;
    @Column(nullable = false) private Integer perks;
    @Version private Date version;

    public static List<SQLPerk> find(UUID queryPlayer) {
        return SQLDB.get().find(SQLPerk.class).where().eq("player", queryPlayer).findList();
    }
}
