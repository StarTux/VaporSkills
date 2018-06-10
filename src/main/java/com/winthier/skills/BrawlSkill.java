package com.winthier.skills;

import com.winthier.custom.CustomPlugin;
import com.winthier.custom.entity.SimpleScriptEntity;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
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
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;

final class BrawlSkill extends Skill {
    static final String STOLEN_SCOREBOARD_TAG = "winthier.brawl_skill.stolen";
    private long killDistanceInterval = 300;
    private double minKillDistance = 16;
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
     * Called by SkillsPlugin.onEntityDeath() only after an entity
     * spawned by a proper reason was killed by a player using melee
     * combat.
     */
    void onEntityKill(Player player, LivingEntity entity) {
        Reward reward = getReward(Reward.Category.KILL_ENTITY, entity.getType().name(), null, null);
        giveReward(player, reward, 1);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        entityDamageByEntityEventIntercept = event;
    }

    /**
     * Sneaking start and stops weapon charge.
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
        final Player player = event.getPlayer();
        final ItemStack weapon = player.getInventory().getItemInMainHand();
        int maxWeaponCharge = getMaxWeaponCharge(player, weapon);
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
                switch (weapon.getType()) {
                case IRON_SWORD:
                    if (chargeLevel == 1) ironSwordSlash(player);
                    if (chargeLevel == 2) ironSwordSpin(player);
                    break;
                case DIAMOND_SWORD:
                    if (chargeLevel == 1) diamondSpearPierce(player);
                    if (chargeLevel == 2) diamondSpearDash(player);
                    break;
                case GOLD_SWORD:
                    if (chargeLevel == 1) goldSwordHeal(player);
                    if (chargeLevel == 2) goldSwordRage(player);
                    break;
                case IRON_AXE:
                    if (chargeLevel == 1) ironHammerSmash(player);
                    if (chargeLevel == 1) ironHammerSmash2(player);
                    break;
                case DIAMOND_AXE:
                    if (chargeLevel == 1) diamondAxeSlash(player); // TODO come up with something unique??
                    if (chargeLevel == 2) diamondAxeThrow(player);
                    break;
                case GOLD_AXE:
                    if (chargeLevel == 1) goldAxeArea(player);
                    if (chargeLevel == 2) goldAxeArea2(player);
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
        final int newSlot = event.getNewSlot();
        if (player.isSneaking()) {
            int maxWeaponCharge = getMaxWeaponCharge(player, player.getInventory().getItem(newSlot));
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
                int maxWeaponCharge = getMaxWeaponCharge(player, player.getInventory().getItemInMainHand());
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

    int getMaxWeaponCharge(Player player, ItemStack weapon) {
        final UUID uuid = player.getUniqueId();
        if (weapon == null) return 0;
        switch (weapon.getType()) {
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

    void diamondSpearPierce(final Player player) {
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
            for (Entity nearby: location.getWorld().getNearbyEntities(location, 0.5, 0.5, 0.5)) {
                if (nearby instanceof LivingEntity && !nearby.isInvulnerable() && !player.equals(nearby) && !affectedEntities.contains(nearby.getUniqueId())) {
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

    void diamondSpearDash(final Player player) {
        final Vector dir = player.getLocation().getDirection().normalize();
        final Vector dirf = dir.clone().multiply(5);
        final Vector dirh = dir.clone().multiply(0.5);
        Vector tmp = dir.clone();
        if (tmp.getY() > 0.25f) tmp = tmp.setY(0.25);
        final Vector velo = tmp.normalize().multiply(1.2);
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
                int oldTicks = ticks;
                ticks += 1;
                if (oldTicks < 15) {
                    player.setVelocity(velo);
                    player.getWorld().spawnParticle(Particle.FIREWORKS_SPARK, player.getEyeLocation().add(dirf), 1, 0, 0, 0, 0);
                    Location location = player.getEyeLocation();
                    for (int i = 0; i < 10; i += 1) {
                        location = location.add(dirh);
                        for (Entity nearby: location.getWorld().getNearbyEntities(location, 0.5, 0.5, 0.5)) {
                            if (nearby instanceof LivingEntity && !nearby.isInvulnerable() && !player.equals(nearby) && !affectedEntities.contains(nearby.getUniqueId())) {
                                affectedEntities.add(nearby.getUniqueId());
                                LivingEntity living = (LivingEntity)nearby;
                                if (damage(living, damage, player, itemInHand) > 0) {
                                    living.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 20, 99));
                                    living.setVelocity(living.getVelocity().add(velo));
                                    player.setVelocity(new Vector(0, 0, 0));
                                    living.getWorld().playSound(living.getEyeLocation(), Sound.ENTITY_PLAYER_ATTACK_STRONG, SoundCategory.HOSTILE, 0.5f, 1.5f);
                                    location.getWorld().spawnParticle(Particle.BLOCK_DUST, location, 16, .25, .25, .25, 0.1, new MaterialData(Material.DIAMOND_BLOCK));
                                }
                            }
                        }
                    }
                } else {
                    NCP.unexempt(player, NCP.MOVING);
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 1, 1);
    }

    void ironSwordSlash(final Player player) {
        Location eyeLocation = player.getEyeLocation();
        Vector eyeVector = eyeLocation.toVector();
        Vector viewDirection = eyeLocation.getDirection();
        final double damage = player.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).getValue();
        final ItemStack itemInHand = player.getInventory().getItemInMainHand();
        player.getWorld().playSound(player.getEyeLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 0.2f, 1.0f);
        player.getWorld().spawnParticle(Particle.SWEEP_ATTACK, eyeLocation.clone().add(viewDirection), 1, 0, 0, 0, 0);
        for (Entity e: player.getNearbyEntities(4, 4, 4)) {
            if (e instanceof LivingEntity && !e.equals(player)) {
                LivingEntity living = (LivingEntity)e;
                Location entityCenter = living.getLocation().add(0, living.getHeight() * 0.5, 0);
                if (eyeLocation.distanceSquared(entityCenter) <= 16) {
                    Vector entityDirection = entityCenter.toVector().subtract(eyeVector);
                    float angle = entityDirection.angle(viewDirection);
                    if (angle < Math.PI * 0.5) {
                        if (damage(living, damage, player, itemInHand) > 0) {
                            living.getWorld().playSound(living.getEyeLocation(), Sound.BLOCK_ANVIL_PLACE, SoundCategory.HOSTILE, 0.3f, 1.3f);
                            living.getWorld().spawnParticle(Particle.BLOCK_DUST, living.getEyeLocation(), 16, .25, .25, .25, 0.1, new MaterialData(Material.IRON_BLOCK));
                        }
                    }
                }
            }
        }
    }

    void ironSwordSpin(final Player player) {
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
                                living.getWorld().playSound(living.getEyeLocation(), Sound.BLOCK_ANVIL_PLACE, SoundCategory.HOSTILE, 0.3f, 1.5f);
                                living.getWorld().spawnParticle(Particle.BLOCK_DUST, living.getEyeLocation(), 32, .25, .25, .25, 0.1, new MaterialData(Material.IRON_BLOCK));
                            }
                        }
                    }
                } else {
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 1, 1);
    }

    void goldSwordHeal(final Player player) {
        Location eyeLocation = player.getEyeLocation();
        Vector eyeVector = eyeLocation.toVector();
        Vector viewDirection = eyeLocation.getDirection();
        Map<LivingEntity, Double> targetDists = new IdentityHashMap<>();
        List<LivingEntity> targets = new ArrayList<>();
        for (Entity nearby: player.getNearbyEntities(4, 4, 4)) {
            if (nearby instanceof LivingEntity && !nearby.isInvulnerable()) {
                LivingEntity living = (LivingEntity)nearby;
                Location entityCenter = living.getLocation().add(0, living.getHeight() * 0.5, 0);
                double distanceSquared = eyeLocation.distanceSquared(entityCenter);
                if (distanceSquared < 16) {
                    Vector entityVector = entityCenter.toVector().subtract(eyeVector).normalize();
                    double angle = viewDirection.angle(entityVector);
                    if (angle < Math.PI * 0.5) {
                        targetDists.put(living, distanceSquared);
                        targets.add(living);
                    }
                }
            }
        }
        player.getWorld().playSound(eyeLocation, Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 0.2f, 1.2f);
        player.getWorld().spawnParticle(Particle.SWEEP_ATTACK, eyeLocation.clone().add(viewDirection), 1, 0, 0, 0, 0);
        Collections.sort(targets, (a, b) -> Double.compare(targetDists.get(a), targetDists.get(b)));
        for (LivingEntity target: targets) {
            double finalDamage = damage(target, player.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).getValue(), player, player.getInventory().getItemInMainHand());
            if (finalDamage > 0) {
                target.getWorld().playSound(target.getEyeLocation(), Sound.ENTITY_PLAYER_ATTACK_STRONG, 0.5f, 1.0f);
                target.getWorld().spawnParticle(Particle.BLOCK_DUST, target.getEyeLocation(), 16, .25, .25, .25, 0.1, new MaterialData(Material.GOLD_BLOCK));
                player.sendMessage("" + finalDamage);
                if (finalDamage >= 4) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.HEAL, 1, 0));
                }
                break;
            }
        }
    }

    void goldSwordRage(final Player player) {
        new BukkitRunnable() {
            private int ticks = 0;
            @Override public void run() {
                if (!player.isValid()) {
                    cancel();
                    return;
                }
                final double damage = player.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).getValue();
                final ItemStack weapon = player.getInventory().getItemInMainHand();
                final Set<UUID> affectedEntities = new HashSet<>();
                ticks += 1;
                if (ticks <= 60) {
                    if (ticks % 3 == 0) {
                        Location eyeLocation = player.getEyeLocation();
                        Vector eyeVector = eyeLocation.toVector();
                        Vector viewDirection = eyeLocation.getDirection();
                        List<LivingEntity> targets = new ArrayList<>();
                        for (Entity nearby: player.getNearbyEntities(4, 4, 4)) {
                            if (nearby instanceof LivingEntity && !nearby.isInvulnerable() && !nearby.equals(player)) {
                                LivingEntity living = (LivingEntity)nearby;
                                Vector entityDirection = living.getLocation().add(0, living.getHeight() * 0.5, 0).toVector().subtract(eyeVector).normalize();
                                double angle = viewDirection.angle(entityDirection);
                                if (angle < Math.PI * 0.5) targets.add(living);
                            }
                        }
                        Collections.shuffle(targets, random);
                        player.getWorld().playSound(eyeLocation, Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 0.2f, 2.0f);
                        player.getWorld().spawnParticle(Particle.SWEEP_ATTACK, eyeLocation.clone().add(viewDirection.clone().multiply(2)), 1);
                        for (LivingEntity target: targets) {
                            if (0 < damage(target, damage, player, weapon)) {
                                target.getWorld().playSound(target.getEyeLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, SoundCategory.HOSTILE, 0.3f, 1.3f);
                                target.getWorld().spawnParticle(Particle.BLOCK_DUST, target.getEyeLocation(), 16, .25, .25, .25, 0.1, new MaterialData(Material.IRON_BLOCK));
                                affectedEntities.add(target.getUniqueId());
                                break;
                            }
                        }
                    }
                } else {
                    int potionLevel;
                    if (affectedEntities.size() > 2) {
                        player.addPotionEffect(new PotionEffect(PotionEffectType.HEAL, 1, 1));
                    }
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 1, 1);
    }

    void diamondAxeSlash(final Player player) {
        Location eyeLocation = player.getEyeLocation();
        Vector eyeVector = eyeLocation.toVector();
        Vector viewDirection = eyeLocation.getDirection();
        final double damage = player.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).getValue();
        final ItemStack itemInHand = player.getInventory().getItemInMainHand();
        player.getWorld().playSound(player.getEyeLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 0.2f, 1.0f);
        player.getWorld().spawnParticle(Particle.SWEEP_ATTACK, eyeLocation.clone().add(viewDirection), 1, 0, 0, 0, 0);
        for (Entity e: player.getNearbyEntities(4, 4, 4)) {
            if (e instanceof LivingEntity && !e.equals(player)) {
                LivingEntity living = (LivingEntity)e;
                Location entityCenter = living.getLocation().add(0, living.getHeight() * 0.5, 0);
                if (eyeLocation.distanceSquared(entityCenter) <= 16) {
                    Vector entityDirection = entityCenter.toVector().subtract(eyeVector);
                    float angle = entityDirection.angle(viewDirection);
                    if (angle < Math.PI * 0.5) {
                        if (damage(living, damage, player, itemInHand) > 0) {
                            living.getWorld().playSound(living.getEyeLocation(), Sound.BLOCK_ANVIL_PLACE, SoundCategory.HOSTILE, 0.3f, 1.3f);
                            living.getWorld().spawnParticle(Particle.BLOCK_DUST, living.getEyeLocation(), 16, .25, .25, .25, 0.1, new MaterialData(Material.DIAMOND_BLOCK));
                        }
                    }
                }
            }
        }
    }

    void diamondAxeThrow(final Player player) {
        final ArmorStand armorStand = player.getWorld().spawn(player.getLocation(), ArmorStand.class, as -> {
                as.setVisible(false);
                as.setArms(true);
                as.setGravity(false);
                as.setInvulnerable(true);
                as.getEquipment().setItemInMainHand(new ItemStack(Material.DIAMOND_AXE));
                as.setRightArmPose(new EulerAngle(Math.PI * 1, 0, 0));
            });
        Vector velo = player.getLocation().getDirection().normalize().multiply(0.7);
        final SimpleScriptEntity.Watcher watcher = (SimpleScriptEntity.Watcher)CustomPlugin.getInstance().getEntityManager().wrapEntity(armorStand, SimpleScriptEntity.CUSTOM_ID);
        watcher.setEventHandler(event -> {
                if (event instanceof EntityDamageEvent) {
                    ((EntityDamageEvent)event).setCancelled(true);
                }
            });
        player.playSound(player.getEyeLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 0.2f, 1.5f);
        double damage = player.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).getValue();
        ItemStack itemInMainHand = player.getInventory().getItemInMainHand();
        new BukkitRunnable() {
            private int ticks = 0;
            @Override
            public void run() {
                if (!player.isValid()) {
                    armorStand.remove();
                    cancel();
                    return;
                }
                if (!armorStand.isValid()) {
                    cancel();
                    return;
                }
                int oldTicks = ticks;
                ticks += 1;
                if (oldTicks < 30) {
                    armorStand.setVelocity(velo);
                    armorStand.teleport(armorStand.getLocation().add(velo));
                    armorStand.setRightArmPose(armorStand.getRightArmPose().add(0.3, 0, 0));
                    boolean didHit = false;
                    for (Entity nearby: armorStand.getNearbyEntities(0.5, 0.5, 0.5)) {
                        if (nearby instanceof LivingEntity && !nearby.isInvulnerable() && !nearby.equals(player)) {
                            didHit = true;
                            break;
                        }
                    }
                    Location axeLoc = armorStand.getLocation().add(0, 1.5, 0);
                    if (axeLoc.getBlock().getType().isSolid()) didHit = true;
                    if (didHit) {
                        armorStand.getWorld().playSound(axeLoc, Sound.ENTITY_WITHER_BREAK_BLOCK, SoundCategory.PLAYERS, 0.5f, 0.8f);
                        armorStand.getWorld().spawnParticle(Particle.EXPLOSION_HUGE, axeLoc, 1);
                        for (Entity nearby: armorStand.getNearbyEntities(3, 3, 3)) {
                            if (nearby instanceof LivingEntity && !nearby.isInvulnerable() && !nearby.equals(player)) {
                                LivingEntity living = (LivingEntity)nearby;
                                damage(living, damage, player, itemInMainHand);
                            }
                        }
                        armorStand.remove();
                        cancel();
                    }
                } else {
                    armorStand.remove();
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 1, 1);
    }

    void goldAxeArea(final Player player) {
    }

    void goldAxeArea2(final Player player) {
    }

    void ironHammerSmash(final Player player) {
    }

    void ironHammerSmash2(final Player player) {
    }
}
