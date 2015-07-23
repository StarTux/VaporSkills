package com.winthier.skills.bukkit;

import org.bukkit.event.Listener;
import lombok.Getter;

@Getter
class BukkitSkillEat extends BukkitSkillAbstractConsume
{
    final BukkitSkillType skillType = BukkitSkillType.EAT;
    final String title = "Eating";
    final String verb = "eat";
    final String personName = "eater";
    final String activityName = "eating";
}
