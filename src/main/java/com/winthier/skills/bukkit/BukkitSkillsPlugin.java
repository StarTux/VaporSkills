package com.winthier.skills.bukkit;

import com.winthier.skills.sql.SQLDB;
import com.winthier.sql.SQLDatabase;
import java.io.File;
import java.util.List;
import javax.persistence.PersistenceException;
import lombok.Getter;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

@Getter
public class BukkitSkillsPlugin extends JavaPlugin implements Listener
{
    @Getter static BukkitSkillsPlugin instance;
    Economy economy;
    final BukkitSkills skills = new BukkitSkills();
    final BukkitCommandAdmin adminCommand = new BukkitCommandAdmin();
    final BukkitCommandSkills skillsCommand = new BukkitCommandSkills();
    final BukkitCommandHighscore highscoreCommand = new BukkitCommandHighscore();
    static final String CONFIG_YML = "config.yml";
    static final String REWARDS_TXT = "rewards.txt";
    private SQLDatabase db;

    public BukkitSkillsPlugin()
    {
        instance = this;
    }

    @Override
    public void onEnable()
    {
        // Files
        writeDefaultFiles(false);
        reloadConfig();
        // Economy
        if (!setupEconomy()) {
            getLogger().warning("Economy setup failed. Disabling skills.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        // Database
        db = new SQLDatabase(this);
        for (Class<?> clazz: SQLDB.getDatabaseClasses()) db.registerTable(clazz);
        if (!db.createAllTables()) {
            getLogger().warning("Database setup failed. Disabling skills.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        // Skills
        skills.configure();
        for (BukkitSkill skill : skills.getSkills()) {
            if (skill instanceof Listener) {
                getServer().getPluginManager().registerEvents((Listener)skill, this);
            } else {
                getLogger().warning("Not an Event Listener: " + skill.getDisplayName());
            }
        }
        // Double check skills
        for (BukkitSkillType type : BukkitSkillType.values()) {
            BukkitSkill skill = skills.skillMap.get(type);
            if (skill == null) {
                getLogger().warning("Missing skill: " + type.name());
            } else {
                skill.configure();
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
                saveSome();
                updateAllPlayers();
            }
        }.runTaskTimer(this, 20, 20);
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

    void writeDefaultFiles(boolean force)
    {
        if (!force) saveDefaultConfig();
        if (force) saveResource(CONFIG_YML, force);
        saveResource(REWARDS_TXT, force);
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

    void saveSome()
    {
        try {
            SQLDB.saveSome();
            skills.depositSomeMoneys();
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
        writeDefaultFiles(false);
        reloadConfig();
        skills.configure();
        for (BukkitSkill skill : skills.getSkills()) skill.configure();
        skills.buildNameMap();
    }

    private boolean setupEconomy()
    {
        RegisteredServiceProvider<Economy> economyProvider = getServer().getServicesManager().getRegistration(Economy.class);
        if (economyProvider != null) economy = economyProvider.getProvider();
        return (economy != null);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event)
    {
        saveAll();
        skills.players.remove(event.getPlayer().getUniqueId());
    }
}
