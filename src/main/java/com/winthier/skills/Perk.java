package com.winthier.skills;

public enum Perk {
    // Base Smithing Improvements
    SMITH_IMPROVE_LEATHER(SkillType.SMITH, null),
    SMITH_IMPROVE_GOLD(SkillType.SMITH, Perk.SMITH_IMPROVE_LEATHER),
    SMITH_IMPROVE_MAIL(SkillType.SMITH, Perk.SMITH_IMPROVE_GOLD),
    SMITH_IMPROVE_IRON(SkillType.SMITH, Perk.SMITH_IMPROVE_MAIL),
    SMITH_IMPROVE_DIAMOND(SkillType.SMITH, Perk.SMITH_IMPROVE_IRON),
    // Smithing Armor Perks
    SMITH_LEATHER_ARMOR_SPEED(SkillType.SMITH, Perk.SMITH_IMPROVE_LEATHER),
    SMITH_GOLD_ARMOR_HEALTH(SkillType.SMITH, Perk.SMITH_IMPROVE_GOLD),
    SMITH_MAIL_ARMOR_DAMAGE(SkillType.SMITH, Perk.SMITH_IMPROVE_MAIL),
    SMITH_IRON_ARMOR_ARMOR(SkillType.SMITH, Perk.SMITH_IMPROVE_IRON),
    SMITH_IRON_ARMOR_KNOCKBACK_RESIST(SkillType.SMITH, Perk.SMITH_IRON_ARMOR_ARMOR),
    SMITH_DIAMOND_ARMOR_ARMOR(SkillType.SMITH, Perk.SMITH_IMPROVE_DIAMOND),
    SMITH_DIAMOND_ARMOR_TOUGH(SkillType.SMITH, Perk.SMITH_DIAMOND_ARMOR_ARMOR),
    SMITH_SHIELD_ARMOR(SkillType.SMITH, Perk.SMITH_IMPROVE_IRON),
    SMITH_SHIELD_KNOCKBACK_RESIST(SkillType.SMITH, Perk.SMITH_SHIELD_ARMOR),
    SMITH_SWORD_DAMAGE(SkillType.SMITH, Perk.SMITH_IMPROVE_IRON),
    SMITH_AXE_DAMAGE(SkillType.SMITH, Perk.SMITH_IMPROVE_IRON),
    // Brawl Swords
    BRAWL_SWORD_DAMAGE(SkillType.BRAWL, null),
    BRAWL_SWORD_DAMAGE_2(SkillType.BRAWL, null),
    BRAWL_IRON_SWORD_KNOCKOUT(SkillType.BRAWL, null),
    BRAWL_GOLD_SWORD_LIFE_STEAL(SkillType.BRAWL, null),
    BRAWL_DIAMOND_SWORD_BLEED(SkillType.BRAWL, null),
    // Brawl Axes
    BRAWL_AXE_AOE(SkillType.BRAWL, null),
    BRAWL_AXE_KNOCKBACK(SkillType.BRAWL, null),
    BRAWL_IRON_AXE_PARALYSIS(SkillType.BRAWL, null),
    BRAWL_GOLD_AXE_FIRE(SkillType.BRAWL, null),
    BRAWL_DIAMOND_AXE_BLEED(SkillType.BRAWL, null),
    // Brawl Unarmed
    BRAWL_UNARMED_DISARM(SkillType.BRAWL, null),
    BRAWL_UNARMED_UNDRESS(SkillType.BRAWL, null),
    BRAWL_UNARMED_STEAL(SkillType.BRAWL, null),
    // Ranching Base
    RANCH_COW(SkillType.RANCH, null),
    RANCH_MUSHROOM_COW(SkillType.RANCH, null),
    RANCH_PIG(SkillType.RANCH, null),
    RANCH_SHEEP(SkillType.RANCH, null),
    RANCH_CHICKEN(SkillType.RANCH, null),
    RANCH_RABBIT(SkillType.RANCH, null),
    // Ranching Utility
    RANCH_CARRY(SkillType.RANCH, null),
    RANCH_TWINS(SkillType.RANCH, Perk.RANCH_CARRY),
    RANCH_TRIPLETS(SkillType.RANCH, Perk.RANCH_TWINS),
    RANCH_QUADRUPLETS(SkillType.RANCH, Perk.RANCH_TRIPLETS),
    RANCH_INSPECT_BOOK(SkillType.RANCH, Perk.RANCH_CARRY),
    RANCH_INSPECT_FAVORITE(SkillType.RANCH, Perk.RANCH_INSPECT_BOOK),
    RANCH_INSPECT_QUIRK(SkillType.RANCH, Perk.RANCH_INSPECT_FAVORITE),
    // Ranching Specific
    RANCH_FINE_LEATHER(SkillType.RANCH, Perk.RANCH_COW),
    RANCH_PIG_TRUFFLE(SkillType.RANCH, Perk.RANCH_PIG),
    RANCH_SHEEP_RAINBOW(SkillType.RANCH, Perk.RANCH_SHEEP),
    RANCH_CHICKEN_GOLD(SkillType.RANCH, Perk.RANCH_CHICKEN),
    // Brewing
    BREW_STACK(SkillType.BREW, null),
    // Brewing for yourself
    BREW_MOVEMENT_DURATION(SkillType.BREW, Perk.BREW_STACK),
    BREW_RESISTANCE_DURATION(SkillType.BREW, Perk.BREW_MOVEMENT_DURATION),
    BREW_ABILITY_DURATION(SkillType.BREW, Perk.BREW_RESISTANCE_DURATION),
    BREW_RESISTANCE_ABSORPTION(SkillType.BREW, Perk.BREW_ABILITY_DURATION),
    BREW_HEAL_REGEN(SkillType.BREW, Perk.BREW_RESISTANCE_ABSORPTION),
    BREW_REGEN_HEALTH(SkillType.BREW, Perk.BREW_HEAL_REGEN),
    // Brewing to hurt enemies
    BREW_DEBUFF_DURATION(SkillType.BREW, Perk.BREW_STACK),
    BREW_DAMAGE_POISON(SkillType.BREW, Perk.BREW_DEBUFF_DURATION),
    BREW_WEAK_BLIND(SkillType.BREW, Perk.BREW_DAMAGE_POISON),
    BREW_SLOW_WITHER(SkillType.BREW, Perk.BREW_WEAK_BLIND),
    // Cook
    COOK_SATURATION(SkillType.COOK, null),
    COOK_MEAT_RESISTANCE(SkillType.COOK, null),
    COOK_VEGETABLE_HEALTH(SkillType.COOK, null),
    COOK_STARCH_STRENGTH(SkillType.COOK, null),
    COOK_SUGAR_SPEED(SkillType.COOK, null),
    COOK_FISH_ABSORPTION(SkillType.COOK, null),
    // Digging
    DIG_BONE(SkillType.DIG, null),
    DIG_STRING(SkillType.DIG, null),
    DIG_FEATHER(SkillType.DIG, null),
    DIG_GUNPOWDER(SkillType.DIG, null),
    DIG_SEEDS(SkillType.DIG, null),
    DIG_IRON(SkillType.DIG, null),
    DIG_GOLD(SkillType.DIG, null),
    DIG_EMERALD(SkillType.DIG, null),
    DIG_DIAMOND(SkillType.DIG, null),
    DIG_FALLING_STACK(SkillType.DIG, null),
    DIG_RADIUS(SkillType.DIG, null),
    // Enchanting
    ENCHANT_TOP_OPTIONS(SkillType.ENCHANT, null),
    ENCHANT_(SkillType.ENCHANT, null),
    // Fishing
    // Taming
    TAME_FOLLOW_TELEPORT(SkillType.TAME, null),
    TAME_DODGE_ENVIRONMENT_DAMAGE(SkillType.TAME, null),
    TAME_CAT_AGGRO_CREEPER(SkillType.TAME, null),
    TAME_CAT_9_LIVES(SkillType.TAME, null),
    TAME_DOG_ATTACK_DAMAGE(SkillType.TAME, null),
    TAME_DOG_MOVEMENT_SPEED(SkillType.TAME, null),
    TAME_DOG_HEALTH(SkillType.TAME, null),
    TAME_DOG_REVIVE(SkillType.TAME, null),
    TAME_DOG_DEATH_HEALS(SkillType.TAME, null),
    TAME_DOG_SACRIFICE(SkillType.TAME, null);

    final SkillType skillType;
    final Perk depends;
    final String key;

    Perk(SkillType skillType, Perk depends) {
        this.skillType = skillType;
        this.depends = depends;
        this.key = name().toLowerCase();
    }
}
