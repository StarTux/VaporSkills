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
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
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
        HUNGRY,
        SHY,
        SOCIAL,
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

    Watcher breed(Watcher mother, Watcher father, LivingEntity child, Player rancher) {
        Watcher watcher = (Watcher)CustomPlugin.getInstance().getEntityManager().wrapEntity(child, CUSTOM_ID);
        watcher.generation = Math.min(mother.generation, father.generation) + 1;
        watcher.owner = rancher.getUniqueId();
        List<Quirk> motherQuirks = new ArrayList<>(mother.quirks);
        List<Quirk> fatherQuirks = new ArrayList<>(father.quirks);
        Random random = new Random(System.currentTimeMillis());
        if (!motherQuirks.isEmpty()) watcher.quirks.add(motherQuirks.remove(random.nextInt(motherQuirks.size())));
        if (!fatherQuirks.isEmpty()) watcher.quirks.add(fatherQuirks.remove(random.nextInt(fatherQuirks.size())));
        if (!motherQuirks.isEmpty()) watcher.quirks.add(motherQuirks.remove(random.nextInt(motherQuirks.size())));
        if (!fatherQuirks.isEmpty()) watcher.quirks.add(fatherQuirks.remove(random.nextInt(fatherQuirks.size())));
        // Mutation
        if (random.nextInt(3) == 0) {
            List<Quirk> quirkPool = new ArrayList<>();
            for (Quirk quirk: Quirk.values()) {
                if (!watcher.quirks.contains(quirk)) quirkPool.add(quirk);
            }
            if (!quirkPool.isEmpty()) watcher.quirks.add(quirkPool.remove(random.nextInt(quirkPool.size())));
        }
        watcher.roll();
        watcher.save();
        return watcher;
    }

    Watcher breed(LivingEntity child, Player rancher) {
        Watcher watcher = (Watcher)CustomPlugin.getInstance().getEntityManager().wrapEntity(child, CUSTOM_ID);
        watcher.generation = 1;
        watcher.owner = rancher.getUniqueId();
        watcher.roll();
        watcher.save();
        return watcher;
    }

    @Getter @RequiredArgsConstructor
    static final class Watcher implements EntityWatcher {
        // Constant
        static final String SCOREBOARD_KEY = "winthier_ranching";
        // Interface implementation
        private final RanchEntity customEntity;
        private final LivingEntity entity;
        // Payload
        private int generation = 0;
        private int happiness = 0;
        private UUID owner = new UUID(0, 0);
        private List<Quirk> quirks = new ArrayList<>();
        private Material toy, treat;
        private Gender gender;
        private long tickOffset = 0;
        private long ticks = 0;

        boolean hasActiveQuirk(Quirk quirk) {
            int index = quirks.indexOf(quirk);
            return index == 0 || index == 1;
        }

        void load() {
            Map<String, Object> map = customEntity.plugin.getScoreboardJSON(entity, SCOREBOARD_KEY);
            if (map == null) return;
            ConfigurationSection config = new YamlConfiguration().createSection("tmp", map);
            generation = config.getInt("generation", 0);
            happiness = config.getInt("happy", 0);
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
            map.put("generation", generation);
            map.put("happy", happiness);
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
        }

        void onTick() {
            if ((ticks++ % tickOffset) != 0) return;
            switch (entity.getType()) {
            case COW:
                break;
            case MUSHROOM_COW:
                break;
            case PIG:
                break;
            case SHEEP:
                break;
            case CHICKEN:
                break;
            case RABBIT:
                break;
            default:
                break;
            }
        }
    }
}
