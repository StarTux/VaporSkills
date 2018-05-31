package com.winthier.skills;

import com.winthier.exploits.bukkit.BukkitExploits;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.attribute.Attribute;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

final class BrawlSkill extends Skill {
    static final String DISARM_COOLDOWN = "brawl_disarm";
    static final String STEAL_COOLDOWN = "brawl_steal";
    static final String STOLEN_SCOREBOARD_TAG = "winthier.brawl_skill.stolen";
    private long killDistanceInterval = 300;
    private double minKillDistance = 16;
    private boolean recursionLock = false;
    private final Random random;
    private EntityDamageByEntityEvent entityDamageByEntityEventIntercept = null;

    BrawlSkill(SkillsPlugin plugin) {
        super(plugin, SkillType.BRAWL);
        this.random = plugin.random;
    }

    @Override
    public void configure() {
        killDistanceInterval = getConfig().getLong("KillDistanceInterval", 300);
        minKillDistance = getConfig().getDouble("MinKillDistance", 16);
    }

    /**
     * Players damaging mobs collects skill points, and may trigger
     * the current charge action (if any).
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        entityDamageByEntityEventIntercept = event;
        if (recursionLock) return;
        if (!(event.getDamager() instanceof Player)) return;
        if (!(event.getEntity() instanceof LivingEntity)) return;
        final Player player = (Player)event.getDamager();
        final UUID uuid = player.getUniqueId();
        if (!allowPlayer(player)) return;
        final LivingEntity entity = (LivingEntity)event.getEntity();
        // Perks
        ItemStack item = player.getInventory().getItemInMainHand();
        // Give Reward
        if (BukkitExploits.getInstance().recentKillDistance(player, entity.getLocation(), killDistanceInterval) < minKillDistance) return;
        double percentage = BukkitExploits.getInstance().getEntityDamageByPlayerRemainderPercentage(entity, Math.min(entity.getHealth(), event.getFinalDamage()));
        if (plugin.hasDebugMode(player)) Msg.msg(player, "&eBrawl Dmg=%.02f/%.02f Pct=%.02f", event.getFinalDamage(), entity.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue(), percentage);
        Reward reward = getReward(Reward.Category.DAMAGE_ENTITY, entity.getType().name(), null, null);
        giveReward(player, reward, percentage);
    }

    /**
     * Sneaking start and stops weapon charge.
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
        final Player player = event.getPlayer();
        int maxWeaponCharge = getMaxWeaponCharge(player);
        if (event.isSneaking()) {
            if (maxWeaponCharge > 0) {
                double attackSpeed = player.getAttribute(Attribute.GENERIC_ATTACK_SPEED).getValue();
                plugin.getSession(player).startCharging(player, maxWeaponCharge, attackSpeed);
            } else {
                plugin.getSession(player).stopCharging();
            }
        } else {
            if (maxWeaponCharge > 0 && plugin.getSession(player).isCharging()) {
                double charge = plugin.getSession(player).getWeaponCharge();
                int chargeLevel = Math.min(maxWeaponCharge, (int)charge);
                switch (player.getInventory().getItemInMainHand().getType()) {
                case IRON_SWORD:
                    if (chargeLevel == 1) slashAttack(player);
                    if (chargeLevel == 2) spinAttack(player);
                    break;
                case DIAMOND_SWORD:
                    if (chargeLevel == 1) pierceAttack(player);
                    if (chargeLevel == 2) dashAttack(player);
                    break;
                case GOLD_SWORD:
                    if (chargeLevel == 2) rageAttack(player);
                    break;
                default: break;
                }
            }
            plugin.getSession(player).stopCharging();
        }
    }

    /**
     * Changing the hotbar item can start and stop charging, assuming
     * we are sneaking.
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPlayerItemHeld(PlayerItemHeldEvent event) {
        final Player player = event.getPlayer();
        if (player.isSneaking()) {
            int maxWeaponCharge = getMaxWeaponCharge(player);
            if (maxWeaponCharge > 0) {
                double chargeSpeed = player.getAttribute(Attribute.GENERIC_ATTACK_SPEED).getValue();
                plugin.getSession(player).startCharging(player, maxWeaponCharge, chargeSpeed);
            } else {
                plugin.getSession(player).stopCharging();
            }
        }
    }

    /**
     * Left clicking restarts the weapon charge (if any) and triggers
     * the action of the current charge (if any).
     */
    @EventHandler(ignoreCancelled = false, priority = EventPriority.MONITOR)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        switch (event.getAction()) {
        case LEFT_CLICK_BLOCK:
        case LEFT_CLICK_AIR:
            final Player player = event.getPlayer();
            if (player.isSneaking()) {
                int maxWeaponCharge = getMaxWeaponCharge(player);
                if (maxWeaponCharge > 0) {
                    double chargeSpeed = player.getAttribute(Attribute.GENERIC_ATTACK_SPEED).getValue();
                    plugin.getSession(player).startCharging(player, maxWeaponCharge, chargeSpeed);
                }
            }
            break;
        default: break;
        }
    }

    double damage(LivingEntity entity, double damage, Player player, ItemStack item) {
        int fireTicks = 0;
        double oldDamage = damage;
        if (item != null) {
            for (Map.Entry<Enchantment, Integer> entry: item.getEnchantments().entrySet()) {
                Enchantment enchantment = entry.getKey();
                int level = entry.getValue();
                if (enchantment.equals(Enchantment.DAMAGE_ALL)) {
                    damage += 0.5 * (double)(level + 1);
                } else if (enchantment.equals(Enchantment.DAMAGE_ARTHROPODS)) {
                    switch (entity.getType()) {
                    case SPIDER:
                    case CAVE_SPIDER:
                    case SILVERFISH:
                    case ENDERMITE:
                        damage += 2.5 * (double)level;
                    default: break;
                    }
                } else if (enchantment.equals(Enchantment.DAMAGE_UNDEAD)) {
                    switch (entity.getType()) {
                    case SKELETON:
                    case ZOMBIE:
                    case WITHER:
                    case WITHER_SKELETON:
                    case PIG_ZOMBIE:
                    case ZOMBIE_HORSE:
                    case SKELETON_HORSE:
                        // case PHANTOM:
                        // case DROWNED:
                        damage += 2.5 * (double)level;
                    default: break;
                    }
                } else if (enchantment.equals(Enchantment.FIRE_ASPECT)) {
                    fireTicks = 80 * level;
                }
            }
        }
        NCP.exempt(player, NCP.FIGHT);
        entity.damage(damage, player);
        NCP.unexempt(player, NCP.FIGHT);
        if (fireTicks > 0) entity.setFireTicks(Math.max(entity.getFireTicks(), fireTicks));
        if (entityDamageByEntityEventIntercept == null) {
            return 0.0;
        } else if (entityDamageByEntityEventIntercept.isCancelled()) {
            entityDamageByEntityEventIntercept = null;
            return 0;
        } else {
            double result = entityDamageByEntityEventIntercept.getFinalDamage();
            entityDamageByEntityEventIntercept = null;
            return result;
        }
    }

    int getMaxWeaponCharge(Player player) {
        final UUID uuid = player.getUniqueId();
        final ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null) return 0;
        switch (item.getType()) {
        case WOOD_SWORD:
        case STONE_SWORD:
            if (plugin.getScore().hasPerk(uuid, Perk.BRAWL_CHARGE)) return 1;
            return 0;
        case GOLD_SWORD:
            if (plugin.getScore().hasPerk(uuid, Perk.BRAWL_CHARGE)) {
                if (plugin.getScore().hasPerk(uuid, Perk.BRAWL_SWORD_GOLD_LIFE_STEAL)) return 2;
                return 1;
            }
            return 0;
        case IRON_SWORD:
            if (plugin.getScore().hasPerk(uuid, Perk.BRAWL_CHARGE)) {
                if (plugin.getScore().hasPerk(uuid, Perk.BRAWL_SWORD_IRON_SPIN)) return 2;
                return 1;
            }
            return 0;
        case DIAMOND_SWORD:
            if (plugin.getScore().hasPerk(uuid, Perk.BRAWL_CHARGE)) {
                if (plugin.getScore().hasPerk(uuid, Perk.BRAWL_SWORD_DIAMOND_DASH)) return 2;
                return 1;
            }
            return 0;
        case WOOD_AXE:
        case STONE_AXE:
        case GOLD_AXE:
        case IRON_AXE:
        case DIAMOND_AXE:
            return 2;
        default:
            return 0;
        }
    }

    void pierceAttack(final Player player) {
        final Vector dir = player.getLocation().getDirection().normalize();
        final Vector dirh = dir.clone().multiply(0.5);
        final Set<UUID> affectedEntities = new HashSet<>();
        final ItemStack itemInHand = player.getInventory().getItemInMainHand();
        final double damage = player.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).getValue();
        Location location = player.getEyeLocation();
        location.getWorld().playSound(location, Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 0.5f, 1.6f);
        for (int i = 0; i < 10; i += 1) {
            location = location.add(dirh);
            location.getWorld().spawnParticle(Particle.FIREWORKS_SPARK, location, 1, 0, 0, 0, 0);
            //            location.getWorld().playSound(location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 0.05f, 2.0f);
            for (Entity nearby: location.getWorld().getNearbyEntities(location, 0.5, 0.5, 0.5)) {
                if (nearby instanceof LivingEntity && !player.equals(nearby) && !affectedEntities.contains(nearby.getUniqueId())) {
                    affectedEntities.add(nearby.getUniqueId());
                    LivingEntity living = (LivingEntity)nearby;
                    if (damage(living, damage, player, itemInHand) > 0) {
                        living.getWorld().playSound(living.getEyeLocation(), Sound.ENTITY_PLAYER_ATTACK_STRONG, SoundCategory.HOSTILE, 0.5f, 0.8f);
                        location.getWorld().spawnParticle(Particle.BLOCK_DUST, location, 16, .25, .25, .25, 0.1, new MaterialData(Material.DIAMOND_BLOCK));
                    }
                }
            }
        }
    }

    void dashAttack(final Player player) {
        final Vector dir = player.getLocation().getDirection().normalize();
        final Vector dirf = dir.clone().multiply(5);
        final Vector dirh = dir.clone().multiply(0.5);
        Vector tmp = dir.clone();
        if (tmp.getY() > 0.25f) tmp = tmp.setY(0.25);
        final Vector velo = tmp.normalize().multiply(3.0);
        final double damage = player.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).getValue();
        final ItemStack itemInHand = player.getInventory().getItemInMainHand();
        final Set<UUID> affectedEntities = new HashSet<>();
        player.getWorld().playSound(player.getEyeLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 0.25f, 2.0f);
        NCP.exempt(player, NCP.MOVING);
        new BukkitRunnable() {
            private int ticks;
            private boolean cancelDash = false;
            @Override public void run() {
                if (!player.isValid()) {
                    NCP.unexempt(player, NCP.MOVING);
                    cancel();
                    return;
                }
                switch (ticks++) {
                case 0: case 1: case 2: case 3: case 4: case 5:
                    if (!cancelDash) player.setVelocity(velo);
                    player.getWorld().spawnParticle(Particle.FIREWORKS_SPARK, player.getEyeLocation().add(dirf), 1, 0, 0, 0, 0);
                    Location location = player.getEyeLocation();
                    for (int i = 0; i < 10; i += 1) {
                        location = location.add(dirh);
                        for (Entity nearby: location.getWorld().getNearbyEntities(location, 0.5, 0.5, 0.5)) {
                            if (nearby instanceof LivingEntity && !player.equals(nearby) && !affectedEntities.contains(nearby.getUniqueId())) {
                                affectedEntities.add(nearby.getUniqueId());
                                LivingEntity living = (LivingEntity)nearby;
                                if (damage(living, damage, player, itemInHand) > 0) {
                                    living.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 20, 99));
                                    living.setVelocity(living.getVelocity().add(velo));
                                    player.setVelocity(new Vector(0, 0, 0));
                                    living.getWorld().playSound(living.getEyeLocation(), Sound.ENTITY_PLAYER_ATTACK_STRONG, SoundCategory.HOSTILE, 0.5f, 1.5f);
                                    location.getWorld().spawnParticle(Particle.BLOCK_DUST, location, 16, .25, .25, .25, 0.1, new MaterialData(Material.DIAMOND_BLOCK));
                                    if (!cancelDash) {
                                        Vector stopVelo = player.getVelocity().setX(0).setZ(0);
                                        stopVelo.setY(Math.min(stopVelo.getY(), 0));
                                        player.setVelocity(new Vector(0, 0, 0));
                                        cancelDash = true;
                                    }
                                }
                            }
                        }
                    }
                    break;
                case 6:
                    if (!cancelDash) {
                        Vector stopVelo = player.getVelocity().setX(0).setZ(0);
                        stopVelo.setY(Math.min(stopVelo.getY(), 0));
                        player.setVelocity(new Vector(0, 0, 0));
                    }
                    break;
                case 15:
                    NCP.unexempt(player, NCP.MOVING);
                    cancel();
                    break;
                default: break;
                }
            }
        }.runTaskTimer(plugin, 1, 1);
    }

    void slashAttack(final Player player) {
        Location eyeLocation = player.getEyeLocation();
        Vector eyeVector = eyeLocation.toVector();
        Vector viewDirection = eyeLocation.getDirection();
        final double damage = player.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).getValue();
        final ItemStack itemInHand = player.getInventory().getItemInMainHand();
        player.getWorld().playSound(player.getEyeLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 0.2f, 1.0f);
        player.getWorld().spawnParticle(Particle.SWEEP_ATTACK, eyeLocation.clone().add(viewDirection), 1, 0, 0, 0, 0);
        for (Entity e: player.getNearbyEntities(4, 1, 4)) {
            if (e instanceof LivingEntity && !e.equals(player)) {
                LivingEntity living = (LivingEntity)e;
                Vector entityDirection = living.getLocation().add(0, living.getHeight() / 2, 0).toVector().subtract(eyeVector);
                float angle = entityDirection.angle(viewDirection);
                if (angle < Math.PI * 0.5) {
                    if (damage(living, damage, player, itemInHand) > 0) {
                        living.getWorld().playSound(living.getEyeLocation(), Sound.ENTITY_PLAYER_ATTACK_STRONG, SoundCategory.HOSTILE, 0.5f, 1.0f);
                        living.getWorld().spawnParticle(Particle.SWEEP_ATTACK, living.getEyeLocation(), 1, 0, 0, 0, 0);
                    }
                }
            }
        }
    }

    void spinAttack(final Player player) {
        final float yaw = player.getLocation().getYaw();
        final Set<UUID> affectedEntities = new HashSet<>();
        final double damage = player.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).getValue();
        final ItemStack itemInHand = player.getInventory().getItemInMainHand();
        new BukkitRunnable() {
            private int ticks = 0;
            @Override public void run() {
                if (!player.isValid()) {
                    cancel();
                    return;
                }
                ticks += 1;
                if (ticks <= 10) {
                    Location location = player.getLocation();
                    location.setYaw((yaw + (float)ticks * 360.f / 10.0f) % 360.0f);
                    player.teleport(location);
                    Vector dir = location.getDirection().setY(0).normalize().multiply(2.0);
                    location = player.getLocation().add(0, 1, 0).add(dir);
                    location.getWorld().playSound(location, Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 0.1f, 1.5f);
                    location.getWorld().spawnParticle(Particle.CLOUD, location, 1, 0, 0, 0, 0);
                    location.getWorld().spawnParticle(Particle.CLOUD, location.clone().add(dir), 1, 0, 0, 0, 0);
                    for (Entity e: location.getWorld().getNearbyEntities(location, 2.0, 1.0, 2.0)) {
                        if (e instanceof LivingEntity && !player.equals(e) && !affectedEntities.contains(e.getUniqueId())) {
                            affectedEntities.add(e.getUniqueId());
                            LivingEntity living = (LivingEntity)e;
                            if (damage(living, damage, player, itemInHand) > 0) {
                                living.getWorld().spawnParticle(Particle.SWEEP_ATTACK, living.getEyeLocation(), 1, 0, 0, 0, 0);
                                living.getWorld().playSound(living.getEyeLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.HOSTILE, 0.5f, 1.0f);
                            }
                        }
                    }
                } else {
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 1, 1);
    }

    void rageAttack(final Player player) {
        new BukkitRunnable() {
            private int ticks = 0;
            @Override public void run() {
                if (!player.isValid()) {
                    cancel();
                    return;
                }
                ticks += 1;
                if (ticks <= 60) {
                    // Location eyeLocation = player.getEyeLocation();
                    // for (Entity nearby: player.getNearbyEntities(4, 1, 4)) {
                    //     if (nearby instanceof LivingEntity && !nearby.equals(player)) {
                    //         float angle = eyeLocation.getDirection().angle(
                    // }
                } else {
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 1, 1);
    }

    // void steal() {
    //     // Unarmed
    //     double luck = player.getAttribute(Attribute.GENERIC_LUCK).getValue();
    //     if (plugin.getScore().hasPerk(uuid, Perk.BRAWL_UNARMED_DISARM)
    //         && plugin.getSession(uuid).getCooldown(DISARM_COOLDOWN) == 0) {
    //         final int skillLevel = plugin.getScore().getSkillLevel(uuid, skillType);
    //         EntityEquipment equip = entity.getEquipment();
    //         boolean attempted = false;
    //         if (equip.getItemInMainHand().getType() != Material.AIR) {
    //             double chance = equip.getItemInMainHandDropChance();
    //             if (chance > 0) {
    //                 plugin.getSession(uuid).setCooldown(DISARM_COOLDOWN, 20);
    //                 attempted = true;
    //                 if (random.nextDouble() < chance + luck * 10) {
    //                     ItemStack drop = equip.getItemInMainHand();
    //                     equip.setItemInMainHand(null);
    //                     player.getWorld().dropItem(player.getEyeLocation(), drop.clone()).setPickupDelay(0);
    //                 }
    //             }
    //         }
    //         if (!attempted && plugin.getScore().hasPerk(uuid, Perk.BRAWL_UNARMED_UNDRESS)) {
    //             ItemStack[] items = equip.getArmorContents();
    //             for (int i = 3; !attempted && i >= 0; i -= 1) {
    //                 ItemStack item = items[i];
    //                 if (item != null && item.getType() != Material.AIR) {
    //                     final double chance;
    //                     switch (i) {
    //                     case 3: chance = equip.getHelmetDropChance(); break;
    //                     case 2: chance = equip.getChestplateDropChance(); break;
    //                     case 1: chance = equip.getLeggingsDropChance(); break;
    //                     case 0: default: chance = equip.getBootsDropChance(); break;
    //                     }
    //                     if (chance > 0) {
    //                         attempted = true;
    //                         plugin.getSession(uuid).setCooldown(DISARM_COOLDOWN, 40);
    //                         if (random.nextDouble() < chance + luck * 10) {
    //                             items[i] = null;
    //                             equip.setArmorContents(items);
    //                             player.getWorld().dropItem(player.getEyeLocation(), item.clone()).setPickupDelay(0);
    //                         }
    //                     }
    //                 }
    //             }
    //         }
    //     }
    //     if (plugin.getScore().hasPerk(uuid, Perk.BRAWL_UNARMED_STEAL)
    //         && plugin.getSession(uuid).getCooldown(STEAL_COOLDOWN) == 0) {
    //         ItemStack stolen = stealItem(entity);
    //         if (stolen != null) {
    //             plugin.getSession(uuid).setCooldown(STEAL_COOLDOWN, 200);
    //             if (random.nextDouble() < luck * 10) {
    //                 if (stolen != null) {
    //                     player.getWorld().dropItem(player.getEyeLocation(), stolen.clone()).setPickupDelay(0);
    //                 }
    //             }
    //         }
    //     }
    // }

    // private ItemStack stealItem(LivingEntity entity) {
    //     if (entity.getScoreboardTags().contains(STOLEN_SCOREBOARD_TAG)) return null;
    //     final double roll;
    //     switch (entity.getType()) {
    //     case SKELETON:
    //     case STRAY:
    //         return new ItemStack(Material.BONE);
    //     case ZOMBIE:
    //     case HUSK:
    //     case ZOMBIE_VILLAGER:
    //         return new ItemStack(Material.ROTTEN_FLESH);
    //     case CREEPER:
    //         return new ItemStack(Material.SULPHUR);
    //     case ENDERMAN:
    //         return new ItemStack(Material.ENDER_PEARL);
    //     case SPIDER:
    //     case CAVE_SPIDER:
    //         if (random.nextDouble() < 0.33) {
    //             return new ItemStack(Material.SPIDER_EYE);
    //         } else {
    //             return new ItemStack(Material.STRING);
    //         }
    //     case WITCH:
    //         roll = random.nextDouble();
    //         if (roll < 0.125) {
    //             return new ItemStack(Material.GLASS_BOTTLE);
    //         } else if (roll < 0.250) {
    //             return new ItemStack(Material.GLOWSTONE_DUST);
    //         } else if (roll < 0.375) {
    //             return new ItemStack(Material.SULPHUR);
    //         } else if (roll < 0.500) {
    //             return new ItemStack(Material.REDSTONE);
    //         } else if (roll < 0.625) {
    //             return new ItemStack(Material.SPIDER_EYE);
    //         } else if (roll < 0.750) {
    //             return new ItemStack(Material.SUGAR);
    //         } else {
    //             return new ItemStack(Material.STICK);
    //         }
    //     case EVOKER:
    //         entity.addScoreboardTag(STOLEN_SCOREBOARD_TAG);
    //         return new ItemStack(Material.TOTEM);
    //     case BLAZE:
    //         return new ItemStack(Material.BLAZE_ROD);
    //     case WITHER_SKELETON:
    //         if (random.nextDouble() < 0.025) {
    //             entity.addScoreboardTag(STOLEN_SCOREBOARD_TAG);
    //             return new ItemStack(Material.SKULL_ITEM, 1, (short)1);
    //         } else {
    //             if (random.nextBoolean()) {
    //                 return new ItemStack(Material.BONE);
    //             } else {
    //                 return new ItemStack(Material.COAL);
    //             }
    //         }
    //     default:
    //         return null;
    //     }
    // }
}
