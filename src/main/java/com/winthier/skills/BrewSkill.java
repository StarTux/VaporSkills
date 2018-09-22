// package com.winthier.skills;

// import org.bukkit.entity.Player;
// import org.bukkit.inventory.BrewerInventory;
// import org.bukkit.inventory.ItemStack;

// final class BrewSkill extends Skill {
//     static final double RADIUS = 40.0;

//     BrewSkill(SkillsPlugin plugin) {
//         super(plugin, SkillType.BREW);
//     }

//     /**
//      * Called by SkillsPlugin.onBrew()
//      */
//     public void onBrew(Player player, BrewerInventory inventory) {
//         if (!allowPlayer(player)) return;
//         // Count potions for percentage
//         int count = 0;
//         ItemStack[] contents = inventory.getContents();
//         for (int i = 0; i < Math.min(3, contents.length); ++i) {
//             if (contents[i] != null) count += 1;
//         }
//         ItemStack ingredient = inventory.getIngredient();
//         Reward reward = getReward(Reward.Category.INGREDIENT, ingredient.getType().name(), null, null);
//         giveReward(player, reward, (double)count / 3.0);
//     }
// }
