package com.winthier.skills;

import com.winthier.exploits.bukkit.BukkitExploits;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

abstract class AbstractBlockBreakSkill extends Skill implements Listener {
    private long repeatInterval = 60 * 60;

    @Override
    void configure() {
        repeatInterval = getConfig().getLong("RepeatInterval", 60 * 60);
    }

    /**
     * Determine if the item in hand is acceptable while breaking
     * blocks. Return false to deny rewards while holding the
     * item, true to allow it. Default function always returns
     * true.
     */
    boolean allowItemInHand(ItemStack item) {
        return true;
    }

    /**
     * Default function checks if the blocks is natural or player
     * placed and returns based on requirePlayerPlacedBlock();
     */
    boolean allowBrokenBlock(Player player, Block block) {
        if (BukkitExploits.getInstance().isPlayerPlaced(block)) return false;
        if (BukkitExploits.getInstance().didRecentlyBreak(player, block, repeatInterval)) return false;
        return true;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        if (!allowPlayer(player)) return;
        if (!allowItemInHand(player.getInventory().getItemInMainHand())) return;
        if (!allowBrokenBlock(player, block)) return;
        giveReward(player, rewardForBlock(block));
    }
}
