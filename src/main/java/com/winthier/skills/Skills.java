package com.winthier.skills;

import com.avaje.ebean.EbeanServer;
import lombok.Getter;

public abstract class Skills
{
    @Getter static Skills instance;

    protected Skills()
    {
        instance = this;
    }
    
    public abstract EbeanServer getDatabase();
}
