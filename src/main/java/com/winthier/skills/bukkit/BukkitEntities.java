package com.winthier.skills.bukkit;

import com.winthier.skills.util.Strings;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.Zombie;

// TODO: Check special names
class BukkitEntities
{
    private static List<String> entityNames;
    static enum SpecialNames {
        WITHER_SKELETON("WitherSkeleton"),
        BABY_ZOMBIE("BabyZombie"),
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
        if (e instanceof Skeleton && ((Skeleton)e).getSkeletonType() == Skeleton.SkeletonType.WITHER) return SpecialNames.WITHER_SKELETON.name;
        if (e instanceof Zombie && ((Zombie)e).isBaby()) return SpecialNames.BABY_ZOMBIE.name;
        return name(e.getType());
    }

    static String name(EntityType et) {
        String[] ls = et.name().split("_");
        for (int i = 0; i < ls.length; ++i) ls[i] = Strings.camelCase(ls[i]);
        return Strings.fold(ls, "");
    }
}
