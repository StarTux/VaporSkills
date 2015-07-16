package com.winthier.skills.sql;

import com.avaje.ebean.validation.Length;
import com.avaje.ebean.validation.NotEmpty;
import com.avaje.ebean.validation.NotNull;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.Value;

@Entity
@Table(name = "enums",
       uniqueConstraints = @UniqueConstraint(columnNames = {"namespace_id", "name"}))
@Getter
@Setter
public class SQLEnum
{
    @Entity
    @Table(name = "enum_namespaces",
           uniqueConstraints = @UniqueConstraint(columnNames = {"name"}))
    @Getter
    @Setter
    public static class SQLNamespace
    {
        @Id int id;
        @NotEmpty String name;
    }

    @Id int id;
    @NotNull @ManyToOne SQLNamespace namespace;
    @NotEmpty String name;
}

