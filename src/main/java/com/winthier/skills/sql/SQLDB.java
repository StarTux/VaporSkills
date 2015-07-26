package com.winthier.skills.sql;

import com.avaje.ebean.EbeanServer;
import com.winthier.skills.Skills;
import java.util.Arrays;
import java.util.List;
import javax.persistence.PersistenceException;

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

    public static boolean isSetup()
    {
	try {
	    for (Class<?> clazz : getDatabaseClasses()) get().find(clazz).findRowCount();
	} catch (PersistenceException pe) {
	    return false;
	}
	return true;
    }

    public static void clearAllCaches()
    {
        SQLString.cache.clear();
        SQLPlayer.cache.clear();
        SQLReward.cache.clear();
        SQLScore.cache.clear();
    }

    public static void saveAll()
    {
        SQLScore.saveAll();
    }

    public static List<Class<?>> getDatabaseClasses()
    {
        return Arrays.asList(
            SQLPlayer.class,
            SQLReward.class,
            SQLScore.class,
            SQLString.class
            );
    }
}
