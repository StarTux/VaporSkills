package com.winthier.skills.bukkit;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

@Getter
class BukkitSkillBrew extends BukkitSkill implements Listener
{
    final BukkitSkillType skillType = BukkitSkillType.BREW;
    final String title = "Brewing";
    final String verb = "brew";
    final String personName = "brewer";
    final String activityName = "brewing";
    final double RADIUS = 40.0;

    @RequiredArgsConstructor
    static class BrewItems
    {
        final ItemStack a, b, c;
        static BrewItems of(BrewerInventory inv)
        {
            ItemStack[] items = inv.getContents();
            ItemStack a = items[0] != null ? items[0].clone() : null;
            ItemStack b = items[1] != null ? items[1].clone() : null;
            ItemStack c = items[2] != null ? items[2].clone() : null;
            return new BrewItems(a, b, c);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onBrew(BrewEvent event)
    {
        final Player player = getNearestPlayer(event.getContents().getHolder().getLocation(), RADIUS);
        if (player == null) return;
        if (!allowPlayer(player)) return;
        final BrewerInventory inv = event.getContents();
        final BrewItems oldBrew = BrewItems.of(inv);
        new BukkitRunnable() {
            @Override public void run() {
                afterBrew(player, inv, oldBrew);
            }
        }.runTaskLater(getPlugin(), 0L);
    }

    void afterBrew(Player player, BrewerInventory inv, BrewItems oldBrew)
    {
        if (!player.isValid()) return;
        BrewItems newBrew = BrewItems.of(inv);
        onPotionBrewed(player, oldBrew.a, newBrew.a);
        onPotionBrewed(player, oldBrew.b, newBrew.b);
        onPotionBrewed(player, oldBrew.c, newBrew.c);
    }

    void onPotionBrewed(Player player, ItemStack oldItem, ItemStack newItem) {
        if (oldItem == null) return;
        if (newItem == null) return;
        if (oldItem.equals(newItem)) return;
        if (oldItem.isSimilar(newItem)) return;
        giveReward(player, rewardForItem(newItem));
    }
}
