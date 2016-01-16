package com.winthier.skills.bukkit;

import com.avaje.ebean.EbeanServer;
import com.winthier.skills.Skill;
import com.winthier.skills.Skills;
import com.winthier.skills.bukkit.event.SkillsLevelUpEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

@Getter
public class BukkitSkills extends Skills
{
    @Getter static BukkitSkills instance;
    final Map<BukkitSkillType, BukkitSkill> skillMap = new EnumMap<>(BukkitSkillType.class);
    final Map<String, BukkitSkill> nameMap = new HashMap<>();
    final Map<UUID, Double> moneys = new HashMap<>();
    final Map<UUID, Double> exps = new HashMap<>();
    final Set<UUID> playersInDebugMode = new HashSet<>();
    final Map<UUID, BukkitPlayer> players = new HashMap<>();
    boolean enabled = true;
    double skillPointsFactor = 1.0;
    double moneyFactor = 1.0;
    double expFactor = 1.0;

    BukkitSkills()
    {
	instance = this;
        List<BukkitSkill> skills = Arrays.asList(
            new BukkitSkillBlacksmith(),
            new BukkitSkillBrawl(),
            new BukkitSkillBreed(),
            new BukkitSkillBrew(),
            new BukkitSkillBuild(),
            new BukkitSkillButcher(),
            new BukkitSkillCook(),
            new BukkitSkillDig(),
            new BukkitSkillEat(),
            new BukkitSkillEnchant(),
            new BukkitSkillFish(),
            new BukkitSkillHarvest(),
            new BukkitSkillHunt(),
            new BukkitSkillMine(),
            new BukkitSkillSacrifice(),
            new BukkitSkillShear(),
            new BukkitSkillSmelt(),
            new BukkitSkillTame(),
            new BukkitSkillTravel(),
            new BukkitSkillWoodcut()
            );
        for (BukkitSkill skill : skills) {
            BukkitSkillType type = skill.getSkillType();
            if (skillMap.containsKey(type)) {
                throw new IllegalStateException("Duplicate skill " + type.name() + ": " + skillMap.get(type).getClass().getSimpleName() + " and " + skill.getClass().getSimpleName());
            }
            skillMap.put(type, skill);
        }
    }

    void configure()
    {
        enabled = getPlugin().getConfig().getBoolean("Enabled", true);
        List<Double> factors = getPlugin().getConfig().getDoubleList("RewardFactors");
        skillPointsFactor = factors.size() >= 1 ? factors.get(0) : 1.0;
        moneyFactor       = factors.size() >= 2 ? factors.get(1) : 1.0;
        expFactor         = factors.size() >= 3 ? factors.get(2) : 1.0;
    }

    void buildNameMap()
    {
        nameMap.clear();
        // Put all the names in the map
        for (BukkitSkill skill : skillMap.values()) {
            nameMap.put(skill.getKey().toLowerCase(), skill);
            nameMap.put(skill.getDisplayName().toLowerCase(), skill);
            nameMap.put(skill.getShorthand().toLowerCase(), skill);
        }
        // Bake the map
        for (Map.Entry<String, BukkitSkill> entry : new ArrayList<>(nameMap.entrySet())) {
            BukkitSkill skill = entry.getValue();
            for (String name = entry.getKey(); name.length() > 0; name = name.substring(0, name.length() - 1)) {
                if (!nameMap.containsKey(name)) nameMap.put(name, skill);
            }
        }
    }

    BukkitSkillsPlugin getPlugin()
    {
	return BukkitSkillsPlugin.instance;
    }

    @Override
    public EbeanServer getDatabase()
    {
	return getPlugin().getDatabase();
    }

    @Override
    public void onLevelUp(UUID uuid, Skill skill, int level)
    {
        Player player = Bukkit.getServer().getPlayer(uuid);
        if (player == null) return;
        if (skill instanceof BukkitSkill) {
            BukkitLevelUpEffect.launch(player, (BukkitSkill)skill, level);
            BukkitPlayer.of(player).displaySkill((BukkitSkill)skill, player);
            Bukkit.getServer().getPluginManager().callEvent(new SkillsLevelUpEvent(player, (BukkitSkill)skill, level));
        }
    }
    
    @Override
    public Collection<? extends BukkitSkill> getSkills()
    {
        return skillMap.values();
    }

    BukkitSkill skillByName(String name)
    {
        return nameMap.get(name.toLowerCase());
    }

    BukkitSkill skillByType(BukkitSkillType type)
    {
        return skillMap.get(type);
    }

    void giveMoney(Player player, double amount) {
        if (amount < 0.01) return;
        Double stored = moneys.get(player.getUniqueId());
        if (stored == null) {
            stored = amount;
        } else {
            stored += amount;
        }
        moneys.put(player.getUniqueId(), stored);
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
        if (stored >= 1.0) {
            int full = stored.intValue();
            stored -= (double)full;
            player.giveExp(full);
        }
        exps.put(uuid, stored);
    }

    void depositAllMoneys()
    {
        try {
            for (Map.Entry<UUID, Double> entry : moneys.entrySet()) {
                Double amount = entry.getValue();
                if (amount == null || amount < 0.01) continue;
                OfflinePlayer player = Bukkit.getServer().getOfflinePlayer(entry.getKey());
                if (player == null) continue;
                getPlugin().getEconomy().depositPlayer(player, amount);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            moneys.clear();
        }
    }

    void depositSomeMoneys()
    {
        try {
            for (Iterator<Map.Entry<UUID, Double>> iter = moneys.entrySet().iterator(); iter.hasNext(); ) {
                Map.Entry<UUID, Double> entry = iter.next();
                Double amount = entry.getValue();
                if (amount == null || amount < 100) continue;
                OfflinePlayer player = Bukkit.getServer().getOfflinePlayer(entry.getKey());
                if (player == null) continue;
                getPlugin().getEconomy().depositPlayer(player, amount);
                iter.remove();
                return;
            }
        } catch (Exception e) {
            System.err.println("Error delivering moneys. Clearing partial amounts.");
            e.printStackTrace();
            moneys.clear();
        }
    }

    boolean hasDebugMode(Player player)
    {
        return (playersInDebugMode.contains(player.getUniqueId()));
    }

    void setDebugMode(Player player, boolean debugMode) {
        if (debugMode) {
            playersInDebugMode.add(player.getUniqueId());
        } else {
            playersInDebugMode.remove(player.getUniqueId());
        }
    }

    BukkitPlayer getBukkitPlayer(UUID uuid)
    {
        BukkitPlayer result = players.get(uuid);
        if (result == null) {
            result = new BukkitPlayer(uuid);
            players.put(uuid, result);
        }
        return result;
    }

    BukkitPlayer getBukkitPlayer(Player player)
    {
        return getBukkitPlayer(player.getUniqueId());
    }

    void updateAllPlayers()
    {
        for (BukkitPlayer player : players.values()) player.updateScoreboard();
    }
}
