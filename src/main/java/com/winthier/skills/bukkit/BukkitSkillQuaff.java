package com.winthier.skills.bukkit;

import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.Potion;
import org.bukkit.potion.PotionEffect;

@Getter
class BukkitSkillQuaff extends BukkitSkillAbstractConsume
{
    final BukkitSkillType skillType = BukkitSkillType.QUAFF;
    final String title = "Quaffing";
    final String verb = "quaff";
    final String personName = "quaffer";
    final String activityName = "quaffing";

    @Override
    void onConsume(Player player, ItemStack item)
    {
        if (item.getType() != Material.POTION) return;
        PotionMeta meta = (PotionMeta)item.getItemMeta();
        if (meta.hasCustomEffects()) {
            for (PotionEffect effect : meta.getCustomEffects()) {
                giveReward(player, rewardForPotionEffect(effect));
            }
        } else {
            Potion potion = Potion.fromItemStack(item);
            for (PotionEffect effect : potion.getEffects()) {
                giveReward(player, rewardForPotionEffect(effect));
            }
        }
    }
}
