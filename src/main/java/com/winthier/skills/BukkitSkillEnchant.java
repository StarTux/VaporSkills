package com.winthier.skills;

import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.scheduler.BukkitRunnable;

class BukkitSkillEnchant extends BukkitSkill implements Listener {
    @Getter final BukkitSkillType skillType = BukkitSkillType.ENCHANT;

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onEnchantItem(EnchantItemEvent event) {
        final Player player = event.getEnchanter();
        if (player == null) return;
        if (!allowPlayer(player)) return;
        final int level = player.getLevel();
        new BukkitRunnable() {
            @Override public void run() {
                onEnchanted(player, level, event.getExpLevelCost());
            }
        }.runTask(BukkitSkillsPlugin.getInstance());
    }

    void onEnchanted(Player player, int oldLevel, int levelUsed) {
        int spent = oldLevel - player.getLevel();
        double factor = Math.min(1.0, (double)levelUsed / (double)spent / 10);
        giveReward(player, rewardForName("exp_level_cost", spent), factor * factor);
    }
}
