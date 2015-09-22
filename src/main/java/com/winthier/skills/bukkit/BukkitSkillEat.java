package com.winthier.skills.bukkit;

import com.winthier.skills.Reward;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

class BukkitSkillEat extends BukkitSkillAbstractConsume
{
    @Getter final BukkitSkillType skillType = BukkitSkillType.EAT;
    double foodLevelFactor = 1;
    double saturationFactor = 1;

    @Override
    void configure()
    {
        super.configure();
        foodLevelFactor = getConfig().getDouble("FoodLevelFactor", 1);
        saturationFactor = getConfig().getDouble("SaturationFactor", 1);
    }

    @Override
    void onConsume(Player player, ItemStack item)
    {
        Reward reward = rewardForItem(item);
        if (reward == null) return;
        final int foodLevel = player.getFoodLevel();
        final float saturation = player.getSaturation();
        new BukkitRunnable() {
            @Override public void run() {
                onDidEat(player, reward, foodLevel, saturation);
            }
        }.runTask(getPlugin());
    }

    void onDidEat(Player player, Reward reward, int oldFoodLevel, float oldSaturation)
    {
        int foodLevelGain = player.getFoodLevel() - oldFoodLevel;
        float saturationGain = player.getSaturation() - oldSaturation;
        if (getSkills().hasDebugMode(player)) BukkitUtil.msg(player, "&eEat Food=%d Sat=%.02f", foodLevelGain, saturationGain);
        giveReward(player, reward, (double)foodLevelGain*foodLevelFactor + (double)saturationGain*saturationFactor);
    }
}
