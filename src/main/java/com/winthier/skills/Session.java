package com.winthier.skills;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.Data;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

/**
 * Maintain player scoreboards and transient named cooldowns for
 * various skills.
 */
@Data
final class Session {
    private final SkillsPlugin plugin;
    private final UUID uuid;
    private long ticks = 0;
    // Progress bar
    private boolean progressBarEnabled = true;
    private BossBar progressBar;
    private int noRewardTimer = 0;
    // Cooldowns
    private final Map<String, Integer> cooldowns = new HashMap<>();
    // Brawling

    Session(SkillsPlugin plugin, UUID uuid) {
        this.plugin = plugin;
        this.uuid = uuid;
        this.progressBar = Bukkit.getServer().createBossBar("Skills", BarColor.PINK, BarStyle.SEGMENTED_20);
    }

    // Accessors

    /**
     * Called by SkillsCommand to change user preferences.
     */
    void setProgressBarEnabled(boolean value) {
        if (progressBarEnabled == value) return;
        progressBarEnabled = value;
        if (!value) progressBar.setVisible(false);
    }

    void setCooldown(String name, int value) {
        cooldowns.put(name, value);
    }

    int getCooldown(String name) {
        Integer result = cooldowns.get(name);
        if (result == null) return 0;
        return result;
    }

    // Handle various "events"

    /**
     * Called by Skill.giveReward()
     */
    void onReward(Player player, Skill skill, double skillPoints) {
        if (!progressBarEnabled) return;
        int points = (int)plugin.getScore().getSkillPoints(uuid, skill.skillType);
        int level = Score.levelForPoints(points);
        int pointsA = Score.pointsForLevel(level);
        int pointsB = Score.pointsForLevel(level + 1);
        points -= pointsA;
        pointsB -= pointsA;
        double progress = (double)points / (double)pointsB;
        progressBar.setProgress(progress);
        noRewardTimer = 0;
        progressBar.setVisible(true);
        progressBar.addPlayer(player);
        progressBar.setTitle(skill.getDisplayName() + " Level " + ChatColor.YELLOW + level);
    }

    /**
     * Called by SkillsPlugin.onTick(), once per tick.
     */
    void onTick() {
        ticks += 1;
        if (progressBarEnabled && ticks % 20 == 0) {
            noRewardTimer += 1;
            if (noRewardTimer == 10) {
                progressBar.setVisible(false);
            }
        }
        for (String key: cooldowns.keySet()) {
            Integer val = cooldowns.get(key);
            if (val == 1) {
                cooldowns.remove(key);
            } else {
                cooldowns.put(key, val - 1);
            }
        }
    }

    /**
     * Called by SkillsPlugin when the plugin is disabled or reloaded
     * via admin command.
     */
    void onDisable() {
        progressBar.setVisible(false);
        progressBar.removeAll();
    }
}
