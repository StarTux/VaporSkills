package com.winthier.skills.bukkit;

import com.winthier.skills.Reward;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockFromToEvent;

@Getter
class BukkitSkillHarvest extends BukkitSkillAbstractBlockBreak
{
    final BukkitSkillType skillType = BukkitSkillType.HARVEST;
    final String title = "Harvesting";
    final String verb = "harvest";
    final String personName = "harvester";
    final String activityName = "harvesting";
    final double RADIUS = 40;

    @Override
    Boolean requirePlayerPlacedBlock()
    {
        // We want blocks to be *not* player placed.
	return false;
    }

    @EventHandler(ignoreCancelled=true, priority=EventPriority.MONITOR)
    public void onBlockFromTo(BlockFromToEvent event) {
        // In addition to block breaks, the Harvest Skill also
        // rewards crops broken by water.
        Block source = event.getBlock();
        if (source.getType() != Material.WATER && source.getType() != Material.STATIONARY_WATER) return;
        Block block = event.getToBlock();
        Reward reward = rewardForBlock(block);
        if (reward == null) return;
        Player player = getNearestPlayer(block.getLocation(), RADIUS);
        if (player == null) return;
        giveReward(player, reward);
    }
}
