package com.winthier.skills.bukkit;

import com.winthier.exploits.bukkit.BukkitExploits;
import lombok.Getter;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

class BukkitSkillButcher extends BukkitSkill implements Listener
{
    @Getter final BukkitSkillType skillType = BukkitSkillType.BUTCHER;

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onEntityDeath(EntityDeathEvent event)
    {
        LivingEntity entity = event.getEntity();
        if (entity.getCustomName() != null) return;
        if (entity instanceof Ageable && !((Ageable)entity).isAdult()) return;
        if (!(entity.getKiller() instanceof Player)) return;
        Player player = entity.getKiller();
        giveReward(player, rewardForEntity(entity));
    }
}
