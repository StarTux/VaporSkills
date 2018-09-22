package com.winthier.skills;

import java.util.HashMap;
import java.util.Map;

public enum SkillType {
    // Combat Skills
    BRAWL(Category.COMBAT),
    //HUNT(Category.COMBAT),
    TAME(Category.COMBAT),
    // Craft Skills
    //BREW(Category.CRAFT),
    //COOK(Category.CRAFT),
    //ENCHANT(Category.CRAFT),
    SMITH(Category.CRAFT),
    // Farming Skills
    RANCH(Category.FARM);
    //FISH(Category.FARM),
    //GARDEN(Category.FARM),
    // Mining Skills
    //DIG(Category.MINE),
    //MINE(Category.MINE),
    //WOODCUT(Category.MINE);

    private static final Map<String, SkillType> keyMap = new HashMap<>();

    public enum Category {
        CRAFT, COMBAT, FARM, MINE;
    }

    public final Category category;
    public final String key;

    SkillType(Category category) {
        this.category = category;
        this.key = name().toLowerCase();
    }

    public static SkillType of(String k) {
        return keyMap.get(k);
    }

    static {
        for (SkillType skillType: SkillType.values()) {
            keyMap.put(skillType.key, skillType);
        }
    }
}
