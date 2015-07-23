package com.winthier.skills.bukkit;

import org.bukkit.event.Listener;
import lombok.Getter;

@Getter
class BukkitSkillSmelt extends BukkitSkillAbstractFurnace
{
    final BukkitSkillType skillType = BukkitSkillType.SMELT;
    final String title = "Smelting";
    final String verb = "smelt";
    final String personName = "smelter";
    final String activityName = "smelting";

    @Override
    double smeltRadius()
    {
        return 40.0;
    }
}
