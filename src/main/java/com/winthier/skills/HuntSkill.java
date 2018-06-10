package com.winthier.skills;

import org.bukkit.ChatColor;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

final class HuntSkill extends Skill {
    private long killDistanceInterval = 300;
    private double minKillDistance = 16;

    HuntSkill(SkillsPlugin plugin) {
        super(plugin, SkillType.HUNT);
    }

    @Override
    public void configure() {
        killDistanceInterval = getConfig().getLong("KillDistanceInterval", 300);
        minKillDistance = getConfig().getDouble("MinKillDistance", 16);
    }

    void onEntityKill(Player player, LivingEntity entity) {
        if (entity.getCustomName() != null && entity.getCustomName().startsWith("" + ChatColor.COLOR_CHAR)) return;
        Reward reward = getReward(Reward.Category.KILL_ENTITY, entity.getType().name(), null, null);
        giveReward(player, reward, 1);
    }
}
