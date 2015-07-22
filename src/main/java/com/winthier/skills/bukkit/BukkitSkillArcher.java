package com.winthier.skills.bukkit;

import lombok.Getter;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

@Getter
class BukkitSkillArcher extends BukkitSkillAbstractEntityKill
{
    final BukkitSkillType skillType = BukkitSkillType.ARCHER;
    final String title = "Archery";
    final String verb = "snipe";
    final String personName = "archer";
    final String activityName = "archery";
    final double KILL_DISTANCE = 8;
    final long KILL_DISTANCE_SECONDS = 60L * 5;
    final double PLAYER_DISTANCE = 16;

    @Override
    boolean allowDamager(Entity entity, Player player)
    {
        return entity instanceof Arrow;
    }

    @Override
    boolean useKillDistance()
    {
        return true;
    }

    @Override
    double fullKillDistance()
    {
        return KILL_DISTANCE;
    }

    @Override
    long killDistanceSeconds()
    {
        return KILL_DISTANCE_SECONDS;
    }

    @Override
    double scoreMultiplier(Player player, Entity entity)
    {
        double distance = player.getLocation().distance(entity.getLocation());
        return Math.min(1.0, distance / PLAYER_DISTANCE);
    }
}
