package com.winthier.skills;

import com.winthier.sql.SQLDatabase;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.Getter;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

@Getter
public final class SkillsPlugin extends JavaPlugin implements Listener {
    // Constants
    static final String CONFIG_YML = "config.yml";
    static final String REWARDS_TXT = "rewards.txt";
    static final String PERKS_YML = "perks.yml";
    // Singleton
    @Getter private static SkillsPlugin instance;
    // External Data
    private Economy economy;
    private SQLDatabase db;
    // Commands
    private final AdminCommand adminCommand = new AdminCommand(this);
    private final SkillsCommand skillsCommand = new SkillsCommand(this);
    private final HighscoreCommand highscoreCommand = new HighscoreCommand(this);
    // Internal Data
    private final Score score = new Score(this);
    private final Map<SkillType, Skill> skillMap = new EnumMap<>(SkillType.class);
    private final Map<String, Skill> nameMap = new HashMap<>();
    private final Map<UUID, Double> moneys = new HashMap<>();
    private final Map<UUID, Double> exps = new HashMap<>();
    private final Set<UUID> playersInDebugMode = new HashSet<>();
    private final Map<UUID, Session> sessions = new HashMap<>();
    private ConfigurationSection perksConfig = null;

    public SkillsPlugin() {
        instance = this;
    }

    @Override
    public void onEnable() {
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
        List<Skill> skills = Arrays.asList(
            new BrawlSkill(this),
            new BrewSkill(this),
            new CookSkill(this),
            new DigSkill(this),
            new EnchantSkill(this),
            new FishSkill(this),
            new GardenSkill(this),
            new HuntSkill(this),
            new MineSkill(this),
            new RanchSkill(this),
            new SmithSkill(this),
            new TameSkill(this),
            new WoodcutSkill(this));
        for (Skill skill : skills) {
            SkillType type = skill.getSkillType();
            if (skillMap.containsKey(type)) {
                throw new IllegalStateException("Duplicate skill " + type.name() + ": " + skillMap.get(type).getClass().getSimpleName() + " and " + skill.getClass().getSimpleName());
            }
            skillMap.put(type, skill);
        }
        for (Skill skill : skills) {
            if (skill instanceof Listener) {
                getServer().getPluginManager().registerEvents((Listener)skill, this);
            } else {
                getLogger().warning("Skill not an Event Listener: " + skill.getDisplayName());
            }
        }
        // Double check skills
        for (SkillType type : SkillType.values()) {
            Skill skill = skillMap.get(type);
            if (skill == null) {
                getLogger().warning("Missing skill: " + type.name());
            } else {
                skill.configureSkill();
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
                on20Ticks();
            }
        }.runTaskTimer(this, 20, 20);
        //
        buildNameMap();
    }

    @Override
    public void onDisable() {
        for (Skill skill : getSkills()) {
            skill.onDisable();
        }
        for (Player player: getServer().getOnlinePlayers()) {
            getSession(player).onDisable();
        }
        saveAll();
        SQLDB.clearAllCaches();
    }

    void writeDefaultFiles(boolean force) {
        saveResource(CONFIG_YML, force);
        saveResource(REWARDS_TXT, force);
        saveResource(PERKS_YML, force);
    }

    void saveAll() {
        try {
            SQLDB.saveAll();
            depositAllMoneys();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void saveSome() {
        try {
            SQLDB.saveSome();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public ConfigurationSection getPerksConfig() {
        if (perksConfig == null) {
            YamlConfiguration tmp;
            tmp = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "perks.yml"));
            tmp.setDefaults(YamlConfiguration.loadConfiguration(new InputStreamReader(getResource("perks.yml"))));
            this.perksConfig = tmp;
        }
        return perksConfig;
    }

    void reloadAll() {
        writeDefaultFiles(false);
        reloadConfig();
        perksConfig = null;
        for (Skill skill : getSkills()) skill.configure();
        buildNameMap();
        score.clear();
    }

    private boolean setupEconomy() {
        RegisteredServiceProvider<Economy> economyProvider = getServer().getServicesManager().getRegistration(Economy.class);
        if (economyProvider != null) economy = economyProvider.getProvider();
        return (economy != null);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        saveAll();
        sessions.remove(event.getPlayer().getUniqueId());
    }

    void buildNameMap() {
        nameMap.clear();
        // Put all the names in the map
        for (Skill skill : skillMap.values()) {
            nameMap.put(skill.skillType.key, skill);
            nameMap.put(skill.getDisplayName().toLowerCase(), skill);
            nameMap.put(skill.getShorthand().toLowerCase(), skill);
        }
        // Bake the map
        for (Map.Entry<String, Skill> entry : new ArrayList<>(nameMap.entrySet())) {
            Skill skill = entry.getValue();
            for (String name = entry.getKey(); name.length() > 0; name = name.substring(0, name.length() - 1)) {
                if (!nameMap.containsKey(name)) nameMap.put(name, skill);
            }
        }
    }

    public void onLevelUp(UUID uuid, SkillType skillType, int level) {
        final Player player = Bukkit.getServer().getPlayer(uuid);
        final Skill skill = getSkill(skillType);
        if (player == null) return;
        if (skill == null) return;
        LevelUpEffect.launch(this, player, (Skill)skill, level);
        Bukkit.getServer().getPluginManager().callEvent(new SkillsLevelUpEvent(player, (Skill)skill, level));
    }

    public Collection<? extends Skill> getSkills() {
        return skillMap.values();
    }

    public Skill skillByName(String name) {
        return nameMap.get(name.toLowerCase());
    }

    public Skill getSkill(SkillType type) {
        return skillMap.get(type);
    }

    void giveMoney(Player player, double amount) {
        if (amount < 0.01) return;
        Double total = moneys.remove(player.getUniqueId());
        if (total == null) {
            total = amount;
        } else {
            total += amount;
        }
        if (total >= 5.0) {
            getEconomy().depositPlayer(player, total);
        } else {
            moneys.put(player.getUniqueId(), total);
        }
    }

    void giveExp(Player player, double amount) {
        if (amount < 0.01) return;
        final UUID uuid = player.getUniqueId();
        Double stored = exps.get(uuid);
        if (stored == null) {
            stored = amount;
        } else {
            stored += amount;
        }
        final int full = stored.intValue();
        if (full > 10) {
            stored -= (double)full;
            player.getWorld().spawn(player.getLocation(), ExperienceOrb.class).setExperience(full);
        }
        exps.put(uuid, stored);
    }

    void depositAllMoneys() {
        try {
            for (Map.Entry<UUID, Double> entry : moneys.entrySet()) {
                Double amount = entry.getValue();
                if (amount == null || amount < 0.01) continue;
                OfflinePlayer player = Bukkit.getServer().getOfflinePlayer(entry.getKey());
                if (player == null) continue;
                getEconomy().depositPlayer(player, amount);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            moneys.clear();
        }
    }

    boolean hasDebugMode(Player player) {
        return (playersInDebugMode.contains(player.getUniqueId()));
    }

    void setDebugMode(Player player, boolean debugMode) {
        if (debugMode) {
            playersInDebugMode.add(player.getUniqueId());
        } else {
            playersInDebugMode.remove(player.getUniqueId());
        }
    }

    Session getSession(UUID uuid) {
        Session result = sessions.get(uuid);
        if (result == null) {
            result = new Session(this, uuid);
            sessions.put(uuid, result);
        }
        return result;
    }

    Session getSession(Player player) {
        return getSession(player.getUniqueId());
    }

    void on20Ticks() {
        saveSome();
        for (Player player : Bukkit.getServer().getOnlinePlayers()) {
            getSession(player.getUniqueId()).on20Ticks();
        }
    }
}
