package com.winthier.skills;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@RequiredArgsConstructor
public final class Score {
    private final SkillsPlugin plugin;
    final Map<SkillType, Highscore> highscores = new HashMap<>();
    final Map<UUID, Map<SkillType, SQLScore>> scores = new HashMap<>();
    final Map<UUID, Set<Perk>> perks = new HashMap<>();
    @Getter @Setter private Map<Reward.Key, Reward> rewards = new HashMap<>();
    final List<SQLScore> dirtyScores = new ArrayList<>();

    // Score getters

    public double getSkillPoints(UUID player, SkillType skill) {
        return getScore(player, skill).getSkillPoints();
    }

    public int getSkillLevel(UUID player, SkillType skill) {
        return getScore(player, skill).getSkillLevel();
    }

    // Score setters

    public void giveSkillPoints(UUID player, SkillType skill, double points) {
        if (points <= 0.01) return;
        SQLScore score = getScore(player, skill);
        double skillPoints = score.getSkillPoints();
        int skillLevel = score.getSkillLevel();
        // Calculate new
        double newSkillPoints = skillPoints + points;
        int newSkillLevel = levelForPoints(newSkillPoints);
        score.setSkillPoints(newSkillPoints);
        // Call hook(s)
        if (newSkillLevel > skillLevel) {
            score.setSkillLevel(newSkillLevel);
            // Save instantly
            plugin.getDb().save(score);
            dirtyScores.remove(score);
            // Flush highscores
            highscores.remove(skill);
            if (skill != SkillType.TOTAL) giveSkillPoints(player, skill, points);
            plugin.onLevelUp(player, skill, newSkillLevel);
        } else {
            if (!dirtyScores.contains(score)) dirtyScores.add(score);
        }
    }

    public void setSkillLevel(UUID player, SkillType skill, int skillLevel) {
        if (skillLevel < 0) throw new IllegalArgumentException("Skill level cannot be less than 0");
        SQLScore score = getScore(player, skill);
        int skillPoints = pointsForLevel(skillLevel);
        score.setSkillPoints(skillPoints);
        score.setSkillLevel(skillLevel);
        plugin.getDb().save(score);
        dirtyScores.remove(score);
    }

    // Score: points-level conversion

    /**
     * This is the base of all points-level calculations.
     */
    public static int pointsToLevelUpTo(int i) {
        if (i <= 0) return 0;
        return i * 10;
    }

    public static int pointsForLevel(int skillLevel) {
        int points = 0;
        for (int i = 1; i <= skillLevel; ++i) {
            points += pointsToLevelUpTo(i);
        }
        return points;
    }

    public static int levelForPoints(double skillPointsDouble) {
        int skillPoints = (int)skillPointsDouble;
        int level = 0;
        while (pointsForLevel(level + 1) <= skillPoints) level += 1;
        return level;
    }

    public static int pointsInLevel(int skillPoints) {
        int level = levelForPoints(skillPoints);
        return skillPoints - pointsForLevel(level);
    }

    public static int pointsForNextLevel(int skillPoints) {
        int level = levelForPoints(skillPoints);
        return pointsForLevel(level + 1) - skillPoints;
    }

    // Other getters

    public Highscore getHighscore(SkillType skill) {
        Highscore result = highscores.get(skill);
        if (result == null) {
            result = Highscore.create(plugin, skill);
            highscores.put(skill, result);
        }
        return result;
    }

    public Set<Perk> getPerks(UUID player) {
        return EnumSet.allOf(Perk.class);
        // Set<Perk> result = perks.get(player);
        // if (result == null) {
        //     result = EnumSet.noneOf(Perk.class);
        //     for (SQLPerk sqlPerk: plugin.getDb().find(SQLPerk.class).where().eq("player", player).findList()) {
        //         try {
        //             Perk perk = Perk.valueOf(sqlPerk.getPerk().toUpperCase());
        //             result.add(perk);
        //         } catch (IllegalArgumentException iae) { }
        //     }
        //     perks.put(player, result);
        // }
        // return result;
    }

    public boolean hasPerk(UUID player, Perk perk) {
        //        return getPerks(player).contains(perk);
        return true;
    }

    public boolean unlockPerk(UUID player, Perk perk) {
        Set<Perk> playerPerks = getPerks(player);
        if (playerPerks.contains(perk)) return false;
        playerPerks.add(perk);
        plugin.getDb().saveIgnore(new SQLPerk(player, perk.key));
        return true;
    }

    // Internal score database getters

    private Map<SkillType, SQLScore> getScore(UUID player) {
        Map<SkillType, SQLScore> result = scores.get(player);
        if (result == null) {
            result = new EnumMap<>(SkillType.class);
            scores.put(player, result);
            for (SQLScore score: plugin.getDb().find(SQLScore.class).where().eq("player", player).findList()) {
                try {
                    SkillType skillType = SkillType.valueOf(score.getSkill().toUpperCase());
                    result.put(skillType, score);
                } catch (IllegalArgumentException iae) { }
            }
            for (SkillType skillType: SkillType.values()) {
                if (!result.containsKey(skillType)) {
                    SQLScore score = new SQLScore(player, skillType.key);
                    plugin.getDb().save(score);
                    result.put(skillType, score);
                }
            }
        }
        return result;
    }

    private SQLScore getScore(UUID player, SkillType skill) {
        return getScore(player).get(skill);
    }

    void saveOneDirtyRow() {
        if (!dirtyScores.isEmpty()) {
            SQLScore score = dirtyScores.remove(0);
            plugin.getDb().save(score);
        }
    }

    void removePlayer(UUID player) {
        scores.remove(player);
        perks.remove(player);
        Iterator<SQLScore> iter = dirtyScores.iterator();
        while (iter.hasNext()) {
            SQLScore score = iter.next();
            if (score.getPlayer().equals(player)) {
                plugin.getDb().save(score);
                iter.remove();
            }
        }
    }

    void clear() {
        highscores.clear();
        perks.clear();
        rewards.clear();
        scores.clear();
        dirtyScores.clear();
    }
}
