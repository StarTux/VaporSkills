package com.winthier.skills.bukkit;

import com.avaje.ebean.EbeanServer;
import com.winthier.skills.Skill;
import com.winthier.skills.Skills;
import java.util.UUID;
import lombok.Getter;

public class BukkitSkills extends Skills
{
    @Getter static BukkitSkills instance;

    BukkitSkills()
    {
	instance = this;
    }

    BukkitSkillsPlugin getPlugin()
    {
	return BukkitSkillsPlugin.instance;
    }

    BukkitSkills getSkills()
    {
	return BukkitSkills.instance;
    }

    @Override
    public EbeanServer getDatabase()
    {
	return getPlugin().getDatabase();
    }

    @Override
    public void onLevelUp(UUID player, Skill skill, int level)
    {
	// TODO: Call an event or whatever
    }
}
