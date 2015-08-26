package com.winthier.skills.sql;

import com.avaje.ebean.validation.Length;
import com.avaje.ebean.validation.NotEmpty;
import com.avaje.ebean.validation.NotNull;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
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
public class SQLPlayerSetting
{
    // Cache
    @Value static class Key { @NonNull UUID uuid; @NonNull String category; String key; }
    final static Map<Key, SQLPlayerSetting> cache = new HashMap<>();
    final static List<SQLPlayerSetting> dirties = new ArrayList<>();
    // Content
    @Id Integer id;
    @NotNull @ManyToOne SQLPlayer player;
    @NotNull @ManyToOne SQLString category; // Usually a skill
    @NotNull @ManyToOne SQLString key;
    @Length(max=255) String value;
    @Version Date version;

    private SQLPlayerSetting(SQLPlayer player, SQLString category, SQLString key)
    {
	setPlayer(player);
        setCategory(category);
        setKey(key);
    }

    static SQLPlayerSetting of(UUID player, String category, String key)
    {
        Key hashKey = new Key(player, category, key);
	SQLPlayerSetting result = cache.get(hashKey);
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
	    cache.put(hashKey, result);
	}
	return result;
    }

    public static String getString(UUID uuid, String category, String key)
    {
        return of(uuid, category, key).getValue();
    }

    public static Integer getInt(UUID uuid, String category, String key)
    {
        String value = of(uuid, category, key).getValue();
        if (value == null) return null;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException nfe) {
            nfe.printStackTrace();
        }
        return null;
    }

    public static Double getDouble(UUID uuid, String category, String key)
    {
        String value = of(uuid, category, key).getValue();
        if (value == null) return null;
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException nfe) {
            nfe.printStackTrace();
        }
        return null;
    }

    public static void set(UUID uuid, String category, String key, Object value)
    {
        SQLPlayerSetting setting = of(uuid, category, key);
        setting.setValue(value.toString());
        dirties.add(setting);
    }

    static void saveAll()
    {
        SQLDB.get().save(dirties);
        dirties.clear();
    }
}
