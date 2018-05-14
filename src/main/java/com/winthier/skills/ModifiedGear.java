package com.winthier.skills;

import com.winthier.custom.item.CustomItem;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public final class ModifiedGear implements CustomItem {
    @Override
    public String getCustomId() {
        return "winthier_skills:modified_gear";
    }

    @Override
    public ItemStack spawnItemStack(int amount) {
        return new ItemStack(Material.WOOD_SWORD);
    }
}
