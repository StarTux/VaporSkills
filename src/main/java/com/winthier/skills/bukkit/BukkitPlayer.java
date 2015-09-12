package com.winthier.skills.bukkit;

import com.winthier.skills.Reward;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
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
    @Data class BukkitPlayerSkill {
        final BukkitSkillType type;
        int count;
        double skillPoints, money, exp;
        long lastReward = 0;
        void reset() {
            count = 0;
            skillPoints = money = exp = 0;
        }
        void checkLastReward(long now) {
            if (now - lastReward > 1000 * 20) {
                reset();
            }
        }
        void onReward(double skillPoints, double money, double exp) {
            long now = System.currentTimeMillis();
            if (forcedSkillOnScoreboard != type) checkLastReward(now);
            lastReward = now;
            count += 1;
            if (skillPoints > 0.01) this.skillPoints += skillPoints;
            if (money > 0.01) this.money += money;
            if (exp > 0.01) this.exp += exp;
        }
    }

    final UUID uuid;
    // Scoreboard
    Scoreboard scoreboard = null;
    Objective sidebarObjective = null;
    String lastProgressBar = null;
    BukkitSkillType skillOnScoreboard = null;
    BukkitSkillType forcedSkillOnScoreboard = null;
    boolean sidebarEnabled = true;
    final Map<Scoreboard, Boolean> scoreboardCache = new WeakHashMap<>();
    // Skill points short-term memory
    final Map<BukkitSkillType, BukkitPlayerSkill> skills = new EnumMap<>(BukkitSkillType.class);
    // Instant feedback task
    BukkitRunnable updateTask;
    boolean shown = false;

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

    void onReward(BukkitSkill skill, Reward reward)
    {
        if (!sidebarEnabled) return;
        skills.get(skill.getSkillType()).onReward(reward.getSkillPoints(), reward.getMoney(), reward.getExp());
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
            int highestCount = 0;
            BukkitSkillType highestType = null;
            long now = System.currentTimeMillis();
            for (BukkitPlayerSkill playerSkill : skills.values()) {
                playerSkill.checkLastReward(now);
                if (highestCount < playerSkill.getCount()) {
                    highestCount = playerSkill.getCount();
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
            if (scoreboardCache.containsKey(player.getScoreboard())) {
                player.setScoreboard(Bukkit.getServer().getScoreboardManager().getMainScoreboard());
            }
        } else {
            BukkitPlayerSkill playerSkill = skills.get(skillOnScoreboard);
            BukkitSkill skill = getSkills().skillByType(skillOnScoreboard);
            if (scoreboard == null) {
                scoreboard = Bukkit.getServer().getScoreboardManager().getNewScoreboard();
                scoreboardCache.put(scoreboard, true);
                sidebarObjective = scoreboard.registerNewObjective("Skills", "dummy");
                sidebarObjective.setDisplaySlot(DisplaySlot.SIDEBAR);
            }
            int skillPoints = (int)getSkills().getScore().getSkillPoints(uuid, skill);
            int skillLevel = getSkills().getScore().getSkillLevel(uuid, skill);
            int pointsInLevel = getSkills().getScore().pointsInLevel(skillPoints);
            int pointsToLevelUp = getSkills().getScore().pointsToLevelUpTo(skillLevel + 1);
            int pointsForNextLevel = getSkills().getScore().pointsForNextLevel(skillPoints);
            sidebarObjective.setDisplayName(BukkitUtil.format("&3&l%s &blvl &f%d", skill.getDisplayName(), skillLevel));
            String progressBar = BukkitUtil.progressBar(pointsInLevel, pointsToLevelUp);
            if (!progressBar.equals(lastProgressBar)) {
                sidebarObjective.getScore(progressBar).setScore(0);
                if (lastProgressBar != null) scoreboard.resetScores(lastProgressBar);
            }
            lastProgressBar = progressBar;
            sidebarObjective.getScore(BukkitUtil.format("&aFor next level")).setScore(pointsForNextLevel);
            sidebarObjective.getScore(BukkitUtil.format("&9Skill Points")).setScore((int)playerSkill.getSkillPoints());
            sidebarObjective.getScore(BukkitUtil.format("&9Money")).setScore((int)playerSkill.getMoney());
            player.setScoreboard(scoreboard);
            if (!shown) {
                shown = true;
                BukkitUtil.raw(player,
                               BukkitUtil.format("You collect skill points. Click "),
                               BukkitUtil.button("&3[&rSk&3]",
                                                 "/sk",
                                                 "Command: &3/sk",
                                                 "&7Skills overview"),
                               BukkitUtil.format("&r or "),
                               BukkitUtil.button("&3[&rHi&3]",
                                                 "/hi",
                                                 "Command: &3/hi",
                                                 "&7Skills highscore"),
                               BukkitUtil.format("&r for more info."));
            }
        }
    }

    void setSidebarEnabled(Player player, boolean value)
    {
        if (sidebarEnabled == value) return;
        sidebarEnabled = value;
        if (!value) {
            if (scoreboardCache.containsKey(player.getScoreboard())) {
                player.setScoreboard(Bukkit.getServer().getScoreboardManager().getMainScoreboard());
            }
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
