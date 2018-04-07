package com.winthier.skills.sql;

import com.winthier.skills.bukkit.BukkitSkillsPlugin;
import com.winthier.sql.SQLDatabase;
import java.util.Arrays;
import java.util.List;

public final class SQLDB {
    private SQLDB() { }

    static SQLDatabase get() {
        return BukkitSkillsPlugin.getInstance().getDb();
    }

    static <E> E unique(List<E> list) {
        if (list.isEmpty()) return null;
        if (list.size() > 1) {
            System.err.println("Expected unique row, but got " + list.size() + " of type " + list.get(0).getClass().getName());
            Thread.dumpStack();
        }
        return list.get(0);
    }

    public static void clearAllCaches() {
        SQLPlayer.CACHE.clear();
        SQLPlayerSetting.CACHE.clear();
        SQLReward.CACHE.clear();
        SQLReward.LIST_CACHE.clear();
        SQLScore.CACHE.clear();
        SQLString.CACHE.clear();
    }

    public static void saveAll() {
        SQLLog.saveAll();
        SQLPlayerSetting.saveAll();
        SQLScore.saveAll();
    }

    public static void saveSome() {
        SQLLog.saveSome();
        SQLScore.saveSome();
    }

    public static List<Class<?>> getDatabaseClasses() {
        return Arrays.asList(
            SQLLog.class,
            SQLPlayer.class,
            SQLPlayerSetting.class,
            SQLReward.class,
            SQLScore.class,
            SQLString.class);
    }
}
