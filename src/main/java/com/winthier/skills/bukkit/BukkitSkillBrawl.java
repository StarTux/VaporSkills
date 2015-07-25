package com.winthier.skills.bukkit;

import lombok.Getter;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

@Getter
class BukkitSkillBrawl extends BukkitSkillAbstractEntityKill
{
    final BukkitSkillType skillType = BukkitSkillType.BRAWL;
    final String title = "Brawling";
    final String verb = "brawl";
    final String personName = "brawler";
    final String activityName = "brawling";
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
