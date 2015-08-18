package com.winthier.skills.bukkit;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

class BukkitSkillBrawl extends BukkitSkillAbstractEntityKill
{
    @lombok.Getter final BukkitSkillType skillType = BukkitSkillType.BRAWL;
    final double KILL_DISTANCE = 16;
    final long KILL_DISTANCE_SECONDS = 60L * 5;

    @Override
    boolean allowDamager(Entity entity, Player player)
    {
        return entity == player;
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
        return 1.0;
    }
}
