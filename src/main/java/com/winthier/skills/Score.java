package com.winthier.skills;

import com.winthier.skills.sql.SQLPlayerScore;
import java.util.UUID;

public class Score
{
    public void giveSkillPoints(UUID player, Skill skill, int points)
    {
	if (points <= 0) return;
	SQLPlayerScore row = SQLPlayerScore.of(player, skill.getKey());
	int skillPoints = row.getSkillPoints();
	int skillLevel = row.getSkillLevel();
	// Calculate new
	int newSkillPoints = skillPoints + points;
	int newSkillLevel = levelForPoints(newSkillPoints);
	// Call hook(s)
	if (newSkillLevel > skillLevel) {
	    Skills.getInstance().onLevelUp(player, skill, newSkillLevel);
	}
	// "Write" data
	row.setSkillPoints(newSkillPoints);
	row.setSkillLevel(newSkillLevel);
	row.setDirty();
    }

    public int getSkillPoints(UUID player, Skill skill)
    {
	return SQLPlayerScore.of(player, skill.getKey()).getSkillPoints();
    }

    public int getSkillLevel(UUID player, Skill skill)
    {
	return SQLPlayerScore.of(player, skill.getKey()).getSkillLevel();
    }

    public int levelForPoints(int skillPoints)
    {
	int level = 0;
	while (pointsForLevel(level) < skillPoints) level += 1;
	return level;
    }

    public int pointsForLevel(int skillLevel)
    {
	int points = 0;
	for (int i = 1; i <= skillLevel; ++i) {
	    points += i;
	}
	return points * 10;
    }
}
