package com.winthier.skills;

import com.winthier.exploits.bukkit.BukkitExploits;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

final class BrawlSkill extends Skill implements Listener {
    private long killDistanceInterval = 300;
    private double minKillDistance = 16;

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
                        heal *= linearSkillBonus(0.1, skillLevel);
                        heal = Math.min(heal + player.getHealth(), player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());
                        player.setHealth(heal);
                    }
                    break;
                case IRON_SWORD:
                    if (plugin.getScore().hasPerk(uuid, Perk.BRAWL_IRON_SWORD_KNOCKOUT)) {
                        entity.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 20, 10));
                        entity.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 20, 10));
                    }
                    break;
                case DIAMOND_SWORD:
                    if (plugin.getScore().hasPerk(uuid, Perk.BRAWL_DIAMOND_SWORD_PIERCE)) {
                        // TODO
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
                for (Entity e: player.getNearbyEntities(1, 0, 1)) {
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
                            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, duration, 10));
                            target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, duration, 10));
                        }
                        break;
                    case DIAMOND_AXE:
                        if (plugin.getScore().hasPerk(uuid, Perk.BRAWL_DIAMOND_AXE_BLEED)) {
                            int duration = (int)linearSkillBonus(skillLevel, 40);
                            target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 200, 1));
                        }
                        break;
                    default:
                        break;
                    }
                    if (plugin.getScore().hasPerk(uuid, Perk.BRAWL_AXE_KNOCKBACK)) {
                        double resist = target.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE).getValue();
                        if (resist < 1.0) {
                            Vector vec = target.getLocation().toVector().subtract(playerVector).normalize().multiply(2 * (1.0 - resist));
                        }
                    }
                }
            }
            break;
        case AIR:
            if (plugin.getScore().hasPerk(uuid, Perk.BRAWL_UNARMED_DISARM)) {
                final int skillLevel = plugin.getScore().getSkillLevel(uuid, skillType);
                if (plugin.getScore().hasPerk(uuid, Perk.BRAWL_UNARMED_UNDRESS)) {
                }
                if (plugin.getScore().hasPerk(uuid, Perk.BRAWL_UNARMED_STEAL)) {
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
        giveReward(player, rewardForEntity(entity), percentage);
    }
}
