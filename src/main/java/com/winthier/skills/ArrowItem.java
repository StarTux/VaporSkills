package com.winthier.skills;

import com.winthier.custom.item.CustomItem;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

@RequiredArgsConstructor
public final class ArrowItem implements CustomItem {
    private final SkillsPlugin plugin;
    private final Type type;

    enum Type {
        EXPLODE,
        POISON,
        WEB,
        WITHER,
        ENDER,
        TRANSMOG,
        BLAZE,
        RELAUNCH,
        MARK,
        FREEZE,
        SLOW,
        WEAK;

        final String customId;
        Type() {
            customId = "skills:arrow_" + name().toLowerCase();
        }
    }

    @Override
    public String getCustomId() {
        return type.customId;
    }

    @Override
    public ItemStack spawnItemStack(int amount) {
        return new ItemStack(Material.ARROW, amount);
    }
}
