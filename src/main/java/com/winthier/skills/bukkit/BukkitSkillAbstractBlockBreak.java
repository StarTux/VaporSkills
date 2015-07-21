package com.winthier.skills.bukkit;

import com.winthier.exploits.bukkit.BukkitExploits;
import com.winthier.skills.Reward;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

abstract class BukkitSkillAbstractBlockBreak extends BukkitSkill implements Listener
{
    @Override
    void enable()
    {
	Bukkit.getServer().getPluginManager().registerEvents(this, getPlugin());
    }

    /**
     * Default function always returns true.
     */
    boolean allowItemInHand(ItemStack item)
    {
	return true;
    }

    Boolean requirePlayerPlacedBlock()
    {
	return null;
    }

    /**
     * Default function checks if the blocks is natural or player
     * placed and returns based on requirePlayerPlacedBlock();
     */
    boolean allowBrokenBlock(Block block)
    {
	if (requirePlayerPlacedBlock() != null) {
	    if (BukkitExploits.getInstance().isPlayerPlaced(block) != requirePlayerPlacedBlock()) return false;
	}
	return true;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onBlockBreak(BlockBreakEvent event)
    {
	if (!allowPlayer(event.getPlayer())) return;
	if (!allowItemInHand(event.getPlayer().getItemInHand())) return;
	if (!allowBrokenBlock(event.getBlock())) return;
	Reward reward = rewardForBlock(event.getBlock());
	if (reward == null) return;
        giveSkillPoints(event.getPlayer(), reward.getSkillPoints());
        giveMoney(event.getPlayer(), reward.getMoney());
	event.setExpToDrop(event.getExpToDrop() + (int)reward.getExp());
    }
}
