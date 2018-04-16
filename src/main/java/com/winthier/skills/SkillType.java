package com.winthier.skills;

public enum SkillType {
    // Combat Skills
    BRAWL(Category.COMBAT),
    HUNT(Category.COMBAT),
    TAME(Category.COMBAT),
    // Craft Skills
    BREW(Category.CRAFT),
    COOK(Category.CRAFT),
    ENCHANT(Category.CRAFT),
    SMITH(Category.CRAFT),
    // Farming Skills
    BREED(Category.FARM),
    FISH(Category.FARM),
    GARDEN(Category.FARM),
    // Mining Skills
    DIG(Category.MINE),
    MINE(Category.MINE),
    WOODCUT(Category.MINE);

    public enum Category {
        CRAFT, COMBAT, FARM, MINE;
    }

    public final Category category;

    SkillType(Category category) {
        this.category = category;
    }
}
