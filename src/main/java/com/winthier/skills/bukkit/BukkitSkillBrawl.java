package com.winthier.skills.bukkit;

import com.winthier.exploits.bukkit.BukkitExploits;
import lombok.Getter;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

class BukkitSkillBrawl extends BukkitSkill implements Listener
{
    @Getter final BukkitSkillType skillType = BukkitSkillType.BRAWL;
    long killDistanceInterval = 300;
    double minKillDistance = 16;

    @Override
    public void configure()
    {
        super.configure();
        killDistanceInterval = getConfig().getLong("KillDistanceInterval", 300);
        minKillDistance = getConfig().getDouble("MinKillDistance", 16);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event)
    {
        if (!(event.getDamager() instanceof Player)) return;
        Player player = (Player)event.getDamager();
        if (!allowPlayer(player)) return;
        LivingEntity entity = (LivingEntity)event.getEntity();
        if (entity.getCustomName() != null) return;
        if (BukkitExploits.getInstance().recentKillDistance(player, entity.getLocation(), killDistanceInterval) < minKillDistance) return;
        double percentage = BukkitExploits.getInstance().getEntityDamageByPlayerRemainderPercentage(entity, Math.min(entity.getHealth(), event.getFinalDamage()));
        if (getSkills().hasDebugMode(player)) BukkitUtil.msg(player, "&eBrawl Dmg=%.02f/%.02f Pct=%.02f", event.getFinalDamage(), entity.getMaxHealth(), percentage);
        giveReward(player, rewardForEntity(entity), percentage);
    }
}
