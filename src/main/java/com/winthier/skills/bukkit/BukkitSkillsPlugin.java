package com.winthier.skills.bukkit;

import com.winthier.skills.sql.SQLDB;
import java.io.File;
import java.util.List;
import javax.persistence.PersistenceException;
import lombok.Getter;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public class BukkitSkillsPlugin extends JavaPlugin implements Listener
{
    @Getter static BukkitSkillsPlugin instance;
    @Getter private Economy economy;
    @Getter private final BukkitSkills skills = new BukkitSkills();
    final BukkitCommandAdmin adminCommand = new BukkitCommandAdmin();
    final BukkitCommandSkills skillsCommand = new BukkitCommandSkills();
    final BukkitCommandHighscore highscoreCommand = new BukkitCommandHighscore();
    YamlConfiguration descriptions = null;
    static final String REWARDS_TXT = "rewards.txt";
    static final String DESCRIPTIONS_YML = "descriptions.yml";

    public BukkitSkillsPlugin()
    {
	instance = this;
    }

    @Override
    public void onEnable()
    {
        // Economy
	if (!setupEconomy()) {
	    getLogger().warning("Economy setup failed. Disabling skills.");
	    getServer().getPluginManager().disablePlugin(this);
	    return;
	}
        // Database
	if (!setupDatabase()) {
	    getLogger().warning("Database setup failed. Disabling skills.");
	    getServer().getPluginManager().disablePlugin(this);
	    return;
	}
        // Skills
        for (BukkitSkill skill : skills.getSkills()) {
            if (skill instanceof Listener) {
                getServer().getPluginManager().registerEvents((Listener)skill, this);
            } else {
                getLogger().warning("Not an Event Listener: " + skill.getTitle());
            }
        }
        // Double check skills
        for (BukkitSkillType type : BukkitSkillType.values()) {
            BukkitSkill skill = skills.skillMap.get(type);
            if (skill == null) {
                getLogger().warning("Missing skill: " + type.name());
            } else {
                skill.onEnable();
            }
        }
        // Commands
        getCommand("skillsadmin").setExecutor(adminCommand);
        getCommand("skills").setExecutor(skillsCommand);
        getCommand("highscore").setExecutor(highscoreCommand);
        // Events
        getServer().getPluginManager().registerEvents(this, this);
        // Tasks
        new BukkitRunnable() {
            @Override public void run() {
                saveAll();
            }
        }.runTaskTimer(this, 20*10, 20*10);
        new BukkitRunnable() {
            @Override public void run() {
                updateAllPlayers();
            }
        }.runTaskTimer(this, 20, 20);
        // Files
        saveResource(REWARDS_TXT, false);
        saveResource(DESCRIPTIONS_YML, false);
        //
        skills.buildNameMap();
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

    void updateAllPlayers()
    {
        skills.updateAllPlayers();
    }

    void reloadAll()
    {
        SQLDB.clearAllCaches();
        descriptions = null;
        skills.buildNameMap();
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

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event)
    {
        skills.players.remove(event.getPlayer().getUniqueId());
    }

    String getDescription(BukkitSkill skill, String key, String dfl)
    {
        if (descriptions == null) {
            descriptions = YamlConfiguration.loadConfiguration(new File(getDataFolder(), DESCRIPTIONS_YML));
        }
        return descriptions.getString(skill.getKey() + "." + key, dfl);
    }
}
