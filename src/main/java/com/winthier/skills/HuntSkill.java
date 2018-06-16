package com.winthier.skills;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.ProjectileLaunchEvent;

final class HuntSkill extends Skill {
    HuntSkill(SkillsPlugin plugin) {
        super(plugin, SkillType.HUNT);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    void onProjectileLaunch(ProjectileLaunchEvent event) {
    }

    /**
     * Called by SkillsPlugin.onEntityDamageByEntity().
     */
    void onProjectileHit(Player player, Projectile projectile, LivingEntity target) {
    }

    void onEntityKill(Player player, LivingEntity entity) {
        Reward reward = getReward(Reward.Category.KILL_ENTITY, entity.getType().name(), null, null);
        giveReward(player, reward, 1);
    }
}
