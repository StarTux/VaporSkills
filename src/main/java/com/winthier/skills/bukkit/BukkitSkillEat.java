package com.winthier.skills.bukkit;

import com.winthier.skills.Reward;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

class BukkitSkillEat extends BukkitSkillAbstractConsume
{
    @lombok.Getter final BukkitSkillType skillType = BukkitSkillType.EAT;

    @Override
    void onConsume(Player player, ItemStack item)
    {
        Reward reward = rewardForItem(item);
        if (reward == null) return;
        final int foodLevel = player.getFoodLevel();
        new BukkitRunnable() {
            @Override public void run() {
                onDidEat(player, reward, foodLevel);
            }
        }.runTask(getPlugin());
    }

    void onDidEat(Player player, Reward reward, int oldFoodLevel)
    {
        int foodLevelGain = player.getFoodLevel() - oldFoodLevel;
        giveReward(player, reward, foodLevelGain);
    }
}
