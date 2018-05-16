package com.winthier.skills;

import com.winthier.custom.CustomPlugin;
import com.winthier.custom.entity.CustomEntity;
import com.winthier.custom.entity.EntityWatcher;
import com.winthier.custom.entity.TickableEntity;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

@RequiredArgsConstructor
public final class RanchEntity implements CustomEntity, TickableEntity {
    final SkillsPlugin plugin;
    static final String CUSTOM_ID = "winthier:Ranching";
    private long nextTicks = 0;

    @Override
    public String getCustomId() {
        return CUSTOM_ID;
    }

    @Override
    public LivingEntity spawnEntity(Location location) {
        return null;
    }

    @Override
    public EntityWatcher createEntityWatcher(Entity e) {
        Watcher result = new Watcher(this, (LivingEntity)e);
        result.tickOffset = nextTicks++;
        if (nextTicks >= 20) nextTicks = 0;
        return result;
    }

    @Override
    public void entityWasDiscovered(EntityWatcher watcher) {
        ((Watcher)watcher).load();
    }

    @Override
    public void entityWillUnload(EntityWatcher watcher) {
        ((Watcher)watcher).save();
    }

    @Override
    public void onTick(EntityWatcher entityWatcher) {
        Watcher watcher = (Watcher)entityWatcher;
        watcher.onTick();
    }

    enum Quirk {
        NOTHING,
        EXPLOSIVE,
        GLUTTON, // More hunger
        ABSTINENT, // Less hunger
        SOCIAL,
        SHY,
        SANGUINE, // More fun
        SAD, // Less fun
        SKINNY, // Little meat
        FAT, // Lots of meat
        RICH, // High yield
        POOR; // Little yield
    }

    enum Gender {
        MALE,
        FEMALE;
    }

    final Material[] treats = {
        Material.APPLE,
        Material.BEETROOT,
        Material.POTATO,
        Material.BAKED_POTATO,
        Material.BREAD,
        Material.CHORUS_FRUIT,
        Material.MELON,
        Material.SUGAR,
        Material.COOKIE
    };

    final Material[] toys = {
        Material.STRING,
        Material.BONE,
        Material.SLIME_BALL,
        Material.ARROW,
        Material.GOLD_NUGGET,
        Material.IRON_NUGGET,
        Material.STICK,
        Material.BUCKET,
        Material.TOTEM,
        Material.FLOWER_POT,
        Material.GLASS_BOTTLE
    };

    /**
     * Called by RanchSkill when two entities are bred and the rancher
     * has the required perks to improve them.
     */
    Watcher onBreed(LivingEntity motherEntity, LivingEntity fatherEntity, LivingEntity childEntity, Player rancher) {
        Watcher mother = null, father = null;
        EntityWatcher tmp;
        tmp = CustomPlugin.getInstance().getEntityManager().getEntityWatcher(motherEntity);
        if (tmp != null && tmp instanceof RanchEntity.Watcher) mother = (RanchEntity.Watcher)tmp;
        tmp = CustomPlugin.getInstance().getEntityManager().getEntityWatcher(fatherEntity);
        if (tmp != null && tmp instanceof RanchEntity.Watcher) father = (RanchEntity.Watcher)tmp;
        Watcher child = (Watcher)CustomPlugin.getInstance().getEntityManager().wrapEntity(childEntity, CUSTOM_ID);
        child.owner = rancher.getUniqueId();
        if (mother != null && father != null
            && mother.owner.equals(child.owner) && father.owner.equals(child.owner)) {
            if (mother.gender == Gender.FEMALE && father.gender != Gender.FEMALE) {
                Watcher tmp2;
                tmp2 = mother;
                mother = father;
                father = tmp2;
                LivingEntity tmp3 = motherEntity;
                motherEntity = fatherEntity;
                fatherEntity = tmp3;
            }
            child.generation = Math.min(mother.generation, father.generation) + 1;
            List<Quirk> motherQuirks = new ArrayList<>(mother.quirks);
            List<Quirk> fatherQuirks = new ArrayList<>(father.quirks);
            Random random = new Random(System.currentTimeMillis());
            if (!motherQuirks.isEmpty()) child.quirks.add(motherQuirks.remove(random.nextInt(motherQuirks.size())));
            if (!fatherQuirks.isEmpty()) child.quirks.add(fatherQuirks.remove(random.nextInt(fatherQuirks.size())));
            if (!motherQuirks.isEmpty()) child.quirks.add(motherQuirks.remove(random.nextInt(motherQuirks.size())));
            if (!fatherQuirks.isEmpty()) child.quirks.add(fatherQuirks.remove(random.nextInt(fatherQuirks.size())));
            // Mutation
            if (random.nextInt(3) == 0) {
                List<Quirk> quirkPool = new ArrayList<>();
                for (Quirk quirk: Quirk.values()) {
                    if (!child.quirks.contains(quirk)) quirkPool.add(quirk);
                }
                if (!quirkPool.isEmpty()) child.quirks.add(quirkPool.remove(random.nextInt(quirkPool.size())));
            }
        } else {
            child.generation = 1;
        }
        child.roll();
        child.save();
        return child;
    }

    @Getter @RequiredArgsConstructor
    static final class Watcher implements EntityWatcher {
        // Constant
        static final String SCOREBOARD_KEY = "winthier_ranching";
        // Interface implementation
        private final RanchEntity customEntity;
        private final LivingEntity entity;
        // Payload
        private String name = null;
        private int generation = 0;
        private int hunger, social, fun, happy; // needs
        private UUID owner = new UUID(0, 0);
        private List<Quirk> quirks = new ArrayList<>();
        private Material toy, treat;
        private Gender gender;
        private long tickOffset = 0;
        private long ticks = 0;
        private int unsaved = 0;

        boolean hasActiveQuirk(Quirk quirk) {
            int index = quirks.indexOf(quirk);
            return index == 0 || index == 1;
        }

        void load() {
            Map<String, Object> map = customEntity.plugin.getScoreboardJSON(entity, SCOREBOARD_KEY);
            if (map == null) return;
            ConfigurationSection config = new YamlConfiguration().createSection("tmp", map);
            generation = config.getInt("gen");
            happy = config.getInt("happy");
            hunger = config.getInt("hunger");
            social = config.getInt("social");
            fun = config.getInt("fun");
            String ownerString = config.getString("owner");
            if (ownerString != null) {
                try {
                    owner = UUID.fromString(ownerString);
                } catch (IllegalArgumentException iae) {
                    iae.printStackTrace();
                }
            }
            quirks.clear();
            List<String> quirkList = config.getStringList("quirks");
            for (String str: quirkList) {
                try {
                    Quirk quirk = Quirk.valueOf(str.toUpperCase());
                    quirks.add(quirk);
                } catch (IllegalArgumentException iae) {
                    iae.printStackTrace();
                }
            }
            String toyString = config.getString("toy");
            if (toyString != null) {
                try {
                    toy = null;
                    toy = Material.valueOf(toyString.toUpperCase());
                } catch (IllegalArgumentException iae) {
                    iae.printStackTrace();
                }
            }
            String treatString = config.getString("treat");
            if (treatString != null) {
                try {
                    treat = Material.valueOf(treatString.toUpperCase());
                } catch (IllegalArgumentException iae) {
                    treat = null;
                    iae.printStackTrace();
                }
            }
            String genderString = config.getString("gender");
            if (genderString != null) {
                try {
                    gender = Gender.valueOf(genderString.toUpperCase());
                } catch (IllegalArgumentException iae) {
                    gender = null;
                    iae.printStackTrace();
                }
            }
        }

        void save() {
            Map<String, Object> map = new HashMap<>();
            map.put("gen", generation);
            map.put("hunger", hunger);
            map.put("social", social);
            map.put("fun", fun);
            map.put("happy", happy);
            map.put("owner", owner.toString());
            map.put("quirks", quirks.stream().map(a -> a.name().toLowerCase()).collect(Collectors.toList()));
            map.put("toy", toy.name().toLowerCase());
            map.put("treat", treat.name().toLowerCase());
            map.put("gender", gender.name().toLowerCase());
            customEntity.plugin.storeScoreboardJSON(entity, SCOREBOARD_KEY, map);
            unsaved = 0;
        }

        void roll() {
            Random random = new Random(System.currentTimeMillis());
            if (quirks.size() < 4) {
                List<Quirk> quirkPool = new ArrayList<>();
                for (Quirk quirk: Quirk.values()) quirkPool.add(quirk);
                while (quirks.size() < 4 && !quirkPool.isEmpty()) {
                    Quirk quirk = quirkPool.remove(random.nextInt(quirkPool.size()));
                    quirks.add(quirk);
                }
            }
            if (toy == null) toy = customEntity.toys[random.nextInt(customEntity.toys.length)];
            if (treat == null) treat = customEntity.treats[random.nextInt(customEntity.treats.length)];
            if (gender == null) gender = random.nextBoolean() ? Gender.FEMALE : Gender.MALE;
            if (name == null) {
                List<String> names;
                if (gender == Gender.FEMALE) {
                    names = customEntity.plugin.getSkill(RanchSkill.class).femaleNames;
                } else {
                    names = customEntity.plugin.getSkill(RanchSkill.class).maleNames;
                }
                name = names.get(random.nextInt(names.size()));
            }
        }

        boolean eat() {
            Block block = null;
            // Crops or flowers
            switch (entity.getType()) {
            case PIG:
                for (int i = 0; i < 2; i += 1) {
                    block = entity.getLocation().getBlock().getRelative(0, i, 0);
                    switch (block.getType()) {
                    case CARROT:
                    case POTATO:
                        if (block.getData() == 7) {
                            block.setType(Material.AIR);
                            hunger = Math.max(0, hunger - 600);
                        }
                        break;
                    case BEETROOT_BLOCK:
                        if (block.getData() == 3) {
                            block.setType(Material.AIR);
                            hunger = Math.max(0, hunger - 600);
                        }
                        break;
                    default: break;
                    }
                }
                break;
            case COW:
                for (int i = 0; i < 2; i += 1) {
                    block = entity.getLocation().getBlock().getRelative(0, i, 0);
                    switch (block.getType()) {
                    case CROPS:
                        if (block.getData() == 7) {
                            block.setType(Material.AIR);
                            hunger = Math.max(0, hunger - 600);
                        }
                        break;
                    default: break;
                    }
                }
                break;
            case RABBIT:
                for (int i = 0; i < 2; i += 1) {
                    block = entity.getLocation().getBlock().getRelative(0, i, 0);
                    switch (block.getType()) {
                    case CARROT:
                        if (block.getData() == 7) {
                            block.setType(Material.AIR);
                            hunger = Math.max(0, hunger - 600);
                        }
                        break;
                    case YELLOW_FLOWER:
                        if (block.getData() == 0) {
                            block.setType(Material.AIR);
                            hunger = Math.max(0, hunger - 600);
                        }
                        break;
                    default: break;
                    }
                }
                break;
            }
            // Tall grass or mushrooms
            switch (entity.getType()) {
            case COW:
            case SHEEP:
            case RABBIT:
                block = entity.getLocation().getBlock();
                if (block.getType() == Material.LONG_GRASS) {
                    block.setType(Material.AIR);
                    hunger = Math.max(0, hunger - 600);
                    return true;
                }
                break;
            case MUSHROOM_COW:
                block = entity.getLocation().getBlock();
                if (block.getType() == Material.RED_MUSHROOM
                    || block.getType() == Material.BROWN_MUSHROOM) {
                    block.setType(Material.AIR);
                    hunger = Math.max(0, hunger - 600);
                    return true;
                }
                break;
            default: break;
            }
            // Flat grass or mycel
            switch (entity.getType()) {
            case COW:
            case SHEEP:
                block = entity.getLocation().getBlock().getRelative(0, -1, 0);
                if (block.getType() == Material.GRASS) {
                    block.setType(Material.DIRT);
                    hunger = Math.max(0, hunger - 300);
                    return true;
                }
                break;
            case CHICKEN:
                block = entity.getLocation().getBlock().getRelative(0, -1, 0);
                if (block.getType() == Material.GRASS
                    || block.getType() == Material.DIRT) {
                    block.setTypeIdAndData(Material.DIRT.getId(), (byte)3, true); // Coarse dirt
                    hunger = Math.max(0, hunger - 300);
                    return true;
                }
                break;
            case MUSHROOM_COW:
                block = entity.getLocation().getBlock().getRelative(0, -1, 0);
                if (block.getType() == Material.MYCEL) {
                    block.setType(Material.DIRT);
                    hunger = Math.max(0, hunger - 300);
                    return true;
                }
                break;
            default: break;
            }
            return false;
        }

        void onTick() {
            if ((ticks++ % tickOffset) != 0) return;
            unsaved += 1;
            if (unsaved >= 60) save();
            hunger += 1;
            Random random = new Random(System.currentTimeMillis());
            if (hasActiveQuirk(Quirk.GLUTTON)) hunger += 1;
            if (hasActiveQuirk(Quirk.ABSTINENT) && random.nextBoolean()) hunger -= 1;
            social += 1;
            fun = Math.min(100, fun + 1);
            if (hasActiveQuirk(Quirk.SANGUINE)) fun += 1;
            if (hasActiveQuirk(Quirk.SAD) && random.nextBoolean()) fun -= 1;
            if (hunger > 300) eat();
            
        }
    }
}
