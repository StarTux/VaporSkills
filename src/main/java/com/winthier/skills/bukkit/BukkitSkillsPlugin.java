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
import org.bukkit.scheduler.BukkitRunnable;

public class BukkitSkillsPlugin extends JavaPlugin
{
    @Getter static BukkitSkillsPlugin instance;
    @Getter private Economy economy;
    @Getter private final BukkitSkills skills = new BukkitSkills();
    final BukkitCommandAdmin adminCommand = new BukkitCommandAdmin();
    final BukkitCommandSkills skillsCommand = new BukkitCommandSkills();
    final BukkitCommandHighscore highscoreCommand = new BukkitCommandHighscore();
    static final String REWARDS_TXT = "rewards.txt";

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
            } else {
                getLogger().warning("Not an Event Listener: " + skill.getTitle());
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
        getCommand("skillsadmin").setExecutor(adminCommand);
        getCommand("skills").setExecutor(skillsCommand);
        getCommand("highscore").setExecutor(highscoreCommand);
        new BukkitRunnable() {
            @Override public void run() {
                saveAll();
            }
        }.runTaskTimer(this, 20*10, 20*10);
        saveResource(REWARDS_TXT, false);
    }

    @Override
    public void onDisable()
    {
        for (BukkitSkill skill : skills.getSkills()) {
            skill.onDisable();
        }
        saveAll();
        SQLDB.clearAllCaches();
    }

    void saveAll()
    {
        try {
            SQLDB.saveAll();
            skills.depositAllMoneys();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void reloadAll()
    {
        SQLDB.clearAllCaches();
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
