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
import org.bukkit.Bukkit;
import org.bukkit.EntityEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Cow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Pig;
import org.bukkit.entity.Player;
import org.bukkit.entity.Sheep;

@RequiredArgsConstructor
public final class RanchEntity implements CustomEntity, TickableEntity {
    final SkillsPlugin plugin;
    static final String CUSTOM_ID = "skills:ranching";
    private long nextTicks = 0;

    @Override
    public String getCustomId() {
        return CUSTOM_ID;
    }

    @Override
    public LivingEntity spawnEntity(Location location) {
        switch ((int)(nextTicks % 3)) {
        case 0: return location.getWorld().spawn(location, Cow.class);
        case 1: return location.getWorld().spawn(location, Sheep.class);
        case 2:
        default: return location.getWorld().spawn(location, Pig.class);
        }
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
    public void entityWasSpawned(EntityWatcher watcher) {
        ((Watcher)watcher).roll();
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
        GLUTTON, // Hungry
        ABSTINENT, // Not hungry
        SOCIAL,
        SHY,
        SANGUINE, // Happy
        SAD,
        FAT, // More meat
        SKINNY, // Little meat
        RICH, // High extra yield
        POOR, // Little extra yield
        SHORT_LIVED,
        LONG_LIVED,
        HEALTHY,
        SICKISH;
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

    final EntityType[] friends = {
        EntityType.COW,
        EntityType.PIG,
        EntityType.CHICKEN,
        EntityType.HORSE,
        EntityType.DONKEY,
        EntityType.MUSHROOM_COW,
        EntityType.SHEEP,
        EntityType.OCELOT,
        EntityType.WOLF
    };

    static void say(LivingEntity e) {
        switch (e.getType()) {
        case COW:
        case MUSHROOM_COW:
            e.getWorld().playSound(e.getEyeLocation(), Sound.ENTITY_COW_AMBIENT, 1f, 1f);
            break;
        case SHEEP:
            e.getWorld().playSound(e.getEyeLocation(), Sound.ENTITY_SHEEP_AMBIENT, 1f, 1f);
            break;
        case PIG:
            e.getWorld().playSound(e.getEyeLocation(), Sound.ENTITY_PIG_AMBIENT, 1f, 1f);
            break;
        case RABBIT:
            e.getWorld().playSound(e.getEyeLocation(), Sound.ENTITY_RABBIT_AMBIENT, 1f, 1f);
            break;
        case CHICKEN:
            e.getWorld().playSound(e.getEyeLocation(), Sound.ENTITY_CHICKEN_AMBIENT, 1f, 1f);
            break;
        }
    }

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
            // Yield
            child.yield = Math.min(mother.yield, father.yield);
            int improveChance = Math.min(75, child.generation * 3);
            if (random.nextInt(100) < improveChance) child.yield += 1;
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
        private int generation = 0, age = 0;
        private int hunger, social, fun, happy; // needs
        private int yield, good;
        private UUID owner = new UUID(0, 0);
        private List<Quirk> quirks = new ArrayList<>();
        private Material toy, treat;
        private EntityType friend;
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
            if (friend == null) friend = customEntity.friends[random.nextInt(customEntity.friends.length)];
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
                    if (entity.getType() == EntityType.SHEEP) entity.playEffect(EntityEffect.SHEEP_EAT);
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
                    if (entity.getType() == EntityType.SHEEP) entity.playEffect(EntityEffect.SHEEP_EAT);
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

        int getMaxAge() {
            if (hasActiveQuirk(Quirk.SHORT_LIVED)) {
                maxAge = 12000;
            } else if (hasActiveQuirk(Quirk.LONG_LIVED)) {
                maxAge = 60000;
            } else {
                maxAge = 32000;
            }
        }

        void die() {
            int effectiveTime = Math.min(age, getMaxAge());
            int effectiveYield = Math.max(1, good * yield / effectiveTime);
            if (hasActiveQuirk(Quirk.
        }

        void onTick() {
            if ((ticks++ % 20) != tickOffset) return;
            unsaved += 1;
            if (unsaved >= 60) save();
            if (Bukkit.getServer().getPlayer(owner) == null) return;
            int maxAge;
            // Modify happiness
            if (hunger > 900) {
                happy -= 1;
            } else if (hunger == 0) {
                happy += 1;
            }
            if (social > 900) {
                happy -= 1;
            } else if (social == 0) {
                happy += 1;
            }
            if (fun > 900) {
                happy -= 1;
            } else if (fun == 0) {
                happy += 1;
            }
            int maxAge = getMaxAge();
            if (age < maxAge) {
                if (happy > 50) good += 1;
                if (happy < 0) good -= 1;
            } else if (age == maxAge) {
                entity.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(1.0);
                entity.setHealth(1.0);
            }
            // Increase needs and age
            age += 1;
            hunger += 1;
            Random random = new Random(System.currentTimeMillis());
            if (hasActiveQuirk(Quirk.GLUTTON)) hunger += 1;
            if (hasActiveQuirk(Quirk.ABSTINENT) && random.nextBoolean()) hunger -= 1;
            social += 1;
            if (hasActiveQuirk(Quirk.SANGUINE)) fun += 1;
            if (hasActiveQuirk(Quirk.SAD) && random.nextBoolean()) fun -= 1;
            // Count nearby entities
            int nearbyFriends = 0;
            int nearbyAnimals = 0;
            int nearbyFamily = 0;
            List<LivingEntity> interactibles = new ArrayList<>();
            List<LivingEntity> friends = new ArrayList<>();
            for (Entity en: entity.getNearbyEntities(8.0, 8.0, 8.0)) {
                if (en instanceof LivingEntity) {
                    LivingEntity e = (LivingEntity)en;
                    if (e.getType() == friend) {
                        nearbyFriends += 1;
                        interactibles.add(e);
                        friends.add(e);
                    }
                    if (e.getType() == entity.getType()) {
                        nearbyFamily += 1;
                        interactibles.add(e);
                    }
                    if (e.getType() != EntityType.ARMOR_STAND) nearbyAnimals += 1;
                }
            }
            // Return and complain if we are crowded
            if (nearbyAnimals > 10) {
                entity.setCustomName(Msg.format("%s &cfeels crowded", name));
                happy -= 1;
                return;
            }
            // Reduce needs
            if (hunger >= 300) {
                eat();
            }
            if (social >= 300) {
                if (hasActiveQuirk(Quirk.SOCIAL) && interactibles.size() >= 2) {
                    social = 0;
                } else if (hasActiveQuirk(Quirk.SHY) && interactibles.size() == 1) {
                    social = 0;
                } else if (interactibles.size() >= 1) {
                    social = 0;
                }
            }
            if (fun >= 300 && friends.size() > 0) {
                fun = 0;
            }
            // Display biggest problem
            if (hunger > 600) {
                entity.setCustomName(Msg.format("%s &cis hungry", name));
            } else if (social > 600) {
                entity.setCustomName(Msg.format("%s &c is lonely", name));
            } else if (fun > 600) {
                entity.setCustomName(Msg.format("%s &c is bored", name));
            } else if (happy > 100) {
                entity.setCustomName(Msg.format("%s &9 is happy", name));
            } else if (happy < -100) {
                entity.setCustomName(Msg.format("%s &9 is unhappy", name));
            } else {
                entity.setCustomName(name);
            }
        }

        @Override
        public void handleMessage(CommandSender sender, String[] msgs) {
            String kind = Msg.camelCase(entity.getType().name());
            switch (msgs[0]) {
            case "eat":
                if (eat()) {
                    sender.sendMessage(kind + " " + name + " ate successfully");
                } else {
                    sender.sendMessage(kind + " " + name + " failed to eat");
                }
                break;
            case "roll":
                roll();
                sender.sendMessage(kind + " " + name + " was rolled");
                break;
            case "info":
                sender.sendMessage(kind + " " + name + ":");
                sender.sendMessage("quirks: " + quirks);
                sender.sendMessage("generation: " + generation);
                sender.sendMessage("age: " + generation);
                sender.sendMessage("happy: " + happy);
                sender.sendMessage("hunger: " + hunger);
                sender.sendMessage("social: " + social);
                sender.sendMessage("fun: " + fun);
                sender.sendMessage("toy: " + toy.name().toLowerCase());
                sender.sendMessage("treat: " + treat.name().toLowerCase());
                break;
            default: break;
            }
        }
    }
}
