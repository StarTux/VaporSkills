package com.winthier.skills;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

final class HuntSkill extends Skill {
    HuntSkill(SkillsPlugin plugin) {
        super(plugin, SkillType.HUNT);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    void onProjectileLaunch(ProjectileLaunchEvent event) {
        final Player player = event.getPlayer();
        if (!allowPlayer(player)) return;
        int slot = findArrowToUse(player);
        player.sendMessage(slot);
    }

    /**
     * Called by SkillsPlugin.onEntityDamageByEntity().
     */
    void onProjectileDamage(Player player, Projectile projectile, LivingEntity target) {
    }

    /**
     * Called by SkillsPlugin.onEntityDamageByEntity().
     */
    void onEntityKill(Player player, LivingEntity entity) {
        Reward reward = getReward(Reward.Category.KILL_ENTITY, entity.getType().name(), null, null);
        giveReward(player, reward, 1);
    }

    // Charge attacks

    private int findArrowToUse(Player player) {
        PlayerInventory inv = player.getInventory();
        ItemStack item;
        int slot = inv.getHeldItemSlot();
        item = inv.getItem(heldItemSlot);
        if (item != null && item.getType() == Material.ARROW) return slot;
        slot = 40; // off-hand
        item = inv.getItem(slot);
        if (item != null && item.getType() == Material.ARROW) return slot;
        for (slot = 0; slot < 36; slot += 1) {
            item = inv.getItem(slot);
            if (item != null && item.getType() == Material.ARROW) return slot;
        }
        return -1;
    }

    void basicArrowCharge(Player player) {
    }

    void arrowMultiple(Player player) {
    }

    void arrowBarrage(Player player) {
    }

    void arrowHail(Player player) {
    }
}
