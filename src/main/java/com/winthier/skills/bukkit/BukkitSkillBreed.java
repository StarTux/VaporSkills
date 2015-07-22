package com.winthier.skills.bukkit;

import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;

@Getter
class BukkitSkillBreed extends BukkitSkill implements Listener
{
    final BukkitSkillType skillType = BukkitSkillType.BREED;
    final String title = "Breeding";
    final String verb = "breed";
    final String personName = "breeder";
    final String activityName = "breeding";
    final double RADIUS = 10.0;

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onCreatureSpawn(CreatureSpawnEvent event)
    {
        if (event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.BREEDING) return;
        Player player = getNearestPlayer(event.getEntity(), RADIUS);
        if (player == null) return;
        if (!allowPlayer(player)) return;
        giveReward(player, rewardForEntity(event.getEntity()));
    }
}
