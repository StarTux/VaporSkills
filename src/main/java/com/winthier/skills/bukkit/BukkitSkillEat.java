package com.winthier.skills.bukkit;

import com.winthier.skills.Reward;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

class BukkitSkillEat extends BukkitSkill implements Listener
{
    @Getter final BukkitSkillType skillType = BukkitSkillType.EAT;
    double foodLevelFactor = 1;
    double saturationFactor = 1;

    @Override
    void configure() {
        foodLevelFactor = getConfig().getDouble("FoodLevelFactor", 1);
        saturationFactor = getConfig().getDouble("SaturationFactor", 1);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPlayerItemConsume(PlayerItemConsumeEvent event) {
        if (!allowPlayer(event.getPlayer())) return;
        Player player = event.getPlayer();
        if (player.hasPotionEffect(PotionEffectType.HUNGER)) return;
        Reward reward = rewardForItem(event.getItem());
        if (reward == null) return;
        final int foodLevel = player.getFoodLevel();
        final float saturation = player.getSaturation();
        new BukkitRunnable() {
            @Override public void run() {
                onDidEat(player, reward, foodLevel, saturation);
            }
        }.runTask(getPlugin());
    }

    void onDidEat(Player player, Reward reward, int oldFoodLevel, float oldSaturation) {
        int foodLevelGain = player.getFoodLevel() - oldFoodLevel;
        float saturationGain = player.getSaturation() - oldSaturation;
        if (getSkills().hasDebugMode(player)) BukkitUtil.msg(player, "&eEat Food=%d Sat=%.02f", foodLevelGain, saturationGain);
        giveReward(player, reward, (double)foodLevelGain*foodLevelFactor + (double)saturationGain*saturationFactor);
    }
}
