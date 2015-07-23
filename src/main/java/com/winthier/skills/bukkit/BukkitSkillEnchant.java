package com.winthier.skills.bukkit;

import java.util.Map;
import lombok.Getter;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;

@Getter
class BukkitSkillEnchant extends BukkitSkill implements Listener
{
    final BukkitSkillType skillType = BukkitSkillType.ENCHANT;
    final String title = "Enchanting";
    final String verb = "enchant";
    final String personName = "enchanter";
    final String activityName = "enchanting";
    final double MAX_EXP_LEVEL = 3;

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onEnchantItem(EnchantItemEvent event)
    {
        Player player = event.getEnchanter();
        if (player == null) return;
        if (!allowPlayer(player)) return;
        double percentage = Math.min(1.0, event.getExpLevelCost() / MAX_EXP_LEVEL);
        for (Map.Entry<Enchantment, Integer> entry : event.getEnchantsToAdd().entrySet()) {
            Enchantment enchant = entry.getKey();
            int level = entry.getValue();
            giveReward(player, rewardForEnchantment(enchant, level), percentage);
        }
    }
}
