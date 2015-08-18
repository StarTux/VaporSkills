package com.winthier.skills.bukkit;

import com.winthier.exploits.bukkit.BukkitExploits;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

class BukkitSkillBuild extends BukkitSkill implements Listener
{
    @lombok.Getter final BukkitSkillType skillType = BukkitSkillType.BUILD;
    final long REPEAT_CHECK_SECONDS = 60L * 10L;
    
    boolean allowPlacedBlock(Block block, Player player)
    {
        if (BukkitExploits.getInstance().didRecentlyPlace(player, block, REPEAT_CHECK_SECONDS)) return false;
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
