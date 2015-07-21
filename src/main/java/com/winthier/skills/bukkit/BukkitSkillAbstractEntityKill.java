package com.winthier.skills.bukkit;

import com.winthier.exploits.bukkit.BukkitExploits;
import com.winthier.skills.Reward;
import org.bukkit.Bukkit;
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
        if (entity instanceof Player) return;
        Player player = entity.getKiller();
        if (player == null) return;
        if (!(entity.getLastDamageCause() instanceof EntityDamageByEntityEvent)) return;
        EntityDamageByEntityEvent edbee = (EntityDamageByEntityEvent)entity.getLastDamageCause();
        if (!allowDamager(edbee.getDamager(), player)) return;
        //
        if (!allowPlayer(player)) return;
        Reward reward = rewardForEntity(entity);
        if (reward == null) return;
        double percentage = BukkitExploits.getInstance().getEntityDamageByPlayerPercentage(entity);
        if (useKillDistance()) {
            percentage *= BukkitExploits.getInstance().getKillDistancePercentageWithinSeconds(player, entity.getLocation(), killDistanceSeconds(), fullKillDistance());
        }
        percentage *= scoreMultiplier(player, entity);
        giveSkillPoints(player, reward.getSkillPoints() * (float)percentage);
        giveMoney(player, reward.getMoney() * percentage);
        event.setDroppedExp((int)(event.getDroppedExp() + reward.getExp() * percentage));
    }                             

    boolean allowDamager(Entity entity, Player player)
    {
        return true;
    }

    boolean useKillDistance() { return false; }
    double fullKillDistance() { return 16.0; }
    long killDistanceSeconds() { return 60; }
    double scoreMultiplier(Player player, Entity entity) { return 1.0; }
}
