package com.winthier.skills;

import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

class EatSkill extends Skill implements Listener {
    @Getter final SkillType skillType = SkillType.EAT;
    private double foodLevelFactor = 1;
    private double saturationFactor = 1;

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
        }.runTask(SkillsPlugin.getInstance());
    }

    void onDidEat(Player player, Reward reward, int oldFoodLevel, float oldSaturation) {
        int foodLevelGain = player.getFoodLevel() - oldFoodLevel;
        float saturationGain = player.getSaturation() - oldSaturation;
        if (SkillsPlugin.getInstance().hasDebugMode(player)) Msg.msg(player, "&eEat Food=%d Sat=%.02f", foodLevelGain, saturationGain);
        giveReward(player, reward, (double)foodLevelGain * foodLevelFactor + (double)saturationGain * saturationFactor);
    }
}