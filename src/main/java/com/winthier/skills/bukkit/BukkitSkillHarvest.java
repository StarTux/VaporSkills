package com.winthier.skills.bukkit;

import com.winthier.exploits.bukkit.BukkitExploits;
import com.winthier.skills.Reward;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFromToEvent;

class BukkitSkillHarvest extends BukkitSkill implements Listener
{
    @Getter final BukkitSkillType skillType = BukkitSkillType.HARVEST;
    final double RADIUS = 100;

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onBlockBreak(BlockBreakEvent event)
    {
        Player player = event.getPlayer();
	if (!allowPlayer(player)) return;
        Block block = event.getBlock();
        onBlockBroken(player, block);
        Material mat = block.getType();
        switch (mat) {
        case CACTUS:
        case SUGAR_CANE_BLOCK:
            block = block.getRelative(0, 1, 0);
            while (block.getType() == mat) {
                onBlockBroken(player, block);
                block = block.getRelative(0, 1, 0);
            }
        }
    }

    boolean onBlockBroken(Player player, Block block)
    {
        if (BukkitExploits.getInstance().isPlayerPlaced(block)) return false;
        giveReward(player, rewardForBlock(block));
        return true;
    }
    
    @EventHandler(ignoreCancelled=true, priority=EventPriority.MONITOR)
    public void onBlockFromTo(BlockFromToEvent event) {
        // In addition to block breaks, the Harvest Skill also
        // rewards crops broken by water.
        Block source = event.getBlock();
        if (source.getType() != Material.WATER && source.getType() != Material.STATIONARY_WATER) return;
        Block block = event.getToBlock();
        Reward reward = rewardForBlockNamed(block, "water");
        if (reward == null) return;
        Player player = getNearestPlayer(block.getLocation(), RADIUS);
        if (player == null) return;
	if (!allowPlayer(player)) return;
        giveReward(player, reward);
    }
}
