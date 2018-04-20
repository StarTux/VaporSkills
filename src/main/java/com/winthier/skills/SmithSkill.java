package com.winthier.skills;

import com.winthier.custom.util.Dirty;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

final class SmithSkill extends Skill implements Listener {
    @RequiredArgsConstructor
    private static class Metadata {
        static final String KEY = "com.winthier.skill.SmithSkill";
        final UUID uuid;
        final ItemStack inputA, inputB, result;
    }

    SmithSkill(SkillsPlugin plugin) {
        super(plugin, SkillType.SMITH);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onInventoryClick(InventoryClickEvent event) {
        final Inventory inv = event.getInventory();
        if (inv.getType() != InventoryType.ANVIL) return;
        if (event.getSlotType() != InventoryType.SlotType.RESULT) return;
        final Player player = (Player)event.getWhoClicked();
        if (!allowPlayer(player)) return;
        // Input and output slot must not be empty
        ItemStack inputA = inv.getItem(0);
        ItemStack inputB = inv.getItem(1);
        ItemStack result = inv.getItem(2);
        if (inputA == null || inputA.getType() == Material.AIR) return;
        if (inputB == null || inputB.getType() == Material.AIR) return;
        if (result == null || result.getType() == Material.AIR) return;
        final Location anvilLoc = event.getInventory().getLocation();
        if (anvilLoc == null) return;
        final Block anvilBlock = anvilLoc.getBlock();
        Metadata metadata = null;
        for (MetadataValue v: anvilBlock.getMetadata(Metadata.KEY)) {
            if (v.getOwningPlugin() == plugin) {
                metadata = (Metadata)v.value();
                break;
            }
        }
        anvilBlock.removeMetadata(Metadata.KEY, plugin);
        if (metadata != null
            && metadata.uuid.equals(player.getUniqueId())
            && metadata.inputA.equals(inputA)
            && metadata.inputB.equals(inputB)
            && metadata.result.equals(result)) {
            if (event.isShiftClick()) {
                event.getInventory().setItem(2, null);
                if (player.getInventory().addItem(metadata.result).isEmpty()) {
                    event.getInventory().setItem(0, null);
                    event.getInventory().setItem(1, null);
                    event.getInventory().setItem(2, null);
                    onDidImprove(player, metadata);
                }
            } else if (event.getCursor() == null
                       || event.getCursor().getType() == Material.AIR) {
                event.getInventory().setItem(0, null);
                event.getInventory().setItem(1, null);
                event.getInventory().setItem(2, null);
                event.setCursor(metadata.result);
                onDidImprove(player, metadata);
            }
        } else {
            final int level = player.getLevel();
            new BukkitRunnable() {
                @Override public void run() {
                    onAnvilUsed(player, inv, level);
                }
            }.runTask(plugin);
        }
    }

    void onDidImprove(Player player, Metadata metadata) {
        // TODO
    }

    void onAnvilUsed(Player player, Inventory inv, int oldLevel) {
        // Was the output item removed?
        ItemStack result = inv.getItem(2);
        if (result != null && result.getType() != Material.AIR) return;
        // Were levels used up?
        int levelsUsed = oldLevel - player.getLevel();
        if (levelsUsed <= 0) return;
        giveReward(player, rewardForNameAndMaximum("exp_level_cost", levelsUsed));
    }

    private static void addAttribute(ItemStack item, EquipmentSlot slot, Attribute attribute, double amount, int operation, UUID uuid) {
        Dirty.TagWrapper itemTag = Dirty.TagWrapper.getItemTagOf(item);
        Dirty.TagListWrapper attrList = itemTag.getList("AttributeModifiers");
        if (attrList == null) attrList = itemTag.createList("AttributeModifiers");
        Dirty.TagWrapper attrTag = attrList.createCompound();
        final String slotName;
        if (slot == null) {
            if (item.getType().name().contains("HELMET")) {
                slot = EquipmentSlot.HEAD;
            } else if (item.getType().name().contains("CHESTPLATE")) {
                slot = EquipmentSlot.CHEST;
            } else if (item.getType().name().contains("LEGGINGS")) {
                slot = EquipmentSlot.LEGS;
            } else if (item.getType().name().contains("BOOTS")) {
                slot = EquipmentSlot.FEET;
            } else if (item.getType().name().contains("SHIELD")) {
                slot = EquipmentSlot.OFF_HAND;
            } else if (item.getType().name().contains("SWORD")) {
                slot = EquipmentSlot.HAND;
            } else if (item.getType().name().contains("AXE")) {
                slot = EquipmentSlot.HAND;
            } else {
                slot = EquipmentSlot.HAND;
            }
        }
        switch (slot) {
        case HAND: slotName = "mainhand"; break;
        case OFF_HAND: slotName = "offhand"; break;
        default: slotName = slot.name().toLowerCase();
        }
        String[] attrNames = attribute.name().split("_");
        StringBuilder sb = new StringBuilder();
        sb.append(attrNames[0].toLowerCase()).append(".");
        sb.append(attrNames[1].toLowerCase());
        for (int i = 2; i < attrNames.length; i += 1) sb.append(attrNames[i].substring(0, 1)).append(attrNames[i].substring(1).toLowerCase());
        if (uuid == null) uuid = new UUID(1 + (long)slot.ordinal(), 1 + (long)attribute.ordinal());
        final String attrName = sb.toString();
        attrTag.setString("Slot", slotName);
        attrTag.setString("AttributeName", attrName);
        attrTag.setString("Name", attrName);
        attrTag.setDouble("Amount", amount);
        attrTag.setInt("Operation", operation);
        attrTag.setLong("UUIDMost", uuid.getMostSignificantBits());
        attrTag.setLong("UUIDLeast", uuid.getLeastSignificantBits());
    }

    static int getDefaultArmor(Material mat) {
        switch (mat) {
        case LEATHER_HELMET: return 1;
        case LEATHER_CHESTPLATE: return 3;
        case LEATHER_LEGGINGS: return 2;
        case LEATHER_BOOTS: return 1;
        case GOLD_HELMET: return 2;
        case GOLD_CHESTPLATE: return 5;
        case GOLD_LEGGINGS: return 3;
        case GOLD_BOOTS: return 1;
        case CHAINMAIL_HELMET: return 2;
        case CHAINMAIL_CHESTPLATE: return 5;
        case CHAINMAIL_LEGGINGS: return 4;
        case CHAINMAIL_BOOTS: return 1;
        case IRON_HELMET: return 2;
        case IRON_CHESTPLATE: return 6;
        case IRON_LEGGINGS: return 5;
        case IRON_BOOTS: return 2;
        case DIAMOND_HELMET: return 3;
        case DIAMOND_CHESTPLATE: return 8;
        case DIAMOND_LEGGINGS: return 6;
        case DIAMOND_BOOTS: return 3;
        default: return 0;
        }
    }

    static int getDefaultDamage(Material mat) {
        switch (mat) {
        case WOOD_SWORD: return 4;
        case GOLD_SWORD: return 4;
        case STONE_SWORD: return 5;
        case IRON_SWORD: return 6;
        case DIAMOND_SWORD: return 7;
            // Axes
        case WOOD_AXE: return 7;
        case GOLD_AXE: return 7;
        case STONE_AXE: return 9;
        case IRON_AXE: return 9;
        case DIAMOND_AXE: return 9;
        default: return 0;
        }
    }

    static double getDefaultAttackSpeed(Material mat) {
        switch (mat) {
        case WOOD_SWORD:
        case GOLD_SWORD:
        case STONE_SWORD:
        case IRON_SWORD:
        case DIAMOND_SWORD:
            return 1.6;
            // Axes
        case WOOD_AXE: return 0.8;
        case GOLD_AXE: return 1.0;
        case STONE_AXE: return 0.8;
        case IRON_AXE: return 0.9;
        case DIAMOND_AXE: return 1.0;
        default: return 0;
        }
    }

    @EventHandler
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        final Location anvilLoc = event.getInventory().getLocation();
        if (anvilLoc == null) return;
        final Block anvilBlock = anvilLoc.getBlock();
        anvilBlock.removeMetadata(Metadata.KEY, plugin);
        final Player player = (Player)event.getView().getPlayer();
        final UUID uuid = player.getUniqueId();
        int skillLevel = plugin.getScore().getSkillLevel(uuid, skillType);
        final ItemStack inputA = event.getInventory().getItem(0);
        final ItemStack inputB = event.getInventory().getItem(1);
        if (event.getInventory().getRenameText() != null && event.getInventory().getRenameText().length() > 0) return;
        if (inputA == null || inputA.getType() == Material.AIR) return;
        if (inputB == null || inputB.getType() == Material.AIR) return;
        if (!new ItemStack(inputA.getType()).equals(inputA)) return;
        if (!new ItemStack(inputB.getType()).equals(inputB)) return;
        ItemStack result = null;
        switch (inputA.getType()) {
        case LEATHER_HELMET:
        case LEATHER_CHESTPLATE:
        case LEATHER_LEGGINGS:
        case LEATHER_BOOTS:
            if (inputB.getType() == Material.LEATHER
                && plugin.getScore().hasPerk(uuid, Perk.SMITH_IMPROVE_LEATHER)) {
                result = new ItemStack(inputA.getType());
                ItemMeta meta = result.getItemMeta();
                meta.setUnbreakable(true);
                result.setItemMeta(meta);
                if (plugin.getScore().hasPerk(uuid, Perk.SMITH_LEATHER_ARMOR_SPEED)) {
                    result = Dirty.assertItemTag(result);
                    double speed = Math.min(0.02, (double)skillLevel * 0.03 / 100.0);
                    addAttribute(result, null, Attribute.GENERIC_MOVEMENT_SPEED, speed, 0, null);
                    double armor = getDefaultArmor(result.getType());
                    addAttribute(result, null, Attribute.GENERIC_ARMOR, armor, 0, null);
                }
            } else if (inputB.getType() == Material.IRON_INGOT
                && plugin.getScore().hasPerk(uuid, Perk.SMITH_IMPROVE_MAIL)) {
                switch (inputA.getType()) {
                case LEATHER_HELMET: result = new ItemStack(Material.CHAINMAIL_HELMET); break;
                case LEATHER_CHESTPLATE: result = new ItemStack(Material.CHAINMAIL_CHESTPLATE); break;
                case LEATHER_LEGGINGS: result = new ItemStack(Material.CHAINMAIL_LEGGINGS); break;
                case LEATHER_BOOTS: result = new ItemStack(Material.CHAINMAIL_BOOTS); break;
                default: return;
                }
                ItemMeta meta = result.getItemMeta();
                meta.setUnbreakable(true);
                result.setItemMeta(meta);
                if (plugin.getScore().hasPerk(uuid, Perk.SMITH_MAIL_ARMOR_DAMAGE)) {
                    result = Dirty.assertItemTag(result);
                    double damage = Math.min(1.0, (double)skillLevel * 0.01);
                    addAttribute(result, null, Attribute.GENERIC_ATTACK_DAMAGE, damage, 1, null);
                    double armor = getDefaultArmor(result.getType());
                    addAttribute(result, null, Attribute.GENERIC_ARMOR, armor, 0, null);
                }
            }
            break;
        case GOLD_HELMET:
        case GOLD_CHESTPLATE:
        case GOLD_LEGGINGS:
        case GOLD_BOOTS:
            if (inputB.getType() == Material.GOLD_INGOT
                && plugin.getScore().hasPerk(uuid, Perk.SMITH_IMPROVE_GOLD)) {
                result = new ItemStack(inputA.getType());
                ItemMeta meta = result.getItemMeta();
                meta.setUnbreakable(true);
                result.setItemMeta(meta);
                if (plugin.getScore().hasPerk(uuid, Perk.SMITH_GOLD_ARMOR_HEALTH)) {
                    result = Dirty.assertItemTag(result);
                    double health = Math.min(20, (double)(skillLevel * 2 * 20 / 100));
                    addAttribute(result, null, Attribute.GENERIC_MAX_HEALTH, health, 0, null);
                    double armor = getDefaultArmor(result.getType());
                    addAttribute(result, null, Attribute.GENERIC_ARMOR, armor, 0, null);
                }
            }
            break;
        case IRON_HELMET:
        case IRON_CHESTPLATE:
        case IRON_LEGGINGS:
        case IRON_BOOTS:
            if (inputB.getType() == Material.IRON_INGOT
                && plugin.getScore().hasPerk(uuid, Perk.SMITH_IMPROVE_IRON)) {
                result = new ItemStack(inputA.getType());
                ItemMeta meta = result.getItemMeta();
                meta.setUnbreakable(true);
                result.setItemMeta(meta);
                if (plugin.getScore().hasPerk(uuid, Perk.SMITH_IRON_ARMOR_ARMOR)) {
                    result = Dirty.assertItemTag(result);
                    double armor = getDefaultArmor(result.getType());
                    armor += Math.min(armor, (double)((int)armor * 10 * skillLevel / 100) * 0.1);
                    addAttribute(result, null, Attribute.GENERIC_ARMOR, armor, 0, null);
                }
            }
            break;
        case DIAMOND_HELMET:
        case DIAMOND_CHESTPLATE:
        case DIAMOND_LEGGINGS:
        case DIAMOND_BOOTS:
            if (inputB.getType() == Material.DIAMOND
                && plugin.getScore().hasPerk(uuid, Perk.SMITH_IMPROVE_DIAMOND)) {
                result = new ItemStack(inputA.getType());
                ItemMeta meta = result.getItemMeta();
                meta.setUnbreakable(true);
                result.setItemMeta(meta);
                if (plugin.getScore().hasPerk(uuid, Perk.SMITH_DIAMOND_ARMOR_ARMOR)) {
                    result = Dirty.assertItemTag(result);
                    double armor = getDefaultArmor(result.getType());
                    armor += Math.min(armor, (double)((int)armor * 10 * skillLevel / 100) * 0.1);
                    addAttribute(result, null, Attribute.GENERIC_ARMOR, armor, 0, null);
                    double armorToughness = 2;
                    if (plugin.getScore().hasPerk(uuid, Perk.SMITH_DIAMOND_ARMOR_TOUGH)) {
                        armorToughness *= 1.0 + Math.min(1.0, (double)skillLevel * 2.0 / 100.0);
                    }
                    addAttribute(result, null, Attribute.GENERIC_ARMOR_TOUGHNESS, armorToughness, 0, null);
                    if (plugin.getScore().hasPerk(uuid, Perk.SMITH_DIAMOND_ARMOR_KNOCKBACK_RESIST)) {
                        double knockbackResist = Math.min(0.2, (double)skillLevel * 0.01 * 0.2);
                        addAttribute(result, null, Attribute.GENERIC_KNOCKBACK_RESISTANCE, knockbackResist, 0, null);
                    }
                }
            }
            break;
        case GOLD_AXE:
        case GOLD_BARDING:
        case GOLD_HOE:
        case GOLD_PICKAXE:
        case GOLD_SPADE:
        case GOLD_SWORD:
            if (inputB.getType() == Material.GOLD_INGOT
                && plugin.getScore().hasPerk(uuid, Perk.SMITH_IMPROVE_GOLD)) {
                result = new ItemStack(inputA.getType());
                ItemMeta meta = result.getItemMeta();
                meta.setUnbreakable(true);
                result.setItemMeta(meta);
                result = improveWeapon(uuid, inputA, inputB, result);
            }
            break;
        case IRON_AXE:
        case IRON_BARDING:
        case IRON_HOE:
        case IRON_PICKAXE:
        case IRON_SPADE:
        case IRON_SWORD:
            if (inputB.getType() == Material.IRON_INGOT
                && plugin.getScore().hasPerk(uuid, Perk.SMITH_IMPROVE_IRON)) {
                result = new ItemStack(inputA.getType());
                ItemMeta meta = result.getItemMeta();
                meta.setUnbreakable(true);
                result.setItemMeta(meta);
                result = improveWeapon(uuid, inputA, inputB, result);
            }
            break;
        case DIAMOND_AXE:
        case DIAMOND_BARDING:
        case DIAMOND_HOE:
        case DIAMOND_PICKAXE:
        case DIAMOND_SPADE:
        case DIAMOND_SWORD:
            if (inputB.getType() == Material.DIAMOND
                && plugin.getScore().hasPerk(uuid, Perk.SMITH_IMPROVE_DIAMOND)) {
                result = new ItemStack(inputA.getType());
                ItemMeta meta = result.getItemMeta();
                meta.setUnbreakable(true);
                result.setItemMeta(meta);
                result = improveWeapon(uuid, inputA, inputB, result);
            }
            break;
        case SHIELD:
            if (inputB.getType() == Material.IRON_INGOT
                && plugin.getScore().hasPerk(uuid, Perk.SMITH_IMPROVE_IRON)) {
                result = new ItemStack(inputA.getType());
                ItemMeta meta = result.getItemMeta();
                meta.setUnbreakable(true);
                result.setItemMeta(meta);
                if (plugin.getScore().hasPerk(uuid, Perk.SMITH_SHIELD_ARMOR)) {
                    result = Dirty.assertItemTag(result);
                    double armor = Math.min(8.0, (double)(8 * 10 * skillLevel / 100) * 0.1);
                    addAttribute(result, null, Attribute.GENERIC_ARMOR, armor, 0, null);
                    if (plugin.getScore().hasPerk(uuid, Perk.SMITH_SHIELD_KNOCKBACK_RESIST)) {
                        double knockbackResist = Math.min(0.5, (double)skillLevel * 0.01 * 0.5);
                        addAttribute(result, null, Attribute.GENERIC_KNOCKBACK_RESISTANCE, knockbackResist, 0, null);
                    }
                }
            }
            break;
        default:
            return;
        }
        if (result == null) return;
        event.setResult(result);
        Metadata metadata = new Metadata(player.getUniqueId(), inputA, inputB, result);
        anvilBlock.setMetadata(Metadata.KEY, new FixedMetadataValue(plugin, metadata));
    }

    private ItemStack improveWeapon(UUID uuid, ItemStack inputA, ItemStack inputB, ItemStack result) {
        switch (inputA.getType()) {
        case IRON_SWORD:
        case GOLD_SWORD:
        case DIAMOND_SWORD:
            if (plugin.getScore().hasPerk(uuid, Perk.SMITH_SWORD_DAMAGE)) {
                int skillLevel = plugin.getScore().getSkillLevel(uuid, skillType);
                result = Dirty.assertItemTag(result);
                double damage = getDefaultDamage(inputA.getType());
                damage *= Math.min(2.0, (double)skillLevel * 2.0 * 0.01);
                addAttribute(result, null, Attribute.GENERIC_ATTACK_DAMAGE, damage, 0, null);
            }
            break;
        case IRON_AXE:
        case GOLD_AXE:
        case DIAMOND_AXE:
            if (plugin.getScore().hasPerk(uuid, Perk.SMITH_AXE_DAMAGE)) {
                result = Dirty.assertItemTag(result);
                int skillLevel = plugin.getScore().getSkillLevel(uuid, skillType);
                result = Dirty.assertItemTag(result);
                double damage = getDefaultDamage(inputA.getType());
                damage *= Math.min(2.0, (double)skillLevel * 2.0 * 0.01);
                addAttribute(result, null, Attribute.GENERIC_ATTACK_DAMAGE, damage, 0, null);
            }
            break;
        default:
            break;
        }
        return result;
    }
}
