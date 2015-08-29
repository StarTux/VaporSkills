package com.winthier.skills.bukkit;

import com.winthier.skills.Reward;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import lombok.Data;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

@Data
class BukkitPlayer
{
    final UUID uuid;
    // Scoreboard
    Scoreboard scoreboard = null;
    Objective sidebarObjective = null;
    String lastProgressBar = null;
    BukkitSkillType skillOnScoreboard = null;
    BukkitSkillType forcedSkillOnScoreboard = null;
    boolean sidebarEnabled = true;
    // Skill points short-term memory
    final Map<BukkitSkillType, BukkitPlayerSkill> skills = new EnumMap<>(BukkitSkillType.class);
    // Instant feedback task
    BukkitRunnable updateTask;

    BukkitPlayer(UUID uuid)
    {
        this.uuid = uuid;
        for (BukkitSkillType type : BukkitSkillType.values()) {
            skills.put(type, new BukkitPlayerSkill(type));
        }
    }

    static BukkitSkills getSkills()
    {
        return BukkitSkills.getInstance();
    }

    static BukkitSkillsPlugin getPlugin()
    {
        return BukkitSkillsPlugin.getInstance();
    }

    Player getPlayer()
    {
        return Bukkit.getServer().getPlayer(uuid);
    }

    static BukkitPlayer of(Player player)
    {
        return getSkills().getBukkitPlayer(player);
    }

    void onReward(BukkitSkill skill, Reward reward, double factor)
    {
        if (!sidebarEnabled) return;
        skills.get(skill.getSkillType()).onReward(reward, factor);
        // Make sure the scoreboard gets updated as soon as
        // possible for instant feedback.
        if (updateTask == null) {
            updateTask = new BukkitRunnable() {
                @Override public void run() {
                    updateTask = null;
                    updateScoreboard();
                }
            };
            updateTask.runTask(getPlugin());
        }
    }

    void updateScoreboard()
    {
        Player player = getPlayer();
        if (player == null) return;
        if (forcedSkillOnScoreboard == null) {
            int highestSkillPoints = 0;
            BukkitSkillType highestType = null;
            long now = System.currentTimeMillis();
            for (BukkitPlayerSkill playerSkill : skills.values()) {
                playerSkill.checkLastReward(now);
                int skillPoints = (int)playerSkill.getSkillPoints();
                if (highestSkillPoints < skillPoints) {
                    highestSkillPoints = skillPoints;
                    highestType = playerSkill.getType();
                }
            }
            skillOnScoreboard = highestType;
        } else {
            skillOnScoreboard = forcedSkillOnScoreboard;
        }
        setupScoreboard(player);
    }

    void displaySkill(BukkitSkill skill, Player player)
    {
        if (forcedSkillOnScoreboard != null) return;
        if (skillOnScoreboard == skill.getSkillType()) return;
        for (BukkitPlayerSkill playerSkill : skills.values()) {
            if (playerSkill.getType() != skill.getSkillType()) playerSkill.reset();
        }
        skillOnScoreboard = skill.getSkillType();
        setupScoreboard(player);
    }

    void setupScoreboard(Player player)
    {
        if (!sidebarEnabled) {
            skillOnScoreboard = null;
        } else if (forcedSkillOnScoreboard != null) {
            skillOnScoreboard = forcedSkillOnScoreboard;
        }
        if (skillOnScoreboard == null) {
            player.setScoreboard(Bukkit.getServer().getScoreboardManager().getMainScoreboard());
        } else {
            BukkitPlayerSkill playerSkill = skills.get(skillOnScoreboard);
            BukkitSkill skill = getSkills().skillByType(skillOnScoreboard);
            if (scoreboard == null) {
                scoreboard = Bukkit.getServer().getScoreboardManager().getNewScoreboard();
                sidebarObjective = scoreboard.registerNewObjective("Skills", "dummy");
                sidebarObjective.setDisplaySlot(DisplaySlot.SIDEBAR);
            }
            int skillPoints = (int)getSkills().getScore().getSkillPoints(uuid, skill);
            int skillLevel = getSkills().getScore().getSkillLevel(uuid, skill);
            int pointsInLevel = getSkills().getScore().pointsInLevel(skillPoints);
            int pointsToLevelUp = getSkills().getScore().pointsToLevelUpTo(skillLevel + 1);
            int pointsForNextLevel = getSkills().getScore().pointsForNextLevel(skillPoints);
            sidebarObjective.setDisplayName(BukkitUtil.format("&3&l%s &blvl &f%d", skill.getTitle(), skillLevel));
            String progressBar = BukkitUtil.progressBar(pointsInLevel, pointsToLevelUp);
            if (!progressBar.equals(lastProgressBar)) {
                sidebarObjective.getScore(progressBar).setScore(0);
                if (lastProgressBar != null) scoreboard.resetScores(lastProgressBar);
            }
            lastProgressBar = progressBar;
            sidebarObjective.getScore(BukkitUtil.format("&aFor next level")).setScore(pointsForNextLevel);
            sidebarObjective.getScore(BukkitUtil.format("&9Skill Points")).setScore((int)playerSkill.getSkillPoints());
            sidebarObjective.getScore(BukkitUtil.format("&9Money")).setScore((int)playerSkill.getMoney());
            // sidebarObjective.getScore(BukkitUtil.format("Exp")).setScore((int)playerSkill.getExp());
            player.setScoreboard(scoreboard);
        }
    }

    void setSidebarEnabled(Player player, boolean value)
    {
        if (sidebarEnabled == value) return;
        sidebarEnabled = value;
        if (!value) {
            player.setScoreboard(Bukkit.getServer().getScoreboardManager().getMainScoreboard());
            scoreboard = null;
            sidebarObjective = null;
            forcedSkillOnScoreboard = null;
            skillOnScoreboard = null;
            for (BukkitPlayerSkill playerSkill : skills.values()) playerSkill.reset();
            if (updateTask != null) {
                updateTask.cancel();
                updateTask = null;
            }
        }
    }

    void setForcedSkill(BukkitSkill skill)
    {
        forcedSkillOnScoreboard = skill.getSkillType();
    }
}

@Data
class BukkitPlayerSkill
{
    final BukkitSkillType type;
    double skillPoints, money, exp;
    long lastReward = 0;

    void reset()
    {
        skillPoints = money = exp = 0;
    }

    void checkLastReward(long now)
    {
        if (now - lastReward > 1000 * 20) {
            reset();
        }
    }

    void onReward(Reward reward, double factor)
    {
        long now = System.currentTimeMillis();
        checkLastReward(now);
        lastReward = now;
        double skillPoints = reward.getSkillPoints() * factor;
        double money = reward.getMoney() * factor;
        double exp = reward.getExp() * factor;
        if (skillPoints > 0.01) this.skillPoints += skillPoints;
        if (money > 0.01) this.money += money;
        if (exp > 0.01) this.exp += exp;
    }
}
