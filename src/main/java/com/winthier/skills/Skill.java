package com.winthier.skills;

public interface Skill
{
    /**
     * A persistent key used for storage to refer to this
     * particular skill.
     */
    String getKey();

    /**
     * A user-friendly, capitalized title, meant for display.
     */
    String getDisplayName();

    /**
     * Same as display name, but shorter, meant for condensed
     * lists.
     */
    String getShorthand();

    /**
     * One or more lines describing this still, intended to inform
     * the user.
     */
    String getDescription();
}
