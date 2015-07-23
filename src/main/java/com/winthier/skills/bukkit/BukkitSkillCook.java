package com.winthier.skills.bukkit;

import org.bukkit.event.Listener;
import lombok.Getter;

@Getter
class BukkitSkillCook extends BukkitSkillAbstractFurnace
{
    final BukkitSkillType skillType = BukkitSkillType.COOK;
    final String title = "Cooking";
    final String verb = "cook";
    final String personName = "chef";
    final String activityName = "cooking";

    @Override
    double smeltRadius()
    {
        return 40.0;
    }
}
