package com.winthier.skills;

import com.winthier.custom.CustomPlugin;
import com.winthier.custom.entity.CustomEntity;
import com.winthier.custom.entity.EntityContext;
import com.winthier.custom.entity.EntityWatcher;
import com.winthier.custom.entity.TickableEntity;
import com.winthier.playercache.PlayerCache;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.CropState;
import org.bukkit.DyeColor;
import org.bukkit.EntityEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Cow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Pig;
import org.bukkit.entity.Player;
import org.bukkit.entity.Sheep;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.entity.PlayerLeashEntityEvent;
import org.bukkit.event.entity.SheepRegrowWoolEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerShearEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.material.Crops;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

@RequiredArgsConstructor
public final class RanchEntity implements CustomEntity, TickableEntity {
    final SkillsPlugin plugin;
    static final String CUSTOM_ID = "skills:ranch_animal";
    private long nextTicks = 0;

    @Override
    public String getCustomId() {
        return CUSTOM_ID;
    }

    @Override
    public Animals spawnEntity(Location location) {
        switch ((int)(nextTicks % 3)) {
        case 0: return location.getWorld().spawn(location, Cow.class);
        case 1: return location.getWorld().spawn(location, Sheep.class);
        case 2:
        default: return location.getWorld().spawn(location, Pig.class);
        }
    }

    @Override
    public EntityWatcher createEntityWatcher(Entity e) {
        Watcher result = new Watcher(this, (Animals)e);
        result.tickOffset = nextTicks++;
        if (nextTicks >= 20) nextTicks = 0;
        return result;
    }

    @Override
    public void entityWasDiscovered(EntityWatcher entityWatcher) {
        Watcher watcher = (Watcher)entityWatcher;
        watcher.load();
        watcher.roll();
    }

    @Override
    public void entityWillUnload(EntityWatcher entityWatcher) {
        Watcher watcher = (Watcher)entityWatcher;
        watcher.save();
        watcher.onUnload();
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

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    public void onEntityDeath(EntityDeathEvent event, EntityContext context) {
        Watcher watcher = (Watcher)context.getEntityWatcher();
        LootEntity.Watcher lootWatcher = watcher.die();
        event.getDrops().clear();
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event, EntityContext context) {
        ((Watcher)context.getEntityWatcher()).onInteract(event);
    }

    @EventHandler(ignoreCancelled = true)
    public void onSheepRegrowWool(SheepRegrowWoolEvent event, EntityContext context) {
        Watcher watcher = (Watcher)context.getEntityWatcher();
        if (watcher.sick == 1 || watcher.happy < 0) event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    public void onPlayerShear(PlayerShearEntityEvent event, EntityContext context) {
        Watcher watcher = (Watcher)context.getEntityWatcher();
        watcher.onShear(event);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPlayerLeashEntity(PlayerLeashEntityEvent event, EntityContext context) {
        Watcher watcher = (Watcher)context.getEntityWatcher();
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        if (uuid.equals(watcher.owner)) {
            Location loc = watcher.entity.getLocation();
            watcher.leashX = loc.getBlockX();
            watcher.leashZ = loc.getBlockZ();
            say(watcher.entity);
        }
    }

    /**
     * Called by RanchSkill.onPlayerDropItem()
     */
    void onPlayerDropItem(PlayerDropItemEvent event) {
        final Player player = event.getPlayer();
        for (Entity e: player.getNearbyEntities(6.0, 6.0, 6.0)) {
            EntityWatcher ew = CustomPlugin.getInstance().getEntityManager().getEntityWatcher(e);
            if (ew != null && ew instanceof Watcher) {
                ((Watcher)ew).onDropItem(player, event.getItemDrop().getItemStack());
            }
        }
    }

    /**
     * Called by RanchSkill.onItemSpawn()
     */
    void onEggSpawn(ItemSpawnEvent event) {
        for (Entity e: event.getEntity().getNearbyEntities(0, 0, 0)) {
            if (e.getType() == EntityType.CHICKEN) {
                EntityWatcher tmp = CustomPlugin.getInstance().getEntityManager().getEntityWatcher(e);
                if (tmp instanceof Watcher) {
                    Watcher watcher = (Watcher)tmp;
                    if (watcher.sick == 1) {
                        event.setCancelled(true);
                        return;
                    }
                }
            }
        }
    }

    enum Quirk {
        NOTHING,
        GLUTTON, // Hungry
        ABSTINENT, // Not hungry
        SOCIAL,
        SHY,
        SANGUINE, // Happy
        STOIC,
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
        Material.TOTEM_OF_UNDYING,
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
        float pitch;
        if (e instanceof Ageable && (((Ageable)e).isAdult())) {
            pitch = 1.0f;
        } else {
            pitch = 2.0f;
        }
        switch (e.getType()) {
        case COW:
        case MUSHROOM_COW:
            e.getWorld().playSound(e.getEyeLocation(), Sound.ENTITY_COW_AMBIENT, 1f, pitch);
            break;
        case SHEEP:
            e.getWorld().playSound(e.getEyeLocation(), Sound.ENTITY_SHEEP_AMBIENT, 1f, pitch);
            break;
        case PIG:
            e.getWorld().playSound(e.getEyeLocation(), Sound.ENTITY_PIG_AMBIENT, 1f, pitch);
            break;
        case RABBIT:
            e.getWorld().playSound(e.getEyeLocation(), Sound.ENTITY_RABBIT_AMBIENT, 1f, pitch);
            break;
        case CHICKEN:
            e.getWorld().playSound(e.getEyeLocation(), Sound.ENTITY_CHICKEN_AMBIENT, 1f, pitch);
            break;
        case WOLF:
            e.getWorld().playSound(e.getEyeLocation(), Sound.ENTITY_WOLF_AMBIENT, 1f, pitch);
            break;
        case OCELOT:
            e.getWorld().playSound(e.getEyeLocation(), Sound.ENTITY_CAT_AMBIENT, 1f, pitch);
            break;
        case DONKEY:
            e.getWorld().playSound(e.getEyeLocation(), Sound.ENTITY_DONKEY_AMBIENT, 1f, pitch);
            break;
        case HORSE:
            e.getWorld().playSound(e.getEyeLocation(), Sound.ENTITY_HORSE_AMBIENT, 1f, pitch);
            break;
        case PLAYER:
            e.getWorld().playSound(e.getEyeLocation(), Sound.BLOCK_NOTE_BLOCK_GUITAR, 0.5f, 1f);
            break;
        default: break;
        }
    }

    void entityEatEffect(final LivingEntity e) {
        new BukkitRunnable() {
            private int i = 0;
            final Random random = plugin.random;
            @Override
            public void run() {
                if (!e.isValid()) {
                    cancel();
                    return;
                }
                float pitch = 1.0f + random.nextFloat() * 0.2f;
                switch (i++) {
                case 0: case 3: case 6: case 9: case 12:
                    e.getWorld().playSound(e.getEyeLocation(), Sound.ENTITY_GENERIC_EAT, 0.6f, pitch);
                    break;
                case 20:
                    e.getWorld().playSound(e.getEyeLocation(), Sound.ENTITY_PLAYER_BURP, 0.8f, 0.9f);
                    break;
                case 21:
                    cancel();
                    break;
                default: break;
                }
            }
        }.runTaskTimer(plugin, 1, 1);
    }

    void entityChatterEffect(final LivingEntity e, final List<? extends LivingEntity> fs) {
        new BukkitRunnable() {
            private int i = 0;
            final Random random = plugin.random;
            @Override
            public void run() {
                if (!e.isValid()) {
                    cancel();
                    return;
                }
                for (LivingEntity f: fs) {
                    if (!f.isValid()) {
                        cancel();
                        return;
                    }
                }
                float pitch = 1.0f + random.nextFloat() * 0.2f;
                switch (i++) {
                case 0: case 30:
                    say(e);
                    e.getWorld().spawnParticle(Particle.NOTE, e.getEyeLocation().add(0, 1, 0), 2, 0.5, 0.5, 0.5);
                    break;
                case 15: case 45:
                    for (LivingEntity f: fs) {
                        say(f);
                        f.getWorld().spawnParticle(Particle.NOTE, f.getEyeLocation().add(0, 1, 0), 1, 0.1, 0.1, 0.1);
                    }
                    break;
                case 46: cancel();
                default: break;
                }
            }
        }.runTaskTimer(plugin, 1, 1);
    }

    void specialEffect(Location loc) {
        new BukkitRunnable() {
            private int ticks = 0;
            @Override public void run() {
                switch (ticks++) {
                case 10:
                    loc.getWorld().spawnParticle(Particle.SPELL_MOB, loc.add(0, 0.5, 0), 16, 0.5, 0.5, 0.5, 1.0);
                    loc.getWorld().playSound(loc, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.NEUTRAL, 1.0f, 1.5f);
                    break;
                case 13:
                    loc.getWorld().playSound(loc, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.NEUTRAL, 1.0f, 1.6f);
                    break;
                case 15:
                    loc.getWorld().playSound(loc, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.NEUTRAL, 1.0f, 1.6f);
                    break;
                case 18:
                    loc.getWorld().playSound(loc, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.NEUTRAL, 1.0f, 1.7f);
                    break;
                case 19:
                    cancel();
                default: break;
                }
            }
        }.runTaskTimer(plugin, 1, 1);
    }

    /**
     * Called by RanchSkill when two entities are bred and the rancher
     * has the required perks to improve them.
     */
    Watcher breed(LivingEntity motherEntity, LivingEntity fatherEntity, LivingEntity childEntity, Player rancher, int skillLevel, boolean canSpecial) {
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
            final Random random = plugin.random;
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
            int improveChance = Math.min(75, skillLevel + child.generation);
            if (random.nextInt(100) < improveChance) child.yield += 1;
            if (canSpecial) {
                int specialChance = Math.min(50, child.yield);
                for (int i = 0; i < 3; i += 1) {
                    if (random.nextInt(100) < 50) child.special += 1;
                }
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
        static final String SCOREBOARD_KEY = "RanchingSkill";
        // Interface implementation
        private final RanchEntity customEntity;
        private final Animals entity;
        // Payload
        private String name = null;
        private int generation = 0, age = 0;
        private int hunger, social, fun, happy; // needs
        private int yield, good, sick, special;
        private UUID owner = new UUID(0, 0);
        private List<Quirk> quirks = new ArrayList<>();
        private Material toy, treat;
        private EntityType friend;
        private Gender gender;
        private long tickOffset = 0, ticks = 0;
        private int unsaved = 0;
        private int eatCooldown = 0, chatCooldown = 0, toyCooldown = 0, specialCooldown = 0;
        private int leashX = 0, leashZ = 0;
        private DyeColor originalColor = null;

        boolean hasActiveQuirk(Quirk quirk) {
            int index = quirks.indexOf(quirk);
            return index == 0 || index == 1;
        }

        void load() {
            Map<String, Object> map = CustomPlugin.getInstance().getEntityManager().loadEntityData(entity, SCOREBOARD_KEY);
            if (map == null) return;
            ConfigurationSection config = new YamlConfiguration().createSection("tmp", map);
            name = config.getString("name");
            generation = config.getInt("generation");
            age = config.getInt("age");
            happy = config.getInt("happy");
            good = config.getInt("good");
            yield = config.getInt("yield");
            hunger = config.getInt("hunger");
            social = config.getInt("social");
            fun = config.getInt("fun");
            sick = config.getInt("sick", 0);
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
            String friendString = config.getString("friend");
            if (friendString != null) {
                try {
                    friend = EntityType.valueOf(friendString.toUpperCase());
                } catch (IllegalArgumentException iae) {
                    friend = null;
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
            special = config.getInt("special", 0);
        }

        void save() {
            unsaved = 0;
            Map<String, Object> map = new HashMap<>();
            map.put("name", name);
            map.put("generation", generation);
            map.put("age", age);
            map.put("hunger", hunger);
            map.put("social", social);
            map.put("fun", fun);
            map.put("yield", yield);
            map.put("happy", happy);
            map.put("good", good);
            map.put("owner", owner.toString());
            map.put("quirks", quirks.stream().map(a -> a.name().toLowerCase()).collect(Collectors.toList()));
            map.put("toy", toy.name().toLowerCase());
            map.put("treat", treat.name().toLowerCase());
            map.put("gender", gender.name().toLowerCase());
            if (sick != 0) map.put("sick", sick);
            if (special != 0) map.put("special", special);
            CustomPlugin.getInstance().getEntityManager().saveEntityData(entity, SCOREBOARD_KEY, map);
        }

        void roll() {
            final Random random = customEntity.plugin.random;
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
                    names = customEntity.plugin.getSkill(RanchSkill.class).getFemaleNames();
                } else {
                    names = customEntity.plugin.getSkill(RanchSkill.class).getMaleNames();
                }
                name = names.get(random.nextInt(names.size()));
            }
            if (yield == 0) yield = 5 + random.nextInt(5);
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
                        // if (block.getData() == 7) {
                        if (((Crops)block.getState().getData()).getState() == CropState.RIPE) {
                            block.setType(Material.AIR);
                            hunger = Math.max(0, hunger - 600);
                            block.getWorld().playSound(block.getLocation().add(0.5, 0, 0.5), Sound.BLOCK_GRASS_BREAK, 1.0f, 0.8f);
                            block.getWorld().spawnParticle(Particle.BLOCK_DUST, block.getLocation().add(.5, .5, .5), 16, .5, .5, .5, 0, Material.GRASS_BLOCK.createBlockData());
                            customEntity.entityEatEffect(entity);
                            return true;
                        }
                        break;
                    case BEETROOT:
                        // if (block.getData() == 3) {
                        if (((Crops)block.getState().getData()).getState() == CropState.RIPE) {
                            block.setType(Material.AIR);
                            hunger = Math.max(0, hunger - 600);
                            block.getWorld().playSound(block.getLocation().add(0.5, 0, 0.5), Sound.BLOCK_GRASS_BREAK, 1.0f, 0.8f);
                            block.getWorld().spawnParticle(Particle.BLOCK_DUST, block.getLocation().add(.5, .5, .5), 16, .5, .5, .5, 0, Material.GRASS_BLOCK.createBlockData());
                            customEntity.entityEatEffect(entity);
                            return true;
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
                    case WHEAT:
                        BlockData blockData = block.getBlockData();
                        if (blockData instanceof org.bukkit.block.data.Ageable
                            && ((org.bukkit.block.data.Ageable)blockData).getAge()
                            == ((org.bukkit.block.data.Ageable)blockData).getMaximumAge()) {
                            block.setType(Material.AIR);
                            hunger = Math.max(0, hunger - 600);
                            block.getWorld().playSound(block.getLocation().add(0.5, 0, 0.5), Sound.BLOCK_GRASS_BREAK, 1.0f, 0.8f);
                            block.getWorld().spawnParticle(Particle.BLOCK_DUST, block.getLocation().add(.5, .5, .5), 16, .5, .5, .5, 0, Material.GRASS_BLOCK.createBlockData());
                            customEntity.entityEatEffect(entity);
                            return true;
                        }
                        break;
                    default: break;
                    }
                }
                break;
            case RABBIT:
                for (int i = 0; i < 2; i += 1) {
                    block = entity.getLocation().getBlock().getRelative(0, i, 0);
                    BlockData blockData;
                    switch (block.getType()) {
                    case CARROT:
                        blockData = block.getBlockData();
                        if (blockData instanceof org.bukkit.block.data.Ageable
                            && ((org.bukkit.block.data.Ageable)blockData).getAge()
                            == ((org.bukkit.block.data.Ageable)blockData).getMaximumAge()) {
                            block.setType(Material.AIR);
                            hunger = Math.max(0, hunger - 600);
                            block.getWorld().playSound(block.getLocation().add(0.5, 0, 0.5), Sound.BLOCK_GRASS_BREAK, 1.0f, 0.8f);
                            block.getWorld().spawnParticle(Particle.BLOCK_DUST, block.getLocation().add(.5, .5, .5), 16, .5, .5, .5, 0, Material.GRASS_BLOCK.createBlockData());
                            customEntity.entityEatEffect(entity);
                            return true;
                        }
                        break;
                    case DANDELION:
                        block.setType(Material.AIR);
                        hunger = Math.max(0, hunger - 600);
                        block.getWorld().playSound(block.getLocation().add(0.5, 0, 0.5), Sound.BLOCK_GRASS_BREAK, 1.0f, 0.8f);
                        block.getWorld().spawnParticle(Particle.BLOCK_DUST, block.getLocation().add(.5, .5, .5), 16, .5, .5, .5, 0, Material.GRASS_BLOCK.createBlockData());
                        customEntity.entityEatEffect(entity);
                        return true;
                    default: break;
                    }
                }
                break;
            default: break;
            }
            // Tall grass or mushrooms
            switch (entity.getType()) {
            case COW:
            case SHEEP:
            case RABBIT:
                block = entity.getLocation().getBlock();
                if (block.getType() == Material.TALL_GRASS) {
                    block.setType(Material.AIR);
                    hunger = Math.max(0, hunger - 600);
                    if (entity.getType() == EntityType.SHEEP) entity.playEffect(EntityEffect.SHEEP_EAT);
                    block.getWorld().playSound(block.getLocation().add(0.5, 0, 0.5), Sound.BLOCK_GRASS_BREAK, 1.0f, 0.8f);
                    block.getWorld().spawnParticle(Particle.BLOCK_DUST, block.getLocation().add(.5, .5, .5), 16, .5, .5, .5, 0, Material.GRASS_BLOCK.createBlockData());
                    customEntity.entityEatEffect(entity);
                    return true;
                }
                break;
            case MUSHROOM_COW:
                block = entity.getLocation().getBlock();
                if (block.getType() == Material.RED_MUSHROOM
                    || block.getType() == Material.BROWN_MUSHROOM) {
                    block.setType(Material.AIR);
                    hunger = Math.max(0, hunger - 600);
                    block.getWorld().playSound(block.getLocation().add(0.5, 0, 0.5), Sound.BLOCK_GRASS_BREAK, 1.0f, 0.8f);
                    block.getWorld().spawnParticle(Particle.BLOCK_DUST, block.getLocation().add(.5, .5, .5), 16, .5, .5, .5, 0, Material.GRASS_BLOCK.createBlockData());
                    customEntity.entityEatEffect(entity);
                    return true;
                }
                break;
            default: break;
            }
            switch (entity.getType()) {
            case COW:
            case SHEEP:
                block = entity.getLocation().getBlock().getRelative(0, -1, 0);
                if (block.getType() == Material.GRASS_BLOCK) {
                    block.setType(Material.DIRT);
                    hunger = Math.max(0, hunger - 300);
                    if (entity.getType() == EntityType.SHEEP) entity.playEffect(EntityEffect.SHEEP_EAT);
                    block.getWorld().playSound(block.getLocation().add(0.5, 0, 0.5), Sound.BLOCK_GRASS_BREAK, 1.0f, 0.8f);
                    block.getWorld().spawnParticle(Particle.BLOCK_DUST, block.getLocation().add(.5, 1.5, .5), 16, .5, .5, .5, 0, Material.GRASS_BLOCK.createBlockData());
                    customEntity.entityEatEffect(entity);
                    return true;
                }
                break;
            case CHICKEN:
                block = entity.getLocation().getBlock().getRelative(0, -1, 0);
                if (block.getType() == Material.GRASS_BLOCK
                    || block.getType() == Material.DIRT) {
                    block.setType(Material.COARSE_DIRT);
                    hunger = Math.max(0, hunger - 300);
                    block.getWorld().playSound(block.getLocation().add(0.5, 0, 0.5), Sound.BLOCK_GRASS_BREAK, 1.0f, 0.8f);
                    block.getWorld().spawnParticle(Particle.BLOCK_DUST, block.getLocation().add(.5, 1.5, .5), 16, .5, .5, .5, 0, Material.GRASS_BLOCK.createBlockData());
                    customEntity.entityEatEffect(entity);
                    return true;
                }
                break;
            case MUSHROOM_COW:
                block = entity.getLocation().getBlock().getRelative(0, -1, 0);
                if (block.getType() == Material.MYCELIUM) {
                    block.setType(Material.DIRT);
                    hunger = Math.max(0, hunger - 300);
                    block.getWorld().playSound(block.getLocation().add(0.5, 0, 0.5), Sound.BLOCK_GRASS_BREAK, 1.0f, 0.8f);
                    block.getWorld().spawnParticle(Particle.BLOCK_DUST, block.getLocation().add(.5, 1.5, .5), 16, .5, .5, .5, 0, Material.GRASS_BLOCK.createBlockData());
                    customEntity.entityEatEffect(entity);
                    return true;
                }
                break;
            default: break;
            }
            return false;
        }

        int getMaxAge() {
            if (hasActiveQuirk(Quirk.SHORT_LIVED)) {
                return 12000;
            } else if (hasActiveQuirk(Quirk.LONG_LIVED)) {
                return 60000;
            } else {
                return 32000;
            }
        }

        LootEntity.Watcher die() {
            int effectiveTime = Math.min(age, getMaxAge());
            int effectiveYield;
            if (effectiveTime > 0) {
                effectiveYield = Math.max(1, (good * yield) / effectiveTime);
            } else {
                effectiveYield = 0;
            }
            int foodDrop;
            if (hasActiveQuirk(Quirk.FAT)) {
                foodDrop = effectiveYield * 2;
            } else if (hasActiveQuirk(Quirk.SKINNY)) {
                foodDrop = effectiveYield / 2;
            } else {
                foodDrop = effectiveYield;
            }
            int itemDrop;
            if (hasActiveQuirk(Quirk.RICH)) {
                itemDrop = effectiveYield * 2;
            } else if (hasActiveQuirk(Quirk.POOR)) {
                itemDrop = effectiveYield / 2;
            } else {
                itemDrop = effectiveYield;
            }
            List<ItemStack> loot = new ArrayList<>();
            int foodAmount = 0, itemAmount = 0;
            int specialDropChance = Math.min(75, effectiveYield);
            if (hasActiveQuirk(Quirk.RICH)) specialDropChance += 10;
            if (hasActiveQuirk(Quirk.POOR)) specialDropChance -= 10;
            final Random random = customEntity.plugin.random;
            switch (entity.getType()) {
            case COW:
                for (int i = 0; i < foodDrop; i += 1) foodAmount += 1 + random.nextInt(3); // 1 - 3
                for (int i = 0; i < itemDrop; i += 1) itemAmount += random.nextInt(3); // 0 - 2
                if (foodAmount > 0) loot.add(new ItemStack(Material.BEEF, foodAmount));
                if (itemAmount < 0) loot.add(new ItemStack(Material.LEATHER, itemAmount));
                if (customEntity.plugin.getScore().hasPerk(owner, Perk.RANCH_COW_OXHIDE)) {
                    if (random.nextInt(100) < specialDropChance) loot.add(IngredientItem.Type.OXHIDE.spawn());
                }
                if (customEntity.plugin.getScore().hasPerk(owner, Perk.RANCH_COW_SIRLOIN)) {
                    if (random.nextInt(100) < specialDropChance) loot.add(IngredientItem.Type.SIRLOIN.spawn());
                }
                break;
            case MUSHROOM_COW:
                for (int i = 0; i < foodDrop; i += 1) foodAmount += 1 + random.nextInt(3); // 1 - 3
                for (int i = 0; i < itemDrop; i += 1) itemAmount += random.nextInt(3); // 0 - 2
                if (foodAmount > 0) loot.add(new ItemStack(Material.BEEF, foodAmount));
                if (itemAmount < 0) loot.add(new ItemStack(Material.LEATHER, itemAmount));
                break;
            case PIG:
                for (int i = 0; i < foodDrop; i += 1) foodAmount += 1 + random.nextInt(3); // 1 - 3
                if (foodAmount > 0) loot.add(new ItemStack(Material.PORKCHOP, foodAmount));
                if (customEntity.plugin.getScore().hasPerk(owner, Perk.RANCH_PIG_PIGSKIN)) {
                    if (random.nextInt(100) < specialDropChance) loot.add(IngredientItem.Type.PIGSKIN.spawn());
                }
                if (customEntity.plugin.getScore().hasPerk(owner, Perk.RANCH_PIG_BACON)) {
                    if (random.nextInt(100) < specialDropChance) loot.add(IngredientItem.Type.BACON.spawn());
                }
                break;
            case SHEEP:
                for (int i = 0; i < foodDrop; i += 1) foodAmount += 1 + random.nextInt(3); // 1 - 3
                for (int i = 0; i < itemDrop; i += 1) itemAmount += random.nextInt(3); // 0 - 2 (vanilla: 1)
                if (foodAmount > 0) loot.add(new ItemStack(Material.MUTTON, foodAmount));
                Material woolMaterial;
                switch (((Sheep)entity).getColor()) {
                case WHITE: woolMaterial = Material.WHITE_WOOL; break;
                case ORANGE: woolMaterial = Material.ORANGE_WOOL; break;
                case MAGENTA: woolMaterial = Material.MAGENTA_WOOL; break;
                case LIGHT_BLUE: woolMaterial = Material.LIGHT_BLUE_WOOL; break;
                case YELLOW: woolMaterial = Material.YELLOW_WOOL; break;
                case LIME: woolMaterial = Material.LIME_WOOL; break;
                case PINK: woolMaterial = Material.PINK_WOOL; break;
                case GRAY: woolMaterial = Material.GRAY_WOOL; break;
                case LIGHT_GRAY: woolMaterial = Material.LIGHT_GRAY_WOOL; break;
                case CYAN: woolMaterial = Material.CYAN_WOOL; break;
                case PURPLE: woolMaterial = Material.PURPLE_WOOL; break;
                case BLUE: woolMaterial = Material.BLUE_WOOL; break;
                case BROWN: woolMaterial = Material.BROWN_WOOL; break;
                case GREEN: woolMaterial = Material.GREEN_WOOL; break;
                case RED: woolMaterial = Material.RED_WOOL; break;
                case BLACK: woolMaterial = Material.BLACK_WOOL; break;
                default: woolMaterial = Material.WHITE_WOOL;
                }
                if (itemAmount > 0) loot.add(new ItemStack(woolMaterial, itemAmount));
                break;
            case RABBIT:
                for (int i = 0; i < foodDrop; i += 1) foodAmount += random.nextInt(2); // 0 - 1
                for (int i = 0; i < itemDrop; i += 1) itemAmount += random.nextInt(2); // 0 - 1
                if (foodAmount > 0) loot.add(new ItemStack(Material.RABBIT, foodAmount));
                if (itemAmount > 0) loot.add(new ItemStack(Material.RABBIT_HIDE, itemAmount));
                if (customEntity.plugin.getScore().hasPerk(owner, Perk.RANCH_RABBIT_FOOT)) {
                    if (random.nextInt(100) < specialDropChance) loot.add(new ItemStack(Material.RABBIT_FOOT));
                }
                break;
            case CHICKEN:
                for (int i = 0; i < foodDrop; i += 1) foodAmount += random.nextInt(3); // 0 - 2 (vanilla: 1)
                for (int i = 0; i < itemDrop; i += 1) itemAmount += random.nextInt(3); // 0 - 2
                if (foodAmount > 0) loot.add(new ItemStack(Material.CHICKEN, foodAmount));
                if (itemAmount > 0) loot.add(new ItemStack(Material.FEATHER, itemAmount));
                if (customEntity.plugin.getScore().hasPerk(owner, Perk.RANCH_CHICKEN_DOWN)) {
                    if (random.nextInt(100) < specialDropChance) loot.add(IngredientItem.Type.CHICKEN_DOWN.spawn());
                }
                break;
            default: break;
            }
            // Transfer to LootEntity
            entity.setAI(false);
            entity.setSilent(true);
            entity.setHealth(1.0);
            entity.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(1.0);
            entity.setCustomName("Dinnerbone");
            CustomPlugin.getInstance().getEntityManager().removeEntityWatcher(this);
            CustomPlugin.getInstance().getEntityManager().removeEntityData(entity, SCOREBOARD_KEY);
            LootEntity.Watcher lootWatcher = (LootEntity.Watcher)CustomPlugin.getInstance().getEntityManager().wrapEntity(entity, LootEntity.CUSTOM_ID);
            lootWatcher.setInventoryTitle(name);
            lootWatcher.getOwners().add(owner);
            for (ItemStack item: loot) lootWatcher.getInventory().addItem(item);
            return lootWatcher;
        }

        void onTick() {
            if ((ticks++ % 20) != tickOffset) return;
            unsaved += 1;
            final Random random = customEntity.plugin.random;
            if (unsaved >= 60) save();
            if (happy < 0 && entity.isAdult()) entity.setBreed(false);
            // Rainbow sheep
            if (special > 0 && entity.getType() == EntityType.SHEEP) {
                Sheep sheep = (Sheep)entity;
                if (happy > 0) {
                    if (originalColor == null) originalColor = sheep.getColor();
                    sheep.setColor(DyeColor.values()[random.nextInt(16)]);
                } else {
                    if (originalColor != null) {
                        sheep.setColor(originalColor);
                        originalColor = null;
                    }
                }
            }
            Player player = Bukkit.getServer().getPlayer(owner);
            if (player == null) return;
            if (eatCooldown > 0) eatCooldown -= 1;
            if (chatCooldown > 0) chatCooldown -= 1;
            if (toyCooldown > 0) toyCooldown -= 1;
            if (specialCooldown > 0) specialCooldown -= 1;
            // Modify happiness
            if (hunger > 900) {
                happy -= 1;
            } else if (hunger < 100) {
                happy += 1;
            }
            if (social > 900) {
                happy -= 1;
            } else if (social < 100) {
                happy += 1;
            }
            if (fun > 900) {
                happy -= 1;
            } else if (fun < 100) {
                happy += 1;
            }
            if (happy > 100) happy = 100;
            if (happy < -100) {
                entity.setCustomName(name);
                CustomPlugin.getInstance().getEntityManager().removeEntityWatcher(this);
                CustomPlugin.getInstance().getEntityManager().removeEntityData(entity, SCOREBOARD_KEY);
                return;
            }
            int maxAge = getMaxAge();
            // While aging, determine goodness (yield factor)
            if (age < maxAge) {
                if (happy > 50) good += 1;
                if (happy < 0) good = Math.max(0, good - 1);
            } else if (age == maxAge) {
                entity.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(1.0);
                entity.setHealth(1.0);
                save();
            }
            // Get sick
            if (sick == 0
                && entity.isAdult()
                && hasActiveQuirk(Quirk.SICKISH)
                && random.nextInt(1000) == 0) {
                sick = 1;
            }
            // Increase needs and age
            age += 1;
            hunger += 1;
            if (hasActiveQuirk(Quirk.GLUTTON)) hunger += 1;
            if (hasActiveQuirk(Quirk.ABSTINENT) && random.nextBoolean()) hunger -= 1;
            if (sick == 1) hunger += 1;
            social += 1;
            fun += 1;
            if (hasActiveQuirk(Quirk.SANGUINE)) fun += 1;
            if (hasActiveQuirk(Quirk.STOIC) && random.nextBoolean()) fun -= 1;
            // Count nearby entities
            List<Animals> interactibles = new ArrayList<>();
            List<Animals> friends = new ArrayList<>();
            List<Animals> animals = new ArrayList<>();
            for (Entity en: entity.getNearbyEntities(6.0, 6.0, 6.0)) {
                if (en instanceof Animals
                    && !en.equals(entity)) {
                    Animals e = (Animals)en;
                    if (e.getType() == friend
                        && entity.hasLineOfSight(e)) {
                        interactibles.add(e);
                        friends.add(e);
                    }
                    if (e.getType() == entity.getType()
                        && entity.hasLineOfSight(e)) {
                        interactibles.add(e);
                    }
                    if (e.getType() != EntityType.ARMOR_STAND) {
                        animals.add(e);
                    }
                }
            }
            // Transmit sickness
            if (sick == 1) {
                if (entity.hasPotionEffect(PotionEffectType.REGENERATION)) {
                    sick = 2;
                    save();
                } else {
                    if (entity.isAdult()) entity.setBreed(false);
                    if (entity instanceof Sheep) {
                        Sheep sheep = (Sheep)entity;
                        if (!sheep.isSheared()) sheep.setSheared(true);
                    } else if (entity.getType() == EntityType.CHICKEN) {
                        Msg.entityData(entity, "EggLayTime", 12000);
                    }
                    entity.getWorld().spawnParticle(Particle.CRIT_MAGIC, entity.getEyeLocation(), 2, 0.5, 0.5, 0.5, 0);
                    entity.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 40, 0, true));
                    for (Animals animal: animals) {
                        EntityWatcher tmp = CustomPlugin.getInstance().getEntityManager().getEntityWatcher(animal);
                        if (animal.isAdult() && tmp instanceof Watcher) {
                            Watcher transmittee = (Watcher)tmp;
                            if (!transmittee.hasActiveQuirk(Quirk.HEALTHY)
                                && transmittee.sick == 0
                                && random.nextInt(10) == 0) {
                                transmittee.sick = 1;
                                say(animal);
                                transmittee.save();
                            }
                        }
                    }
                }
            }
            // Return and complain if we are crowded
            if (animals.size() > 8) {
                if (entity.isAdult()) {
                    entity.setCustomName(Msg.format("%s &cfeels crowded", name));
                    happy -= 1;
                } else {
                    entity.setCustomName(name);
                    CustomPlugin.getInstance().getEntityManager().removeEntityWatcher(this);
                    CustomPlugin.getInstance().getEntityManager().removeEntityData(entity, SCOREBOARD_KEY);
                }
                return;
            }
            // Reduce needs
            if (hunger >= 200 && eatCooldown == 0) {
                if (eat()) {
                    eatCooldown = 10;
                    if (sick != 1 && entity instanceof Sheep) ((Sheep)entity).setSheared(false);
                }
            }
            if (social >= 100 && chatCooldown == 0) {
                if (hasActiveQuirk(Quirk.SOCIAL) && interactibles.size() >= 2) {
                    social = Math.max(0, social - 100);
                    customEntity.entityChatterEffect(entity, interactibles);
                    chatCooldown = 10;
                } else if (hasActiveQuirk(Quirk.SHY) && interactibles.size() == 1) {
                    social = Math.max(0, social - 100);
                    customEntity.entityChatterEffect(entity, interactibles);
                    chatCooldown = 10;
                } else if (interactibles.size() >= 1) {
                    social = Math.max(0, social - 100);
                    customEntity.entityChatterEffect(entity, interactibles);
                    chatCooldown = 10;
                }
            }
            if (fun >= 300 && chatCooldown == 0 && friends.size() > 0) {
                fun = Math.max(0, fun - 300);
                customEntity.entityChatterEffect(entity, friends);
                chatCooldown = 10;
            }
            // Special leash activities
            if (special > 0 && sick != 1 && happy > 0 && specialCooldown == 0
                && entity.isAdult() && entity.isLeashed() && player.equals(entity.getLeashHolder())) {
                switch (entity.getType()) {
                case PIG:
                case CHICKEN:
                    Location loc = entity.getLocation();
                    int x = loc.getBlockX();
                    int z = loc.getBlockZ();
                    if (Math.abs(x - leashX) > 2 && Math.abs(z - leashZ) > 2) {
                        leashX = x;
                        leashZ = z;
                        if (random.nextInt(3) == 0) {
                            special -= 1;
                            specialCooldown = 10;
                            save();
                            switch (entity.getType()) {
                            case PIG:
                                IngredientItem.Type.TRUFFLE.drop(loc);
                                loc.getWorld().playSound(loc, Sound.BLOCK_GRASS_BREAK, 1.0f, 0.8f);
                                loc.getWorld().spawnParticle(Particle.BLOCK_DUST, loc, 16, .5, .5, .5, 0, Material.GRASS_BLOCK.createBlockData());
                                break;
                            case CHICKEN:
                                IngredientItem.Type.GOLDEN_EGG.drop(loc);
                                loc.getWorld().playSound(loc, Sound.ENTITY_CHICKEN_EGG, 1.0f, 1.0f);
                                break;
                            default: break;
                            }
                            customEntity.specialEffect(loc);
                            say(entity);
                        }
                    }
                    break;
                default: break;
                }
            }
            // Display biggest problem
            if (sick == 1) {
                entity.setCustomName(Msg.format("%s &2is sick", name));
            } else if (hunger > 600) {
                entity.setCustomName(Msg.format("%s &cis hungry", name));
            } else if (social > 600) {
                entity.setCustomName(Msg.format("%s &cis lonely", name));
            } else if (fun > 600) {
                entity.setCustomName(Msg.format("%s &cis bored", name));
            } else if (happy < 0) {
                entity.setCustomName(Msg.format("%s &cis unhappy", name));
            } else if (age > maxAge) {
                entity.setCustomName(Msg.format("%s &7is old", name));
            } else {
                entity.setCustomName(name);
            }
        }

        @Override
        public void handleMessage(CommandSender sender, String[] msgs) {
            String kind = Msg.capitalize(entity.getType().name());
            switch (msgs[0]) {
            case "debug":
                sender.sendMessage(RanchEntity.CUSTOM_ID + " " + Msg.capitalize(entity.getType().name()) + " received debug message");
                if (sender instanceof Player) owner = ((Player)sender).getUniqueId();
                special = 3;
                yield = 99;
                age = getMaxAge();
                good = age;
                happy = 100;
                hunger = 0;
                fun = 0;
                social = 0;
                save();
                break;
            case "eat":
                if (eat()) {
                    sender.sendMessage(kind + " " + name + " ate successfully");
                    if (sick != 1 && entity instanceof Sheep) ((Sheep)entity).setSheared(false);
                } else {
                    sender.sendMessage(kind + " " + name + " failed to eat");
                }
                break;
            case "roll":
                if (owner.equals(new UUID(0, 0))
                    && sender instanceof Player) {
                    owner = ((Player)sender).getUniqueId();
                }
                roll();
                save();
                sender.sendMessage(kind + " " + name + " was rolled");
                break;
            case "info":
                sender.sendMessage(kind + " " + name + ":");
                sender.sendMessage("owner: " + owner);
                sender.sendMessage("quirks: " + quirks);
                sender.sendMessage("generation: " + generation);
                sender.sendMessage("age: " + age);
                sender.sendMessage("yield: " + yield);
                sender.sendMessage("happy: " + happy);
                sender.sendMessage("good: " + good);
                sender.sendMessage("hunger: " + hunger);
                sender.sendMessage("social: " + social);
                sender.sendMessage("fun: " + fun);
                sender.sendMessage("toy: " + toy.name().toLowerCase());
                sender.sendMessage("friend: " + friend.name().toLowerCase());
                sender.sendMessage("treat: " + treat.name().toLowerCase());
                sender.sendMessage("sick: " + sick);
                sender.sendMessage("special: " + special);
                break;
            default: break;
            }
        }

        void onInteract(PlayerInteractEntityEvent event) {
            final Player player = event.getPlayer();
            final UUID uuid = player.getUniqueId();
            final ItemStack item;
            if (event.getHand() == EquipmentSlot.HAND) {
                item = player.getInventory().getItemInMainHand();
            } else {
                item = player.getInventory().getItemInOffHand();
            }
            boolean isFood = false, isTreat = false;
            if (item == null || item.getType() == Material.AIR) {
                if (event.getHand() == EquipmentSlot.HAND && !entity.isLeashed() && !entity.getLeashHolder().equals(player)) {
                    if (customEntity.plugin.getScore().hasPerk(player.getUniqueId(), Perk.RANCH_CARRY) && owner.equals(uuid)) {
                        if (player.getPassengers().isEmpty()) {
                            player.addPassenger(entity);
                            Msg.actionBar(player, "Click again to drop %s", name);
                        } else {
                            player.eject();
                        }
                        event.setCancelled(true);
                    }
                }
            } else if (item.getType() == treat) {
                isFood = true;
                isTreat = true;
            } else if (item.getType() == Material.BOOK) {
                if (entity.isAdult()
                    && event.getHand() == EquipmentSlot.HAND
                    && new ItemStack(Material.BOOK).equals(item)
                    && customEntity.plugin.getScore().hasPerk(uuid, Perk.RANCH_INSPECT_BOOK)) {
                    ItemStack newItem = new ItemStack(Material.WRITTEN_BOOK);
                    BookMeta meta = (BookMeta)newItem.getItemMeta();
                    StringBuilder sb = new StringBuilder();
                    if (gender == Gender.FEMALE) {
                        sb.append("&d&l").append(name).append("&d\u2640 the ").append(Msg.capitalize(Msg.niceEnumName(entity.getType())));
                    } else {
                        sb.append("&9&l").append(name).append("&9\u2642 the ").append(Msg.capitalize(Msg.niceEnumName(entity.getType())));
                    }
                    sb.append("\n");
                    sb.append("\n&rOwner &7").append(PlayerCache.nameForUuid(owner));
                    sb.append("\n&rAge &7").append(age / 60 / 20 + 1).append(" days");
                    sb.append("\n&rGeneration &7").append(generation);
                    if (customEntity.plugin.getScore().hasPerk(uuid, Perk.RANCH_INSPECT_FAVORITE)) {
                        sb.append("\n&rToy &7").append(Msg.capitalize(Msg.niceEnumName(toy)));
                        sb.append("\n&rTreat &7").append(Msg.capitalize(Msg.niceEnumName(treat)));
                        sb.append("\n&rFriend &7").append(Msg.capitalize(Msg.niceEnumName(friend)));
                    }
                    if (customEntity.plugin.getScore().hasPerk(uuid, Perk.RANCH_INSPECT_QUIRK)) {
                        sb.append("\n&rTraits&7");
                        for (int i = 0; i < 2 && i < quirks.size(); i += 1) {
                            Quirk quirk = quirks.get(i);
                            if (quirk != Quirk.NOTHING) {
                                sb.append(" ").append(Msg.capitalize(Msg.niceEnumName(quirk)));
                            }
                        }
                    }
                    sb.append("\n");
                    sb.append("\n&rRating ");
                    if (yield <= 10) {
                        sb.append("&8Average");
                    } else if (yield <= 20) {
                        sb.append("&7Improved");
                    } else if (yield <= 40) {
                        sb.append("&9Superb");
                    } else if (yield <= 64) {
                        sb.append("&6Outstanding");
                    } else {
                        sb.append("&6&lUnvelievable");
                    }
                    meta.setPages(Msg.format(sb.toString()));
                    meta.setTitle(name + " the " + Msg.capitalize(Msg.niceEnumName(entity.getType())));
                    meta.setAuthor(player.getName());
                    newItem.setItemMeta(meta);
                    // Main hand check is in the main if clause
                    player.getInventory().setItemInMainHand(newItem);
                    player.playSound(player.getEyeLocation(), Sound.ITEM_ARMOR_EQUIP_ELYTRA, 0.5f, 2.0f);
                }
            } else if (item.getType() == Material.NAME_TAG) {
                if (uuid.equals(owner)) {
                    ItemMeta itemMeta = item.getItemMeta();
                    if (itemMeta != null) {
                        String itemName = itemMeta.getDisplayName();
                        if (itemName != null && !itemName.isEmpty()) {
                            this.name = itemName;
                        }
                    }
                } else {
                    event.setCancelled(true);
                }
            } else if (item.getType() == Material.BUCKET) {
                if (entity.getType() == EntityType.COW && sick == 1) {
                    event.setCancelled(true);
                }
            } else if (item.getType() == Material.GLASS_BOTTLE) {
                // Cow special
                if (event.getHand() == EquipmentSlot.HAND && entity.getType() == EntityType.COW && sick != 1 && entity.isAdult() && special > 0 && specialCooldown == 0 && happy > 0) {
                    special -= 1;
                    save();
                    specialCooldown = 10;
                    event.setCancelled(true);
                    item.setAmount(item.getAmount() - 1);
                    entity.getWorld().playSound(entity.getEyeLocation(), Sound.ENTITY_COW_MILK, 1.0f, 1.0f);
                    say(entity);
                    for (ItemStack drop: player.getInventory().addItem(IngredientItem.Type.FRESH_MILK.spawn()).values()) {
                        player.getWorld().dropItem(player.getEyeLocation(), drop);
                    }
                    customEntity.specialEffect(entity.getEyeLocation());
                }
            } else {
                switch (entity.getType()) {
                case COW:
                    switch (item.getType()) {
                    case WHEAT:
                        isFood = true;
                    default: break;
                    }
                    break;
                case MUSHROOM_COW:
                    switch (item.getType()) {
                    case WHEAT:
                        isFood = true;
                    default: break;
                    }
                    break;
                case PIG:
                    switch (item.getType()) {
                    case POTATO:
                    case CARROT:
                    case BEETROOT:
                        isFood = true;
                    default: break;
                    }
                    break;
                case CHICKEN:
                    switch (item.getType()) {
                    case WHEAT_SEEDS:
                    case BEETROOT_SEEDS:
                    case MELON_SEEDS:
                    case PUMPKIN_SEEDS:
                        isFood = true;
                    default: break;
                    }
                    break;
                case SHEEP:
                    switch (item.getType()) {
                    case WHEAT:
                        isFood = true;
                    default: break;
                    }
                    break;
                case RABBIT:
                    switch (item.getType()) {
                    case CARROT:
                    case DANDELION:
                        isFood = true;
                    default: break;
                    }
                    break;
                default: break;
                }
            }
            if (isFood) {
                if (hunger < 100 || eatCooldown > 0) return;
                if (isTreat) {
                    hunger = 0;
                } else {
                    hunger = Math.max(0, hunger - 100);
                }
                eatCooldown = 5;
                item.setAmount(item.getAmount() - 1);
                customEntity.entityEatEffect(entity);
                event.setCancelled(true);
            }
        }

        void onDropItem(Player player, ItemStack item) {
            if (item.getType() == toy && fun > 30 && toyCooldown == 0) {
                toyCooldown = 5;
                fun = Math.max(0, fun - 100);
                customEntity.entityChatterEffect(entity, Arrays.asList(player));
            }
        }

        void onShear(PlayerShearEntityEvent event) {
            if (sick == 1) {
                event.setCancelled(true);
            } else if (special > 0 && entity.getType() == EntityType.SHEEP) {
                special -= 1;
                save();
                if (special == 0 && originalColor != null) {
                    ((Sheep)entity).setColor(originalColor);
                    originalColor = null;
                }
                final Random random = customEntity.plugin.random;
                for (int i = 0; i < 16; i += 1) {
                    Material woolMaterial;
                    switch (i) {
                    case 0: woolMaterial = Material.WHITE_WOOL; break;
                    case 1: woolMaterial = Material.ORANGE_WOOL; break;
                    case 2: woolMaterial = Material.MAGENTA_WOOL; break;
                    case 3: woolMaterial = Material.LIGHT_BLUE_WOOL; break;
                    case 4: woolMaterial = Material.YELLOW_WOOL; break;
                    case 5: woolMaterial = Material.LIME_WOOL; break;
                    case 6: woolMaterial = Material.PINK_WOOL; break;
                    case 7: woolMaterial = Material.GRAY_WOOL; break;
                    case 8: woolMaterial = Material.LIGHT_GRAY_WOOL; break;
                    case 9: woolMaterial = Material.CYAN_WOOL; break;
                    case 10: woolMaterial = Material.PURPLE_WOOL; break;
                    case 11: woolMaterial = Material.BLUE_WOOL; break;
                    case 12: woolMaterial = Material.BROWN_WOOL; break;
                    case 13: woolMaterial = Material.GREEN_WOOL; break;
                    case 14: woolMaterial = Material.RED_WOOL; break;
                    case 15: woolMaterial = Material.BLACK_WOOL; break;
                    default: woolMaterial = Material.WHITE_WOOL;
                    }
                    ItemStack item = new ItemStack(woolMaterial, 1 + random.nextInt(3));
                    entity.getWorld().dropItemNaturally(entity.getEyeLocation(), item);
                }
                say(entity);
                customEntity.specialEffect(entity.getEyeLocation());
            }
        }

        void onUnload() {
            if (entity instanceof Sheep && originalColor != null) {
                Sheep sheep = (Sheep)entity;
                sheep.setColor(originalColor);
                originalColor = null;
            }
        }
    }
}
