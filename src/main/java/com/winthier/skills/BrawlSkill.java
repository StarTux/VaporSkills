package com.winthier.skills;

import com.winthier.exploits.bukkit.BukkitExploits;
import lombok.Getter;
import org.bukkit.ChatColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

final class BrawlSkill extends Skill implements Listener {
    private long killDistanceInterval = 300;
    private double minKillDistance = 16;

    BrawlSkill() {
        super(SkillType.BRAWL);
    }

    @Override
    public void configure() {
        killDistanceInterval = getConfig().getLong("KillDistanceInterval", 300);
        minKillDistance = getConfig().getDouble("MinKillDistance", 16);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;
        if (!(event.getEntity() instanceof LivingEntity)) return;
        Player player = (Player)event.getDamager();
        if (!allowPlayer(player)) return;
        LivingEntity entity = (LivingEntity)event.getEntity();
        if (entity.getCustomName() != null && entity.getCustomName().startsWith("" + ChatColor.COLOR_CHAR)) return;
        if (BukkitExploits.getInstance().recentKillDistance(player, entity.getLocation(), killDistanceInterval) < minKillDistance) return;
        double percentage = BukkitExploits.getInstance().getEntityDamageByPlayerRemainderPercentage(entity, Math.min(entity.getHealth(), event.getFinalDamage()));
        if (SkillsPlugin.getInstance().hasDebugMode(player)) Msg.msg(player, "&eBrawl Dmg=%.02f/%.02f Pct=%.02f", event.getFinalDamage(), entity.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue(), percentage);
        giveReward(player, rewardForEntity(entity), percentage);
    }
}
