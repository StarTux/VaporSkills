package com.winthier.skills;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.Data;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
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
    private double weaponCharge = 0;
    private BossBar weaponChargeBar;
    private boolean charging = false;
    private double weaponChargeSpeed = 0;
    private int maxWeaponCharge = 0;

    Session(SkillsPlugin plugin, UUID uuid) {
        this.plugin = plugin;
        this.uuid = uuid;
        this.progressBar = Bukkit.getServer().createBossBar("Progress", BarColor.PURPLE, BarStyle.SEGMENTED_20);
        this.weaponChargeBar = Bukkit.getServer().createBossBar("Weapon Charge", BarColor.YELLOW, BarStyle.SEGMENTED_10);
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
        progressBar.setTitle(ChatColor.BLUE + skill.getDisplayName() + " Level " + level);
    }

    /**
     * Called by SkillsPlugin.onTick(), once per tick.
     */
    void onTick(Player player) {
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
        if (charging) {
            double summand = weaponChargeSpeed / 10.0;
            double oldWeaponCharge = weaponCharge;
            weaponCharge = Math.min((double)maxWeaponCharge, weaponCharge + summand);
            boolean isLevelUp = (int)weaponCharge != (int)oldWeaponCharge;
            if (isLevelUp) {
                weaponChargeBar.removeAll();
                int chargeLevel = (int)weaponCharge;
                boolean isMax = chargeLevel == maxWeaponCharge;
                ChatColor color;
                switch (chargeLevel) {
                case 1:
                    color = ChatColor.YELLOW;
                    weaponChargeBar.setColor(BarColor.YELLOW);
                    break;
                case 2:
                    color = ChatColor.GOLD;
                    weaponChargeBar.setColor(BarColor.RED);
                    break;
                case 3:
                    color = ChatColor.RED;
                    weaponChargeBar.setColor(BarColor.RED);
                    break;
                default:
                    color = ChatColor.WHITE;
                    weaponChargeBar.setTitle("Charge");
                }
                if (isMax) {
                    weaponChargeBar.setTitle("" + color + ChatColor.BOLD + "Charge " + chargeLevel);
                } else {
                    weaponChargeBar.setTitle(color + "Charge " + chargeLevel);
                }
                player.playSound(player.getEyeLocation(), Sound.BLOCK_NOTE_BELL, SoundCategory.PLAYERS, 0.1f, 1.4f + 0.1f * (float)chargeLevel);
            }
            if (weaponCharge < 0.01) {
                weaponChargeBar.setProgress(0);
            } else if (Math.abs(Math.ceil(weaponCharge) - weaponCharge) < summand) {
                weaponChargeBar.setProgress(1);
            } else {
                weaponChargeBar.setProgress(weaponCharge % 1.0);
            }
            if (isLevelUp) weaponChargeBar.addPlayer(player);
        }
    }

    void startCharging(Player player, int maxCharge, double chargeSpeed) {
        charging = true;
        weaponChargeBar.setProgress(0);
        weaponChargeBar.setTitle("Charge");
        weaponChargeBar.setColor(BarColor.WHITE);
        weaponCharge = 0;
        maxWeaponCharge = maxCharge;
        weaponChargeSpeed = chargeSpeed;
        weaponChargeBar.addPlayer(player);
    }

    void stopCharging() {
        weaponChargeBar.removeAll();
        charging = false;
    }

    /**
     * Called by SkillsPlugin when the plugin is disabled or reloaded
     * via admin command.
     */
    void onDisable() {
        progressBar.removeAll();
        weaponChargeBar.removeAll();
    }
}
