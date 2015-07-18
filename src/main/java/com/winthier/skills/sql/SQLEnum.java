package com.winthier.skills.sql;

import com.avaje.ebean.validation.Length;
import com.avaje.ebean.validation.NotEmpty;
import com.avaje.ebean.validation.NotNull;
import java.util.HashMap;
import java.util.Map;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NonNull;
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
public class SQLEnum
{
    // Cache
    final static Map<Enum, SQLEnum> cache = new HashMap<>();
    // Payload
    @Id int id;
    @NotNull @ManyToOne SQLString namespace;
    @NotNull @ManyToOne SQLString name;

    static String namespaceOf(Enum key)
    {
	return key.getDeclaringClass().getName();
    }

    static String nameOf(Enum key)
    {
	return key.name();
    }

    private SQLEnum(Enum key)
    {
        setNamespace(SQLString.of(namespaceOf(key)));
        setName(SQLString.of(nameOf(key)));
    }

    static SQLEnum of(Enum key)
    {
        SQLEnum result = cache.get(key);
        if (result == null) {
	    result = SQLDB.get().find(SQLEnum.class).where()
		.eq("namespace", SQLString.of(namespaceOf(key)))
		.eq("name", SQLString.of(nameOf(key)))
		.findUnique();
	    if (result == null) {
		result = new SQLEnum(key);
		SQLDB.get().save(result);
	    }
	    cache.put(key, result);
        }
        return result;
    }
}

