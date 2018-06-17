package com.winthier.skills;

import com.winthier.custom.CustomPlugin;
import com.winthier.skills.SkillsPlugin.AnvilStore;
import com.winthier.skills.SkillsPlugin.AttributeEntry;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

final class SmithSkill extends Skill {
    SmithSkill(SkillsPlugin plugin) {
        super(plugin, SkillType.SMITH);
    }

    static double getDefaultArmor(Material mat) {
        switch (mat) {
        case LEATHER_HELMET: return 1;
        case LEATHER_CHESTPLATE: return 3;
        case LEATHER_LEGGINGS: return 2;
        case LEATHER_BOOTS: return 1;
        case GOLD_HELMET: return 2;
        case GOLD_CHESTPLATE: return 5;
        case GOLD_LEGGINGS: return 3;
        case GOLD_BOOTS: return 1;
        case CHAINMAIL_HELMET: return 2;
        case CHAINMAIL_CHESTPLATE: return 5;
        case CHAINMAIL_LEGGINGS: return 4;
        case CHAINMAIL_BOOTS: return 1;
        case IRON_HELMET: return 2;
        case IRON_CHESTPLATE: return 6;
        case IRON_LEGGINGS: return 5;
        case IRON_BOOTS: return 2;
        case DIAMOND_HELMET: return 3;
        case DIAMOND_CHESTPLATE: return 8;
        case DIAMOND_LEGGINGS: return 6;
        case DIAMOND_BOOTS: return 3;
        default: return 0;
        }
    }

    static double getDefaultAttackDamage(Material mat) {
        switch (mat) {
        case WOOD_SWORD: return 4;
        case GOLD_SWORD: return 4;
        case STONE_SWORD: return 5;
        case IRON_SWORD: return 6;
        case DIAMOND_SWORD: return 7;
            // Axes
        case WOOD_AXE: return 7;
        case GOLD_AXE: return 7;
        case STONE_AXE: return 9;
        case IRON_AXE: return 9;
        case DIAMOND_AXE: return 9;
            // Pickaxes
        case WOOD_PICKAXE: return 2;
        case GOLD_PICKAXE: return 2;
        case STONE_PICKAXE: return 3;
        case IRON_PICKAXE: return 4;
        case DIAMOND_PICKAXE: return 5;
            // Shovel
        case WOOD_SPADE: return 2.5;
        case GOLD_SPADE: return 2.5;
        case STONE_SPADE: return 3.5;
        case IRON_SPADE: return 4.5;
        case DIAMOND_SPADE: return 5.5;
            // Hoe
        case WOOD_HOE: return 1;
        case GOLD_HOE: return 1;
        case STONE_HOE: return 1;
        case IRON_HOE: return 1;
        case DIAMOND_HOE: return 1;
        default: return 0;
        }
    }

    static double getDefaultAttackSpeed(Material mat) {
        switch (mat) {
        case WOOD_SWORD:
        case GOLD_SWORD:
        case STONE_SWORD:
        case IRON_SWORD:
        case DIAMOND_SWORD:
            return 1.6;
            // Axes
        case WOOD_AXE: return 0.8;
        case GOLD_AXE: return 1.0;
        case STONE_AXE: return 0.8;
        case IRON_AXE: return 0.9;
        case DIAMOND_AXE: return 1.0;
            // Pickaxes
        case WOOD_PICKAXE: return 1.2;
        case STONE_PICKAXE: return 1.2;
        case GOLD_PICKAXE: return 1.2;
        case IRON_PICKAXE: return 1.2;
        case DIAMOND_PICKAXE: return 1.2;
            // Shovels
        case WOOD_SPADE: return 1.0;
        case GOLD_SPADE: return 1.0;
        case STONE_SPADE: return 1.0;
        case IRON_SPADE: return 1.0;
        case DIAMOND_SPADE: return 1.0;
            // Hoes
        case WOOD_HOE: return 1.0;
        case GOLD_HOE: return 1.0;
        case STONE_HOE: return 2.0;
        case IRON_HOE: return 3.0;
        case DIAMOND_HOE: return 4.0;
        default: return 0;
        }
    }

    static double getBaseArmor(Gear gear) {
        switch (gear) {
        case HELMET: return 5;
        case CHESTPLATE: return 8;
        case LEGGINGS: return 7;
        case BOOTS: return 4;
        default: return 0;
        }
    }

    static double getBaseAttackDamage(Gear gear) {
        switch (gear) {
        case SWORD: return 8;
        case AXE: return 10;
        case PICKAXE: return 5;
        case SHOVEL: return 5;
        case HOE: return 5;
        default: return 0;
        }
    }

    enum Gear {
        HELMET(EquipmentSlot.HEAD, Category.ARMOR),
        CHESTPLATE(EquipmentSlot.CHEST, Category.ARMOR),
        LEGGINGS(EquipmentSlot.LEGS, Category.ARMOR),
        BOOTS(EquipmentSlot.FEET, Category.ARMOR),
        SWORD(EquipmentSlot.HAND, Category.WEAPON),
        AXE(EquipmentSlot.HAND, Category.WEAPON_TOOL),
        PICKAXE(EquipmentSlot.HAND, Category.TOOL),
        SHOVEL(EquipmentSlot.HAND, Category.TOOL),
        HOE(EquipmentSlot.HAND, Category.TOOL),
        SHIELD(EquipmentSlot.OFF_HAND, Category.SHIELD),
        BOW(EquipmentSlot.HAND, Category.BOW),
        BARDING(EquipmentSlot.CHEST, Category.BARDING);

        enum Category {
            ARMOR, WEAPON, TOOL, WEAPON_TOOL, SHIELD, BARDING, BOW;

            boolean isWeapon() {
                switch (this) {
                case WEAPON:
                case WEAPON_TOOL:
                    return true;
                default:
                    return false;
                }
            }

            boolean isTool() {
                switch (this) {
                case WEAPON:
                case WEAPON_TOOL:
                    return true;
                default:
                    return false;
                }
            }
        }

        final EquipmentSlot slot;
        final Category category;

        Gear(EquipmentSlot slot, Category category) {
            this.slot = slot;
            this.category = category;
        }

        static Gear of(Material mat) {
            switch (mat) {
            case BOW:
                return Gear.BOW;
            case SHIELD:
                return Gear.SHIELD;
            case CHAINMAIL_HELMET:
            case DIAMOND_HELMET:
            case GOLD_HELMET:
            case IRON_HELMET:
            case LEATHER_HELMET:
                return Gear.HELMET;
            case CHAINMAIL_CHESTPLATE:
            case DIAMOND_CHESTPLATE:
            case GOLD_CHESTPLATE:
            case IRON_CHESTPLATE:
            case LEATHER_CHESTPLATE:
                return Gear.CHESTPLATE;
            case CHAINMAIL_LEGGINGS:
            case DIAMOND_LEGGINGS:
            case GOLD_LEGGINGS:
            case IRON_LEGGINGS:
            case LEATHER_LEGGINGS:
                return Gear.LEGGINGS;
            case CHAINMAIL_BOOTS:
            case DIAMOND_BOOTS:
            case GOLD_BOOTS:
            case IRON_BOOTS:
            case LEATHER_BOOTS:
                return Gear.BOOTS;
            case DIAMOND_SWORD:
            case GOLD_SWORD:
            case IRON_SWORD:
            case STONE_SWORD:
            case WOOD_SWORD:
                return Gear.SWORD;
            case DIAMOND_AXE:
            case GOLD_AXE:
            case IRON_AXE:
            case STONE_AXE:
            case WOOD_AXE:
                return Gear.AXE;
            case DIAMOND_PICKAXE:
            case GOLD_PICKAXE:
            case IRON_PICKAXE:
            case STONE_PICKAXE:
            case WOOD_PICKAXE:
                return Gear.PICKAXE;
            case DIAMOND_SPADE:
            case GOLD_SPADE:
            case IRON_SPADE:
            case STONE_SPADE:
            case WOOD_SPADE:
                return Gear.SHOVEL;
            case DIAMOND_BARDING:
            case GOLD_BARDING:
            case IRON_BARDING:
                return Gear.BARDING;
            case DIAMOND_HOE:
            case GOLD_HOE:
            case IRON_HOE:
            case STONE_HOE:
            case WOOD_HOE:
                return Gear.HOE;
            default:
                return null;
            }
        }
    }

    enum Quality {
        WOOD(Material.WOOD, null),
        STONE(Material.COBBLESTONE, null),
        LEATHER(Material.LEATHER, Perk.SMITH_LEATHER),
        IRON(Material.IRON_INGOT, Perk.SMITH_IRON),
        GOLD(Material.GOLD_INGOT, Perk.SMITH_GOLD),
        MAIL(Material.IRON_INGOT, Perk.SMITH_MAIL),
        DIAMOND(Material.DIAMOND, Perk.SMITH_DIAMOND);

        final Material material;
        final Perk requiredPerk;

        Quality(Material material, Perk requiredPerk) {
            this.material = material;
            this.requiredPerk = requiredPerk;
        }

        static Quality of(Material mat) {
            switch (mat) {
            case WOOD_AXE:
            case WOOD_HOE:
            case WOOD_PICKAXE:
            case WOOD_SPADE:
            case WOOD_SWORD:
                return Quality.WOOD;
            case STONE_AXE:
            case STONE_HOE:
            case STONE_PICKAXE:
            case STONE_SPADE:
            case STONE_SWORD:
                return Quality.STONE;
            case LEATHER_BOOTS:
            case LEATHER_CHESTPLATE:
            case LEATHER_HELMET:
            case LEATHER_LEGGINGS:
                return Quality.LEATHER;
            case IRON_AXE:
            case IRON_BARDING:
            case IRON_BOOTS:
            case IRON_CHESTPLATE:
            case IRON_HELMET:
            case IRON_HOE:
            case IRON_LEGGINGS:
            case IRON_PICKAXE:
            case IRON_SPADE:
            case IRON_SWORD:
                return Quality.IRON;
            case GOLD_AXE:
            case GOLD_BARDING:
            case GOLD_BOOTS:
            case GOLD_CHESTPLATE:
            case GOLD_HELMET:
            case GOLD_HOE:
            case GOLD_LEGGINGS:
            case GOLD_PICKAXE:
            case GOLD_SPADE:
            case GOLD_SWORD:
                return Quality.GOLD;
            case CHAINMAIL_BOOTS:
            case CHAINMAIL_CHESTPLATE:
            case CHAINMAIL_HELMET:
            case CHAINMAIL_LEGGINGS:
                return Quality.MAIL;
            case DIAMOND_AXE:
            case DIAMOND_BARDING:
            case DIAMOND_BOOTS:
            case DIAMOND_CHESTPLATE:
            case DIAMOND_HELMET:
            case DIAMOND_HOE:
            case DIAMOND_LEGGINGS:
            case DIAMOND_PICKAXE:
            case DIAMOND_SPADE:
            case DIAMOND_SWORD:
                return Quality.DIAMOND;
            default:
                return null;
            }
        }
    }

    void anvilRecipe(Player player, AnvilStore anvilStore) {
        final UUID uuid = player.getUniqueId();
        final int skillLevel = plugin.getScore().getSkillLevel(uuid, skillType);
        // If a gear item is in slot A, this is an attempted gear
        // improvement.  The Gear and Quality enums are designed to
        // figure out if this is the case and provide an abstraction
        // to work on.
        final Gear gear = Gear.of(anvilStore.inputA.getType());
        final Quality quality = Quality.of(anvilStore.inputA.getType());
        Set<Perk> perks = plugin.getScore().getPerks(uuid);
        if (gear != null && quality != null && anvilStore.inputB == null) {
            int enchantCount = anvilStore.inputA.getEnchantments().size();
            if (enchantCount == 0) return;
            anvilStore.setOutput(new ItemStack(Material.EXP_BOTTLE, enchantCount));
        } else if (gear != null && quality != null) {
            // Gear improvements require exactly 1 item per slot.  The
            // first item has to be an undamaged, unmodified vanilla
            // gear item.
            if (anvilStore.inputA.getAmount() != 1) return;
            if (anvilStore.inputB == null) return;
            if (anvilStore.inputB.getAmount() != 1) return;
            if (!anvilStore.inputA.equals(new ItemStack(anvilStore.inputA.getType()))) return;
            if (quality.requiredPerk == null || !perks.contains(quality.requiredPerk)) return;
            // Slot B may either be a vanilla item used to create the
            // gear in question, or a similar IngredientItem.  Each
            // item has a specific fineness which determines the
            // quality of the resulting gear item.
            IngredientItem.Type ingredient = IngredientItem.Type.of(anvilStore.inputB);
            final int fineness;
            switch (quality) {
            case LEATHER:
                if (ingredient != null) {
                    switch (ingredient) {
                    case OXHIDE:
                    case PIGSKIN:
                        fineness = 2; break;
                    case TANNED_LEATHER:
                        fineness = 3; break;
                    case LEATHER_SCRAPS:
                        fineness = 4; break;
                    case HARDENED_LEATHER:
                        fineness = 5; break;
                    default:
                        fineness = 0;
                    }
                } else if (anvilStore.inputB.getType() == Material.LEATHER) {
                    fineness = 1;
                } else {
                    fineness = 0;
                }
                break;
            case MAIL:
            case IRON:
                if (ingredient != null) {
                    switch (ingredient) {
                    case FINE_IRON_NUGGET:
                        fineness = 2; break;
                    case FINE_IRON_BAR:
                        fineness = 3; break;
                    case STEEL_BAR:
                        fineness = 4; break;
                    case HARDENED_STEEL:
                        fineness = 5; break;
                    default:
                        fineness = 0;
                    }
                } else if (anvilStore.inputB.getType() == Material.IRON_INGOT) {
                    fineness = 1;
                } else {
                    fineness = 0;
                }
                break;
            case GOLD:
                if (ingredient != null) {
                    switch (ingredient) {
                    case FINE_GOLD_NUGGET:
                    case GOLDEN_EGG:
                        fineness = 2; break;
                    case GOLD_BULLION:
                        fineness = 3; break;
                    case FINE_GOLD_BULLION:
                        fineness = 4; break;
                    case WHITE_GOLD:
                        fineness = 5; break;
                    default:
                        fineness = 0;
                    }
                } else if (anvilStore.inputB.getType() == Material.GOLD_INGOT) {
                    fineness = 1;
                } else {
                    fineness = 0;
                }
                break;
            case DIAMOND:
                if (ingredient != null) {
                    switch (ingredient) {
                    case FLAWLESS_DIAMOND:
                    case FLAWLESS_EMERALD:
                        fineness = 2; break;
                    case EDGED_DIAMOND:
                    case EDGED_EMERALD:
                        fineness = 3; break;
                    case DIAMOND_DUST:
                    case EMERALD_DUST:
                        fineness = 4; break;
                    case GEMSTONE_DUST:
                        fineness = 5; break;
                    default:
                        fineness = 0;
                    }
                } else if (anvilStore.inputB.getType() == Material.DIAMOND) {
                    fineness = 1;
                } else {
                    fineness = 0;
                }
                break;
            default:
                fineness = 0;
                break;
            }
            if (fineness == 0) return;
            double finenessFactor = (double)fineness / 5.0;
            ItemStack output = CustomPlugin.getInstance().getItemManager().wrapItemStack(new ItemStack(anvilStore.inputA.getType()), GearItem.CUSTOM_ID);
            ItemMeta meta = output.getItemMeta();
            if (perks.contains(Perk.SMITH_UNBREAKABLE)) meta.setUnbreakable(true);
            meta.setLore(Arrays.asList("Made by " + player.getName()));
            switch (fineness) {
            case 1: meta.setDisplayName(ChatColor.WHITE + "Simple " + Msg.capitalEnumName(output.getType())); break;
            case 2: meta.setDisplayName(ChatColor.GRAY + "Common " + Msg.capitalEnumName(output.getType())); break;
            case 3: meta.setDisplayName(ChatColor.BLUE + "Nice " + Msg.capitalEnumName(output.getType())); break;
            case 4: meta.setDisplayName(ChatColor.LIGHT_PURPLE + "Epic " + Msg.capitalEnumName(output.getType())); break;
            default: meta.setDisplayName(ChatColor.GOLD + "Legendary " + Msg.capitalEnumName(output.getType())); break;
            }
            output.setItemMeta(meta);
            if (gear.category == Gear.Category.ARMOR) {
                final double baseArmor = getBaseArmor(gear);
                double armor = baseArmor + linearSkillBonus(finenessFactor * baseArmor, skillLevel);
                // Different qualities of armor have various bonus
                // attributes.  In the case of diamond, it's the
                // default which we must make sure to maintain.
                switch (quality) {
                case LEATHER:
                    if (perks.contains(Perk.SMITH_LEATHER_ARMOR_SPEED)) {
                        double speed = 0.1 * finenessFactor;
                        new AttributeEntry(gear.slot, "skills:movementSpeed", Attribute.GENERIC_MOVEMENT_SPEED, speed, 0, null).addTo(output);
                    }
                    break;
                case IRON:
                    if (perks.contains(Perk.SMITH_IRON_ARMOR_ARMOR)) {
                        armor += finenessFactor * baseArmor;
                    }
                    break;
                case GOLD:
                    if (perks.contains(Perk.SMITH_GOLD_ARMOR_HEALTH)) {
                        double health = finenessFactor * baseArmor * 0.5;
                        new AttributeEntry(gear.slot, "skills:maxHealth", Attribute.GENERIC_MAX_HEALTH, health, 0, null).addTo(output);
                    }
                    break;
                case MAIL:
                    if (perks.contains(Perk.SMITH_MAIL_ARMOR_DAMAGE)) {
                        double damage = finenessFactor * baseArmor * 0.3;
                        new AttributeEntry(gear.slot, "skills:attackDamage", Attribute.GENERIC_ATTACK_DAMAGE, damage, 0, null).addTo(output);
                    }
                    break;
                case DIAMOND:
                    double armorToughness = 2;
                    if (perks.contains(Perk.SMITH_DIAMOND_ARMOR_TOUGH)) {
                        armorToughness += 2.0 * finenessFactor;
                    }
                    new AttributeEntry(gear.slot, "skills:armorToughness", Attribute.GENERIC_ARMOR_TOUGHNESS, armorToughness, 0, null).addTo(output);
                    break;
                default:
                    break;
                }
                new AttributeEntry(gear.slot, "skills:armor", Attribute.GENERIC_ARMOR, armor, 0, null).addTo(output);
            }
            if (gear.category.isWeapon() || gear.category.isTool()) {
                final double defaultAttackSpeed = getDefaultAttackSpeed(output.getType());
                final double baseDamage = getBaseAttackDamage(gear);
                double attackDamage = baseDamage + linearSkillBonus(baseDamage * finenessFactor, skillLevel);
                double attackSpeed = defaultAttackSpeed;
                switch (quality) {
                case IRON:
                    if (gear == Gear.SWORD && perks.contains(Perk.SMITH_IRON_SWORD_DAMAGE)) {
                        attackDamage += finenessFactor * baseDamage;
                    }
                    if (gear == Gear.AXE && perks.contains(Perk.SMITH_IRON_AXE_KNOCKBACK_RESIST)) {
                        double knockbackResist = 0.5 * finenessFactor;
                        new AttributeEntry(gear.slot, "skills:knockbackResistance", Attribute.GENERIC_KNOCKBACK_RESISTANCE, knockbackResist, 0, null).addTo(output);
                    }
                    break;
                case GOLD:
                    if (gear == Gear.SWORD && perks.contains(Perk.SMITH_GOLD_SWORD_ATTACK_SPEED)) {
                        attackSpeed -= attackSpeed * finenessFactor * 0.5;
                    }
                    if (gear == Gear.AXE && perks.contains(Perk.SMITH_GOLD_AXE_ATTACK_SPEED)) {
                        attackSpeed -= attackSpeed * finenessFactor * 0.5;
                    }
                    break;
                case DIAMOND:
                    if (gear == Gear.SWORD && perks.contains(Perk.SMITH_DIAMOND_SWORD_SPEED)) {
                        double speed = finenessFactor * 0.1;
                        new AttributeEntry(gear.slot, "skills:movementSpeed", Attribute.GENERIC_MOVEMENT_SPEED, speed, 0, null).addTo(output);
                    }
                    if (gear == Gear.AXE && perks.contains(Perk.SMITH_DIAMOND_AXE_DAMAGE)) {
                        attackDamage += finenessFactor * baseDamage;
                    }
                    break;
                default:
                    break;
                }
                new AttributeEntry(gear.slot, "skills:attackDamage", Attribute.GENERIC_ATTACK_DAMAGE, attackDamage, 0, null).addTo(output);
                new AttributeEntry(gear.slot, "skills:attackSpeed", Attribute.GENERIC_ATTACK_SPEED, attackSpeed, 0, null).addTo(output);
            }
            if (gear == Gear.SHIELD) {
                if (perks.contains(Perk.SMITH_SHIELD_KNOCKBACK_RESIST)) {
                    double knockbackResist = finenessFactor * 0.5;
                    new AttributeEntry(gear.slot, "skills:knockbackResistance", Attribute.GENERIC_KNOCKBACK_RESISTANCE, knockbackResist, 0, null).addTo(output);
                }
            }
            anvilStore.setOutput(output);
            Reward reward = getReward(Reward.Category.CRAFT_GEAR, output.getType().name(), fineness, null);
            if (reward == null) reward = getReward(Reward.Category.CRAFT_GEAR, null, fineness, null);
            giveReward(player, reward, 1);
        }
    }

    /**
     * Called by SkillsPlugin.onFurnaceSmelt()
     */
    void onItemSmelt(Player player, ItemStack source, ItemStack result) {
        Reward reward = getReward(Reward.Category.SMELT_ITEM, source.getType().name(), null, null);
        giveReward(player, reward, 1);
    }

    void onAnvilCraft(Player player, AnvilStore anvilStore) {
        IngredientItem.Type ingredient = IngredientItem.Type.of(anvilStore.getOutput());
        if (ingredient != null) {
            Reward reward = getReward(Reward.Category.CRAFT_INGREDIENT, ingredient.name(), null, null);
            giveReward(player, reward, 1);
        }
    }
}
