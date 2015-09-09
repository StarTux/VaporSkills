package com.winthier.skills.bukkit;

import com.winthier.skills.CustomReward;
import com.winthier.skills.Reward;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;

class BukkitSkillSacrifice extends BukkitSkill implements Listener
{
    @lombok.Getter final BukkitSkillType skillType = BukkitSkillType.SACRIFICE;
    final double RADIUS = 20;
    
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onEntityDamage(EntityDamageEvent event)
    {
        if (!(event.getEntity() instanceof Item)) return;
        Player player = getNearestPlayer(event.getEntity().getLocation(), RADIUS);
        if (player == null) return;
        if (!allowPlayer(player)) return;
        Item item = (Item)event.getEntity();
        List<Reward> rewards = rewardsForItem(item.getItemStack());
        if (rewards.isEmpty()) return;
        double factor = (double)item.getItemStack().getAmount();
        item.remove();
        for (Reward reward : rewards) giveReward(player, reward, factor);
    }

    static void addReward(List<Reward> list, Reward reward)
    {
        if (reward == null) return;
        list.add(reward);
    }

    List<Reward> rewardsForItem(ItemStack item)
    {
        List<Reward> result = new ArrayList<>();
        if (item == null) return result;
        addReward(result, rewardForItem(item));
        for (Map.Entry<Enchantment, Integer> entry : item.getEnchantments().entrySet()) {
            addReward(result, rewardForEnchantment(entry.getKey(), entry.getValue()));
        }
        if (item.hasItemMeta() && item.getItemMeta() instanceof EnchantmentStorageMeta) {
            for (Map.Entry<Enchantment, Integer> entry : ((EnchantmentStorageMeta)item.getItemMeta()).getStoredEnchants().entrySet()) {
                addReward(result, rewardForEnchantment(entry.getKey(), entry.getValue()));
            }
        }
        return result;
    }

    /**
     * Utility function for the checkitem command. Do not use for
     * actual rewarding!
     */
    public Reward fullRewardForItem(ItemStack item)
    {
        float skillPoints = 0;
        float money = 0;
        float exp = 0;
        for (Reward reward : rewardsForItem(item)) {
            skillPoints += reward.getSkillPoints();
            money += reward.getMoney();
            exp += reward.getExp();
        }
        skillPoints *= getSkillPointsFactor() * getSkills().getSkillPointsFactor();
        money       *= getMoneyFactor()       * getSkills().getMoneyFactor();
        exp         *= getExpFactor()         * getSkills().getExpFactor();
        return new CustomReward(skillPoints, money, exp);
    }
}
