package com.winthier.skills;

public enum Perk {
    // Base Smithing Improvements
    SMITH_LEATHER(SkillType.SMITH, null),
    SMITH_GOLD(SkillType.SMITH, Perk.SMITH_LEATHER),
    SMITH_MAIL(SkillType.SMITH, Perk.SMITH_GOLD),
    SMITH_IRON(SkillType.SMITH, Perk.SMITH_MAIL),
    SMITH_DIAMOND(SkillType.SMITH, Perk.SMITH_IRON),
    SMITH_UNBREAKABLE(SkillType.SMITH, Perk.SMITH_DIAMOND),
    // Smithing Armor Perks
    SMITH_LEATHER_ARMOR_SPEED(SkillType.SMITH, Perk.SMITH_LEATHER),
    SMITH_GOLD_ARMOR_HEALTH(SkillType.SMITH, Perk.SMITH_GOLD),
    SMITH_MAIL_ARMOR_DAMAGE(SkillType.SMITH, Perk.SMITH_MAIL),
    SMITH_IRON_ARMOR_ARMOR(SkillType.SMITH, Perk.SMITH_IRON),
    SMITH_DIAMOND_ARMOR_TOUGH(SkillType.SMITH, Perk.SMITH_DIAMOND),
    SMITH_SHIELD_KNOCKBACK_RESIST(SkillType.SMITH, Perk.SMITH_IRON),
    // Smithing Sword Perks
    SMITH_IRON_SWORD_DAMAGE(SkillType.SMITH, Perk.SMITH_IRON),
    SMITH_GOLD_SWORD_ATTACK_SPEED(SkillType.SMITH, Perk.SMITH_GOLD),
    SMITH_DIAMOND_SWORD_SPEED(SkillType.SMITH, Perk.SMITH_GOLD),
    // Smithing Axe Perks
    SMITH_IRON_AXE_KNOCKBACK_RESIST(SkillType.SMITH, Perk.SMITH_IRON),
    SMITH_GOLD_AXE_ATTACK_SPEED(SkillType.SMITH, Perk.SMITH_GOLD),
    SMITH_DIAMOND_AXE_DAMAGE(SkillType.SMITH, Perk.SMITH_DIAMOND),
    // Brawl Swords
    BRAWL_CHARGE(SkillType.BRAWL, null),

    BRAWL_SWORD_DIAMOND_PIERCE(SkillType.BRAWL, Perk.BRAWL_CHARGE),
    BRAWL_SWORD_DIAMOND_DASH(SkillType.BRAWL, Perk.BRAWL_SWORD_DIAMOND_PIERCE),
    //    BRAWL_SWORD_DIAMOND_STUN(SkillType.BRAWL, Perk.BRAWL_SWORD_DIAMOND_DASH),

    BRAWL_SWORD_IRON_SLASH(SkillType.BRAWL, Perk.BRAWL_CHARGE),
    BRAWL_SWORD_IRON_SPIN(SkillType.BRAWL, Perk.BRAWL_SWORD_IRON_SLASH),
    // BRAWL_SWORD_IRON_KNOCKBACK(SkillType.BRAWL, Perk.BRAWL_SWORD_IRON_SPIN),

    BRAWL_SWORD_GOLD_LIFE_STEAL(SkillType.BRAWL, Perk.BRAWL_CHARGE),
    BRAWL_SWORD_GOLD_REGENERATION(SkillType.BRAWL, Perk.BRAWL_SWORD_GOLD_LIFE_STEAL),
    BRAWL_SWORD_GOLD_HEALTH(SkillType.BRAWL, Perk.BRAWL_SWORD_GOLD_REGENERATION),

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
    RANCH_COW_OXHIDE(SkillType.RANCH, Perk.RANCH_COW),
    RANCH_COW_SIRLOIN(SkillType.RANCH, Perk.RANCH_COW),
    RANCH_COW_MILK(SkillType.RANCH, Perk.RANCH_COW),
    RANCH_PIG_TRUFFLE(SkillType.RANCH, Perk.RANCH_PIG),
    RANCH_PIG_PIGSKIN(SkillType.RANCH, Perk.RANCH_PIG),
    RANCH_PIG_BACON(SkillType.RANCH, Perk.RANCH_PIG),
    RANCH_SHEEP_RAINBOW(SkillType.RANCH, Perk.RANCH_SHEEP),
    RANCH_CHICKEN_GOLD_EGG(SkillType.RANCH, Perk.RANCH_CHICKEN),
    RANCH_CHICKEN_DOWN(SkillType.RANCH, Perk.RANCH_CHICKEN),
    RANCH_RABBIT_FOOT(SkillType.RANCH, Perk.RANCH_RABBIT),
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
    // Taming Base
    TAME_BASE(SkillType.TAME, null), // done
    TAME_FOLLOW_TELEPORT(SkillType.TAME, null), // done
    // Taming Cat
    TAME_CAT_AGGRO_CREEPER(SkillType.TAME, null), // done
    TAME_CAT_RAGE(SkillType.TAME, null), // done
    TAME_CAT_LIVES(SkillType.TAME, null), // done
    // Taming Dog
    TAME_DOG_ATTACK_DAMAGE(SkillType.TAME, null), // done
    TAME_DOG_MOVEMENT_SPEED(SkillType.TAME, null), // done
    TAME_DOG_HEALTH(SkillType.TAME, null), // done
    TAME_DOG_DODGE(SkillType.TAME, null), // done
    TAME_DOG_DEATH_HEALS(SkillType.TAME, null), // done
    TAME_DOG_SACRIFICE(SkillType.TAME, null), // done
    TAME_DOG_SPAWN_SNOWMAN(SkillType.TAME, null),
    TAME_DOG_SPAWN_GOLEM(SkillType.TAME, null),
    // Taming Horse
    TAME_HORSE_SPEED_CHANCE(SkillType.TAME, null),
    TAME_HORSE_JUMP_CHANCE(SkillType.TAME, null),
    TAME_HORSE_KNOCKBACK(SkillType.TAME, null),
    TAME_HORSE_RIDE_DOWN(SkillType.TAME, null),
    ;

    final SkillType skillType;
    final Perk depends;
    final String key;

    Perk(SkillType skillType, Perk depends) {
        this.skillType = skillType;
        this.depends = depends;
        this.key = name().toLowerCase();
    }
}
