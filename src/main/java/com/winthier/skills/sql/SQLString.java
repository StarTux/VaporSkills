package com.winthier.skills.sql;

import com.avaje.ebean.validation.Length;
import com.avaje.ebean.validation.NotEmpty;
import com.avaje.ebean.validation.NotNull;
import java.util.HashMap;
import java.util.Map;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

/**
 * General purpose table to store repeatedly used strings so we can conveniently reference them by ID.
 */
@Entity
@Table(name = "enum_namespaces",
       uniqueConstraints = @UniqueConstraint(columnNames = {"name"}))
@Getter
@Setter
class SQLString
{
    // Cache
    final static Map<String, SQLString> cache = new HashMap<>();
    // Payload
    @Id Integer ID;
    @NotNull @Length(max=255) String value;

    private SQLString(String string)
    {
        setValue(string);
    }

    public static SQLString of(String string)
    {
        SQLString result = cache.get(string);
        if (result == null) {
            result = new SQLString(string);
            SQLDB.get().save(result);
            cache.put(string, result);
        }
        return result;
    }
}
