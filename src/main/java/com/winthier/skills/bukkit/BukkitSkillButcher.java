package com.winthier.skills.bukkit;

import lombok.Getter;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

@Getter
class BukkitSkillButcher extends BukkitSkillAbstractEntityKill
{
    final BukkitSkillType skillType = BukkitSkillType.BUTCHER;
    final String title = "Butchery";
    final String verb = "butcher";
    final String personName = "butcher";
    final String activityName = "butchering";

    @Override
    boolean allowDamager(Entity entity, Player player)
    {
        return true;
    }

    @Override
    boolean useKillDistance()
    {
        return false;
    }

    @Override
    double scoreMultiplier(Player player, Entity entity)
    {
        return 1.0;
    }
}
