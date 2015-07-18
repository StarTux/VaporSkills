package com.winthier.skills;

import com.avaje.ebean.EbeanServer;
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
    
    public abstract EbeanServer getDatabase();
    public abstract void onLevelUp(UUID player, Skill skill, int level);
}
