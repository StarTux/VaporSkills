package com.winthier.skills;

public enum Perk {
    // Smithing ====================================================
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
    // Brawl Swords ================================================
    BRAWL_SWORD_CHARGE(SkillType.BRAWL, null),
    BRAWL_SWORD_DIAMOND_PIERCE(SkillType.BRAWL, Perk.BRAWL_SWORD_CHARGE),
    BRAWL_SWORD_DIAMOND_DASH(SkillType.BRAWL, Perk.BRAWL_SWORD_DIAMOND_PIERCE),
    BRAWL_SWORD_IRON_SLASH(SkillType.BRAWL, Perk.BRAWL_SWORD_CHARGE),
    BRAWL_SWORD_IRON_SPIN(SkillType.BRAWL, Perk.BRAWL_SWORD_IRON_SLASH),
    BRAWL_SWORD_GOLD_LIFE_STEAL(SkillType.BRAWL, Perk.BRAWL_SWORD_CHARGE),
    BRAWL_SWORD_GOLD_RAGE(SkillType.BRAWL, Perk.BRAWL_SWORD_GOLD_LIFE_STEAL),
    // Brawl Axes
    BRAWL_AXE_CHARGE(SkillType.BRAWL, null),
    BRAWL_AXE_IRON_HAMMER(SkillType.BRAWL, Perk.BRAWL_AXE_CHARGE),
    BRAWL_AXE_IRON_HAMMER2(SkillType.BRAWL, Perk.BRAWL_AXE_IRON_HAMMER),
    BRAWL_AXE_GOLD_LIFE_STEAL(SkillType.BRAWL, Perk.BRAWL_AXE_CHARGE),
    BRAWL_AXE_GOLD_LIFE_STEAL2(SkillType.BRAWL, Perk.BRAWL_AXE_GOLD_LIFE_STEAL),
    BRAWL_AXE_DIAMOND_SLASH(SkillType.BRAWL, Perk.BRAWL_AXE_CHARGE),
    BRAWL_AXE_DIAMOND_THROW(SkillType.BRAWL, Perk.BRAWL_AXE_DIAMOND_SLASH),
    // Hunting =====================================================
    HUNT_BASE(SkillType.HUNT, null),
    // Special hits
    HUNT_HEADSHOT(SkillType.HUNT, Perk.HUNT_BASE),
    HUNT_FOOTSHOT(SkillType.HUNT, Perk.HUNT_BASE),
    HUNT_CRIT(SkillType.HUNT, Perk.HUNT_BASE),
    HUNT_CRIT2(SkillType.HUNT, Perk.HUNT_BASE),
    // Improve arrows
    HUNT_ARROW_RETRIEVE(SkillType.HUNT, Perk.HUNT_BASE),
    HUNT_ARROW_ABSORB(SkillType.HUNT, Perk.HUNT_ARROW_RETRIEVE),
    HUNT_ARROW_ABSORB2(SkillType.HUNT, Perk.HUNT_ARROW_ABSORB),
    // Bow charging
    HUNT_CHARGE_BOW(SkillType.HUNT, Perk.HUNT_BASE), // 1
    HUNT_CHARGE_MULTIPLE(SkillType.HUNT, Perk.HUNT_CHARGE_BOW), // 2
    HUNT_CHARGE_BARRAGE(SkillType.HUNT, Perk.HUNT_CHARGE_MULTIPLE), // 3
    HUNT_CHARGE_HAIL(SkillType.HUNT, Perk.HUNT_CHARGE_BARRAGE), // 4
    // Ranching ====================================================
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
    // Brewing =====================================================
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
    // Cooking =====================================================
    COOK_SATURATION(SkillType.COOK, null),
    COOK_MEAT_RESISTANCE(SkillType.COOK, null),
    COOK_VEGETABLE_HEALTH(SkillType.COOK, null),
    COOK_STARCH_STRENGTH(SkillType.COOK, null),
    COOK_SUGAR_SPEED(SkillType.COOK, null),
    COOK_FISH_ABSORPTION(SkillType.COOK, null),
    // Digging =====================================================
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
    // Enchanting ==================================================
    ENCHANT_ALTAR(SkillType.ENCHANT, null),
    ENCHANT_FIND_BOOKS(SkillType.ENCHANT, Perk.ENCHANT_ALTAR),
    ENCHANT_KEEP_BOOKS(SkillType.ENCHANT, Perk.ENCHANT_FIND_BOOKS),
    // Taming ======================================================
    TAME_BASE(SkillType.TAME, null),
    TAME_FOLLOW_TELEPORT(SkillType.TAME, Perk.TAME_BASE),
    // Taming Cat
    TAME_CAT_LIVES(SkillType.TAME, Perk.TAME_BASE),
    TAME_CAT_AGGRO_CREEPER(SkillType.TAME, Perk.TAME_CAT_LIVES),
    TAME_CAT_RAGE(SkillType.TAME, Perk.TAME_CAT_AGGRO_CREEPER),
    // Taming Dog
    TAME_DOG_ATTACK_DAMAGE(SkillType.TAME, Perk.TAME_BASE),
    TAME_DOG_MOVEMENT_SPEED(SkillType.TAME, Perk.TAME_DOG_ATTACK_DAMAGE),
    TAME_DOG_HEALTH(SkillType.TAME, Perk.TAME_DOG_MOVEMENT_SPEED),
    TAME_DOG_DODGE(SkillType.TAME, Perk.TAME_BASE),
    TAME_DOG_DEATH_HEALS(SkillType.TAME, Perk.TAME_DOG_DODGE),
    TAME_DOG_SACRIFICE(SkillType.TAME, Perk.TAME_DOG_DEATH_HEALS),
    // Taming Horse
    TAME_HORSE_INHERIT(SkillType.TAME, Perk.TAME_BASE),
    TAME_HORSE_SPEED_CHANCE(SkillType.TAME, Perk.TAME_HORSE_INHERIT),
    TAME_HORSE_JUMP_CHANCE(SkillType.TAME, Perk.TAME_HORSE_SPEED_CHANCE),
    TAME_HORSE_KNOCKBACK(SkillType.TAME, Perk.TAME_BASE),
    TAME_HORSE_RIDE_DOWN(SkillType.TAME, Perk.TAME_HORSE_KNOCKBACK),
    TAME_HORSE_RESURRECT(SkillType.TAME, Perk.TAME_HORSE_RIDE_DOWN);

    final SkillType skillType;
    final Perk depends;
    final String key;

    Perk(SkillType skillType, Perk depends) {
        this.skillType = skillType;
        this.depends = depends;
        this.key = name().toLowerCase();
    }
}
