package com.winthier.skills;

import com.winthier.custom.item.CustomItem;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

@RequiredArgsConstructor
public final class GearItem implements CustomItem {
    public static final String CUSTOM_ID = "skills:gear";
    final SkillsPlugin plugin;

    static final class Meta {
        static final class Enchant {
            enum Type {
                FOOBAR;
            }
            private Type type;
            private int maxCharges;
            private int charges;
        }
        private final List<Enchant> enchants = new ArrayList<>();
        private int durability;
        private int enhanced;
    }

    @Override
    public String getCustomId() {
        return CUSTOM_ID;
    }

    @Override
    public ItemStack spawnItemStack(int amount) {
        return new ItemStack(Material.IRON_CHESTPLATE, amount);
    }
}
