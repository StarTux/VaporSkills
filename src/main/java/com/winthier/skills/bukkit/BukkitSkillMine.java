package com.winthier.skills.bukkit;

import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

@Getter
class BukkitSkillMine extends BukkitSkillAbstractBlockBreak
{
    final BukkitSkillType skillType = BukkitSkillType.MINE;
    final String title = "Mining";
    final String verb = "mine";
    final String personName = "miner";
    final String activityName = "mining";

    @Override
    Boolean requirePlayerPlacedBlock()
    {
	return false;
    }

    @Override
    boolean allowItemInHand(ItemStack item)
    {
	// Any pickaxe goes
	switch (item.getType()) {
	case DIAMOND_PICKAXE:
	case GOLD_PICKAXE:
	case IRON_PICKAXE:
	case STONE_PICKAXE:
	case WOOD_PICKAXE:
	    return true;
	default:
	    return false;
	}
    }
}
