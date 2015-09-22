package com.winthier.skills.bukkit;

import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerShearEntityEvent;

class BukkitSkillShear extends BukkitSkill implements Listener
{
    @Getter final BukkitSkillType skillType = BukkitSkillType.SHEAR;

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onEntityDamageByEntity(PlayerShearEntityEvent event)
    {
        Player player = event.getPlayer();
        if (!allowPlayer(player)) return;
        giveReward(player, rewardForEntity(event.getEntity()));
    }
}
