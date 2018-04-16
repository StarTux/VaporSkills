package com.winthier.skills;

import com.winthier.skills.sql.SQLLog;
import com.winthier.skills.sql.SQLPerk;
import com.winthier.skills.sql.SQLReward;
import com.winthier.skills.sql.SQLScore;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class Score {
    private final Map<String, Highscore> highscores = new HashMap<>();
    private Highscore totalHighscore = null;
    private final Map <UUID, Set<Perk>> perks = new HashMap<>();

    public void giveSkillPoints(UUID player, SkillType skill, double points) {
        if (points <= 0) return;
        SQLScore row = SQLScore.of(player, skill);
        double skillPoints = row.getSkillPoints();
        int skillLevel = row.getSkillLevel();
        // Calculate new
        double newSkillPoints = skillPoints + points;
        int newSkillLevel = levelForPoints(newSkillPoints);
        // Call hook(s)
        if (newSkillLevel > skillLevel) {
            SkillsPlugin.getInstance().onLevelUp(player, skill, newSkillLevel);
        }
        // "Write" data
        row.setSkillPoints((float)newSkillPoints);
        row.setSkillLevel(newSkillLevel);
        row.setDirty((float)points);
    }

    public void setSkillLevel(UUID player, SkillType skill, int skillLevel) {
        if (skillLevel < 0) throw new IllegalArgumentException("Skill level cannot be less than 0");
        SQLScore row = SQLScore.of(player, skill);
        int skillPoints = pointsForLevel(skillLevel);
        row.setSkillPoints((float)skillPoints);
        row.setSkillLevel(skillLevel);
        row.setDirty();
    }

    public double getSkillPoints(UUID player, SkillType skill) {
        return SQLScore.of(player, skill).getSkillPoints();
    }

    public int getSkillLevel(UUID player, SkillType skill) {
        return SQLScore.of(player, skill).getSkillLevel();
    }

    public int levelForPoints(double skillPointsDouble) {
        int skillPoints = (int)skillPointsDouble;
        int level = 0;
        while (pointsForLevel(level + 1) <= skillPoints) level += 1;
        return level;
    }

    public int pointsToLevelUpTo(int i) {
        if (i <= 0) return 0;
        return i * 10;
    }

    public int pointsForLevel(int skillLevel) {
        int points = 0;
        for (int i = 1; i <= skillLevel; ++i) {
            points += pointsToLevelUpTo(i);
        }
        return points;
    }

    public int pointsInLevel(int skillPoints) {
        int level = levelForPoints(skillPoints);
        return skillPoints - pointsForLevel(level);
    }

    public int pointsForNextLevel(int skillPoints) {
        int level = levelForPoints(skillPoints);
        return pointsForLevel(level + 1) - skillPoints;
    }

    private Reward rewardForTypeAndData(SkillType skill, SQLReward.Target target, int type, int data) {
        Reward result = SQLReward.find(skill, target, type, data, null);
        if (result == null) {
            result = SQLReward.find(skill, target, type, null, null);
        }
        return result;
    }

    public Reward rewardForBlock(SkillType skill, int blockType, int blockData) {
        return rewardForTypeAndData(skill, SQLReward.Target.BLOCK, blockType, blockData);
    }

    public Reward rewardForItem(SkillType skill, int blockType, int blockData) {
        return rewardForTypeAndData(skill, SQLReward.Target.ITEM, blockType, blockData);
    }

    public Reward rewardForBlockNamed(SkillType skill, int blockType, int blockData, String name) {
        return SQLReward.find(skill, SQLReward.Target.BLOCK, blockType, blockData, name);
    }

    public Reward rewardForEntity(SkillType skill, String entityType) {
        return SQLReward.find(skill, SQLReward.Target.ENTITY, null, null, entityType);
    }

    public Reward rewardForPotionEffect(SkillType skill, int effectType, int effectAmplifier) {
        return rewardForTypeAndData(skill, SQLReward.Target.POTION_EFFECT, effectType, effectAmplifier);
    }

    public Reward rewardForEnchantment(SkillType skill, int enchantType, int enchantLevel) {
        return rewardForTypeAndData(skill, SQLReward.Target.ENCHANTMENT, enchantType, enchantLevel);
    }

    public Reward rewardForName(SkillType skill, String name) {
        return SQLReward.find(skill, SQLReward.Target.NAME, null, null, name);
    }

    public Reward rewardForName(SkillType skill, String name, int data) {
        return SQLReward.find(skill, SQLReward.Target.NAME, null, data, name);
    }

    public Reward rewardForNameAndMaximum(SkillType skill, String name, int dataMax) {
        Reward result = null;
        int data = 0;
        for (SQLReward reward : SQLReward.findList(skill, SQLReward.Target.NAME, null, null, name)) {
            if (reward.getData() > data && reward.getData() <= dataMax) {
                result = reward;
                data = reward.getData();
            }
        }
        return result;
    }

    public void logReward(Reward reward, UUID player, Reward outcome) {
        if (!(reward instanceof SQLReward)) return;
        SQLLog.log((SQLReward)reward, player, outcome);
    }

    public Highscore getHighscore(SkillType skill) {
        if (skill == null) {
            Highscore result = totalHighscore;
            if (result == null || result.ageInSeconds() > 60 * 10) {
                result = Highscore.create(null);
                totalHighscore = result;
            }
            return result;
        } else {
            Highscore result = highscores.get(skill);
            if (result == null || result.ageInSeconds() > 60) {
                result = Highscore.create(skill);
                highscores.put(skill.key, result);
            }
            return result;
        }
    }

    Set<Perk> getPerks(UUID player) {
        Set<Perk> result = perks.get(player);
        if (result == null) {
            result = EnumSet.noneOf(Perk.class);
            for (SQLPerk sqlPerk: SQLPerk.find(player)) {
                try {
                    Perk perk = Perk.valueOf(sqlPerk.getPerk().toUpperCase());
                    result.add(perk);
                } catch (IllegalArgumentException iae) {}
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
        Set<Perk> perks = getPerks(player);
        if (perks.contains(perk)) return false;
        perks.add(perk);
        SQLPerk.unlock(player, perk.name().toLowerCase());
        return true;
    }

    void clear() {
        highscores.clear();
        totalHighscore = null;
        perks.clear();
    }
}
