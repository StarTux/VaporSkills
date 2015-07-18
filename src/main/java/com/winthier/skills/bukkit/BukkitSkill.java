package com.winthier.skills.bukkit;

import com.winthier.skills.Skill;
import lombok.Getter;
import org.bukkit.entity.Player;

/**
 * Abstract class which implements Skill in a Bukkit-y manner.
 */
abstract class BukkitSkill implements Skill
{
    BukkitSkill()
    {
	// skillType = null;
	// title = null;
	// activityName = null;
	// personName = null;
    }
    
    BukkitSkillsPlugin getPlugin()
    {
	return BukkitSkillsPlugin.instance;
    }

    BukkitSkills getSkills()
    {
	return BukkitSkills.instance;
    }

    abstract BukkitSkillType getSkillType();
    void enable() {};
    String getPermissionNode()
    {
	return "skills.skill" + getSkillType().name().toLowerCase();
    }

    boolean allowPlayer(Player player)
    {
	return player.hasPermission(getPermissionNode());
    }
}
