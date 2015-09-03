package com.winthier.skills.bukkit;

import java.util.Map;
import lombok.Getter;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;

class BukkitSkillEnchant extends BukkitSkill implements Listener
{
    @Getter final BukkitSkillType skillType = BukkitSkillType.ENCHANT;

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onEnchantItem(EnchantItemEvent event)
    {
        Player player = event.getEnchanter();
        if (player == null) return;
        if (!allowPlayer(player)) return;
        giveReward(player, rewardForName("exp_level_cost", event.getExpLevelCost()));
    }
}
