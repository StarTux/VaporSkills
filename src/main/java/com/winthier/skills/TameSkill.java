package com.winthier.skills;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTameEvent;

final class TameSkill extends Skill implements Listener {
    TameSkill(SkillsPlugin plugin) {
        super(plugin, SkillType.TAME);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onEntityTame(EntityTameEvent event) {
        Player player = event.getOwner() instanceof Player ? (Player)event.getOwner() : null;
        if (player == null || !allowPlayer(player)) return;
        Reward reward = getReward(Reward.Category.TAME_ENTITY, event.getEntity().getType().name(), null, null);
        giveReward(player, reward);
    }
}
