package com.winthier.skills;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.inventory.ItemStack;

final class CookSkill extends Skill implements Listener {
    CookSkill(SkillsPlugin plugin) {
        super(plugin, SkillType.COOK);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onFurnaceSmelt(FurnaceSmeltEvent event) {
        Player player = null; // TODO
        if (player == null) return;
        if (!allowPlayer(player)) return;
        onItemSmelt(player, event.getSource(), event.getResult());
    }

    void onItemSmelt(Player player, ItemStack source, ItemStack result) {
        // By default, reward the result of the smelting
        Reward reward = getReward(Reward.Category.SMELT_ITEM, result.getType().name(), null, null);
        giveReward(player, reward);
    }
}
