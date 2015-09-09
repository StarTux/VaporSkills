package com.winthier.skills.bukkit;

import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

class BukkitSkillWoodcut extends BukkitSkillAbstractBlockBreak
{
    @Getter final BukkitSkillType skillType = BukkitSkillType.WOODCUT;

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
