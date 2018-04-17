package com.winthier.skills;

import com.winthier.exploits.bukkit.BukkitExploits;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

class MineSkill extends Skill implements Listener {
    private long repeatInterval = 60 * 60;

    MineSkill(SkillsPlugin plugin) {
        super(plugin, SkillType.MINE);
    }

    @Override
    void configure() {
        repeatInterval = getConfig().getLong("RepeatInterval", 60 * 60);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onBlockBreak(BlockBreakEvent event) {
        final Player player = event.getPlayer();
        if (!allowPlayer(player)) return;
        switch (player.getInventory().getItemInMainHand().getType()) {
        case DIAMOND_PICKAXE:
        case GOLD_PICKAXE:
        case IRON_PICKAXE:
        case STONE_PICKAXE:
        case WOOD_PICKAXE:
            break;
        default:
            return;
        }
        final Block block = event.getBlock();
        if (BukkitExploits.getInstance().isPlayerPlaced(block)) return;
        if (BukkitExploits.getInstance().didRecentlyBreak(player, block, repeatInterval)) return;
        giveReward(player, rewardForBlock(block));
    }
}
