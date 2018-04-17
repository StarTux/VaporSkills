package com.winthier.skills;

import com.winthier.exploits.bukkit.BukkitExploits;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;

class FishSkill extends Skill implements Listener {
    private long fishInterval = 15;

    FishSkill(SkillsPlugin plugin) {
        super(plugin, SkillType.FISH);
    }

    @Override
    public void configure() {
        fishInterval = getConfig().getLong("FishInterval", fishInterval);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPlayerFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;
        Player player = event.getPlayer();
        if (player == null) return;
        if (!allowPlayer(player)) return;
        if (!(event.getCaught() instanceof Item)) return;
        if (BukkitExploits.getInstance().didRecentlyFish(player, player.getLocation().getBlock(), fishInterval)) return;
        ItemStack item = ((Item)event.getCaught()).getItemStack();
        giveReward(player, rewardForItem(item));
    }
}
