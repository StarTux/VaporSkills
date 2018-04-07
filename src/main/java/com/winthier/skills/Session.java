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
    private final UUID uuid;
    @Getter private double sessionMoney = 0.0;
    private boolean progressBarEnabled = true;
    private BukkitRunnable updateTask;
    private BossBar progressBar;
    private int noRewardTimer = 0;

    Session(UUID uuid) {
        this.uuid = uuid;
        this.progressBar = Bukkit.getServer().createBossBar("Skills", BarColor.PINK, BarStyle.SEGMENTED_20);
    }

    void onReward(Player player, Skill skill, Reward reward) {
        if (!progressBarEnabled) return;
        sessionMoney += reward.getMoney();
        Score score = SkillsPlugin.getInstance().getScore();
        int points = (int)score.getSkillPoints(uuid, skill);
        int level = score.levelForPoints(points);
        int pointsA = score.pointsForLevel(level);
        int pointsB = score.pointsForLevel(level + 1);
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

    public static Session of(Player player) {
        return SkillsPlugin.getInstance().getSession(player);
    }

    void onDisable() {
        progressBar.setVisible(false);
        progressBar.removeAll();
    }
}
