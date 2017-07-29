package com.winthier.skills.bukkit;

import com.winthier.skills.CustomReward;
import com.winthier.skills.Reward;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.event.entity.ItemMergeEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SpawnEggMeta;

class BukkitSkillSacrifice extends BukkitSkill implements Listener
{
    @Getter final BukkitSkillType skillType = BukkitSkillType.SACRIFICE;
    final double RADIUS = 20;
    final Map<UUID, UUID> dropped = new HashMap<>();

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPlayerDropItem(PlayerDropItemEvent event)
    {
        dropped.put(event.getItemDrop().getUniqueId(), event.getPlayer().getUniqueId());
    }

    void onItemSacrificed(Item item)
    {
        if (!item.isValid()) return;
        UUID uuid = dropped.remove(item.getUniqueId());
        if (uuid == null) return;
        Player player = getPlugin().getServer().getPlayer(uuid);
        if (player == null) return;
        if (!allowPlayer(player)) return;
        List<Reward> rewards = rewardsForItem(item.getItemStack());
        if (rewards.isEmpty()) return;
        double factor = (double)item.getItemStack().getAmount();
        item.remove();
        for (Reward reward : rewards) giveReward(player, reward, factor);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onEntityDamage(EntityDamageEvent event)
    {
        if (!(event.getEntity() instanceof Item)) return;
        onItemSacrificed((Item)event.getEntity());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    public void onEntityCombust(EntityCombustEvent event) {
        if (!(event.getEntity() instanceof Item)) return;
        onItemSacrificed((Item)event.getEntity());
    }

    // Items removed from world

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onInventoryPickupItem(InventoryPickupItemEvent event)
    {
        dropped.remove(event.getItem().getUniqueId());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPlayerPickupItem(PlayerPickupItemEvent event)
    {
        dropped.remove(event.getItem().getUniqueId());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onItemDespawn(ItemDespawnEvent event)
    {
        dropped.remove(event.getEntity().getUniqueId());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onItemMerge(ItemMergeEvent event)
    {
        dropped.remove(event.getEntity().getUniqueId());
    }

    // Util

    static void addReward(List<Reward> list, Reward reward)
    {
        if (reward == null) return;
        list.add(reward);
    }

    List<Reward> rewardsForItem(ItemStack item)
    {
        List<Reward> result = new ArrayList<>();
        if (item == null) return result;
        addReward(result, rewardForItem(item));
        for (Map.Entry<Enchantment, Integer> entry : item.getEnchantments().entrySet()) {
            addReward(result, rewardForEnchantment(entry.getKey(), entry.getValue()));
        }
        if (item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            if (meta instanceof EnchantmentStorageMeta) {
                for (Map.Entry<Enchantment, Integer> entry : ((EnchantmentStorageMeta)meta).getStoredEnchants().entrySet()) {
                    addReward(result, rewardForEnchantment(entry.getKey(), entry.getValue()));
                }
            }
            if (meta instanceof SpawnEggMeta) {
                SpawnEggMeta spawnEggMeta = (SpawnEggMeta)meta;
                if (spawnEggMeta.getSpawnedType() != null) {
                    addReward(result, rewardForEntityType(spawnEggMeta.getSpawnedType()));
                }
            }
        }
        return result;
    }

    /**
     * Utility function for the checkitem command. Do not use for
     * actual rewarding!
     */
    public Reward fullRewardForItem(ItemStack item)
    {
        float skillPoints = 0;
        float money = 0;
        float exp = 0;
        for (Reward reward : rewardsForItem(item)) {
            skillPoints += reward.getSkillPoints();
            money += reward.getMoney();
            exp += reward.getExp();
        }
        return new CustomReward(skillPoints, money, exp);
    }
}
