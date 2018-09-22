// package com.winthier.skills;

// import com.winthier.exploits.bukkit.BukkitExploits;
// import org.bukkit.block.Block;
// import org.bukkit.entity.Player;
// import org.bukkit.event.EventHandler;
// import org.bukkit.event.EventPriority;
// import org.bukkit.event.block.BlockBreakEvent;

// final class DigSkill extends Skill {
//     private long repeatInterval = 60 * 60;

//     DigSkill(SkillsPlugin plugin) {
//         super(plugin, SkillType.DIG);
//     }

//     @Override
//     void configure() {
//         repeatInterval = getConfig().getLong("RepeatInterval", 60 * 60);
//     }

//     @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
//     public void onBlockBreak(BlockBreakEvent event) {
//         final Player player = event.getPlayer();
//         if (!allowPlayer(player)) return;
//         switch (player.getInventory().getItemInMainHand().getType()) {
//         case DIAMOND_SHOVEL:
//         case GOLDEN_SHOVEL:
//         case IRON_SHOVEL:
//         case STONE_SHOVEL:
//         case WOODEN_SHOVEL:
//             break;
//         default:
//             return;
//         }
//         final Block block = event.getBlock();
//         if (BukkitExploits.getInstance().isPlayerPlaced(block)) return;
//         if (BukkitExploits.getInstance().didRecentlyBreak(player, block, repeatInterval)) return;
//         Reward reward = getReward(Reward.Category.BREAK_BLOCK, block.getType().name(), null, null);
//         giveReward(player, reward);
//     }
// }
