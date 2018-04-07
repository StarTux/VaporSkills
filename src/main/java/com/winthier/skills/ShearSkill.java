package com.winthier.skills;

import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerShearEntityEvent;

class ShearSkill extends Skill implements Listener {
    @Getter final SkillType skillType = SkillType.SHEAR;

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onEntityDamageByEntity(PlayerShearEntityEvent event) {
        Player player = event.getPlayer();
        if (!allowPlayer(player)) return;
        giveReward(player, rewardForEntity(event.getEntity()));
    }
}
