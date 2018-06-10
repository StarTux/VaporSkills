package com.winthier.skills;

import com.winthier.custom.item.CustomItem;
import lombok.RequiredArgsConstructor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

@RequiredArgsConstructor
public final class GearItem implements CustomItem {
    public static final String CUSTOM_ID = "skills:gear";
    final SkillsPlugin plugin;

    @Override
    public String getCustomId() {
        return CUSTOM_ID;
    }

    @Override
    public ItemStack spawnItemStack(int amount) {
        return new ItemStack(Material.IRON_CHESTPLATE);
    }
}
