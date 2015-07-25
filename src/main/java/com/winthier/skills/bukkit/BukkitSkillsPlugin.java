package com.winthier.skills.bukkit;

import com.winthier.skills.sql.SQLDB;
import java.util.List;
import javax.persistence.PersistenceException;
import lombok.Getter;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.event.Listener;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class BukkitSkillsPlugin extends JavaPlugin
{
    @Getter static BukkitSkillsPlugin instance;
    @Getter private Economy economy;
    @Getter private final BukkitSkills skills = new BukkitSkills();

    public BukkitSkillsPlugin()
    {
	instance = this;
    }

    @Override
    public void onEnable()
    {
	if (!setupEconomy()) {
	    getLogger().warning("Economy setup failed. Disabling skills.");
	    getServer().getPluginManager().disablePlugin(this);
	    return;
	}
	if (!setupDatabase()) {
	    getLogger().warning("Database setup failed. Disabling skills.");
	    getServer().getPluginManager().disablePlugin(this);
	    return;
	}
        for (BukkitSkill skill : skills.getSkills()) {
            if (skill instanceof Listener) {
                getServer().getPluginManager().registerEvents((Listener)skill, this);
            }
        }
        for (BukkitSkillType type : BukkitSkillType.values()) {
            BukkitSkill skill = skills.skillMap.get(type);
            if (skill == null) {
                getLogger().warning("Missing skill: " + type.name());
            } else {
                skill.onEnable();
            }
        }
    }

    @Override
    public void onDisable()
    {
        for (BukkitSkill skill : skills.getSkills()) {
            skill.onDisable();
        }
    }

    @Override
    public List<Class<?>> getDatabaseClasses()
    {
	return SQLDB.getDatabaseClasses();
    }

    private boolean setupEconomy()
    {
        RegisteredServiceProvider<Economy> economyProvider = getServer().getServicesManager().getRegistration(Economy.class);
        if (economyProvider != null) economy = economyProvider.getProvider();
        return (economy != null);
    }

    private boolean setupDatabase()
    {
	if (!SQLDB.isSetup()) {
	    try {
		installDDL();
	    } catch (PersistenceException pe) {
		pe.printStackTrace();
		return false;
	    }
	}
        return true;
    }
}
