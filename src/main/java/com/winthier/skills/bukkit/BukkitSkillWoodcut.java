package com.winthier.skills.bukkit;

import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

@Getter
class BukkitSkillWoodcut extends BukkitSkillAbstractBlockBreak
{
    final BukkitSkillType skillType = BukkitSkillType.WOODCUT;
    final String title = "Woodcutting";
    final String verb = "cut wood";
    final String personName = "woodcutter";
    final String activityName = "woodcutting";

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
	case DIAMOND_AXE:
	case GOLD_AXE:
	case IRON_AXE:
	case STONE_AXE:
	case WOOD_AXE:
	    return true;
	default:
	    return false;
	}
    }
}
