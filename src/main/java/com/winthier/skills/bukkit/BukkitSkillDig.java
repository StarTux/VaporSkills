package com.winthier.skills.bukkit;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

class BukkitSkillDig extends BukkitSkillAbstractBlockBreak
{
    @lombok.Getter final BukkitSkillType skillType = BukkitSkillType.DIG;

    @Override
    Boolean requirePlayerPlacedBlock()
    {
        // We want blocks to be *not* player placed.
	return false;
    }

    @Override
    boolean allowItemInHand(ItemStack item)
    {
	// Any shovel goes
	switch (item.getType()) {
	case DIAMOND_SPADE:
	case GOLD_SPADE:
	case IRON_SPADE:
	case STONE_SPADE:
	case WOOD_SPADE:
	    return true;
	default:
	    return false;
	}
    }
}
