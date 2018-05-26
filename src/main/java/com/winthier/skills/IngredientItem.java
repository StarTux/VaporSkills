package com.winthier.skills;

import com.winthier.custom.CustomPlugin;
import com.winthier.custom.item.CustomItem;
import com.winthier.custom.item.ItemContext;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class IngredientItem implements CustomItem {
    final SkillsPlugin plugin;
    final Type type;
    final ItemStack itemStack;

    enum Type {
        OXHIDE(Material.LEATHER),
        PIGSKIN(Material.LEATHER),
        SIRLOIN(Material.RAW_BEEF),
        BACON(Material.PORK),
        FRESH_MILK(Material.DRAGONS_BREATH),
        TRUFFLE(Material.BROWN_MUSHROOM),
        GOLDEN_EGG(Material.GOLD_INGOT),
        CHICKEN_DOWN(Material.FEATHER),
        STEEL(Material.IRON_INGOT);

        final Material material;
        final String extra;
        final String customId;

        Type(Material material) {
            this(material, null);
        }

        Type(Material material, String extra) {
            this.material = material;
            this.extra = extra;
            this.customId = "skills:" + name().toLowerCase();
        }

        ItemStack spawn() {
            return CustomPlugin.getInstance().getItemManager().spawnItemStack(customId, 1);
        }

        Item drop(Location location) {
            return CustomPlugin.getInstance().getItemManager().dropItemStack(location, customId, 1);
        }
    }

    IngredientItem(SkillsPlugin plugin, Type type) {
        this.plugin = plugin;
        this.type = type;
        this.itemStack = new ItemStack(type.material);
        ItemMeta meta = this.itemStack.getItemMeta();
        meta.addEnchant(Enchantment.DURABILITY, 1, false);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        meta.setDisplayName(ChatColor.BLUE + Msg.capitalEnumName(type));
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
