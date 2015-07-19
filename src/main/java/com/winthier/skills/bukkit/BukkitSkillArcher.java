package com.winthier.skills.bukkit;

import lombok.Getter;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

@Getter
class BukkitSkillArcher extends BukkitSkillAbstractDamageEntity
{
    final BukkitSkillType skillType = BukkitSkillType.ARCHER;
    final String title = "Archery";
    final String verb = "snipe";
    final String personName = "archer";
    final String activityName = "archery";

    @Override
    boolean allowDamager(Entity entity, Player player)
    {
        return entity instanceof Arrow;
    }
}
