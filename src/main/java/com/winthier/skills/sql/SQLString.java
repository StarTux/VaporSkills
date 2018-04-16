package com.winthier.skills.sql;

import com.winthier.skills.SkillType;
import java.util.HashMap;
import java.util.Map;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * General purpose table to store repeatedly used strings so we can conveniently reference them by id.
 */
@Entity
@Table(name = "strings",
       uniqueConstraints = @UniqueConstraint(columnNames = {"value"}))
@Getter
@Setter
@NoArgsConstructor
public final class SQLString {
    // Cache
    static final Map<String, SQLString> CACHE = new HashMap<>();
    // Payload
    @Id private Integer id;
    @Column(nullable = false, length = 255) private String value;

    private SQLString(String string) {
        setValue(string);
    }

    public static SQLString of(String string) {
        if (string == null) return null;
        SQLString result = CACHE.get(string);
        if (result == null) {
            result = SQLDB.get().find(SQLString.class).where().eq("value", string).findUnique();
            if (result == null) {
                result = new SQLString(string);
                SQLDB.get().save(result);
            }
            CACHE.put(string, result);
        }
        return result;
    }

    public static SQLString of(SkillType skill) {
        return of(skill.key);
    }
}
