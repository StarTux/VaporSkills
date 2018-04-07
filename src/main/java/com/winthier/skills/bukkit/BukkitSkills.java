package com.winthier.skills.bukkit;

import com.winthier.skills.Skill;
import com.winthier.skills.Skills;
import com.winthier.skills.bukkit.event.SkillsLevelUpEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

@Getter
public final class BukkitSkills extends Skills {
    @Getter private static BukkitSkills instance;
    private final Map<BukkitSkillType, BukkitSkill> skillMap = new EnumMap<>(BukkitSkillType.class);
    private final Map<String, BukkitSkill> nameMap = new HashMap<>();
    private final Map<UUID, Double> moneys = new HashMap<>();
    private final Map<UUID, Double> exps = new HashMap<>();
    private final Set<UUID> playersInDebugMode = new HashSet<>();
    private final Map<UUID, BukkitPlayer> players = new HashMap<>();
    private boolean enabled = true;

    BukkitSkills() {
        instance = this;
        List<BukkitSkill> skills = Arrays.asList(
            new BukkitSkillBlacksmith(),
            new BukkitSkillBrawl(),
            new BukkitSkillBreed(),
            new BukkitSkillBrew(),
            new BukkitSkillBuild(),
            new BukkitSkillButcher(),
            new BukkitSkillCook(),
            new BukkitSkillDig(),
            new BukkitSkillEat(),
            new BukkitSkillEnchant(),
            new BukkitSkillFish(),
            new BukkitSkillHarvest(),
            new BukkitSkillHunt(),
            new BukkitSkillMine(),
            new BukkitSkillSacrifice(),
            new BukkitSkillShear(),
            new BukkitSkillSmelt(),
            new BukkitSkillTame(),
            new BukkitSkillTravel(),
            new BukkitSkillWoodcut()
            );
        for (BukkitSkill skill : skills) {
            BukkitSkillType type = skill.getSkillType();
            if (skillMap.containsKey(type)) {
                throw new IllegalStateException("Duplicate skill " + type.name() + ": " + skillMap.get(type).getClass().getSimpleName() + " and " + skill.getClass().getSimpleName());
            }
            skillMap.put(type, skill);
        }
    }

    void configure() {
        enabled = BukkitSkillsPlugin.getInstance().getConfig().getBoolean("Enabled", true);
    }

    void buildNameMap() {
        nameMap.clear();
        // Put all the names in the map
        for (BukkitSkill skill : skillMap.values()) {
            nameMap.put(skill.getKey().toLowerCase(), skill);
            nameMap.put(skill.getDisplayName().toLowerCase(), skill);
            nameMap.put(skill.getShorthand().toLowerCase(), skill);
        }
        // Bake the map
        for (Map.Entry<String, BukkitSkill> entry : new ArrayList<>(nameMap.entrySet())) {
            BukkitSkill skill = entry.getValue();
            for (String name = entry.getKey(); name.length() > 0; name = name.substring(0, name.length() - 1)) {
                if (!nameMap.containsKey(name)) nameMap.put(name, skill);
            }
        }
    }

    @Override
    public void onLevelUp(UUID uuid, Skill skill, int level) {
        Player player = Bukkit.getServer().getPlayer(uuid);
        if (player == null) return;
        if (!(skill instanceof BukkitSkill)) return;
            BukkitLevelUpEffect.launch(player, (BukkitSkill)skill, level);
            Bukkit.getServer().getPluginManager().callEvent(new SkillsLevelUpEvent(player, (BukkitSkill)skill, level));
            // Give bonus potion effect
            Random random = new Random(System.currentTimeMillis());
            int duration = 40 * (level + random.nextInt(level));
            int power = level / 100;
            int maxAmp = Math.min(4, power + 1);
            switch (((BukkitSkill)skill).getSkillType()) {
            case BLACKSMITH:
                player.addPotionEffect(new PotionEffect(PotionEffectType.FAST_DIGGING, duration, random.nextInt(maxAmp), true), true);
                if (power >= 1) player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, duration, random.nextInt(maxAmp), true), true);
                break;
            case BRAWL:
            case BUTCHER:
                player.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, duration, random.nextInt(maxAmp), true), true);
                if (power >= 1) player.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, duration, random.nextInt(maxAmp), true), true);
                if (power >= 2) player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, duration, random.nextInt(maxAmp), true), true);
                break;
            case BREED:
            case SHEAR:
            case TAME:
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, duration, random.nextInt(maxAmp), true), true);
                if (power >= 1) player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, duration, random.nextInt(maxAmp), true), true);
                break;
            case BREW:
            case ENCHANT:
                player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, duration, random.nextInt(maxAmp), true), true);
                if (power >= 1) player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, duration, random.nextInt(maxAmp), true), true);
                if (power >= 2) player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, duration, random.nextInt(maxAmp), true), true);
                break;
            case BUILD:
                player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, duration, random.nextInt(maxAmp), true), true);
                if (power >= 1) player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, duration, random.nextInt(maxAmp), true), true);
                break;
            case EAT:
            case COOK:
            case SMELT:
                player.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, duration / 2, random.nextInt(maxAmp), true), true);
                if (power >= 1) player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, duration, random.nextInt(maxAmp), true), true);
                break;
            case DIG:
            case MINE:
            case SACRIFICE:
                player.addPotionEffect(new PotionEffect(PotionEffectType.FAST_DIGGING, duration, random.nextInt(maxAmp), true), true);
                if (power >= 1) player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, duration, random.nextInt(maxAmp), true), true);
                if (power >= 2) player.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, duration, random.nextInt(maxAmp), true), true);
                if (power >= 3) player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, duration, random.nextInt(maxAmp), true), true);
                break;
            case FISH:
                player.addPotionEffect(new PotionEffect(PotionEffectType.LUCK, duration, random.nextInt(maxAmp), true), true);
                if (power >= 1) player.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, duration, random.nextInt(maxAmp), true), true);
                if (power >= 2) player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, duration, random.nextInt(maxAmp), true), true);
                if (power >= 3) player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, duration, random.nextInt(maxAmp), true), true);
                break;
            case HUNT:
            case TRAVEL:
            case HARVEST:
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, duration * 2, random.nextInt(maxAmp), true), true);
                if (power >= 1) player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, duration, random.nextInt(maxAmp), true), true);
                if (power >= 2) player.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, duration, random.nextInt(maxAmp), true), true);
                break;
            case WOODCUT:
                player.addPotionEffect(new PotionEffect(PotionEffectType.FAST_DIGGING, duration, random.nextInt(maxAmp), true), true);
                if (power >= 1) player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, duration, random.nextInt(maxAmp), true), true);
                break;
            default: break;
            }
    }

    @Override
    public Collection<? extends BukkitSkill> getSkills() {
        return skillMap.values();
    }

    BukkitSkill skillByName(String name) {
        return nameMap.get(name.toLowerCase());
    }

    BukkitSkill skillByType(BukkitSkillType type) {
        return skillMap.get(type);
    }

    void giveMoney(Player player, double amount) {
        if (amount < 0.01) return;
        Double total = moneys.remove(player.getUniqueId());
        if (total == null) {
            total = amount;
        } else {
            total += amount;
        }
        if (total >= 5.0) {
            BukkitSkillsPlugin.getInstance().getEconomy().depositPlayer(player, total);
        } else {
            moneys.put(player.getUniqueId(), total);
        }
    }

    void giveExp(Player player, double amount) {
        if (amount < 0.01) return;
        final UUID uuid = player.getUniqueId();
        Double stored = exps.get(uuid);
        if (stored == null) {
            stored = amount;
        } else {
            stored += amount;
        }
        final int full = stored.intValue();
        if (full > 10) {
            stored -= (double)full;
            player.getWorld().spawn(player.getLocation(), ExperienceOrb.class).setExperience(full);
        }
        exps.put(uuid, stored);
    }

    void depositAllMoneys() {
        try {
            for (Map.Entry<UUID, Double> entry : moneys.entrySet()) {
                Double amount = entry.getValue();
                if (amount == null || amount < 0.01) continue;
                OfflinePlayer player = Bukkit.getServer().getOfflinePlayer(entry.getKey());
                if (player == null) continue;
                BukkitSkillsPlugin.getInstance().getEconomy().depositPlayer(player, amount);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            moneys.clear();
        }
    }

    boolean hasDebugMode(Player player) {
        return (playersInDebugMode.contains(player.getUniqueId()));
    }

    void setDebugMode(Player player, boolean debugMode) {
        if (debugMode) {
            playersInDebugMode.add(player.getUniqueId());
        } else {
            playersInDebugMode.remove(player.getUniqueId());
        }
    }

    BukkitPlayer getBukkitPlayer(UUID uuid) {
        BukkitPlayer result = players.get(uuid);
        if (result == null) {
            result = new BukkitPlayer(uuid);
            players.put(uuid, result);
        }
        return result;
    }

    BukkitPlayer getBukkitPlayer(Player player) {
        return getBukkitPlayer(player.getUniqueId());
    }

    void updateAllPlayers() {
        for (Player player : Bukkit.getServer().getOnlinePlayers()) {
            getBukkitPlayer(player.getUniqueId()).on20Ticks();
        }
    }
}
