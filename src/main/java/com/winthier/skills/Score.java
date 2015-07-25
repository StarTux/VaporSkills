package com.winthier.skills;

import com.winthier.skills.sql.SQLScore;
import com.winthier.skills.sql.SQLReward;
import java.util.UUID;

public class Score
{
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
	while (pointsForLevel(level) < skillPoints) level += 1;
	return level;
    }

    public double pointsForLevel(int skillLevel)
    {
	int points = 0;
	for (int i = 1; i <= skillLevel; ++i) {
	    points += i;
	}
	return (double)(points * 10);
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
}
