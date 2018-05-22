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
import org.bukkit.EntityEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
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
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;
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
        Watcher result = new Watcher(this, (Animals)e);
        result.tickOffset = nextTicks++;
        if (nextTicks >= 20) nextTicks = 0;
        return result;
    }

    @Override
    public void entityWasDiscovered(EntityWatcher watcher) {
        ((Watcher)watcher).load();
        ((Watcher)watcher).roll();
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

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onEntityDeath(EntityDeathEvent event, EntityContext context) {
        Watcher watcher = (Watcher)context.getEntityWatcher();
        LootEntity.Watcher lootWatcher = watcher.die();
        event.getDrops().clear();
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event, EntityContext context) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        ((Watcher)context.getEntityWatcher()).onInteract(event);
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

    void pickup(Player player, Entity entity) {
        if (!player.getPassengers().isEmpty()) {
            player.eject();
            player.playSound(player.getEyeLocation(), Sound.ENTITY_HORSE_SADDLE, 1f, 1.9f);
            return;
        }
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item != null && item.getType() != Material.AIR) return;
        if (!(entity instanceof Animals)) return;
        if (!entity.getPassengers().isEmpty()) return;
        if (entity.getVehicle() != null) return;
        EntityWatcher ew = CustomPlugin.getInstance().getEntityManager().getEntityWatcher(entity);
        boolean doPickup = false;
        if (ew instanceof Watcher) {
            Watcher watcher = (Watcher)ew;
            doPickup = player.getUniqueId().equals(watcher.owner);
        } else {
            doPickup = true;
        }
        if (doPickup) {
            player.playSound(player.getEyeLocation(), Sound.ENTITY_HORSE_SADDLE, 1f, 1.5f);
            say((Animals)entity);
            player.eject();
            player.addPassenger(entity);
            Msg.actionBar(player, "Click again to drop the %s", Msg.niceEnumName(entity.getType()));
        }
    }

    enum Quirk {
        NOTHING,
        EXPLOSIVE,
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
        Material.TOTEM,
        Material.FLOWER_POT_ITEM,
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
        case WOLF:
            e.getWorld().playSound(e.getEyeLocation(), Sound.ENTITY_WOLF_AMBIENT, 1f, 1f);
            break;
        case OCELOT:
            e.getWorld().playSound(e.getEyeLocation(), Sound.ENTITY_CAT_AMBIENT, 1f, 1f);
            break;
        case DONKEY:
            e.getWorld().playSound(e.getEyeLocation(), Sound.ENTITY_DONKEY_AMBIENT, 1f, 1f);
            break;
        case HORSE:
            e.getWorld().playSound(e.getEyeLocation(), Sound.ENTITY_HORSE_AMBIENT, 1f, 1f);
            break;
        case PLAYER:
            e.getWorld().playSound(e.getEyeLocation(), Sound.BLOCK_NOTE_GUITAR, 0.5f, 1f);
            break;
        }
    }

    void entityEatEffect(final LivingEntity e) {
        new BukkitRunnable() {
            int i = 0;
            final Random random = new Random(System.currentTimeMillis());
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

    void entityChatterEffect(final LivingEntity e, final List<LivingEntity> fs) {
        new BukkitRunnable() {
            int i = 0;
            final Random random = new Random(System.currentTimeMillis());
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
            int improveChance = Math.min(75, 1 + child.generation * 3);
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
        private final Animals entity;
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
        private int eatCooldown = 0, chatCooldown = 0, toyCooldown = 0;

        boolean hasActiveQuirk(Quirk quirk) {
            int index = quirks.indexOf(quirk);
            return index == 0 || index == 1;
        }

        void load() {
            Map<String, Object> map = customEntity.plugin.getScoreboardJSON(entity, SCOREBOARD_KEY);
            if (map == null) return;
            ConfigurationSection config = new YamlConfiguration().createSection("tmp", map);
            name = config.getString("name");
            generation = config.getInt("gen");
            age = config.getInt("age");
            happy = config.getInt("happy");
            good = config.getInt("good");
            yield = config.getInt("yield");
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
        }

        void save() {
            unsaved = 0;
            Map<String, Object> map = new HashMap<>();
            map.put("name", name);
            map.put("gen", generation);
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
            customEntity.plugin.storeScoreboardJSON(entity, SCOREBOARD_KEY, map);
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
                        if (block.getData() == 7) {
                            block.setType(Material.AIR);
                            hunger = Math.max(0, hunger - 600);
                            block.getWorld().playSound(block.getLocation().add(0.5, 0, 0.5), Sound.BLOCK_GRASS_BREAK, 1.0f, 0.8f);
                            block.getWorld().spawnParticle(Particle.BLOCK_DUST, block.getLocation().add(.5, .5, .5), 16, .5, .5, .5, 0, Material.GRASS.getNewData((byte)0));
                            customEntity.entityEatEffect(entity);
                            return true;
                        }
                        break;
                    case BEETROOT_BLOCK:
                        if (block.getData() == 3) {
                            block.setType(Material.AIR);
                            hunger = Math.max(0, hunger - 600);
                            block.getWorld().playSound(block.getLocation().add(0.5, 0, 0.5), Sound.BLOCK_GRASS_BREAK, 1.0f, 0.8f);
                            block.getWorld().spawnParticle(Particle.BLOCK_DUST, block.getLocation().add(.5, .5, .5), 16, .5, .5, .5, 0, Material.GRASS.getNewData((byte)0));
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
                    case CROPS:
                        if (block.getData() == 7) {
                            block.setType(Material.AIR);
                            hunger = Math.max(0, hunger - 600);
                            block.getWorld().playSound(block.getLocation().add(0.5, 0, 0.5), Sound.BLOCK_GRASS_BREAK, 1.0f, 0.8f);
                            block.getWorld().spawnParticle(Particle.BLOCK_DUST, block.getLocation().add(.5, .5, .5), 16, .5, .5, .5, 0, Material.GRASS.getNewData((byte)0));
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
                    switch (block.getType()) {
                    case CARROT:
                        if (block.getData() == 7) {
                            block.setType(Material.AIR);
                            hunger = Math.max(0, hunger - 600);
                            block.getWorld().playSound(block.getLocation().add(0.5, 0, 0.5), Sound.BLOCK_GRASS_BREAK, 1.0f, 0.8f);
                            block.getWorld().spawnParticle(Particle.BLOCK_DUST, block.getLocation().add(.5, .5, .5), 16, .5, .5, .5, 0, Material.GRASS.getNewData((byte)0));
                            customEntity.entityEatEffect(entity);
                            return true;
                        }
                        break;
                    case YELLOW_FLOWER:
                        if (block.getData() == 0) {
                            block.setType(Material.AIR);
                            hunger = Math.max(0, hunger - 600);
                            block.getWorld().playSound(block.getLocation().add(0.5, 0, 0.5), Sound.BLOCK_GRASS_BREAK, 1.0f, 0.8f);
                            block.getWorld().spawnParticle(Particle.BLOCK_DUST, block.getLocation().add(.5, .5, .5), 16, .5, .5, .5, 0, Material.GRASS.getNewData((byte)0));
                            customEntity.entityEatEffect(entity);
                            return true;
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
                    block.getWorld().playSound(block.getLocation().add(0.5, 0, 0.5), Sound.BLOCK_GRASS_BREAK, 1.0f, 0.8f);
                    block.getWorld().spawnParticle(Particle.BLOCK_DUST, block.getLocation().add(.5, .5, .5), 16, .5, .5, .5, 0, Material.GRASS.getNewData((byte)0));
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
                    block.getWorld().spawnParticle(Particle.BLOCK_DUST, block.getLocation().add(.5, .5, .5), 16, .5, .5, .5, 0, Material.GRASS.getNewData((byte)0));
                    customEntity.entityEatEffect(entity);
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
                    block.getWorld().playSound(block.getLocation().add(0.5, 0, 0.5), Sound.BLOCK_GRASS_BREAK, 1.0f, 0.8f);
                    block.getWorld().spawnParticle(Particle.BLOCK_DUST, block.getLocation().add(.5, 1.5, .5), 16, .5, .5, .5, 0, Material.GRASS.getNewData((byte)0));
                    customEntity.entityEatEffect(entity);
                    return true;
                }
                break;
            case CHICKEN:
                block = entity.getLocation().getBlock().getRelative(0, -1, 0);
                if (block.getType() == Material.GRASS
                    || block.getType() == Material.DIRT) {
                    block.setTypeIdAndData(Material.DIRT.getId(), (byte)3, true); // Coarse dirt
                    hunger = Math.max(0, hunger - 300);
                    block.getWorld().playSound(block.getLocation().add(0.5, 0, 0.5), Sound.BLOCK_GRASS_BREAK, 1.0f, 0.8f);
                    block.getWorld().spawnParticle(Particle.BLOCK_DUST, block.getLocation().add(.5, 1.5, .5), 16, .5, .5, .5, 0, Material.GRASS.getNewData((byte)0));
                    customEntity.entityEatEffect(entity);
                    return true;
                }
                break;
            case MUSHROOM_COW:
                block = entity.getLocation().getBlock().getRelative(0, -1, 0);
                if (block.getType() == Material.MYCEL) {
                    block.setType(Material.DIRT);
                    hunger = Math.max(0, hunger - 300);
                    block.getWorld().playSound(block.getLocation().add(0.5, 0, 0.5), Sound.BLOCK_GRASS_BREAK, 1.0f, 0.8f);
                    block.getWorld().spawnParticle(Particle.BLOCK_DUST, block.getLocation().add(.5, 1.5, .5), 16, .5, .5, .5, 0, Material.GRASS.getNewData((byte)0));
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
            Random random = new Random(System.currentTimeMillis());
            int foodAmount = 0, itemAmount = 0;
            switch (entity.getType()) {
            case COW:
            case MUSHROOM_COW:
                for (int i = 0; i < foodDrop; i += 1) foodAmount += 1 + random.nextInt(3); // 1 - 3
                for (int i = 0; i < itemDrop; i += 1) itemAmount += random.nextInt(3); // 0 - 2
                if (foodAmount > 0) loot.add(new ItemStack(Material.RAW_BEEF, foodAmount));
                if (itemAmount < 0) loot.add(new ItemStack(Material.LEATHER, itemAmount));
                break;
            case PIG:
                for (int i = 0; i < foodDrop; i += 1) foodAmount += 1 + random.nextInt(3); // 1 - 3
                if (foodAmount > 0) loot.add(new ItemStack(Material.PORK, foodAmount));
                break;
            case SHEEP:
                for (int i = 0; i < foodDrop; i += 1) foodAmount += 1 + random.nextInt(3); // 1 - 3
                for (int i = 0; i < itemDrop; i += 1) itemAmount += random.nextInt(3); // 0 - 2 (vanilla: 1)
                if (foodAmount > 0) loot.add(new ItemStack(Material.MUTTON, foodAmount));
                if (itemAmount > 0) loot.add(new ItemStack(Material.WOOL, itemAmount)); // TODO colorize
                break;
            case RABBIT:
                for (int i = 0; i < foodDrop; i += 1) foodAmount += random.nextInt(2); // 0 - 1
                for (int i = 0; i < itemDrop; i += 1) itemAmount += random.nextInt(2); // 0 - 1
                if (foodAmount > 0) loot.add(new ItemStack(Material.RABBIT, foodAmount));
                if (itemAmount > 0) loot.add(new ItemStack(Material.RABBIT_HIDE, itemAmount));
                break;
            case CHICKEN:
                for (int i = 0; i < foodDrop; i += 1) foodAmount += random.nextInt(3); // 0 - 2 (vanilla: 1)
                for (int i = 0; i < itemDrop; i += 1) itemAmount += random.nextInt(3); // 0 - 2
                if (foodAmount > 0) loot.add(new ItemStack(Material.RAW_CHICKEN, foodAmount));
                if (itemAmount > 0) loot.add(new ItemStack(Material.FEATHER, itemAmount));
                break;
            }
            // Transfer to LootEntity
            entity.setAI(false);
            entity.setSilent(true);
            entity.setHealth(1.0);
            entity.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(1.0);
            entity.setCustomName("Dinnerbone");
            CustomPlugin.getInstance().getEntityManager().removeEntityWatcher(this);
            LootEntity.Watcher lootWatcher = (LootEntity.Watcher)CustomPlugin.getInstance().getEntityManager().wrapEntity(entity, LootEntity.CUSTOM_ID);
            lootWatcher.setInventoryTitle(name);
            lootWatcher.getOwners().add(owner);
            for (ItemStack item: loot) lootWatcher.getInventory().addItem(item);
            return lootWatcher;
        }

        void onTick() {
            if ((ticks++ % 20) != tickOffset) return;
            unsaved += 1;
            if (unsaved >= 60) save();
            if (happy < 0 && entity.isAdult()) entity.setBreed(false);
            if (Bukkit.getServer().getPlayer(owner) == null) return;
            if (eatCooldown > 0) eatCooldown -= 1;
            if (chatCooldown > 0) chatCooldown -= 1;
            if (toyCooldown > 0) toyCooldown -= 1;
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
            // Increase needs and age
            age += 1;
            hunger += 1;
            Random random = new Random(System.currentTimeMillis());
            if (hasActiveQuirk(Quirk.GLUTTON)) hunger += 1;
            if (hasActiveQuirk(Quirk.ABSTINENT) && random.nextBoolean()) hunger -= 1;
            social += 1;
            fun += 1;
            if (hasActiveQuirk(Quirk.SANGUINE)) fun += 1;
            if (hasActiveQuirk(Quirk.STOIC) && random.nextBoolean()) fun -= 1;
            // Count nearby entities
            int nearbyFriends = 0;
            int nearbyAnimals = 0;
            int nearbyFamily = 0;
            List<LivingEntity> interactibles = new ArrayList<>();
            List<LivingEntity> friends = new ArrayList<>();
            for (Entity en: entity.getNearbyEntities(6.0, 6.0, 6.0)) {
                if (en instanceof LivingEntity
                    && !en.equals(entity)) {
                    LivingEntity e = (LivingEntity)en;
                    if (e.getType() == friend
                        && entity.hasLineOfSight(e)) {
                        nearbyFriends += 1;
                        interactibles.add(e);
                        friends.add(e);
                    }
                    if (e.getType() == entity.getType()
                        && entity.hasLineOfSight(e)) {
                        nearbyFamily += 1;
                        interactibles.add(e);
                    }
                    if (e.getType() != EntityType.ARMOR_STAND) nearbyAnimals += 1;
                }
            }
            // Return and complain if we are crowded
            if (nearbyAnimals > 8) {
                entity.setCustomName(Msg.format("%s &cfeels crowded", name));
                happy -= 1;
                return;
            }
            // Reduce needs
            if (hunger >= 200 && eatCooldown == 0) {
                if (eat()) eatCooldown = 10;
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
            // Display biggest problem
            if (hunger > 600) {
                entity.setCustomName(Msg.format("%s &cis hungry", name));
            } else if (social > 600) {
                entity.setCustomName(Msg.format("%s &cis lonely", name));
            } else if (fun > 600) {
                entity.setCustomName(Msg.format("%s &cis bored", name));
            } else if (happy > 100) {
                entity.setCustomName(Msg.format("%s &9is happy", name));
            } else if (happy < -100) {
                entity.setCustomName(Msg.format("%s &cis unhappy", name));
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
                generation = 5;
                yield = 32;
                save();
                break;
            case "eat":
                if (eat()) {
                    sender.sendMessage(kind + " " + name + " ate successfully");
                } else {
                    sender.sendMessage(kind + " " + name + " failed to eat");
                }
                break;
            case "roll":
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
                break;
            default: break;
            }
        }

        void onInteract(PlayerInteractEntityEvent event) {
            final Player player = event.getPlayer();
            final UUID uuid = player.getUniqueId();
            final ItemStack item = player.getInventory().getItemInMainHand();
            boolean isFood = false, isTreat = false;
            if (item == null || item.getType() == Material.AIR) {
                return;
            } else if (item.getType() == treat) {
                isFood = true;
                isTreat = true;
            } else if (item.getType() == Material.BOOK) {
                if (customEntity.plugin.getScore().hasPerk(uuid, Perk.RANCH_INSPECT_BOOK)
                    && new ItemStack(Material.BOOK).equals(item)) {
                    ItemStack newItem = new ItemStack(Material.WRITTEN_BOOK);
                    BookMeta meta = (BookMeta)newItem.getItemMeta();
                    StringBuilder sb = new StringBuilder();
                    if (gender == Gender.FEMALE) {
                        sb.append("&d&l").append(name).append("&d\u2640 the ").append(Msg.capitalEnumName(entity.getType()));
                    } else {
                        sb.append("&9&l").append(name).append("&9\u2642 the ").append(Msg.capitalEnumName(entity.getType()));
                    }
                    sb.append("\n");
                    sb.append("\n&rOwner &7").append(PlayerCache.nameForUuid(owner));
                    sb.append("\n&rAge &7").append(age / 60 / 20 + 1).append(" days");
                    sb.append("\n&rGeneration &7").append(generation);
                    if (customEntity.plugin.getScore().hasPerk(uuid, Perk.RANCH_INSPECT_FAVORITE)) {
                        sb.append("\n&rToy &7").append(Msg.capitalEnumName(toy));
                        sb.append("\n&rTreat &7").append(Msg.capitalEnumName(treat));
                        sb.append("\n&rFriend &7").append(Msg.capitalEnumName(friend));
                    }
                    if (customEntity.plugin.getScore().hasPerk(uuid, Perk.RANCH_INSPECT_QUIRK)) {
                        sb.append("\n&rTraits&7");
                        for (int i = 0; i < 2 && i < quirks.size(); i += 1) {
                            Quirk quirk = quirks.get(i);
                            if (quirk != Quirk.NOTHING) {
                                sb.append(" ").append(Msg.capitalEnumName(quirk));
                            }
                        }
                    }
                    sb.append("\n");
                    sb.append("\n&rRating ");
                    if (yield <= 10) {
                        sb.append("&7Average");
                    } else if (yield <= 20) {
                        sb.append("&fImproved");
                    } else if (yield <= 40) {
                        sb.append("&eSuperb");
                    } else if (yield <= 64) {
                        sb.append("&6Outstanding");
                    } else {
                        sb.append("&6&lUnvelievable");
                    }
                    meta.setPages(Msg.format(sb.toString()));
                    meta.setTitle(name + " the " + Msg.capitalEnumName(entity.getType()));
                    meta.setAuthor(player.getName());
                    newItem.setItemMeta(meta);
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
            } else {
                switch (entity.getType()) {
                case COW:
                    switch (item.getType()) {
                    case WHEAT:
                        isFood = true;
                    }
                    break;
                case MUSHROOM_COW:
                    switch (item.getType()) {
                    case WHEAT:
                        isFood = true;
                    }
                    break;
                case PIG:
                    switch (item.getType()) {
                    case POTATO_ITEM:
                    case CARROT_ITEM:
                    case BEETROOT:
                        isFood = true;
                    }
                    break;
                case CHICKEN:
                    switch (item.getType()) {
                    case SEEDS:
                    case BEETROOT_SEEDS:
                    case MELON_SEEDS:
                    case PUMPKIN_SEEDS:
                        isFood = true;
                    }
                    break;
                case SHEEP:
                    switch (item.getType()) {
                    case WHEAT:
                        isFood = true;
                    }
                    break;
                case RABBIT:
                    switch (item.getType()) {
                    case CARROT:
                    case YELLOW_FLOWER:
                        isFood = true;
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
            }
        }

        void onDropItem(Player player, ItemStack item) {
            if (item.getType() == toy && fun > 30 && toyCooldown == 0) {
                toyCooldown = 5;
                fun = Math.max(0, fun - 100);
                customEntity.entityChatterEffect(entity, Arrays.asList(player));
            }
        }
    }
}
