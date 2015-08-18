package com.winthier.skills.bukkit;

import com.avaje.ebean.EbeanServer;
import com.winthier.skills.Skill;
import com.winthier.skills.Skills;
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
import org.apache.commons.lang3.text.WordUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

public class BukkitSkills extends Skills
{
    @Getter static BukkitSkills instance;
    final Map<BukkitSkillType, BukkitSkill> skillMap = new EnumMap<>(BukkitSkillType.class);
    final Map<String, BukkitSkill> nameMap = new HashMap<>();
    final Map<UUID, Double> moneys = new HashMap<>();
    final Set<UUID> playersInDebugMode = new HashSet<>();
    final Map<UUID, BukkitPlayer> players = new HashMap<>();

    BukkitSkills()
    {
	instance = this;
        List<BukkitSkill> skills = Arrays.asList(
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
            new BukkitSkillQuaff(),
            new BukkitSkillSacrifice(),
            new BukkitSkillSmelt(),
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

    void buildNameMap()
    {
        nameMap.clear();
        // Put all the names in the map
        for (BukkitSkill skill : skillMap.values()) {
            nameMap.put(skill.getKey().toLowerCase(), skill);
            nameMap.put(skill.getTitle().toLowerCase(), skill);
            nameMap.put(skill.getVerb().toLowerCase(), skill);
            nameMap.put(skill.getActivityName().toLowerCase(), skill);
            nameMap.put(skill.getPersonName().toLowerCase(), skill);
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
        BukkitUtil.title(player,
                         "{color:aqua,text:'" + skill.getTitle() + "'}",
                         "{color:aqua,text:'Level " + level + "'}");
        if (level >= 10) {
            BukkitUtil.announceRaw(
                BukkitUtil.format("&f%s reached level %d in ", player.getName(), level),
                BukkitUtil.button(
                    "&a[" + skill.getTitle() + "]",
                    "/sk " + skill.getVerb(),
                    "&a" + skill.getTitle(),
                    // TODO: Put something more interesting here?
                    "&f&oSkill",
                    "&r" + WordUtils.wrap(skill.getDescription(), 32)));
        }
        if (skill instanceof BukkitSkill) {
            BukkitPlayer.of(player).displaySkill((BukkitSkill)skill, player);
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
