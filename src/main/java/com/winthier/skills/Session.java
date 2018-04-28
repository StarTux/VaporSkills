package com.winthier.skills;

import java.util.UUID;
import lombok.Data;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

@Data
class Session {
    private final SkillsPlugin plugin;
    private final UUID uuid;
    @Getter private double sessionMoney = 0.0;
    private boolean progressBarEnabled = true;
    private BukkitRunnable updateTask;
    private BossBar progressBar;
    private int noRewardTimer = 0;

    Session(SkillsPlugin plugin, UUID uuid) {
        this.plugin = plugin;
        this.uuid = uuid;
        this.progressBar = Bukkit.getServer().createBossBar("Skills", BarColor.PINK, BarStyle.SEGMENTED_20);
    }

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

    void setProgressBarEnabled(boolean value) {
        if (progressBarEnabled == value) return;
        progressBarEnabled = value;
        if (!value) progressBar.setVisible(false);
    }

    // Called once every second by SkillsPlugin
    void on20Ticks() {
        if (!progressBarEnabled) return;
        noRewardTimer += 1;
        if (noRewardTimer == 10) {
            progressBar.setVisible(false);
        }
    }

    void onDisable() {
        progressBar.setVisible(false);
        progressBar.removeAll();
    }
}
