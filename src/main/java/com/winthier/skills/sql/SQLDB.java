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
        SQLPlayer.cache.clear();
        SQLPlayerSetting.cache.clear();
        SQLReward.cache.clear();
        SQLReward.listCache.clear();
        SQLScore.cache.clear();
        SQLString.cache.clear();
    }

    public static void saveAll()
    {
        SQLLog.saveAll();
        SQLPlayerSetting.saveAll();
        SQLScore.saveAll();
    }

    public static List<Class<?>> getDatabaseClasses()
    {
        return Arrays.asList(
            SQLLog.class,
            SQLPlayer.class,
            SQLPlayerSetting.class,
            SQLReward.class,
            SQLScore.class,
            SQLString.class
            );
    }
}
