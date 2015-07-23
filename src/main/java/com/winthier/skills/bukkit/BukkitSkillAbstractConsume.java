package com.winthier.skills.bukkit;

import com.winthier.skills.Reward;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;

abstract class BukkitSkillAbstractConsume extends BukkitSkill implements Listener
{
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPlayerItemConsume(PlayerItemConsumeEvent event)
    {
        if (!allowPlayer(event.getPlayer())) return;
        
    }

    void onConsume(Player player, ItemStack item)
    {
        // By default, reward for the consumed item
        giveReward(player, rewardForItem(item));
    }
}
