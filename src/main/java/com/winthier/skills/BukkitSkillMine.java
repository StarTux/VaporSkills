package com.winthier.skills;

import lombok.Getter;
import org.bukkit.inventory.ItemStack;

class BukkitSkillMine extends BukkitSkillAbstractBlockBreak {
    @Getter final BukkitSkillType skillType = BukkitSkillType.MINE;

    @Override
    boolean allowItemInHand(ItemStack item) {
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
