package com.winthier.skills;

import com.winthier.custom.entity.CustomEntity;
import com.winthier.custom.entity.EntityWatcher;
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
public final class RanchEntity implements CustomEntity {
    final SkillsPlugin plugin;

    @Override
    public String getCustomId() {
        return "winthier:Ranching";
    }

    @Override
    public LivingEntity spawnEntity(Location location) {
        return null;
    }

    @Override
    public EntityWatcher createEntityWatcher(Entity e) {
        return new Watcher(this, (LivingEntity)e);
    }

    enum Quirk {
        EXPLOSIVE,
        HUNGRY,
        SHY,
        SOCIALITE,
        SKINNY,
        FAT;
    }

    void breed(Watcher mother, Watcher father, LivingEntity result, Player rancher) {
    }

    @Getter @RequiredArgsConstructor
    final class Watcher implements EntityWatcher {
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

        boolean hasActiveQuirk(Quirk quirk) {
            int index = quirks.indexOf(quirk);
            return index == 0 || index == 1;
        }

        void load() {
            Map<String, Object> map = customEntity.plugin.getScoreboardJSON(entity, SCOREBOARD_KEY);
            if (map == null) return;
            ConfigurationSection config = new YamlConfiguration().createSection("tmp", map);
            generation = config.getInt("generation", 0);
            happiness = config.getInt("happiness", 0);
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
        }

        void save() {
            Map<String, Object> map = new HashMap<>();
            map.put("generation", generation);
            map.put("happiness", happiness);
            map.put("owner", owner.toString());
            map.put("quirks", quirks.stream().map(a -> a.name().toLowerCase()).collect(Collectors.toList()));
            map.put("toy", toy.name().toLowerCase());
            map.put("treat", treat.name().toLowerCase());
            plugin.storeScoreboardJSON(entity, SCOREBOARD_KEY, map);
        }

        void roll() {
            if (quirks.isEmpty()) {
                List<Quirk> quirkPool = new ArrayList<>();
                for (Quirk quirk: Quirk.values()) quirkPool.add(quirk);
                Random random = new Random(System.currentTimeMillis());
                for (int i = 0; i < 4; i += 1) {
                    Quirk quirk = quirkPool.remove(random.nextInt(quirkPool.size()));
                    quirks.add(quirk);
                }
            }
        }
    }
}
