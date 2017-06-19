package com.winthier.skills.bukkit;

import com.winthier.skills.util.Strings;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.MagmaCube;
import org.bukkit.entity.Slime;

final class BukkitEntities {
    private static List<String> entityNames;
    static enum SpecialNames {
        BIG_SLIME("BigSlime"),
        SMALL_SLIME("SmallSlime"),
        TINY_SLIME("TinySlime"),
        BIG_MAGMA_CUBE("BigMagmaCube"),
        SMALL_MAGMA_CUBE("SmallMagmaCube"),
        TINY_MAGMA_CUBE("TinyMagmaCube"),
        ;
        final String name;
        SpecialNames(String name) { this.name = name; }
    }

    static List<String> getEntityNames() {
        if (entityNames == null) {
            entityNames = new ArrayList<>();
            for (SpecialNames name : SpecialNames.values()) entityNames.add(name.name);
            for (EntityType et : EntityType.values()) entityNames.add(name(et));
        }
        return entityNames;
    }

    static String name(Entity e) {
        EntityType type = e.getType();
        switch (e.getType()) {
        case SLIME:
            switch (((Slime)e).getSize()) {
            case 4: return SpecialNames.BIG_SLIME.name;
            case 2: return SpecialNames.SMALL_SLIME.name;
            case 1: return SpecialNames.TINY_SLIME.name;
            default: break;
            }
            break;
        case MAGMA_CUBE:
            switch (((Slime)e).getSize()) {
            case 4: return SpecialNames.BIG_MAGMA_CUBE.name;
            case 2: return SpecialNames.SMALL_MAGMA_CUBE.name;
            case 1: return SpecialNames.TINY_MAGMA_CUBE.name;
            default: break;
            }
        default: break;
        }
        return name(e.getType());
    }

    static String name(EntityType et) {
        String[] ls = et.name().split("_");
        for (int i = 0; i < ls.length; ++i) ls[i] = Strings.camelCase(ls[i]);
        return Strings.fold(ls, "");
    }
}
