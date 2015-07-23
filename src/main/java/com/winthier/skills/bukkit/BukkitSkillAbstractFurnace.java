package com.winthier.skills.bukkit;

import com.winthier.skills.Reward;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.inventory.ItemStack;

abstract class BukkitSkillAbstractFurnace extends BukkitSkill implements Listener
{
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onFurnaceSmelt(FurnaceSmeltEvent event)
    {
        Player player = getNearestPlayer(event.getBlock().getLocation(), smeltRadius());
        if (player == null) return;
	if (!allowPlayer(player)) return;
        onItemSmelt(player, event.getSource(), event.getResult());
    }

    void onItemSmelt(Player player, ItemStack source, ItemStack result)
    {
        // By default, reward the result of the smelting
        giveReward(player, rewardForItem(result));
    }

    abstract double smeltRadius();
}
