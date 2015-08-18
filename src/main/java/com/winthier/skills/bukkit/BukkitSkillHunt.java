package com.winthier.skills.bukkit;

import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

class BukkitSkillHunt extends BukkitSkillAbstractEntityKill
{
    @lombok.Getter final BukkitSkillType skillType = BukkitSkillType.HUNT;
    final double KILL_DISTANCE = 16;
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
    double minKillDistance()
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
