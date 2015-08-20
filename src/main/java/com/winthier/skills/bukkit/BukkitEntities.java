package com.winthier.skills.bukkit;

import com.winthier.skills.util.Strings;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.MagmaCube;
import org.bukkit.entity.PigZombie;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.Slime;
import org.bukkit.entity.Zombie;

// TODO: Check special names
class BukkitEntities
{
    private static List<String> entityNames;
    static enum SpecialNames {
        WITHER_SKELETON("WitherSkeleton"),
        BABY_ZOMBIE("BabyZombie"),
        BABY_PIG_ZOMBIE("BabyPigZombie"),
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

    static List<String> getEntityNames()
    {
        if (entityNames == null) {
            entityNames = new ArrayList<>();
            for (SpecialNames name : SpecialNames.values()) entityNames.add(name.name);
            for (EntityType et : EntityType.values()) entityNames.add(name(et));
        }
        return entityNames;
    }
    
    static String name(Entity e)
    {
        EntityType type = e.getType();
        if (type == EntityType.SKELETON && e instanceof Skeleton && ((Skeleton)e).getSkeletonType() == Skeleton.SkeletonType.WITHER) return SpecialNames.WITHER_SKELETON.name;
        if (type == EntityType.PIG_ZOMBIE && e instanceof PigZombie && ((PigZombie)e).isBaby()) return SpecialNames.BABY_ZOMBIE.name;
        if (type == EntityType.ZOMBIE && e instanceof Zombie && ((Zombie)e).isBaby()) return SpecialNames.BABY_PIG_ZOMBIE.name;
        if (type == EntityType.SLIME && e instanceof Slime) {
            switch (((Slime)e).getSize()) {
            case 4: return SpecialNames.BIG_SLIME.name;
            case 2: return SpecialNames.SMALL_SLIME.name;
            case 1: return SpecialNames.TINY_SLIME.name;
            }
        }
        if (type == EntityType.MAGMA_CUBE && e instanceof Slime) {
            switch (((Slime)e).getSize()) {
            case 4: return SpecialNames.BIG_MAGMA_CUBE.name;
            case 2: return SpecialNames.SMALL_MAGMA_CUBE.name;
            case 1: return SpecialNames.TINY_MAGMA_CUBE.name;
            }
        }
        return name(e.getType());
    }

    static String name(EntityType et) {
        String[] ls = et.name().split("_");
        for (int i = 0; i < ls.length; ++i) ls[i] = Strings.camelCase(ls[i]);
        return Strings.fold(ls, "");
    }
}
