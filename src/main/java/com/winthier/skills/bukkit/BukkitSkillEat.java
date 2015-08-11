package com.winthier.skills.bukkit;

import com.winthier.skills.Reward;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

@Getter
class BukkitSkillEat extends BukkitSkillAbstractConsume
{
    final BukkitSkillType skillType = BukkitSkillType.EAT;
    final String title = "Eating";
    final String verb = "eat";
    final String personName = "eater";
    final String activityName = "eating";

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
