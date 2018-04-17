package com.winthier.skills;

import com.winthier.exploits.bukkit.BukkitExploits;
import lombok.Getter;
import org.bukkit.ChatColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

final class HuntSkill extends Skill implements Listener {
    private long killDistanceInterval = 300;
    private double minKillDistance = 16;

    HuntSkill(SkillsPlugin plugin) {
        super(plugin, SkillType.HUNT);
    }

    @Override
    public void configure() {
        killDistanceInterval = getConfig().getLong("KillDistanceInterval", 300);
        minKillDistance = getConfig().getDouble("MinKillDistance", 16);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof LivingEntity)) return;
        LivingEntity entity = (LivingEntity)event.getEntity();
        if (!(event.getDamager() instanceof Arrow)) return;
        Arrow arrow = (Arrow)event.getDamager();
        if (!(arrow.getShooter() instanceof Player)) return;
        Player player = (Player)arrow.getShooter();
        if (!allowPlayer(player)) return;
        if (entity.getCustomName() != null && entity.getCustomName().startsWith("" + ChatColor.COLOR_CHAR)) return;
        if (BukkitExploits.getInstance().recentKillDistance(player, entity.getLocation(), killDistanceInterval) < minKillDistance) return;
        double percentage = BukkitExploits.getInstance().getEntityDamageByPlayerRemainderPercentage(entity, Math.min(entity.getHealth(), event.getFinalDamage()));
        if (plugin.hasDebugMode(player)) Msg.msg(player, "&eHunt Dmg=%.02f/%.02f Pct=%.02f", event.getFinalDamage(), entity.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue(), percentage);
        giveReward(player, rewardForEntity(entity), percentage);
    }
}
