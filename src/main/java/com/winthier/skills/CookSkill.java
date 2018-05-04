package com.winthier.skills;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

final class CookSkill extends Skill {
    CookSkill(SkillsPlugin plugin) {
        super(plugin, SkillType.COOK);
    }

    /**
     * Called by SkillsPlugin.onFurnaceSmelt()
     */
    void onItemSmelt(Player player, ItemStack source, ItemStack result) {
        // By default, reward the result of the smelting
        Reward reward = getReward(Reward.Category.SMELT_ITEM, result.getType().name(), null, null);
        giveReward(player, reward);
    }
}
