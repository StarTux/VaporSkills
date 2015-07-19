package com.winthier.skills.bukkit;

import lombok.Getter;

@Getter
class BukkitSkillHarvest extends BukkitSkillAbstractBlockBreak
{
    final BukkitSkillType skillType = BukkitSkillType.HARVEST;
    final String title = "Harvesting";
    final String verb = "harvest";
    final String personName = "harvester";
    final String activityName = "harvesting";

    @Override
    Boolean requirePlayerPlacedBlock()
    {
        // We want blocks to be *not* player placed.
	return false;
    }
}
