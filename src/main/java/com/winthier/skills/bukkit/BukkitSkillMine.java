package com.winthier.skills.bukkit;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

class BukkitSkillMine extends BukkitSkillAbstractBlockBreak
{
    @lombok.Getter final BukkitSkillType skillType = BukkitSkillType.MINE;

    @Override
    Boolean requirePlayerPlacedBlock()
    {
        // We want blocks to be *not* player placed.
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
