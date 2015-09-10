package com.winthier.skills.bukkit;

import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTameEvent;

class BukkitSkillTame extends BukkitSkill implements Listener
{
    @Getter final BukkitSkillType skillType = BukkitSkillType.TAME;
    
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onEntityTame(EntityTameEvent event) {
        Player player = event.getOwner() instanceof Player ? (Player)event.getOwner() : null;
        if (player == null || !allowPlayer(player)) return;
        giveReward(player, rewardForEntity(event.getEntity()));
    }
}
