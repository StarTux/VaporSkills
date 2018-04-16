package com.winthier.skills;

import com.winthier.custom.util.Dirty;
import java.util.UUID;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
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
import org.bukkit.scheduler.BukkitRunnable;

class SmithSkill extends Skill implements Listener {
    static final int INPUT_SLOT_1 = 0;
    static final int INPUT_SLOT_2 = 1;
    static final int OUTPUT_SLOT = 2;

    SmithSkill() {
        super(SkillType.SMITH);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onInventoryClick(InventoryClickEvent event) {
        final Inventory inv = event.getInventory();
        if (inv.getType() != InventoryType.ANVIL) return;
        if (event.getSlotType() != InventoryType.SlotType.RESULT) return;
        final Player player = (Player)event.getWhoClicked();
        if (!allowPlayer(player)) return;
        // Input and output slot must not be empty
        ItemStack inputItem1 = inv.getItem(INPUT_SLOT_1);
        ItemStack inputItem2 = inv.getItem(INPUT_SLOT_2);
        ItemStack outputItem = inv.getItem(OUTPUT_SLOT);
        if (inputItem1 == null || inputItem1.getType() == Material.AIR) return;
        if (inputItem2 == null || inputItem2.getType() == Material.AIR) return;
        if (outputItem == null || outputItem.getType() == Material.AIR) return;
        final int level = player.getLevel();
        new BukkitRunnable() {
            @Override public void run() {
                onAnvilUsed(player, inv, level);
            }
        }.runTask(SkillsPlugin.getInstance());
    }

    void onAnvilUsed(Player player, Inventory inv, int oldLevel) {
        // Was the output item removed?
        ItemStack outputItem = inv.getItem(OUTPUT_SLOT);
        if (outputItem != null && outputItem.getType() != Material.AIR) return;
        // Were levels used up?
        int levelsUsed = oldLevel - player.getLevel();
        if (levelsUsed <= 0) return;
        giveReward(player, rewardForNameAndMaximum("exp_level_cost", levelsUsed));
    }

    private final void addAttribute(ItemStack item, EquipmentSlot slot, Attribute attribute, double amount, int operation, UUID uuid) {
        Dirty.TagWrapper itemTag = Dirty.TagWrapper.getItemTagOf(item);
        Dirty.TagListWrapper attrList = itemTag.getList("AttributeModifiers");
        if (attrList == null) attrList = itemTag.createList("AttributeModifiers");
        Dirty.TagWrapper attrTag = attrList.createCompound();
        final String slotName;
        if (slot == null) {
            if (item.getType().name().contains("HELMET")) slot = EquipmentSlot.HEAD;
            else if (item.getType().name().contains("CHESTPLATE")) slot = EquipmentSlot.CHEST;
            else if (item.getType().name().contains("LEGGINGS")) slot = EquipmentSlot.LEGS;
            else if (item.getType().name().contains("BOOTS")) slot = EquipmentSlot.FEET;
            else if (item.getType().name().contains("SHIELD")) slot = EquipmentSlot.OFF_HAND;
            else if (item.getType().name().contains("SWORD")) slot = EquipmentSlot.HAND;
            else if (item.getType().name().contains("AXE")) slot = EquipmentSlot.HAND;
            else slot = EquipmentSlot.HAND;
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

    static final int getDefaultArmor(Material mat) {
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

    static final int getDefaultDamage(Material mat) {
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

    static final double getDefaultAttackSpeed(Material mat) {
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
        final Player player = (Player)event.getView().getPlayer();
        final UUID uuid = player.getUniqueId();
        int skillLevel = SkillsPlugin.getInstance().getScore().getSkillLevel(uuid, skillType);
        final ItemStack itemA = event.getInventory().getItem(0);
        final ItemStack itemB = event.getInventory().getItem(1);
        if (itemA == null || itemA.getType() == Material.AIR) return;
        if (itemB == null || itemB.getType() == Material.AIR) return;
        if (!new ItemStack(itemA.getType()).equals(itemA)) return;
        if (!new ItemStack(itemB.getType()).equals(itemB)) return;
        ItemStack result = null;
        switch (itemA.getType()) {
        case LEATHER_HELMET:
        case LEATHER_CHESTPLATE:
        case LEATHER_LEGGINGS:
        case LEATHER_BOOTS:
            if (itemB.getType() == Material.LEATHER
                && SkillsPlugin.getInstance().getScore().hasPerk(uuid, Perk.SMITH_IMPROVE_LEATHER)) {
                result = new ItemStack(itemA.getType());
                ItemMeta meta = result.getItemMeta();
                meta.setUnbreakable(true);
                result.setItemMeta(meta);
                if (SkillsPlugin.getInstance().getScore().hasPerk(uuid, Perk.SMITH_LEATHER_ARMOR_SPEED)) {
                    result = Dirty.assertItemTag(result);
                    double speed = Math.min(0.02, (double)skillLevel * 0.03 / 100.0);
                    addAttribute(result, null, Attribute.GENERIC_MOVEMENT_SPEED, speed, 0, null);
                    double armor = getDefaultArmor(result.getType());
                    addAttribute(result, null, Attribute.GENERIC_ARMOR, armor, 0, null);
                }
            } else if (itemB.getType() == Material.IRON_INGOT
                && SkillsPlugin.getInstance().getScore().hasPerk(uuid, Perk.SMITH_IMPROVE_MAIL)) {
                switch (itemA.getType()) {
                case LEATHER_HELMET: result = new ItemStack(Material.CHAINMAIL_HELMET); break;
                case LEATHER_CHESTPLATE: result = new ItemStack(Material.CHAINMAIL_CHESTPLATE); break;
                case LEATHER_LEGGINGS: result = new ItemStack(Material.CHAINMAIL_LEGGINGS); break;
                case LEATHER_BOOTS: result = new ItemStack(Material.CHAINMAIL_BOOTS); break;
                default: return;
                }
                ItemMeta meta = result.getItemMeta();
                meta.setUnbreakable(true);
                result.setItemMeta(meta);
                if (SkillsPlugin.getInstance().getScore().hasPerk(uuid, Perk.SMITH_MAIL_ARMOR_DAMAGE)) {
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
            if (itemB.getType() == Material.GOLD_INGOT
                && SkillsPlugin.getInstance().getScore().hasPerk(uuid, Perk.SMITH_IMPROVE_GOLD)) {
                result = new ItemStack(itemA.getType());
                ItemMeta meta = result.getItemMeta();
                meta.setUnbreakable(true);
                result.setItemMeta(meta);
                if (SkillsPlugin.getInstance().getScore().hasPerk(uuid, Perk.SMITH_GOLD_ARMOR_HEALTH)) {
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
            if (itemB.getType() == Material.IRON_INGOT
                && SkillsPlugin.getInstance().getScore().hasPerk(uuid, Perk.SMITH_IMPROVE_IRON)) {
                result = new ItemStack(itemA.getType());
                ItemMeta meta = result.getItemMeta();
                meta.setUnbreakable(true);
                result.setItemMeta(meta);
                if (SkillsPlugin.getInstance().getScore().hasPerk(uuid, Perk.SMITH_IRON_ARMOR_ARMOR)) {
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
            if (itemB.getType() == Material.DIAMOND
                && SkillsPlugin.getInstance().getScore().hasPerk(uuid, Perk.SMITH_IMPROVE_DIAMOND)) {
                result = new ItemStack(itemA.getType());
                ItemMeta meta = result.getItemMeta();
                meta.setUnbreakable(true);
                result.setItemMeta(meta);
                if (SkillsPlugin.getInstance().getScore().hasPerk(uuid, Perk.SMITH_DIAMOND_ARMOR_ARMOR)) {
                    result = Dirty.assertItemTag(result);
                    double armor = getDefaultArmor(result.getType());
                    armor += Math.min(armor, (double)((int)armor * 10 * skillLevel / 100) * 0.1);
                    addAttribute(result, null, Attribute.GENERIC_ARMOR, armor, 0, null);
                    double armorToughness = 2;
                    if (SkillsPlugin.getInstance().getScore().hasPerk(uuid, Perk.SMITH_DIAMOND_ARMOR_TOUGH)) {
                        armorToughness *= 1.0 + Math.min(1.0, (double)skillLevel * 2.0 / 100.0);
                    }
                    addAttribute(result, null, Attribute.GENERIC_ARMOR_TOUGHNESS, armorToughness, 0, null);
                }
            }
            break;
        case GOLD_AXE:
        case GOLD_BARDING:
        case GOLD_HOE:
        case GOLD_PICKAXE:
        case GOLD_SPADE:
        case GOLD_SWORD:
            if (itemB.getType() == Material.GOLD_INGOT
                && SkillsPlugin.getInstance().getScore().hasPerk(uuid, Perk.SMITH_IMPROVE_GOLD)) {
                result = new ItemStack(itemA.getType());
                ItemMeta meta = result.getItemMeta();
                meta.setUnbreakable(true);
                result.setItemMeta(meta);
            }
            break;
        case IRON_AXE:
        case IRON_BARDING:
        case IRON_HOE:
        case IRON_PICKAXE:
        case IRON_SPADE:
        case IRON_SWORD:
            if (itemB.getType() == Material.IRON_INGOT
                && SkillsPlugin.getInstance().getScore().hasPerk(uuid, Perk.SMITH_IMPROVE_IRON)) {
                result = new ItemStack(itemA.getType());
                ItemMeta meta = result.getItemMeta();
                meta.setUnbreakable(true);
                result.setItemMeta(meta);
            }
            break;
        case DIAMOND_AXE:
        case DIAMOND_BARDING:
        case DIAMOND_HOE:
        case DIAMOND_PICKAXE:
        case DIAMOND_SPADE:
        case DIAMOND_SWORD:
            if (itemB.getType() == Material.DIAMOND
                && SkillsPlugin.getInstance().getScore().hasPerk(uuid, Perk.SMITH_IMPROVE_DIAMOND)) {
                result = new ItemStack(itemA.getType());
                ItemMeta meta = result.getItemMeta();
                meta.setUnbreakable(true);
                result.setItemMeta(meta);
            }
            break;
        case SHIELD:
            if (itemB.getType() == Material.IRON_INGOT
                && SkillsPlugin.getInstance().getScore().hasPerk(uuid, Perk.SMITH_IMPROVE_IRON)) {
                result = new ItemStack(itemA.getType());
                ItemMeta meta = result.getItemMeta();
                meta.setUnbreakable(true);
                result.setItemMeta(meta);
                if (SkillsPlugin.getInstance().getScore().hasPerk(uuid, Perk.SMITH_SHIELD_ARMOR)) {
                    result = Dirty.assertItemTag(result);
                    double armor = Math.min(8.0, (double)(8 * 10 * skillLevel / 100) * 0.1);
                    addAttribute(result, null, Attribute.GENERIC_ARMOR, armor, 0, null);
                }
            }
            break;
        default:
            return;
        }
        if (result == null) return;
        event.setResult(result);
    }
}
