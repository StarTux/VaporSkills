package com.winthier.skills.bukkit;

import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

@Getter
class BukkitSkillDig extends BukkitSkillAbstractBlockBreak
{
    final BukkitSkillType skillType = BukkitSkillType.DIG;
    final String title = "Digging";
    final String verb = "dig";
    final String personName = "digger";
    final String activityName = "digging";

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
