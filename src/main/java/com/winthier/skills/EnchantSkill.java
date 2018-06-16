package com.winthier.skills;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

final class EnchantSkill extends Skill {
    EnchantSkill(SkillsPlugin plugin) {
        super(plugin, SkillType.ENCHANT);
    }

    static final class EnchantingStore {
        private ItemStack itemStack;
        private Enchantment enchantment;
        private int enchantmentLevel;
        private int requiredExp;

        static EnchantingStore of(ArmorStand armorStand) {
            return null;
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onEnchantItem(EnchantItemEvent event) {
        final Player player = event.getEnchanter();
        if (!allowPlayer(player)) return;
        final int oldLevel = player.getLevel();
        final int levelUsed = event.getExpLevelCost();
        new BukkitRunnable() {
            @Override public void run() {
                int spent = oldLevel - player.getLevel();
                double factor = Math.min(1.0, (double)levelUsed / (double)spent / 10);
                Reward reward = getReward(Reward.Category.SPEND_LEVELS, null, spent, null);
                giveReward(player, reward, factor * factor);
            }
        }.runTask(plugin);
    }
}
