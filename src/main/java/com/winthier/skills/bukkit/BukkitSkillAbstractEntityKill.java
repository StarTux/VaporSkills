package com.winthier.skills.bukkit;

import com.winthier.exploits.bukkit.BukkitExploits;
import com.winthier.skills.Reward;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;

abstract class BukkitSkillAbstractEntityKill extends BukkitSkill implements Listener
{
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onEntityDeath(EntityDeathEvent event)
    {
        LivingEntity entity = event.getEntity();
        if (!allowEntity(entity)) return;
        Player player = entity.getKiller();
        if (player == null) return;
        if (!(entity.getLastDamageCause() instanceof EntityDamageByEntityEvent)) return;
        EntityDamageByEntityEvent edbee = (EntityDamageByEntityEvent)entity.getLastDamageCause();
        if (!allowDamager(edbee.getDamager(), player)) return;
        if (!allowPlayer(player)) return;
        if (useKillDistance() && BukkitExploits.getInstance().recentKillDistance(player, entity.getLocation(), killDistanceSeconds()) < minKillDistance()) return;
        Reward reward = rewardForEntity(entity);
        if (reward == null) return;
        double percentage = BukkitExploits.getInstance().getEntityDamageByPlayerPercentage(entity);
        percentage *= scoreMultiplier(player, entity);
        giveReward(player, reward, percentage);
    }                             

    boolean allowDamager(Entity entity, Player player)
    {
        return true;
    }

    boolean allowEntity(LivingEntity entity) { return !(entity instanceof Player); }
    boolean useKillDistance() { return false; }
    double minKillDistance() { return 16.0; }
    long killDistanceSeconds() { return 60; }
    double scoreMultiplier(Player player, Entity entity) { return 1.0; }
}
