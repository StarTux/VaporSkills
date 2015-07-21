package com.winthier.skills.bukkit;

import com.winthier.skills.Reward;
import com.winthier.skills.Skill;
import lombok.Getter;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

/**
 * Abstract class which implements Skill in a Bukkit-y manner.
 */
abstract class BukkitSkill implements Skill
{
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
	return "skills.skill" + getKey();
    }

    boolean allowPlayer(Player player)
    {
	return player.hasPermission(getPermissionNode());
    }

    @Override
    public String getKey()
    {
        return getSkillType().name().toLowerCase();
    }

    Reward rewardForBlock(Block block)
    {
        @SuppressWarnings("deprecation")
	int blockType = block.getType().getId();
        @SuppressWarnings("deprecation")
	int blockData = (int)block.getData();
        return getSkills().getScore().rewardForBlock(this, blockType, blockData);
    }

    Reward rewardForEntity(Entity e)
    {
        return getSkills().getScore().rewardForEntity(this, BukkitEntities.name(e));
    }

    void giveSkillPoints(Player player, double skillPoints)
    {
        if (skillPoints == 0) return;
	getSkills().getScore().giveSkillPoints(player.getUniqueId(), this, skillPoints);
    }

    void giveMoney(Player player, double money)
    {
        if (money < 0.01) return;
	getPlugin().getEconomy().depositPlayer(player, money);
    }
}
