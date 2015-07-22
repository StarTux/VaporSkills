package com.winthier.skills.bukkit;

import com.winthier.exploits.bukkit.BukkitExploits;
import lombok.Getter;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

@Getter
class BukkitSkillBuild extends BukkitSkill implements Listener
{
    final BukkitSkillType skillType = BukkitSkillType.BUILD;
    final String title = "Building";
    final String verb = "build";
    final String personName = "builder";
    final String activityName = "building";
    final long REPEAT_CHECK_SECONDS = 60L * 10L;
    
    boolean allowPlacedBlock(Block block, Player player)
    {
        if (BukkitExploits.getInstance().didPlaceBlockWithinSeconds(player, block, REPEAT_CHECK_SECONDS)) return false;
        return true;
    }
    
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onBlockPlace(BlockPlaceEvent event)
    {
	if (!allowPlayer(event.getPlayer())) return;
	if (!allowPlacedBlock(event.getBlock(), event.getPlayer())) return;
        giveReward(event.getPlayer(), rewardForBlock(event.getBlock()));
    }
}
