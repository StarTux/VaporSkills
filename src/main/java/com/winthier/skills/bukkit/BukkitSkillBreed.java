package com.winthier.skills.bukkit;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;

class BukkitSkillBreed extends BukkitSkill implements Listener {
    @lombok.Getter final BukkitSkillType skillType = BukkitSkillType.BREED;
    static final double RADIUS = 10.0;

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.BREEDING) return;
        Player player = getNearestPlayer(event.getEntity(), RADIUS);
        if (player == null) return;
        if (!allowPlayer(player)) return;
        giveReward(player, rewardForEntity(event.getEntity()));
    }
}
