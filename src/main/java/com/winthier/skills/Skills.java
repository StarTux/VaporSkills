package com.winthier.skills;

import com.avaje.ebean.EbeanServer;
import lombok.Getter;

public abstract class Skills
{
    @Getter
    static Skills instance;

    protected Skills()
    {
        instance = this;
    }
    
    public abstract EbeanServer getDatabase();

    String gerund(SkillType skillType) {
        switch (skillType) {
        case ARCHER   : return "archery";
        case BRAWL    : return "brawling";
        case BREED    : return "breeding";
        case BREW     : return "brewing";
        case BUILD    : return "building";
        case BUTCHER  : return "butchering";
        case COOK     : return "cooking";
        case DIG      : return "digging";
        case EAT      : return "eating";
        case ENCHANT  : return "enchanting";
        case EXPLORE  : return "exploration";
        case FISH     : return "fishing";
        case GARDEN   : return "gardening";
        case HARVEST  : return "harvesting";
        case MINE     : return "mining";
        case SACRIFICE: return "sacrifice";
        case SMELT    : return "smelting";
        case TRAVEL   : return "traveling";
        }
        throw new IllegalArgumentException("Missing Skill Type: " + skillType.name());
    }
}
