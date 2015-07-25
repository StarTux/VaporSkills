package com.winthier.skills.bukkit;

import com.avaje.ebean.EbeanServer;
import com.winthier.skills.Skill;
import com.winthier.skills.Skills;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;

public class BukkitSkills extends Skills
{
    @Getter static BukkitSkills instance;
    final Map<BukkitSkillType, BukkitSkill> skillMap = new EnumMap<>(BukkitSkillType.class);

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
            new BukkitSkillHunter(),
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
    public void onLevelUp(UUID player, Skill skill, int level)
    {
	// TODO: Call an event or whatever
    }

    @Override
    public Collection<? extends BukkitSkill> getSkills()
    {
        return skillMap.values();
    }
}
