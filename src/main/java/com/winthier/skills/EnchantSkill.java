package com.winthier.skills;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownExpBottle;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

final class EnchantSkill extends Skill {
    EnchantSkill(SkillsPlugin plugin) {
        super(plugin, SkillType.ENCHANT);
    }

    enum Enchant {
        AQUA_AFFINITY(Enchantment.WATER_WORKER, Material.WATER_LILY),
        BANE_OF_ARTHROPODS(Enchantment.DAMAGE_ARTHROPODS, Material.SPIDER_EYE),
        BLAST_PROTECTION(Enchantment.PROTECTION_EXPLOSIONS, Material.SULPHUR),
        CURSE_OF_BINDING(Enchantment.BINDING_CURSE, Material.WEB),
        CURSE_OF_VANISHING(Enchantment.VANISHING_CURSE, Material.ENDER_PEARL),
        DEPTH_STRIDER(Enchantment.DEPTH_STRIDER, Material.ENDER_STONE), // TODO: Maybe Nautilus Shell?
        EFFICIENCY(Enchantment.DIG_SPEED, Material.REDSTONE),
        FEATHER_FALLING(Enchantment.PROTECTION_FALL, Material.FEATHER),
        FIRE_ASPECT(Enchantment.FIRE_ASPECT, Material.BLAZE_POWDER),
        FIRE_PROTECTION(Enchantment.PROTECTION_FIRE, Material.BLAZE_ROD),
        FLAME(Enchantment.ARROW_FIRE, Material.BLAZE_POWDER),
        FORTUNE(Enchantment.LOOT_BONUS_BLOCKS, Material.RABBIT_FOOT),
        FROST_WALKER(Enchantment.FROST_WALKER, Material.PACKED_ICE),
        INFINITY(Enchantment.ARROW_INFINITE, Material.SPECTRAL_ARROW),
        KNOCKBACK(Enchantment.KNOCKBACK, Material.PISTON_BASE),
        LOOTING(Enchantment.LOOT_BONUS_MOBS, Material.RABBIT_FOOT),
        LUCK_OF_THE_SEA(Enchantment.LUCK, Material.COOKED_FISH), // TODO: Clownfish
        LURE(Enchantment.LURE, Material.RAW_FISH), // TODO: Sea Pickle
        MENDING(Enchantment.MENDING, Material.GHAST_TEAR),
        POWER(Enchantment.ARROW_DAMAGE, Material.DIAMOND),
        PROJECTILE_PROTECTION(Enchantment.PROTECTION_PROJECTILE, Material.LEATHER), // TODO: Scute
        PROTECTION(Enchantment.PROTECTION_ENVIRONMENTAL, Material.IRON_INGOT),
        PUNCH(Enchantment.ARROW_KNOCKBACK, Material.PISTON_STICKY_BASE),
        RESPIRATION(Enchantment.OXYGEN, Material.RAW_FISH), // TODO: Pufferfish
        SHARPNESS(Enchantment.DAMAGE_ALL, Material.EMERALD),
        SILK_TOUCH(Enchantment.SILK_TOUCH, Material.FEATHER),
        SMITE(Enchantment.DAMAGE_UNDEAD, Material.ROTTEN_FLESH),
        SWEEPING_EDGE(Enchantment.SWEEPING_EDGE, Material.EYE_OF_ENDER),
        THORNS(Enchantment.THORNS, Material.DOUBLE_PLANT), // TODO: Rose bush
        UNBREAKING(Enchantment.DURABILITY, Material.OBSIDIAN);
        // TODO: Loyalty: Name tag
        // TODO: Impaling: Nautilus shell
        // TODO: Riptide: Prismarine Crystal
        // TODO: Channeling: Glowstone

        final Enchantment bukkitEnchant;
        final Material sacrificeMaterial;
        final String displayName;
        static final Map<Enchantment, Enchant> BUKKIT_ENCHANT_MAP = new HashMap<>();

        static {
            for (Enchantment bukkitEnchant: Enchantment.values()) {
                for (Enchant enchant: values()) {
                    if (bukkitEnchant.equals(enchant.bukkitEnchant)) {
                        BUKKIT_ENCHANT_MAP.put(bukkitEnchant, enchant);
                        break;
                    }
                }
                if (!BUKKIT_ENCHANT_MAP.containsKey(bukkitEnchant)) {
                    System.err.println("EnchantSkill: Missing Bukkit Enchant: " + bukkitEnchant);
                }
            }
        }

        Enchant(Enchantment bukkitEnchant, Material sacrificeMaterial) {
            this.bukkitEnchant = bukkitEnchant;
            this.sacrificeMaterial = sacrificeMaterial;
            String[] toks = name().split("_");
            StringBuilder sb = new StringBuilder(Msg.capitalize(toks[0]));
            for (int i = 1; i < toks.length; i += 1) {
                switch (toks[i]) {
                case "OF": sb.append(" of"); break;
                case "THE": sb.append(" the"); break;
                default: sb.append(" ").append(Msg.capitalize(toks[i])); break;
                }
            }
            displayName = sb.toString();
        }
    }

    @RequiredArgsConstructor
    static final class EnchantingStore {
        enum Error {
            ALREADY_ENCHANTED,
            NO_ENCHANTS,
            NO_ITEMS,
            TOO_MANY_ENCHANTS,
            IMPOSSIBLE_ENCHANT_LEVEL,
            INVALID_SACRIFICE,
            ENCHANT_CONFLICT,
            TOO_MANY_ITEMS,
            WRONG_TARGET,
            TOO_LOW_LEVEL;
            EnchantingStore store() {
                EnchantingStore result = new EnchantingStore();
                result.error = this;
                return result;
            }
        }
        private Error error = null;
        private final ArmorStand armorStand;
        private final Block topBlock;
        private final ItemStack enchantedItem;
        private final EquipmentSlot enchantedItemSlot;
        private final Map<Enchant, Integer> enchantMap;
        private final Map<Material, Integer> sacrificeMap;
        private final int expLevelCost;

        EnchantingStore() {
            this.armorStand = null;
            this.topBlock = null;
            this.enchantedItem = null;
            this.enchantedItemSlot = null;
            this.enchantMap = null;
            this.sacrificeMap = null;
            this.expLevelCost = 0;
        }

        static EnchantingStore of(ArmorStand armorStand, Player player) {
            // Altar
            final Block topBlock = armorStand.getLocation().getBlock().getRelative(0, -1, 0);
            if (topBlock.getType() != Material.BOOKSHELF) return null;
            final int radius = 1;
            for (int dz = -radius; dz <= radius; dz += 1) {
                for (int dx = -radius; dx <= radius; dx += 1) {
                    if (topBlock.getRelative(dx, -1, dz).getType() != Material.LAPIS_BLOCK) return null;
                }
            }
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
            if (!enchantedItem.getEnchantments().isEmpty()) return Error.ALREADY_ENCHANTED.store();
            // Enchantments
            Map<Enchant, Integer> enchantMap = new EnumMap<>(Enchant.class);
            Map<Material, Integer> sacrificeMap = new HashMap<>();
            for (int dx = -1; dx <= 1; dx += 1) {
                for (int dz = -1; dz <= 1; dz += 1) {
                    BlockState state = topBlock.getRelative(dx, 0, dz).getState();
                    if (!(state instanceof Container)) continue;
                    for (ItemStack item: ((Container)state).getInventory()) {
                        if (item == null || item.getType() == Material.AIR) continue;
                        boolean isIngredient = false;
                        for (Enchant enchant: Enchant.values()) {
                            Material material = item.getType();
                            if (enchant.sacrificeMaterial == material
                                && enchant.bukkitEnchant.getItemTarget().includes(enchantedItem)) {
                                Integer amount = enchantMap.get(enchant);
                                if (amount == null) amount = 0;
                                amount += item.getAmount();
                                enchantMap.put(enchant, amount);
                                isIngredient = true;
                                amount = sacrificeMap.get(material);
                                if (amount == null) amount = 0;
                                amount += item.getAmount();
                                sacrificeMap.put(material, amount);
                                break;
                            }
                        }
                        if (!isIngredient) return Error.INVALID_SACRIFICE.store();
                    }
                }
            }
            // Conflicts
            int totalLevel = 0;
            for (Map.Entry<Enchant, Integer> entry: enchantMap.entrySet()) {
                Enchant enchant = entry.getKey();
                int level = entry.getValue();
                totalLevel += level;
                if (level > enchant.bukkitEnchant.getMaxLevel()) {
                    return Error.IMPOSSIBLE_ENCHANT_LEVEL.store();
                }
                if (!enchant.bukkitEnchant.getItemTarget().includes(enchantedItem)) {
                    return Error.WRONG_TARGET.store();
                }
                for (Enchant enchant2: enchantMap.keySet()) {
                    if (enchant != enchant2 && enchant.bukkitEnchant.conflictsWith(enchant2.bukkitEnchant)) {
                        return Error.ENCHANT_CONFLICT.store();
                    }
                }
            }
            // Exp. Winging it for now.
            int expLevelCost = totalLevel;
            EnchantingStore result = new EnchantingStore(armorStand, topBlock, enchantedItem, enchantedItemSlot, enchantMap, sacrificeMap, expLevelCost);
            if (player.getLevel() < expLevelCost * 10) result.error = Error.TOO_LOW_LEVEL;
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
                updateEnchantingArmorStand(player, armorStand);
            }
        }.runTask(plugin);
    }

    private void updateEnchantingArmorStand(Player player, ArmorStand armorStand) {
        EnchantingStore store = EnchantingStore.of(armorStand, player);
        if (store == null) return;
        if (store.error != null) {
            switch (store.error) {
            case NO_ENCHANTS:
            case NO_ITEMS:
                armorStand.setCustomName(null);
                armorStand.setCustomNameVisible(false);
                return;
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
                armorStand.setCustomName(Msg.format("&c%s", Msg.capitalEnumName(store.error)));
                armorStand.setCustomNameVisible(true);
                break;
            }
            Location loc = armorStand.getEyeLocation();
            armorStand.getWorld().playSound(loc, Sound.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.BLOCKS, 1.0f, 0.5f);
        } else {
            StringBuilder sb = new StringBuilder(ChatColor.GRAY.toString());
            boolean latter = false;
            for (Map.Entry<Enchant, Integer> entry: store.enchantMap.entrySet()) {
                if (latter) {
                    sb.append(", ");
                } else {
                    latter = true;
                }
                int level = entry.getValue();
                Enchant enchant = entry.getKey();
                if (level > 0) {
                    sb.append(enchant.displayName).append(" ").append(Msg.roman(level));
                } else {
                    sb.append(enchant.displayName);
                }
            }
            sb.append(Msg.format("&d %d Levels", store.expLevelCost));
            armorStand.setCustomName(sb.toString());
            armorStand.setCustomNameVisible(true);
            Location loc = armorStand.getEyeLocation();
            armorStand.getWorld().playSound(loc, Sound.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.BLOCKS, 1.0f, 2.0f);
        }
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
        ItemStack enchantedItem = store.enchantedItem.clone();
        for (Map.Entry<Enchant, Integer> entry: store.enchantMap.entrySet()) {
            enchantedItem.addEnchantment(entry.getKey().bukkitEnchant, entry.getValue());
        }
        switch (store.enchantedItemSlot) {
        case HEAD: armorStand.setHelmet(enchantedItem); break;
        case CHEST: armorStand.setChestplate(enchantedItem); break;
        case LEGS: armorStand.setLeggings(enchantedItem); break;
        case FEET: armorStand.setBoots(enchantedItem); break;
        case HAND: armorStand.setItemInHand(enchantedItem); break;
        default: break;
        }
        player.setLevel(player.getLevel() - store.expLevelCost);
        for (int dx = -1; dx <= 1; dx += 1) {
            for (int dz = -1; dz <= 1; dz += 1) {
                BlockState state = store.topBlock.getRelative(dx, 0, dz).getState();
                if (!(state instanceof Container)) continue;
                for (ItemStack item: ((Container)state).getInventory()) {
                    if (item == null || item.getType() == Material.AIR) continue;
                    boolean isIngredient = false;
                    Material material = item.getType();
                    for (Map.Entry<Material, Integer> entry: store.sacrificeMap.entrySet()) {
                        if (entry.getKey() == material) {
                            int required = entry.getValue();
                            if (required == 0) continue;
                            int have = item.getAmount();
                            int reduce = Math.min(required, have);
                            entry.setValue(required - reduce);
                            item.setAmount(have - reduce);
                            break;
                        }
                    }
                }
            }
        }
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
