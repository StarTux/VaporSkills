package com.winthier.skills.sql;

import java.util.HashMap;
import java.util.Map;
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
@Table(name = "players",
       uniqueConstraints = @UniqueConstraint(columnNames = {"uuid"}))
@Getter
@Setter
@NoArgsConstructor
public final class SQLPlayer {
    // Cache
    static final Map<UUID, SQLPlayer> CACHE = new HashMap<>();
    // Content
    @Id private Integer id;
    @Column(nullable = false) private UUID uuid;

    private SQLPlayer(UUID uuid) {
        setUuid(uuid);
    }

    public static SQLPlayer of(UUID uuid) {
        SQLPlayer result = CACHE.get(uuid);
        if (result == null) {
            result = SQLDB.get().find(SQLPlayer.class).where().eq("uuid", uuid).findUnique();
            if (result == null) {
                result = new SQLPlayer(uuid);
                SQLDB.get().save(result);
            }
            CACHE.put(uuid, result);
        }
        return result;
    }
}
