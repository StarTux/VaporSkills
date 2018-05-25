package com.winthier.skills;

import com.winthier.custom.CustomPlugin;
import com.winthier.custom.item.CustomItem;
import com.winthier.custom.item.ItemContext;
import org.bukkit.ChatColor;
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
        PRIME_OXHIDE(Material.LEATHER),
        FABLED_OXHIDE(Material.LEATHER),
        PRIME_PIGSKIN(Material.LEATHER),
        FABLED_PIGSKIN(Material.LEATHER),
        PRIME_LEATHER_SCRAP(Material.LEATHER),
        FABLED_LEATHER_SCRAP(Material.LEATHER),
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

    IngredientItem(SkillsPlugin plugin, Type type) {
        this.plugin = plugin;
        this.type = type;
        this.customId = "skills:" + type.name().toLowerCase();
        this.itemStack = new ItemStack(type.material);
        ItemMeta meta = this.itemStack.getItemMeta();
        meta.addEnchant(Enchantment.DURABILITY, 1, false);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        meta.setDisplayName(ChatColor.BLUE + Msg.capitalEnumName(type));
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
