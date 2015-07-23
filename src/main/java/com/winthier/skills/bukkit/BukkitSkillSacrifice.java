package com.winthier.skills.bukkit;

import com.winthier.skills.Reward;
import org.bukkit.block.Block;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import lombok.Getter;

@Getter
class BukkitSkillSacrifice extends BukkitSkill implements Listener
{
    final BukkitSkillType skillType = BukkitSkillType.SACRIFICE;
    final String title = "Sacrifice";
    final String verb = "sacrifice";
    final String personName = "sacrificer";
    final String activityName = "sacrifing";
    final double RADIUS = 20;
    
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onEntityDamage(EntityDamageEvent event)
    {
        if (!(event.getEntity() instanceof Item)) return;
        Player player = getNearestPlayer(event.getEntity().getLocation(), RADIUS);
        if (player == null) return;
        if (!allowPlayer(player)) return;
        Item entity = (Item)event.getEntity();
        ItemStack item = entity.getItemStack();
        Reward reward = rewardForItem(item);
        if (reward == null) return;
        entity.remove();
        giveReward(player, reward);
    }
}
