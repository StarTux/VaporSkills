package com.winthier.skills;

import lombok.Getter;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

class ButcherSkill extends Skill implements Listener {
    @Getter final SkillType skillType = SkillType.BUTCHER;

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.getCustomName() != null) return;
        if (entity instanceof Ageable && !((Ageable)entity).isAdult()) return;
        if (!(entity.getKiller() instanceof Player)) return;
        Player player = entity.getKiller();
        if (!allowPlayer(player)) return;
        giveReward(player, rewardForEntity(entity));
    }
}
