package com.winthier.skills;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.attribute.Attributable;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.AnimalTamer;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Ocelot;
import org.bukkit.entity.Player;
import org.bukkit.entity.Sittable;
import org.bukkit.entity.Tameable;
import org.bukkit.entity.Wolf;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTameEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

final class TameSkill extends Skill {
    TameSkill(SkillsPlugin plugin) {
        super(plugin, SkillType.TAME);
    }

    private final List<Tameable> warpPets = new ArrayList<>();

    private void addAttribute(Attributable entity, String name, Attribute attribute, double value) {
        final UUID uuid = new UUID(1337, 1 + (long)attribute.ordinal());
        final AttributeModifier modifier;
        modifier = new AttributeModifier(uuid, name, value, AttributeModifier.Operation.ADD_NUMBER);
        entity.getAttribute(attribute).addModifier(modifier);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onEntityTame(EntityTameEvent event) {
        if (!(event.getOwner() instanceof Player)) return;
        final Player player = (Player)event.getOwner();
        final UUID uuid = player.getUniqueId();
        final Set<Perk> perks = plugin.getScore().getPerks(uuid);
        if (!perks.contains(Perk.TAME_BASE)) return;
        final int skillLevel = plugin.getScore().getSkillLevel(uuid, skillType);
        switch (event.getEntity().getType()) {
        case WOLF:
            final Wolf wolf = (Wolf)event.getEntity();
            double damage = linearSkillBonus(4.0, skillLevel);
            if (perks.contains(Perk.TAME_DOG_ATTACK_DAMAGE)) {
                damage += 4.0;
            }
            AttributeModifier attackDamage;
            addAttribute(wolf, "skills:tame.attackDamage", Attribute.GENERIC_ATTACK_DAMAGE, damage);
            if (perks.contains(Perk.TAME_DOG_MOVEMENT_SPEED)) {
                addAttribute(wolf, "skills:tame.movementSpeed", Attribute.GENERIC_MOVEMENT_SPEED, 0.2);
            }
            if (perks.contains(Perk.TAME_DOG_HEALTH)) {
                addAttribute(wolf, "skills:tame.maxHealth", Attribute.GENERIC_MAX_HEALTH, 20);
            }
            break;
        case HORSE:
            
            break;
        default:
            break;
        }
        // Give reward
        Reward reward = getReward(Reward.Category.TAME_ENTITY, event.getEntity().getType().name(), null, null);
        giveReward(player, reward);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Wolf)) return;
        Wolf pet = (Wolf)event.getEntity();
        AnimalTamer tamer = pet.getOwner();
        if (!(tamer instanceof Player)) return;
        final Player player = (Player)tamer;
        final UUID uuid = player.getUniqueId();
        if (plugin.getScore().hasPerk(uuid, Perk.TAME_DOG_DODGE)) {
            switch (event.getCause()) {
            case BLOCK_EXPLOSION:
            case CONTACT:
            case CRAMMING:
            case ENTITY_EXPLOSION:
            case HOT_FLOOR:
            case LAVA:
            case FIRE:
            case LIGHTNING:
            case SUFFOCATION:
            case FALL:
            case VOID:
                Integer lastDodge = (Integer)plugin.getMetadata(pet, "LastDodge");
                if (lastDodge == null || lastDodge < pet.getTicksLived() - 100) {
                    event.setCancelled(true);
                    pet.teleport(player);
                    plugin.setMetadata(pet, "LastDodge", pet.getTicksLived());
                    Location loc = ((LivingEntity)pet).getEyeLocation();
                    loc.getWorld().spawnParticle(Particle.SPELL_MOB, loc, 16, .5, .5, .5, 1.0f);
                    loc.getWorld().playSound(loc, Sound.ENTITY_ENDERMEN_TELEPORT, SoundCategory.NEUTRAL, 0.3f, 1.5f);
                }
                break;
            default:
                break;
            }
        }
    }

    /**
     * Keep chunk with pets in memory so they can teleport to their
     * owner.
     */
    @EventHandler(ignoreCancelled = true)
    public void onChunkUnload(ChunkUnloadEvent event) {
        Iterator<Tameable> iter = warpPets.iterator();
        final Chunk chunk = event.getChunk();
        while (iter.hasNext()) {
            Tameable pet = iter.next();
            if (!pet.isValid()) {
                iter.remove();
            } else {
                Chunk petChunk = pet.getLocation().getChunk();
                if (petChunk.getWorld().equals(chunk.getWorld()) && petChunk.getX() == chunk.getX() && petChunk.getZ() == chunk.getZ()) {
                    event.setCancelled(true);
                }
            }
        }
    }

    /**
     * Listen on Player Teleport to warp tamed entities along.
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        final Player player = (Player)event.getPlayer();
        if (!allowPlayer(player)) return;
        final UUID uuid = player.getUniqueId();
        if (!plugin.getScore().hasPerk(uuid, Perk.TAME_FOLLOW_TELEPORT)) return;
        Location fromLocation = event.getFrom();
        Location toLocation = event.getTo();
        if (fromLocation.getWorld().equals(toLocation.getWorld()) && fromLocation.distanceSquared(toLocation) < 256) return;
        for (Entity nearby: player.getNearbyEntities(64, 64, 64)) {
            if (nearby.getType() != EntityType.OCELOT && nearby.getType() != EntityType.WOLF) continue;
            if (!(nearby instanceof Tameable)) continue;
            final Tameable pet = (Tameable)nearby;
            if (!pet.isTamed()) continue;
            final AnimalTamer owner = pet.getOwner();
            if (owner == null || !owner.equals(player)) continue;
            if (((Sittable)pet).isSitting()) continue;
            ((Sittable)pet).setSitting(true);
            // Teleporting a pet
            if (plugin.getMetadata(pet, "Warping") != null) continue;
            plugin.setMetadata(pet, "Warping", true);
            warpPets.add(pet);
            new BukkitRunnable() {
                @Override public void run() {
                    warpPets.remove(pet);
                    if (!pet.isValid()) return;
                    if (!player.isValid()) return;
                    plugin.removeMetadata(pet, "Warping");
                    Location loc = toLocation;
                    Location petLoc = pet.getLocation();
                    loc.setYaw(petLoc.getYaw());
                    loc.setPitch(petLoc.getPitch());
                    if (!loc.getWorld().equals(petLoc.getWorld()) || loc.distanceSquared(petLoc) >= 256) {
                        pet.teleport(loc);
                        loc.getWorld().spawnParticle(Particle.SPELL_MOB, loc, 16, .5, .5, .5, 1.0f);
                        loc.getWorld().playSound(loc, Sound.ENTITY_ENDERMEN_TELEPORT, SoundCategory.NEUTRAL, 0.3f, 1.5f);
                    }
                    ((Sittable)pet).setSitting(false);
                }
            }.runTaskLater(plugin, 50);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onEntityTarget(EntityTargetEvent event) {
        if (event.getEntity().getType() == EntityType.OCELOT) {
            System.out.println("Kitty Target");
        }
        if (event.getEntity().getType() == EntityType.CREEPER) {
            System.out.println("Creeper Target");
            if (!(event.getTarget() instanceof Player)) return;
            final Creeper creeper = (Creeper)event.getEntity();
            final Player player = (Player)event.getTarget();
            final UUID uuid = player.getUniqueId();
            if (plugin.getScore().hasPerk(uuid, Perk.TAME_CAT_AGGRO_CREEPER)) {
                Ocelot cat = null;
                for (Entity nearby: player.getNearbyEntities(16, 16, 16)) {
                    if (!(nearby instanceof Ocelot)) continue;
                    Ocelot ocelot = (Ocelot)nearby;
                    if (!ocelot.isTamed()) continue;
                    AnimalTamer owner = ocelot.getOwner();
                    if (owner == null || !owner.equals(player)) continue;
                    if (ocelot.getTarget() != null && ocelot.getTarget().isValid()) continue;
                    cat = ocelot;
                    break;
                }
                if (cat != null) {
                    if (plugin.getScore().hasPerk(uuid, Perk.TAME_CAT_RAGE)) {
                        cat.teleport(creeper);
                        cat.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 200, 1));
                        cat.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 200, 1));
                    }
                    cat.setTarget(creeper);
                    cat.getWorld().playSound(cat.getEyeLocation(), Sound.ENTITY_CAT_HISS, SoundCategory.NEUTRAL, 0.5f, 0.8f);
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onDogDeath(EntityDeathEvent event) {
        if (event.getEntity().getType() != EntityType.WOLF) return;
        Wolf dog = (Wolf)event.getEntity();
        if (plugin.getMetadata(dog, "Sacrificed") != null) return;
        if (!dog.isTamed()) return;
        AnimalTamer owner = dog.getOwner();
        if (owner == null || !(owner instanceof Player)) return;
        final Player player = (Player)owner;
        final UUID uuid = player.getUniqueId();
        if (plugin.getScore().hasPerk(uuid, Perk.TAME_DOG_DEATH_HEALS)) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.HEAL, 1, 1));
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    public void onDeadlyEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            final Player player = (Player)event.getEntity();
            double overkill = event.getFinalDamage() - player.getHealth();
            if (overkill < 0) return;
            final UUID uuid = player.getUniqueId();
            if (plugin.getScore().hasPerk(uuid, Perk.TAME_DOG_SACRIFICE)) {
                Wolf dog = null;
                for (Entity nearby: player.getNearbyEntities(16, 16, 16)) {
                    if (!(nearby instanceof Wolf)) continue;
                    Wolf wolf = (Wolf)nearby;
                    if (!wolf.isTamed()) continue;
                    AnimalTamer owner = wolf.getOwner();
                    if (owner == null || !owner.equals(player)) continue;
                    if (wolf.getTarget() != null) continue;
                    dog = wolf;
                    break;
                }
                if (dog != null) {
                    plugin.setMetadata(dog, "Sacrificed", true);
                    dog.setHealth(0);
                    double damage = Math.max(0, event.getFinalDamage() - overkill - 1);
                    event.setDamage(damage);
                }
            }
        } else if (event.getEntity() instanceof Ocelot) {
            Ocelot cat = (Ocelot)event.getEntity();
            if (cat.getHealth() - event.getFinalDamage() > 0) return;
            if (!cat.isTamed()) return;
            AnimalTamer owner = cat.getOwner();
            if (owner == null || !(owner instanceof Player)) return;
            final Player player = (Player)owner;
            final UUID uuid = player.getUniqueId();
            if (plugin.getScore().hasPerk(uuid, Perk.TAME_CAT_LIVES)) {
                int lives;
                String val = plugin.getScoreboardValue(cat, "Lives");
                if (val == null) {
                    lives = 9;
                } else {
                    try {
                        lives = Integer.parseInt(val);
                    } catch (NumberFormatException nfe) {
                        lives = 9;
                    }
                }
                lives -= 1;
                if (lives <= 0) {
                    return;
                } else {
                    cat.getWorld().spawnParticle(Particle.HEART, cat.getEyeLocation().add(0, 1, 0), lives, .25, .25, .25, 0);
                    event.setCancelled(true);
                    cat.setHealth(cat.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());
                    plugin.setScoreboardValue(cat, "Lives", "" + lives);
                    cat.teleport(player);
                    cat.getWorld().playSound(cat.getEyeLocation(), Sound.ENTITY_CAT_PURR, SoundCategory.NEUTRAL, 1.0f, 1.5f);
                }
            }
        }
    }
}
