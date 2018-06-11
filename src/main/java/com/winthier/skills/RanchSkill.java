package com.winthier.skills;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Animals;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Sheep;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * The ranching skill works in conjunction with the RanchingEntity.
 */
final class RanchSkill extends Skill {
    @Getter private List<String> maleNames, femaleNames;

    RanchSkill(SkillsPlugin plugin) {
        super(plugin, SkillType.RANCH);
    }

    @Override
    void configure() {
        maleNames = getConfig().getStringList("MaleNames");
        if (maleNames.isEmpty()) maleNames = Arrays.asList("Adam");
        femaleNames = getConfig().getStringList("FemaleNames");
        if (femaleNames.isEmpty()) femaleNames = Arrays.asList("Eve");
    }

    void breedEffect(LivingEntity e, int power) {
        new BukkitRunnable() {
            private int ticks = 0;
            @Override public void run() {
                if (!e.isValid()) {
                    cancel();
                    return;
                }
                Location loc = e.getEyeLocation();
                switch (ticks++) {
                case 1:
                    switch (power) {
                    case 1:
                        e.getWorld().spawnParticle(Particle.REDSTONE, loc.add(0, 1, 0), 8, .8, .8, .8, 1);
                        break;
                    case 2:
                        e.getWorld().spawnParticle(Particle.REDSTONE, loc.add(0, 1, 0), 8, .8, .8, .8, 1);
                        break;
                    case 3:
                        e.getWorld().spawnParticle(Particle.SPELL_MOB, loc.add(0, 1, 0), 16, .8, .8, .8, 1);
                        break;
                    default:
                        break;
                    }
                    break;
                case 2:
                    e.getWorld().playSound(loc, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.NEUTRAL, 0.5f, 1.0f);
                    break;
                case 4:
                    e.getWorld().playSound(loc, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.NEUTRAL, 0.5f, 1.2f);
                    break;
                case 6:
                    if (power > 1) e.getWorld().playSound(loc, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.NEUTRAL, 0.5f, 1.2f);
                    break;
                case 8:
                    if (power > 1) e.getWorld().playSound(loc, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.NEUTRAL, 0.5f, 1.4f);
                    break;
                case 10:
                    if (power >= 2) e.getWorld().playSound(loc, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.NEUTRAL, 0.5f, 1.4f);
                    break;
                case 12:
                    if (power >= 2) e.getWorld().playSound(loc, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.NEUTRAL, 0.5f, 1.6f);
                    break;
                case 13:
                    cancel();
                default:
                    break;
                }
            }
        }.runTaskTimer(plugin, 1, 1);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onEntityBreed(EntityBreedEvent event) {
        if (!(event.getBreeder() instanceof Player)) return;
        final Player player = (Player)event.getBreeder();
        final UUID uuid = player.getUniqueId();
        if (!(event.getEntity() instanceof Animals)) return;
        final Animals entity = (Animals)event.getEntity();
        final boolean doImprove;
        final boolean canSpecial;
        switch (entity.getType()) {
        case COW:
            doImprove = plugin.getScore().hasPerk(uuid, Perk.RANCH_COW);
            canSpecial = plugin.getScore().hasPerk(uuid, Perk.RANCH_COW_MILK);
            break;
        case PIG:
            doImprove = plugin.getScore().hasPerk(uuid, Perk.RANCH_PIG);
            canSpecial = plugin.getScore().hasPerk(uuid, Perk.RANCH_PIG_TRUFFLE);
            break;
        case CHICKEN:
            doImprove = plugin.getScore().hasPerk(uuid, Perk.RANCH_CHICKEN);
            canSpecial = plugin.getScore().hasPerk(uuid, Perk.RANCH_CHICKEN_GOLD_EGG);
            break;
        case SHEEP:
            doImprove = plugin.getScore().hasPerk(uuid, Perk.RANCH_SHEEP);
            canSpecial = plugin.getScore().hasPerk(uuid, Perk.RANCH_SHEEP_RAINBOW);
            break;
        case RABBIT:
            doImprove = plugin.getScore().hasPerk(uuid, Perk.RANCH_RABBIT);
            canSpecial = false;
            break;
        default:
            doImprove = false;
            canSpecial = false;
            break;
        }
        if (doImprove) {
            final int skillLevel = plugin.getScore().getSkillLevel(uuid, skillType);
            List<String> names = new ArrayList<>();
            RanchEntity.Watcher watcher;
            watcher = plugin.getRanchEntity().breed(event.getMother(), event.getFather(), entity, player, skillLevel, canSpecial);
            names.add(watcher.getName());
            int siblings = 0;
            if (plugin.getScore().hasPerk(uuid, Perk.RANCH_TWINS)) siblings += 1;
            if (plugin.getScore().hasPerk(uuid, Perk.RANCH_TRIPLETS)) siblings += 1;
            if (plugin.getScore().hasPerk(uuid, Perk.RANCH_QUADRUPLETS)) siblings += 1;
            for (int i = 0; i < siblings; i += 1) {
                if (plugin.random.nextInt(10) == 0) {
                    Animals sibling = (Animals)entity.getWorld().spawn(event.getMother().getLocation(), entity.getType().getEntityClass(), e -> {
                            ((Animals)e).setBaby();
                            if (entity.getType() == EntityType.SHEEP) {
                                ((Sheep)e).setColor(((Sheep)entity).getColor());
                            }
                        });
                    watcher = plugin.getRanchEntity().breed(event.getMother(), event.getFather(), sibling, player, skillLevel, canSpecial);
                    names.add(watcher.getName());
                }
            }
            breedEffect(entity, names.size());
            if (names.size() == 1) {
                Msg.actionBar(player, "%s was born!", names.get(0));
            } else if (names.size() > 1) {
                StringBuilder sb = new StringBuilder(names.get(0));
                for (int i = 1; i < names.size() - 1; i += 1) sb.append(", ").append(names.get(i));
                sb.append(" and ").append(names.get(names.size() - 1)).append(" were born!");
                Msg.actionBar(player, sb.toString());
            }
        }
        // Give Reward
        Reward reward = getReward(Reward.Category.BREED_ENTITY, entity.getType().name(), null, null);
        giveReward(player, reward);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        plugin.getRanchEntity().onPlayerDropItem(event);
    }

    // Listen for spawned eggs
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onItemSpawn(ItemSpawnEvent event) {
        if (event.getEntity().getItemStack().getType() == Material.EGG) {
            plugin.getRanchEntity().onEggSpawn(event);
        }
    }
}
