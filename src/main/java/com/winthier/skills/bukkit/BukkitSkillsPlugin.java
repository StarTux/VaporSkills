package com.winthier.skills.bukkit;

import com.winthier.skills.sql.SQLDB;
import java.util.List;
import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;

public class BukkitSkillsPlugin extends JavaPlugin
{
    @Getter static BukkitSkillsPlugin instance;

    BukkitSkillsPlugin()
    {
	instance = this;
    }

    @Override
    public void onEnable()
    {
    }

    @Override
    public List<Class<?>> getDatabaseClasses()
    {
	return SQLDB.getDatabaseClasses();
    }
}
