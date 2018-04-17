package com.winthier.skills;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;

class BreedSkill extends Skill implements Listener {
    static final double RADIUS = 10.0;

    BreedSkill(SkillsPlugin plugin) {
        super(plugin, SkillType.BREED);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.BREEDING) return;
        Player player = getNearestPlayer(event.getEntity(), RADIUS);
        if (player == null) return;
        if (!allowPlayer(player)) return;
        giveReward(player, rewardForEntity(event.getEntity()));
    }
}
