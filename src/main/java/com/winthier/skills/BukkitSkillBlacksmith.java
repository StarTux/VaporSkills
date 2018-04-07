package com.winthier.skills;

import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

class BukkitSkillBlacksmith extends BukkitSkill implements Listener {
    @Getter final BukkitSkillType skillType = BukkitSkillType.BLACKSMITH;
    static final int INPUT_SLOT_1 = 0;
    static final int INPUT_SLOT_2 = 1;
    static final int OUTPUT_SLOT = 2;

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onInventoryClick(InventoryClickEvent event) {
        final Inventory inv = event.getInventory();
        if (inv.getType() != InventoryType.ANVIL) return;
        if (event.getSlotType() != InventoryType.SlotType.RESULT) return;
        final Player player = (Player)event.getWhoClicked();
        if (!allowPlayer(player)) return;
        // Input and output slot must not be empty
        ItemStack inputItem1 = inv.getItem(INPUT_SLOT_1);
        ItemStack inputItem2 = inv.getItem(INPUT_SLOT_2);
        ItemStack outputItem = inv.getItem(OUTPUT_SLOT);
        if (inputItem1 == null || inputItem1.getType() == Material.AIR) return;
        if (inputItem2 == null || inputItem2.getType() == Material.AIR) return;
        if (outputItem == null || outputItem.getType() == Material.AIR) return;
        final int level = player.getLevel();
        new BukkitRunnable() {
            @Override public void run() {
                onAnvilUsed(player, inv, level);
            }
        }.runTask(BukkitSkillsPlugin.getInstance());
    }

    void onAnvilUsed(Player player, Inventory inv, int oldLevel) {
        // Was the output item removed?
        ItemStack outputItem = inv.getItem(OUTPUT_SLOT);
        if (outputItem != null && outputItem.getType() != Material.AIR) return;
        // Were levels used up?
        int levelsUsed = oldLevel - player.getLevel();
        if (levelsUsed <= 0) return;
        giveReward(player, rewardForNameAndMaximum("exp_level_cost", levelsUsed));
    }
}
