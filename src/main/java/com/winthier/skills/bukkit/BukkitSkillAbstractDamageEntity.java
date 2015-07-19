package com.winthier.skills.bukkit;

import com.winthier.exploits.bukkit.BukkitExploits;
import com.winthier.skills.Reward;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.projectiles.ProjectileSource;

abstract class BukkitSkillAbstractDamageEntity extends BukkitSkill implements Listener
{
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event)
    {
        Player player = findPlayerDamager(event.getDamager());
        if (player == null || !allowPlayer(player)) return;
        if (!allowDamager(event.getDamager(), player)) return;
        Reward reward = rewardForEntity(event.getEntity());
        if (reward == null) return;
        double fraction = Math.min(1.0 ,event.getFinalDamage() / event.getEntity().getMaxHealth());
        // TODO: Everything else
        //
    }

    final Player findPlayerDamager(Entity entity)
    {
        if (entity instanceof Player) return (Player)entity;
        if (entity instanceof Projectile) {
            ProjectileSource source = ((Projectile)entity).getShooter();
            if (source instanceof Player) return (Player)source;
        }
        return null;
    }

    boolean allowDamager(Entity entity, Player player)
    {
        return true;
    }
}
