package com.winthier.skills;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.inventory.ItemStack;

final class BrewSkill extends Skill implements Listener {
    static final double RADIUS = 40.0;

    BrewSkill(SkillsPlugin plugin) {
        super(plugin, SkillType.BREW);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onBrew(BrewEvent event) {
        final Player player = null; // TODO
        if (player == null) return;
        if (!allowPlayer(player)) return;
        // Count potions for percentage
        int count = 0;
        ItemStack[] contents = event.getContents().getContents();
        for (int i = 0; i < Math.min(3, contents.length); ++i) {
            if (contents[i] != null) count += 1;
        }
        ItemStack ingredient = event.getContents().getIngredient();
        Reward reward = getReward(Reward.Category.INGREDIENT, ingredient.getType().name(), (int)ingredient.getDurability(), null);
        giveReward(player, reward, (double)count / 3.0);
    }
}
