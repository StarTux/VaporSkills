package com.winthier.skills.bukkit;

import org.bukkit.entity.Ageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

class BukkitSkillButcher extends BukkitSkillAbstractEntityKill
{
    @lombok.Getter final BukkitSkillType skillType = BukkitSkillType.BUTCHER;

    @Override
    boolean allowEntity(LivingEntity entity) {
        if (entity instanceof Player) return false;
        if (entity instanceof Ageable && !((Ageable)entity).isAdult()) return false;
        return true;
    }

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
