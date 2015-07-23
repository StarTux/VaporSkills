package com.winthier.skills.bukkit;

import lombok.Getter;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;

@Getter
class BukkitSkillFish extends BukkitSkill implements Listener
{
    final BukkitSkillType skillType = BukkitSkillType.FISH;
    final String title = "Fishing";
    final String verb = "fish";
    final String personName = "fisher";
    final String activityName = "fishing";

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPlayerFish(PlayerFishEvent event)
    {
        // TODO: Consider afk fishing exploit
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;
        Player player = event.getPlayer();
        if (player == null) return;
        if (!allowPlayer(player)) return;
        if (!(event.getCaught() instanceof Item)) return;
        ItemStack item = ((Item)event.getCaught()).getItemStack();
        giveReward(player, rewardForItem(item));
    }
}
