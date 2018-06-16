package com.winthier.skills;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownExpBottle;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

final class EnchantSkill extends Skill {
    EnchantSkill(SkillsPlugin plugin) {
        super(plugin, SkillType.ENCHANT);
    }

    @RequiredArgsConstructor
    static final class EnchantingStore {
        enum Error {
            NO_ENCHANTS,
            NO_ITEMS,
            TOO_MANY_ENCHANTS,
            ENCHANT_CONFLICT,
            TOO_MANY_ITEMS,
            WRONG_TARGET,
            TOO_LOW_LEVEL;
            EnchantingStore store() {
                EnchantingStore result = new EnchantingStore(null, null, null, 0, 0);
                result.error = this;
                return result;
            }
        }
        private Error error = null;
        private final ItemStack enchantedItem;
        private final EquipmentSlot enchantedItemSlot;
        private final Enchantment enchantment;
        private final int enchantmentLevel;
        private final int expLevelCost;

        static EnchantingStore of(ArmorStand armorStand, Player player) {
            // Altar
            final Block topBlock = armorStand.getLocation().getBlock().getRelative(0, -1, 0);
            if (topBlock.getType() != Material.BOOKSHELF) return null;
            final int radius = 1;
            for (int dz = -radius; dz <= radius; dz += 1) {
                for (int dx = -radius; dx <= radius; dx += 1) {
                    if (topBlock.getRelative(dx, -1, dz).getType() != Material.BOOKSHELF) return null;
                }
            }
            // Enchantment
            Enchantment enchantment = null;
            int enchantmentLevel = 0;
            for (Entity nearby: armorStand.getWorld().getNearbyEntities(topBlock.getLocation().add(0.5, 0.5, 0.5), 1, 1, 1)) {
                if (!(nearby instanceof ItemFrame)) continue;
                ItemFrame itemFrame = (ItemFrame)nearby;
                if (!itemFrame.getLocation().getBlock().getRelative(itemFrame.getAttachedFace()).equals(topBlock)) continue;
                ItemStack itemFrameItem = itemFrame.getItem();
                if (itemFrameItem == null || itemFrameItem.getType() == Material.AIR) continue;
                if (itemFrameItem.getType() != Material.ENCHANTED_BOOK) continue;
                EnchantmentStorageMeta meta = (EnchantmentStorageMeta)itemFrameItem.getItemMeta();
                for (Map.Entry<Enchantment, Integer> entry: meta.getStoredEnchants().entrySet()) {
                    if (enchantment != null) return Error.TOO_MANY_ENCHANTS.store();
                    enchantment = entry.getKey();
                    enchantmentLevel = entry.getValue();
                }
                for (Map.Entry<Enchantment, Integer> entry: meta.getEnchants().entrySet()) {
                    if (enchantment != null) return Error.TOO_MANY_ENCHANTS.store();
                    enchantment = entry.getKey();
                    enchantmentLevel = entry.getValue();
                }
            }
            if (enchantment == null) return Error.NO_ENCHANTS.store();
            // Enchanted item
            ItemStack enchantedItem = null;
            EquipmentSlot enchantedItemSlot = null;
            List<ItemStack> items = Arrays.asList(armorStand.getHelmet(), armorStand.getChestplate(), armorStand.getLeggings(), armorStand.getBoots(), armorStand.getItemInHand());
            for (int i = 0; i < items.size(); i += 1) {
                ItemStack item = items.get(i);
                if (item == null || item.getType() == Material.AIR) continue;
                if (enchantedItem != null) return Error.TOO_MANY_ITEMS.store();
                enchantedItem = item;
                switch (i) {
                case 0: enchantedItemSlot = EquipmentSlot.HEAD; break;
                case 1: enchantedItemSlot = EquipmentSlot.CHEST; break;
                case 2: enchantedItemSlot = EquipmentSlot.LEGS; break;
                case 3: enchantedItemSlot = EquipmentSlot.FEET; break;
                case 4: enchantedItemSlot = EquipmentSlot.HAND; break;
                default: break;
                }
            }
            if (enchantedItem == null) return Error.NO_ITEMS.store();
            if (!enchantment.getItemTarget().includes(enchantedItem)) {
                return Error.WRONG_TARGET.store();
            }
            ItemMeta meta = enchantedItem.getItemMeta();
            for (Map.Entry<Enchantment, Integer> entry: meta.getEnchants().entrySet()) {
                if (entry.getKey().conflictsWith(enchantment)) {
                    return Error.ENCHANT_CONFLICT.store();
                }
            }
            // Exp. Winging it for now.
            int expLevelCost = 3;
            EnchantingStore result = new EnchantingStore(enchantedItem, enchantedItemSlot, enchantment, enchantmentLevel, expLevelCost);
            if (player.getLevel() < expLevelCost * 30) result.error = Error.TOO_LOW_LEVEL;
            return result;
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    public void onPlayerArmorStandManipulate(PlayerArmorStandManipulateEvent event) {
        final Player player = event.getPlayer();
        if (!allowPlayer(player)) return;
        final ArmorStand armorStand = event.getRightClicked();
        new BukkitRunnable() {
            @Override public void run() {
                EnchantingStore store = EnchantingStore.of(armorStand, player);
                if (store == null) return;
                if (store.error != null) {
                    switch (store.error) {
                    case NO_ENCHANTS:
                    case NO_ITEMS:
                        armorStand.setCustomName(null);
                        armorStand.setCustomNameVisible(false);
                        break;
                    case TOO_MANY_ENCHANTS:
                        armorStand.setCustomName(Msg.format("&cToo Many Enchantments"));
                        armorStand.setCustomNameVisible(true);
                        break;
                    case ENCHANT_CONFLICT:
                        armorStand.setCustomName(Msg.format("&cConflicting Enchantments"));
                        armorStand.setCustomNameVisible(true);
                        break;
                    case TOO_MANY_ITEMS:
                        armorStand.setCustomName(Msg.format("&cToo Many Items"));
                        armorStand.setCustomNameVisible(true);
                        break;
                    case TOO_LOW_LEVEL:
                        armorStand.setCustomName(Msg.format("&cRequires Exp Level %d", store.expLevelCost));
                        armorStand.setCustomNameVisible(true);
                        break;
                    case WRONG_TARGET:
                        armorStand.setCustomName(Msg.format("&cIncompatible Item"));
                        armorStand.setCustomNameVisible(true);
                        break;
                    default:
                        break;
                    }
                } else {
                    armorStand.setCustomName(Msg.format("&aCost %d Exp Levels", store.expLevelCost));
                    armorStand.setCustomNameVisible(true);
                }
            }
        }.runTask(plugin);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof ThrownExpBottle)) return;
        if (!(event.getHitEntity() instanceof ArmorStand)) return;
        final ThrownExpBottle expBottle = (ThrownExpBottle)event.getEntity();
        if (!(expBottle.getShooter() instanceof Player)) return;
        final Player player = (Player)expBottle.getShooter();
        if (!allowPlayer(player)) return;
        ArmorStand armorStand = (ArmorStand)event.getHitEntity();
        EnchantingStore store = EnchantingStore.of(armorStand, player);
        if (store == null) return;
        if (store.error != null) return;
        ItemStack item = store.enchantedItem.clone();
        item.addEnchantment(store.enchantment, store.enchantmentLevel);
        switch (store.enchantedItemSlot) {
        case HEAD: armorStand.setHelmet(item); break;
        case CHEST: armorStand.setChestplate(item); break;
        case LEGS: armorStand.setLeggings(item); break;
        case FEET: armorStand.setBoots(item); break;
        case HAND: armorStand.setItemInHand(item); break;
        default: break;
        }
        player.setLevel(player.getLevel() - store.expLevelCost);
        armorStand.setCustomName(null);
        armorStand.setCustomNameVisible(false);
        Location loc = armorStand.getEyeLocation();
        armorStand.getWorld().strikeLightningEffect(loc);
        armorStand.getWorld().playSound(loc, Sound.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.BLOCKS, 1.0f, 1.0f);
    }

        @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onEnchantItem(EnchantItemEvent event) {
        final Player player = event.getEnchanter();
        if (!allowPlayer(player)) return;
        final int oldLevel = player.getLevel();
        final int levelUsed = event.getExpLevelCost();
        new BukkitRunnable() {
            @Override public void run() {
                int spent = oldLevel - player.getLevel();
                double factor = Math.min(1.0, (double)levelUsed / (double)spent / 10);
                Reward reward = getReward(Reward.Category.SPEND_LEVELS, null, spent, null);
                giveReward(player, reward, factor * factor);
            }
        }.runTask(plugin);
    }
}
