package com.winthier.skills.sql;

import java.util.Date;
import java.util.List;
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
    @Column(nullable = false) private String perk;
    @Column(nullable = false) private Date unlocked;

    public static List<SQLPerk> find(UUID queryPlayer) {
        return SQLDB.get().find(SQLPerk.class).where().eq("player", queryPlayer).findList();
    }

    public static void unlock(UUID updatePlayer, String updatePerk) {
        SQLPerk row = new SQLPerk();
        row.setPlayer(updatePlayer);
        row.setPerk(updatePerk);
        row.setUnlocked(new Date());
        SQLDB.get().getTable(SQLPerk.class).saveIgnore(row);
    }
}
