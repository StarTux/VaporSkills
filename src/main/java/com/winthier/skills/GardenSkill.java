package com.winthier.skills;

import com.winthier.exploits.bukkit.BukkitExploits;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;

final class GardenSkill extends Skill {
    private static final double RADIUS = 100;

    GardenSkill(SkillsPlugin plugin) {
        super(plugin, SkillType.GARDEN);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (!allowPlayer(player)) return;
        Block block = event.getBlock();
        if (BukkitExploits.getInstance().isPlayerPlaced(block)) return;
        Material mat = block.getType();
        int factor = 1;
        switch (mat) {
        case CACTUS:
        case SUGAR_CANE:
            Block stackedBlock = block.getRelative(0, 1, 0);
            while (stackedBlock.getType() == mat
                   && BukkitExploits.getInstance().isPlayerPlaced(stackedBlock)) {
                factor += 1;
                stackedBlock = stackedBlock.getRelative(0, 1, 0);
            }
            break;
        default: break;
        }
        Reward reward = getReward(Reward.Category.BREAK_BLOCK, mat.name(), null, null);
        giveReward(player, reward, (double)factor);
    }
}
