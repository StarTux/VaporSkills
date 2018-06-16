package com.winthier.skills;

import com.winthier.custom.CustomPlugin;
import com.winthier.custom.item.CustomItem;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;

public final class IngredientItem implements CustomItem {
    final SkillsPlugin plugin;
    final Type type;
    final ItemStack itemStack;

    enum Type {
        // Leather
        OXHIDE(Material.LEATHER),
        PIGSKIN(Material.LEATHER),
        TANNED_LEATHER(Material.LEATHER),
        LEATHER_SCRAPS(Material.LEATHER),
        HARDENED_LEATHER(Material.LEATHER),
        // Food
        SIRLOIN(Material.RAW_BEEF),
        BACON(Material.PORK),
        FRESH_MILK(Material.DRAGONS_BREATH),
        TRUFFLE(Material.BROWN_MUSHROOM),
        CHICKEN_DOWN(Material.FEATHER),
        // Iron
        FINE_IRON_NUGGET(Material.IRON_NUGGET),
        FINE_IRON_BAR(Material.IRON_INGOT),
        STEEL_BAR(Material.IRON_INGOT),
        HARDENED_STEEL(Material.IRON_INGOT),
        // Gold
        FINE_GOLD_NUGGET(Material.GOLD_NUGGET),
        GOLDEN_EGG(Material.GOLD_NUGGET),
        GOLD_BULLION(Material.GOLD_INGOT),
        FINE_GOLD_BULLION(Material.GOLD_INGOT),
        WHITE_GOLD(Material.GOLD_INGOT),
        // Diamond
        FLAWLESS_DIAMOND(Material.DIAMOND),
        FLAWLESS_EMERALD(Material.EMERALD),
        EDGED_DIAMOND(Material.DIAMOND),
        EDGED_EMERALD(Material.EMERALD),
        DIAMOND_DUST(Material.SUGAR),
        EMERALD_DUST(Material.INK_SACK, 2, (String)null), // TODO cactus_green in 1.13
        GEMSTONE_DUST(Material.BLAZE_POWDER),
        // Rare
        CREEPER_OIL(Material.POTION),
        WITHER_OIL(Material.POTION),
        NICKEL(Material.GHAST_TEAR);

        final Material material;
        final int data; // TODO remove in 1.13
        final String extra;
        final String customId;

        Type(Material material) {
            this(material, 0, null);
        }

        Type(Material material, int data, String extra) {
            this.material = material;
            this.data = data;
            this.extra = extra;
            this.customId = "skills:" + name().toLowerCase();
        }

        ItemStack spawn() {
            return CustomPlugin.getInstance().getItemManager().spawnItemStack(customId, 1);
        }

        ItemStack spawn(int amount) {
            return CustomPlugin.getInstance().getItemManager().spawnItemStack(customId, amount);
        }

        Item drop(Location location) {
            return CustomPlugin.getInstance().getItemManager().dropItemStack(location, customId, 1);
        }

        static Type of(ItemStack item) {
            if (item == null) return null;
            CustomItem custom = CustomPlugin.getInstance().getItemManager().getCustomItem(item);
            if (custom == null || !(custom instanceof IngredientItem)) return null;
            return ((IngredientItem)custom).type;
        }
    }

    IngredientItem(SkillsPlugin plugin, Type type) {
        this.plugin = plugin;
        this.type = type;
        this.itemStack = new ItemStack(type.material, 1, (short)type.data);
        ItemMeta meta = this.itemStack.getItemMeta();
        meta.addEnchant(Enchantment.DURABILITY, 1, false);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        meta.setDisplayName(ChatColor.BLUE + Msg.capitalEnumName(type));
        switch (type) {
        case CREEPER_OIL:
            ((PotionMeta)meta).setBasePotionData(new PotionData(PotionType.POISON));
            meta.addItemFlags(ItemFlag.HIDE_POTION_EFFECTS);
            break;
        case WITHER_OIL:
            ((PotionMeta)meta).setBasePotionData(new PotionData(PotionType.INSTANT_DAMAGE));
            meta.addItemFlags(ItemFlag.HIDE_POTION_EFFECTS);
            break;
        default:
            break;
        }
        this.itemStack.setItemMeta(meta);
    }

    @Override
    public String getCustomId() {
        return type.customId;
    }

    @Override
    public ItemStack spawnItemStack(int amount) {
        ItemStack result = itemStack.clone();
        result.setAmount(amount);
        return result;
    }
}
