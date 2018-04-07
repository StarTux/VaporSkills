package com.winthier.skills.sql;

import java.util.HashMap;
import java.util.Map;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * General purpose table to store enums by name, so we can
 * reference them by id.
 */
@Entity
@Table(name = "enums",
       uniqueConstraints = @UniqueConstraint(columnNames = {"namespace_id", "name"}))
@Getter
@Setter
@NoArgsConstructor
public final class SQLEnum {
    // Cache
    private static final Map<Enum, SQLEnum> CACHE = new HashMap<>();
    // Payload
    @Id private int id;
    @Column(nullable = false) @ManyToOne private SQLString namespace;
    @Column(nullable = false) @ManyToOne private SQLString name;

    static String namespaceOf(Enum key) {
        return key.getDeclaringClass().getName();
    }

    static String nameOf(Enum key) {
        return key.name();
    }

    private SQLEnum(Enum key) {
        setNamespace(SQLString.of(namespaceOf(key)));
        setName(SQLString.of(nameOf(key)));
    }

    static SQLEnum of(Enum key) {
        SQLEnum result = CACHE.get(key);
        if (result == null) {
            result = SQLDB.get().find(SQLEnum.class).where()
                .eq("namespace", SQLString.of(namespaceOf(key)))
                .eq("name", SQLString.of(nameOf(key)))
                .findUnique();
            if (result == null) {
                result = new SQLEnum(key);
                SQLDB.get().save(result);
            }
            CACHE.put(key, result);
        }
        return result;
    }
}

