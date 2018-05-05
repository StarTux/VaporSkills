package com.winthier.skills;

import com.winthier.exploits.bukkit.BukkitExploits;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

final class BrawlSkill extends Skill {
    static final String DISARM_COOLDOWN = "brawl_disarm";
    static final String STEAL_COOLDOWN = "brawl_steal";
    static final String STOLEN_SCOREBOARD_TAG = "winthier.brawl_skill.stolen";
    private long killDistanceInterval = 300;
    private double minKillDistance = 16;
    private final Random random = new Random(System.currentTimeMillis());
    private boolean recursionLock = false;

    BrawlSkill(SkillsPlugin plugin) {
        super(plugin, SkillType.BRAWL);
    }

    @Override
    public void configure() {
        killDistanceInterval = getConfig().getLong("KillDistanceInterval", 300);
        minKillDistance = getConfig().getDouble("MinKillDistance", 16);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (recursionLock) return;
        if (!(event.getDamager() instanceof Player)) return;
        if (!(event.getEntity() instanceof LivingEntity)) return;
        final Player player = (Player)event.getDamager();
        final UUID uuid = player.getUniqueId();
        if (!allowPlayer(player)) return;
        final LivingEntity entity = (LivingEntity)event.getEntity();
        // Perks
        ItemStack handItem = player.getInventory().getItemInMainHand();
        switch (handItem.getType()) {
        case WOOD_SWORD:
        case STONE_SWORD:
        case GOLD_SWORD:
        case IRON_SWORD:
        case DIAMOND_SWORD:
            if (plugin.getScore().hasPerk(uuid, Perk.BRAWL_SWORD_DAMAGE)) {
                final int skillLevel = plugin.getScore().getSkillLevel(uuid, skillType);
                switch (handItem.getType()) {
                case GOLD_SWORD:
                    if (plugin.getScore().hasPerk(uuid, Perk.BRAWL_GOLD_SWORD_LIFE_STEAL)) {
                        double heal = event.getFinalDamage();
                        heal *= linearSkillBonus(0.5, skillLevel);
                        heal = Math.min(heal + player.getHealth(), player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());
                        player.setHealth(heal);
                    }
                    break;
                case IRON_SWORD:
                    if (plugin.getScore().hasPerk(uuid, Perk.BRAWL_IRON_SWORD_KNOCKOUT)) {
                        entity.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 20, 9));
                        entity.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 20, 9));
                    }
                    break;
                case DIAMOND_SWORD:
                    if (plugin.getScore().hasPerk(uuid, Perk.BRAWL_DIAMOND_SWORD_BLEED)) {
                        int duration = (int)linearSkillBonus(skillLevel, 400);
                        entity.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, duration, 1));
                    }
                    break;
                default:
                    break;
                }
                // Doing damage increase last. Perk is checked at the top.
                double damage = event.getFinalDamage();
                damage += linearSkillBonus(damage, skillLevel);
                if (plugin.getScore().hasPerk(uuid, Perk.BRAWL_SWORD_DAMAGE_2)) damage *= 2.0;
                event.setDamage(damage);
            }
            break;
        case WOOD_AXE:
        case STONE_AXE:
        case GOLD_AXE:
        case IRON_AXE:
        case DIAMOND_AXE:
            if (plugin.getScore().hasPerk(uuid, Perk.BRAWL_AXE_AOE)) {
                final int skillLevel = plugin.getScore().getSkillLevel(uuid, skillType);
                List<LivingEntity> targets = new ArrayList<>();
                for (Entity e: player.getNearbyEntities(4, 2, 4)) {
                    if (e instanceof Monster) targets.add((LivingEntity)e);
                }
                Location playerLocation = player.getLocation();
                Vector playerVector = playerLocation.toVector();
                for (LivingEntity target: targets) {
                    switch (handItem.getType()) {
                    case GOLD_AXE:
                        if (plugin.getScore().hasPerk(uuid, Perk.BRAWL_GOLD_AXE_FIRE)) {
                            int duration = (int)linearSkillBonus(skillLevel, 400);
                            target.setFireTicks(Math.max(target.getFireTicks(), duration));
                        }
                        break;
                    case IRON_AXE:
                        if (plugin.getScore().hasPerk(uuid, Perk.BRAWL_IRON_AXE_PARALYSIS)) {
                            int duration = (int)linearSkillBonus(skillLevel, 40);
                            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, duration, 9));
                            target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, duration, 9));
                        }
                        break;
                    case DIAMOND_AXE:
                        if (plugin.getScore().hasPerk(uuid, Perk.BRAWL_DIAMOND_AXE_BLEED)) {
                            int duration = (int)linearSkillBonus(skillLevel, 200);
                            target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, duration, 0));
                        }
                        break;
                    default:
                        break;
                    }
                    if (plugin.getScore().hasPerk(uuid, Perk.BRAWL_AXE_KNOCKBACK)) {
                        double resist = target.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE).getValue();
                        if (resist < 1.0) {
                            Vector vec = target.getLocation().toVector().subtract(playerVector).normalize().multiply(2 * (1.0 - resist));
                            target.setVelocity(vec);
                        }
                    }
                    if (!target.equals(entity)) {
                        recursionLock = true;
                        target.damage(event.getFinalDamage(), player);
                        recursionLock = false;
                    }
                }
            }
            break;
        case AIR:
            // Unarmed
            double luck = player.getAttribute(Attribute.GENERIC_LUCK).getValue();
            if (plugin.getScore().hasPerk(uuid, Perk.BRAWL_UNARMED_DISARM)
                && plugin.getSession(uuid).getCooldown(DISARM_COOLDOWN) == 0) {
                final int skillLevel = plugin.getScore().getSkillLevel(uuid, skillType);
                EntityEquipment equip = entity.getEquipment();
                boolean attempted = false;
                if (equip.getItemInMainHand().getType() != Material.AIR) {
                    double chance = equip.getItemInMainHandDropChance();
                    if (chance > 0) {
                        plugin.getSession(uuid).setCooldown(DISARM_COOLDOWN, 20);
                        attempted = true;
                        if (random.nextDouble() < chance + luck * 10) {
                            ItemStack drop = equip.getItemInMainHand();
                            equip.setItemInMainHand(null);
                            player.getWorld().dropItem(player.getEyeLocation(), drop.clone()).setPickupDelay(0);
                        }
                    }
                }
                if (!attempted && plugin.getScore().hasPerk(uuid, Perk.BRAWL_UNARMED_UNDRESS)) {
                    ItemStack[] items = equip.getArmorContents();
                    for (int i = 3; !attempted && i >= 0; i -= 1) {
                        ItemStack item = items[i];
                        if (item != null && item.getType() != Material.AIR) {
                            final double chance;
                            switch (i) {
                            case 3: chance = equip.getHelmetDropChance(); break;
                            case 2: chance = equip.getChestplateDropChance(); break;
                            case 1: chance = equip.getLeggingsDropChance(); break;
                            case 0: default: chance = equip.getBootsDropChance(); break;
                            }
                            if (chance > 0) {
                                attempted = true;
                                plugin.getSession(uuid).setCooldown(DISARM_COOLDOWN, 40);
                                if (random.nextDouble() < chance + luck * 10) {
                                    items[i] = null;
                                    equip.setArmorContents(items);
                                    player.getWorld().dropItem(player.getEyeLocation(), item.clone()).setPickupDelay(0);
                                }
                            }
                        }
                    }
                }
            }
            if (plugin.getScore().hasPerk(uuid, Perk.BRAWL_UNARMED_STEAL)
                && plugin.getSession(uuid).getCooldown(STEAL_COOLDOWN) == 0) {
                ItemStack stolen = stealItem(entity);
                if (stolen != null) {
                    plugin.getSession(uuid).setCooldown(STEAL_COOLDOWN, 200);
                    if (random.nextDouble() < luck * 10) {
                        if (stolen != null) {
                            player.getWorld().dropItem(player.getEyeLocation(), stolen.clone()).setPickupDelay(0);
                        }
                    }
                }
            }
            break;
        default:
            return;
        }
        // Give Reward
        if (BukkitExploits.getInstance().recentKillDistance(player, entity.getLocation(), killDistanceInterval) < minKillDistance) return;
        double percentage = BukkitExploits.getInstance().getEntityDamageByPlayerRemainderPercentage(entity, Math.min(entity.getHealth(), event.getFinalDamage()));
        if (plugin.hasDebugMode(player)) Msg.msg(player, "&eBrawl Dmg=%.02f/%.02f Pct=%.02f", event.getFinalDamage(), entity.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue(), percentage);
        Reward reward = getReward(Reward.Category.DAMAGE_ENTITY, entity.getType().name(), null, null);
        giveReward(player, reward, percentage);
    }

    private ItemStack stealItem(LivingEntity entity) {
        if (entity.getScoreboardTags().contains(STOLEN_SCOREBOARD_TAG)) return null;
        final double roll;
        switch (entity.getType()) {
        case SKELETON:
        case STRAY:
            return new ItemStack(Material.BONE);
        case ZOMBIE:
        case HUSK:
        case ZOMBIE_VILLAGER:
            return new ItemStack(Material.ROTTEN_FLESH);
        case CREEPER:
            return new ItemStack(Material.SULPHUR);
        case ENDERMAN:
            return new ItemStack(Material.ENDER_PEARL);
        case SPIDER:
        case CAVE_SPIDER:
            if (random.nextDouble() < 0.33) {
                return new ItemStack(Material.SPIDER_EYE);
            } else {
                return new ItemStack(Material.STRING);
            }
        case WITCH:
            roll = random.nextDouble();
            if (roll < 0.125) {
                return new ItemStack(Material.GLASS_BOTTLE);
            } else if (roll < 0.250) {
                return new ItemStack(Material.GLOWSTONE_DUST);
            } else if (roll < 0.375) {
                return new ItemStack(Material.SULPHUR);
            } else if (roll < 0.500) {
                return new ItemStack(Material.REDSTONE);
            } else if (roll < 0.625) {
                return new ItemStack(Material.SPIDER_EYE);
            } else if (roll < 0.750) {
                return new ItemStack(Material.SUGAR);
            } else {
                return new ItemStack(Material.STICK);
            }
        case EVOKER:
            entity.addScoreboardTag(STOLEN_SCOREBOARD_TAG);
            return new ItemStack(Material.TOTEM);
        case BLAZE:
            return new ItemStack(Material.BLAZE_ROD);
        case WITHER_SKELETON:
            if (random.nextDouble() < 0.025) {
                entity.addScoreboardTag(STOLEN_SCOREBOARD_TAG);
                return new ItemStack(Material.SKULL_ITEM, 1, (short)1);
            } else {
                if (random.nextBoolean()) {
                    return new ItemStack(Material.BONE);
                } else {
                    return new ItemStack(Material.COAL);
                }
            }
        default:
            return null;
        }
    }
}
