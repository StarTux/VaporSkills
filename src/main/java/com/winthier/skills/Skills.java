package com.winthier.skills;

import java.util.Collection;
import java.util.UUID;
import lombok.Getter;

public abstract class Skills
{
    @Getter static Skills instance;
    @Getter final Score score;

    protected Skills()
    {
        instance = this;
	score = new Score();
    }
    
    public abstract void onLevelUp(UUID player, Skill skill, int level);
    public abstract Collection<? extends Skill> getSkills();
}
