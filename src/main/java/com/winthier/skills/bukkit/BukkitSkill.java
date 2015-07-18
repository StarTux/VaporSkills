package com.winthier.skills.bukkit;

import com.winthier.skills.Skill;
import com.winthier.skills.sql.SQLReward;
import com.winthier.skills.sql.SQLRewardBlock;
import lombok.Getter;
import org.bukkit.block.Block;
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

    SQLReward rewardForBlock(Block block)
    {
	int blockType = block.getType().getId();
	int blockData = (int)block.getData();
	SQLRewardBlock reward = SQLRewardBlock.find(getKey(), blockType, blockData);
	if (reward == null) reward = SQLRewardBlock.find(getKey(), blockType);
	if (reward == null) return null;
	return reward.getReward();
    }

    void giveMoney(Player player, double money)
    {
	getPlugin().getEconomy().depositPlayer(player, money);
    }
}
