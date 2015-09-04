package com.winthier.skills;

import com.winthier.skills.sql.SQLReward;
import com.winthier.skills.sql.SQLScore;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Score
{
    final Map<String, Highscore> highscores = new HashMap<>();
    
    public void giveSkillPoints(UUID player, Skill skill, double points)
    {
	if (points <= 0) return;
	SQLScore row = SQLScore.of(player, skill.getKey());
	double skillPoints = row.getSkillPoints();
	int skillLevel = row.getSkillLevel();
	// Calculate new
	double newSkillPoints = skillPoints + points;
	int newSkillLevel = levelForPoints(newSkillPoints);
	// Call hook(s)
	if (newSkillLevel > skillLevel) {
	    Skills.getInstance().onLevelUp(player, skill, newSkillLevel);
	}
	// "Write" data
	row.setSkillPoints((float)newSkillPoints);
	row.setSkillLevel(newSkillLevel);
	row.setDirty();
    }

    public double getSkillPoints(UUID player, Skill skill)
    {
	return SQLScore.of(player, skill.getKey()).getSkillPoints();
    }

    public int getSkillLevel(UUID player, Skill skill)
    {
	return SQLScore.of(player, skill.getKey()).getSkillLevel();
    }

    public int levelForPoints(double skillPointsDouble)
    {
        int skillPoints = (int)skillPointsDouble;
	int level = 0;
	while (pointsForLevel(level + 1) <= skillPoints) level += 1;
	return level;
    }

    public int pointsToLevelUpTo(int i)
    {
        if (i <= 0) return 0;
        return i * 10;
    }

    public int pointsForLevel(int skillLevel)
    {
	int points = 0;
	for (int i = 1; i <= skillLevel; ++i) {
	    points += pointsToLevelUpTo(i);
	}
	return points;
    }

    public int pointsInLevel(int skillPoints)
    {
        int level = levelForPoints(skillPoints);
        return skillPoints - pointsForLevel(level);
    }

    public int pointsForNextLevel(int skillPoints)
    {
        int level = levelForPoints(skillPoints);
        return pointsForLevel(level + 1) - skillPoints;
    }

    private Reward rewardForTypeAndData(Skill skill, SQLReward.Target target, int type, int data)
    {
        Reward result = SQLReward.find(skill.getKey(), target, type, data, null);
        if (result == null) {
            result = SQLReward.find(skill.getKey(), target, type, null, null);
        }
        return result;
    }

    public Reward rewardForBlock(Skill skill, int blockType, int blockData)
    {
        return rewardForTypeAndData(skill, SQLReward.Target.BLOCK, blockType, blockData);
    }

    public Reward rewardForItem(Skill skill, int blockType, int blockData)
    {
        return rewardForTypeAndData(skill, SQLReward.Target.ITEM, blockType, blockData);
    }

    public Reward rewardForBlockNamed(Skill skill, int blockType, int blockData, String name)
    {
        return SQLReward.find(skill.getKey(), SQLReward.Target.BLOCK, blockType, blockData, name);
    }
    
    public Reward rewardForEntity(Skill skill, String entityType)
    {
        return SQLReward.find(skill.getKey(), SQLReward.Target.ENTITY, null, null, entityType);
    }

    public Reward rewardForPotionEffect(Skill skill, int effectType, int effectAmplifier)
    {
        return rewardForTypeAndData(skill, SQLReward.Target.POTION_EFFECT, effectType, effectAmplifier);
    }

    public Reward rewardForEnchantment(Skill skill, int enchantType, int enchantLevel)
    {
        return rewardForTypeAndData(skill, SQLReward.Target.ENCHANTMENT, enchantType, enchantLevel);
    }

    public Reward rewardForName(Skill skill, String name)
    {
        return SQLReward.find(skill.getKey(), SQLReward.Target.NAME, null, null, name);
    }

    public Reward rewardForName(Skill skill, String name, int data)
    {
        return SQLReward.find(skill.getKey(), SQLReward.Target.NAME, null, data, name);
    }
    
    public Highscore getHighscore(Skill skill)
    {
        Highscore result = highscores.get(skill.getKey());
        if (result == null || result.ageInSeconds() > 60) {
            result = Highscore.create(skill);
            highscores.put(skill.getKey(), result);
        }
        return result;
    }
}
