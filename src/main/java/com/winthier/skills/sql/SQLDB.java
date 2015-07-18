package com.winthier.skills.sql;

import com.avaje.ebean.EbeanServer;
import com.winthier.skills.Skills;
import java.util.Arrays;
import java.util.List;

public class SQLDB {
    static EbeanServer get() {
        return Skills.getInstance().getDatabase();
    }

    static <E> E unique(List<E> list) {
        if (list.isEmpty()) return null;
        if (list.size() > 1) {
            System.err.println("Expected unique row, but got " + list.size() + " of type " + list.get(0).getClass().getName());
            Thread.dumpStack();
        }
        return list.get(0);
    }

    static void clearAllCaches()
    {
    }

    public static List<Class<?>> getDatabaseClasses()
    {
        return Arrays.asList(
            SQLEnum.class
            );
    }
}
