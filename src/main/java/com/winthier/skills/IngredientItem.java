package com.winthier.skills;

import com.winthier.custom.CustomPlugin;
import com.winthier.custom.item.CustomItem;
import com.winthier.custom.item.ItemContext;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class IngredientItem implements CustomItem {
    final SkillsPlugin plugin;
    final Type type;
    final String customId;
    final ItemStack itemStack;

    enum Type {
        FINE_LEATHER(Material.LEATHER),
        STEEL(Material.IRON_INGOT);

        final Material material;
        final String extra;

        Type(Material material) {
            this.material = material;
            this.extra = null;
        }

        Type(Material material, String extra) {
            this.material = material;
            this.extra = extra;
        }
    }

    private IngredientItem(SkillsPlugin plugin, Type type, Material material) {
        this.plugin = plugin;
        this.type = type;
        this.customId = "skills:" + type.name().toLowerCase();
        this.itemStack = new ItemStack(material);
        ItemMeta meta = this.itemStack.getItemMeta();
        meta.addEnchant(Enchantment.DURABILITY, 1, false);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        meta.setDisplayName(Msg.capitalEnumName(type));
        this.itemStack.setItemMeta(meta);
    }

    @Override
    public String getCustomId() {
        return customId;
    }

    @Override
    public ItemStack spawnItemStack(int amount) {
        ItemStack result = itemStack.clone();
        result.setAmount(amount);
        return result;
    }
}
