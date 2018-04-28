package com.winthier.skills;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@RequiredArgsConstructor
public final class Score {
    private final SkillsPlugin plugin;
    private final Map<SkillType, Highscore> highscores = new HashMap<>();
    private final Map<UUID, Map<SkillType, SQLScore>> scores = new HashMap<>();
    private final Map<UUID, Set<Perk>> perks = new HashMap<>();
    @Getter @Setter private Map<Reward.Key, Reward> rewards = new HashMap<>();

    public void giveSkillPoints(UUID player, SkillType skill, double points) {
        if (points <= 0) return;
        SQLScore score = getScore(player, skill);
        double skillPoints = score.getSkillPoints();
        int skillLevel = score.getSkillLevel();
        // Calculate new
        double newSkillPoints = skillPoints + points;
        int newSkillLevel = levelForPoints(newSkillPoints);
        // Call hook(s)
        if (newSkillLevel > skillLevel) {
            plugin.onLevelUp(player, skill, newSkillLevel);
        }
        // "Write" data
        score.setSkillPoints(newSkillPoints);
        score.setSkillLevel(newSkillLevel);
        plugin.getDb().save(score);
    }

    public void setSkillLevel(UUID player, SkillType skill, int skillLevel) {
        if (skillLevel < 0) throw new IllegalArgumentException("Skill level cannot be less than 0");
        SQLScore score = getScore(player, skill);
        int skillPoints = pointsForLevel(skillLevel);
        score.setSkillPoints(skillPoints);
        score.setSkillLevel(skillLevel);
        plugin.getDb().save(score);
    }

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

    public double getSkillPoints(UUID player, SkillType skill) {
        return getScore(player, skill).getSkillPoints();
    }

    public int getSkillLevel(UUID player, SkillType skill) {
        return getScore(player, skill).getSkillLevel();
    }

    public static int levelForPoints(double skillPointsDouble) {
        int skillPoints = (int)skillPointsDouble;
        int level = 0;
        while (pointsForLevel(level + 1) <= skillPoints) level += 1;
        return level;
    }

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

    public static int pointsInLevel(int skillPoints) {
        int level = levelForPoints(skillPoints);
        return skillPoints - pointsForLevel(level);
    }

    public static int pointsForNextLevel(int skillPoints) {
        int level = levelForPoints(skillPoints);
        return pointsForLevel(level + 1) - skillPoints;
    }

    private Reward rewardForTypeAndData(SkillType skill, Reward.Target target, int type, int data) {
        return rewards.get(new Reward.Key(skill, target, type, data, null));
    }

    public Reward rewardForBlock(SkillType skill, int blockType, int blockData) {
        return rewardForTypeAndData(skill, Reward.Target.BLOCK, blockType, blockData);
    }

    public Reward rewardForItem(SkillType skill, int blockType, int blockData) {
        return rewardForTypeAndData(skill, Reward.Target.ITEM, blockType, blockData);
    }

    public Reward rewardForBlockNamed(SkillType skill, int blockType, int blockData, String name) {
        return rewards.get(new Reward.Key(skill, Reward.Target.BLOCK, blockType, blockData, name));
    }

    public Reward rewardForEntity(SkillType skill, String entityType) {
        return rewards.get(new Reward.Key(skill, Reward.Target.ENTITY, null, null, entityType));
    }

    public Reward rewardForPotionEffect(SkillType skill, int effectType, int effectAmplifier) {
        return rewards.get(new Reward.Key(skill, Reward.Target.POTION_EFFECT, effectType, effectAmplifier, null));
    }

    public Reward rewardForEnchantment(SkillType skill, int enchantType, int enchantLevel) {
        return rewardForTypeAndData(skill, Reward.Target.ENCHANTMENT, enchantType, enchantLevel);
    }

    public Reward rewardForName(SkillType skill, String name) {
        return rewards.get(new Reward.Key(skill, Reward.Target.NAME, null, null, name));
    }

    public Reward rewardForName(SkillType skill, String name, int data) {
        return rewards.get(new Reward.Key(skill, Reward.Target.NAME, null, data, name));
    }

    public Highscore getHighscore(SkillType skill) {
        Highscore result = highscores.get(skill);
        if (result == null || result.ageInSeconds() > 60) {
            result = Highscore.create(plugin, skill);
            highscores.put(skill, result);
        }
        return result;
    }

    Set<Perk> getPerks(UUID player) {
        Set<Perk> result = perks.get(player);
        if (result == null) {
            result = EnumSet.noneOf(Perk.class);
            for (SQLPerk sqlPerk: plugin.getDb().find(SQLPerk.class).where().eq("player", player).findList()) {
                try {
                    Perk perk = Perk.valueOf(sqlPerk.getPerk().toUpperCase());
                    result.add(perk);
                } catch (IllegalArgumentException iae) { }
            }
            perks.put(player, result);
        }
        return result;
    }

    boolean hasPerk(UUID player, Perk perk) {
        //        return getPerks(player).contains(perk);
        return true;
    }

    boolean unlockPerk(UUID player, Perk perk) {
        Set<Perk> playerPerks = getPerks(player);
        if (playerPerks.contains(perk)) return false;
        playerPerks.add(perk);
        plugin.getDb().saveIgnore(new SQLPerk(player, perk.key));
        return true;
    }

    void clear() {
        highscores.clear();
        perks.clear();
        rewards.clear();
    }
}
