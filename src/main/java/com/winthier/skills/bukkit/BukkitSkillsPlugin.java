package com.winthier.skills.bukkit;

import com.winthier.skills.sql.SQLDB;
import java.util.List;
import javax.persistence.PersistenceException;
import lombok.Getter;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class BukkitSkillsPlugin extends JavaPlugin
{
    @Getter static BukkitSkillsPlugin instance;
    @Getter private Economy economy;

    BukkitSkillsPlugin()
    {
	instance = this;
	if (!SQLDB.isSetup()) {
	    try {
		installDDL();
	    } catch (PersistenceException pe) {
		getLogger().warning("Database setup failed. Disabling skills.");
		pe.printStackTrace();
		getServer().getPluginManager().disablePlugin(this);
		return;
	    }
	}
	if (!setupEconomy()) {
	    getLogger().warning("Economy setup failed. Disabling skills.");
	    getServer().getPluginManager().disablePlugin(this);
	    return;
	}
    }

    @Override
    public void onEnable()
    {
	setupEconomy();
    }

    @Override
    public List<Class<?>> getDatabaseClasses()
    {
	return SQLDB.getDatabaseClasses();
    }

    private boolean setupEconomy() {
        RegisteredServiceProvider<Economy> economyProvider = getServer().getServicesManager().getRegistration(Economy.class);
        if (economyProvider != null) economy = economyProvider.getProvider();
        return (economy != null);
    }
}
