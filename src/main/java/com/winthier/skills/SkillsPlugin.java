package com.winthier.skills;

import com.winthier.sql.SQLDatabase;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
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
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
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
    private SQLDatabase db;
    // Commands
    private final AdminCommand adminCommand = new AdminCommand(this);
    private final SkillsCommand skillsCommand = new SkillsCommand(this);
    private final HighscoreCommand highscoreCommand = new HighscoreCommand(this);
    // Internal Data
    private final Score score = new Score(this);
    private final Map<SkillType, Skill> skillMap = new EnumMap<>(SkillType.class);
    private final Map<String, Skill> nameMap = new HashMap<>();
    private final Map<UUID, Double> exps = new HashMap<>();
    private final Set<UUID> playersInDebugMode = new HashSet<>();
    private final Map<UUID, Session> sessions = new HashMap<>();
    private ConfigurationSection perksConfig = null;

    public SkillsPlugin() {
        instance = this;
    }

    static List<Class<?>> getDatabaseClasses() {
        return Arrays.asList(SQLScore.class,
                             SQLPerk.class,
                             SQLPerkProgress.class);
    }

    @Override
    public void onEnable() {
        // Files
        writeDefaultFiles(false);
        reloadConfig();
        // Database
        db = new SQLDatabase(this);
        for (Class<?> clazz: getDatabaseClasses()) db.registerTable(clazz);
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
        // Cache
        buildNameMap();
        importRewards();
    }

    @Override
    public void onDisable() {
        for (Skill skill : getSkills()) {
            skill.onDisable();
        }
        for (Player player: getServer().getOnlinePlayers()) {
            getSession(player).onDisable();
        }
    }

    void writeDefaultFiles(boolean force) {
        saveResource(CONFIG_YML, force);
        saveResource(REWARDS_TXT, force);
        saveResource(PERKS_YML, force);
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
        importRewards();
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
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
        for (Player player : Bukkit.getServer().getOnlinePlayers()) {
            getSession(player.getUniqueId()).on20Ticks();
        }
    }

    void importRewards() {
        File file = new File(getDataFolder(), REWARDS_TXT);
        if (!file.exists()) {
            getLogger().warning(REWARDS_TXT + " not found");
            return;
        }
        int linum = 0;
        BufferedReader in = null;
        Reward reward = null;
        try {
            Map<Reward.Key, Reward> map = new HashMap<>();
            in = new BufferedReader(new FileReader(file));
            String line = null;
            while (null != (line = in.readLine())) {
                linum++;
                line = line.split("#")[0];
                if (line.isEmpty()) continue;
                String[] tokens = line.split("\\s+");
                try {
                    reward = Reward.parse(tokens);
                } catch (RuntimeException re) {
                    System.err.println("Skipping " + REWARDS_TXT + " line " + linum);
                    re.printStackTrace();
                    continue;
                }
                if (map.containsKey(reward.key)) getLogger().warning("Warning: Duplicate key '" + reward.key + "' in line " + linum);
                map.put(reward.key, reward);
            }
            score.setRewards(map);
            getLogger().info("Imported " + map.size() + " rewards from " + REWARDS_TXT);
        } catch (IOException ioe) {
            getLogger().warning("Error reading " + REWARDS_TXT + ". See console.");
            ioe.printStackTrace();
        } catch (RuntimeException re) {
            getLogger().warning("Error parsing " + REWARDS_TXT + ", line " + linum + ". See console.");
            getLogger().warning("" + reward);
            re.printStackTrace();
        } finally {
            try {
                in.close();
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
    }
}
