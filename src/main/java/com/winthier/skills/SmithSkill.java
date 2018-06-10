package com.winthier.skills;

import com.winthier.custom.CustomPlugin;
import com.winthier.custom.util.Dirty;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

final class SmithSkill extends Skill {
    SmithSkill(SkillsPlugin plugin) {
        super(plugin, SkillType.SMITH);
    }

    private static void addAttribute(ItemStack item, EquipmentSlot slot, String name, Attribute attribute, double amount, int operation, UUID uuid) {
        // Attribute name
        String[] attrNames = attribute.name().split("_");
        StringBuilder sb = new StringBuilder();
        sb.append(attrNames[0].toLowerCase()).append(".");
        sb.append(attrNames[1].toLowerCase());
        for (int i = 2; i < attrNames.length; i += 1) sb.append(attrNames[i].substring(0, 1)).append(attrNames[i].substring(1).toLowerCase());
        final String attributeName = sb.toString();
        // Name
        if (name == null) name = attributeName;
        // UUID
        if (uuid == null) uuid = new UUID(1 + (long)slot.ordinal(), 1 + (long)attribute.ordinal());
        addAttribute(item, slot, name, attributeName, amount, operation, uuid);
    }

    private static void addAttribute(ItemStack item, EquipmentSlot slot, String name, String attributeName, double amount, int operation, UUID uuid) {
        Dirty.TagWrapper itemTag = Dirty.TagWrapper.getItemTagOf(item);
        Dirty.TagListWrapper attrList = itemTag.getList("AttributeModifiers");
        if (attrList == null) attrList = itemTag.createList("AttributeModifiers");
        Dirty.TagWrapper attrTag = attrList.createCompound();
        final String slotName;
        switch (slot) {
        case HAND: slotName = "mainhand"; break;
        case OFF_HAND: slotName = "offhand"; break;
        default: slotName = slot.name().toLowerCase();
        }
        attrTag.setString("Slot", slotName);
        attrTag.setString("AttributeName", attributeName);
        attrTag.setString("Name", name);
        attrTag.setDouble("Amount", amount);
        attrTag.setInt("Operation", operation);
        attrTag.setLong("UUIDMost", uuid.getMostSignificantBits());
        attrTag.setLong("UUIDLeast", uuid.getLeastSignificantBits());
    }

    static int getDefaultArmor(Material mat) {
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

    static int getDefaultDamage(Material mat) {
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
        default: return 0;
        }
    }

    static double getDefaultAttackSpeed(Material mat) {
        switch (mat) {
        // Swords
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
            default:
                return null;
            }
        }
    }

    enum Quality {
        WOOD(Material.WOOD),
        STONE(Material.COBBLESTONE),
        LEATHER(Material.LEATHER),
        IRON(Material.IRON_INGOT),
        GOLD(Material.GOLD_INGOT),
        CHAINMAIL(Material.IRON_INGOT),
        DIAMOND(Material.DIAMOND);

        final Material material;

        Quality(Material material) {
            this.material = material;
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
                return Quality.CHAINMAIL;
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

    void anvilRecipe(Player player, SkillsPlugin.AnvilStore anvilStore) {
        final UUID uuid = player.getUniqueId();
        final int skillLevel = plugin.getScore().getSkillLevel(uuid, skillType);
        // If a gear item is in slot A, this is an attempted gear
        // improvement.  The Gear and Quality enums are designed to
        // figure out if this is the case and provide an abstraction
        // to work on.
        final Gear gear = Gear.of(anvilStore.inputA.getType());
        final Quality quality = Quality.of(anvilStore.inputA.getType());
        if (gear != null && quality != null) {
            player.sendMessage(gear + " " + quality);
            // Gear improvements require exactly 1 item per slot.  The
            // first item has to be an undamaged, unmodified vanilla
            // gear item.
            if (anvilStore.inputA.getAmount() != 1) return;
            if (anvilStore.inputB == null) return;
            if (anvilStore.inputB.getAmount() != 1) return;
            if (!anvilStore.inputA.equals(new ItemStack(anvilStore.inputA.getType()))) return;
            // Slot B may either be a vanilla item used to create the
            // gear in question, or a similar IngredientItem.  Each
            // item has a specific fineness which determines the
            // quality of the resulting gear item.
            IngredientItem.Type ingredient = IngredientItem.Type.of(anvilStore.inputB);
            int fineness = 0;
            if (ingredient == null) {
                if (anvilStore.inputB.getType() == quality.material) {
                    fineness = 1;
                } else {
                    return;
                }
            }
            switch (quality) {
            case WOOD:
                break;
            case STONE:
                break;
            case LEATHER:
                if (plugin.getScore().hasPerk(uuid, Perk.SMITH_LEATHER)) {
                    if (ingredient != null) {
                        switch (ingredient) {
                        case OXHIDE:
                        case PIGSKIN:
                            fineness = 2; break;
                        case LEATHER_SCRAPS:
                            fineness = 3; break;
                        default:
                            fineness = 0;
                        }
                    }
                }
                break;
            case IRON:
                break;
            case GOLD:
                break;
            case CHAINMAIL:
                break;
            case DIAMOND:
                break;
            default:
                break;
            }
            if (fineness <= 0) return;
            double finenessFactor = (double)fineness / 4.0;
            switch (gear.category) {
            case ARMOR:
                ItemStack output = CustomPlugin.getInstance().getItemManager().wrapItemStack(new ItemStack(anvilStore.inputA.getType()), GearItem.CUSTOM_ID);
                ItemMeta meta = output.getItemMeta();
                meta.setUnbreakable(true);
                output.setItemMeta(meta);
                double armor = getDefaultArmor(output.getType());
                armor *= 1.0 + linearSkillBonus(finenessFactor, skillLevel);
                addAttribute(output, gear.slot, "skills:armor", Attribute.GENERIC_ARMOR, armor, 0, null);
                anvilStore.setOutput(output);
                break;
            default:
                break;
            }
        }
    }

    /**
     * Called by SkillsPlugin.onFurnaceSmelt()
     */
    void onItemSmelt(Player player, ItemStack source, ItemStack result) {
    }
}
