package com.winthier.skills.sql;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.PersistenceException;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import lombok.Value;

@Entity
@Table(name = "player_settings",
       uniqueConstraints = @UniqueConstraint(columnNames = {"player_id", "category_id", "key_id"}))
@Getter
@Setter
@NoArgsConstructor
public final class SQLPlayerSetting {
    // Cache
    @Value static class Key {
        @NonNull final UUID uuid;
        @NonNull final String category;
        final String key;
    }

    static final Map<Key, SQLPlayerSetting> CACHE = new HashMap<>();
    static final List<SQLPlayerSetting> DIRTIES = new ArrayList<>();
    // Content
    @Id private Integer id;
    @Column(nullable = false) @ManyToOne private SQLPlayer player;
    @Column(nullable = false) @ManyToOne private SQLString category; // Usually a skill
    @Column(nullable = false) @ManyToOne private SQLString key;
    @Column(length = 255) private String value;
    @Version private Date version;

    private SQLPlayerSetting(SQLPlayer player, SQLString category, SQLString key) {
        setPlayer(player);
        setCategory(category);
        setKey(key);
    }

    static SQLPlayerSetting of(UUID player, String category, String key) {
        Key hashKey = new Key(player, category, key);
        SQLPlayerSetting result = CACHE.get(hashKey);
        if (result == null) {
            result = SQLDB.get().find(SQLPlayerSetting.class).where()
                .eq("player", SQLPlayer.of(player))
                .eq("category", SQLString.of(category))
                .eq("key", SQLString.of(key))
                .findUnique();
            if (result == null) {
                result = new SQLPlayerSetting(SQLPlayer.of(player), SQLString.of(category), SQLString.of(key));
                SQLDB.get().save(result);
            }
            CACHE.put(hashKey, result);
        }
        return result;
    }

    public static String getString(UUID uuid, String category, String key) {
        return of(uuid, category, key).getValue();
    }

    public static Integer getInt(UUID uuid, String category, String key) {
        String value = of(uuid, category, key).getValue();
        if (value == null) return null;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException nfe) {
            nfe.printStackTrace();
        }
        return null;
    }

    public static Double getDouble(UUID uuid, String category, String key) {
        String value = of(uuid, category, key).getValue();
        if (value == null) return null;
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException nfe) {
            nfe.printStackTrace();
        }
        return null;
    }

    public static void set(UUID uuid, String category, String key, Object value) {
        SQLPlayerSetting setting = of(uuid, category, key);
        setting.setValue(value.toString());
        DIRTIES.add(setting);
    }

    static void saveAll() {
        try {
            SQLDB.get().save(DIRTIES);
        } catch (PersistenceException pe) {
            System.err.println("SQLPlayerSetting saveAll throws PersistenceException. Clearing cache");
            pe.printStackTrace();
            CACHE.clear();
        }
        DIRTIES.clear();
    }
}
