package com.winthier.skills.bukkit;

import com.winthier.exploits.bukkit.BukkitExploits;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.Data;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

class BukkitSkillBuild extends BukkitSkill implements Listener
{
    @Getter final BukkitSkillType skillType = BukkitSkillType.BUILD;
    long repeatInterval = 60*60;
    int placementThreshold = 5;
    long placementInterval = 10;
    final Set<Material> placementBlacklist = EnumSet.noneOf(Material.class);
    Map<UUID, PlayerData> players = new HashMap<>();

    @Data
    class PlayerData {
        final UUID uuid;
        long start;
        int blocksPlaced = 0;
        void reset() {
            blocksPlaced = 0;
            start = System.currentTimeMillis();
        }
        int addBlocksPlaced(int count) {
            blocksPlaced += count;
            return blocksPlaced;
        }
        long age() {
            return System.currentTimeMillis() - start;
        }
    }

    PlayerData getPlayerData(UUID uuid)
    {
        PlayerData result = players.get(uuid);
        if (result == null) {
            result = new PlayerData(uuid);
            result.reset();
            players.put(uuid, result);
        }
        return result;
    }

    PlayerData getPlayerData(Player player)
    {
        return getPlayerData(player.getUniqueId());
    }

    @Override
    void configure()
    {
        super.configure();
        placementThreshold = getConfig().getInt("PlacementThreshold", 5);
        placementInterval = getConfig().getLong("PlacementInterval", 10);
        repeatInterval = getConfig().getLong("RepeatInterval", 60*60);
        placementBlacklist.clear();
        for (String i : getConfig().getStringList("PlacementBlacklist")) {
            try {
                Material mat = Material.valueOf(i.toUpperCase());
                placementBlacklist.add(mat);
            } catch (IllegalArgumentException iae) {
                getPlugin().getLogger().warning("Build configure: Material not found: " + i);
            }
        }
    }
    
    @Override
    public void onDisable()
    {
        players.clear();
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onBlockPlace(BlockPlaceEvent event)
    {
        Player player = event.getPlayer();
	if (!allowPlayer(player)) return;
	if (!allowPlacedBlock(event.getBlock(), player)) return;
        PlayerData data = getPlayerData(player);
        if (data.age() >= placementInterval * 1000L) data.reset();
        if (data.addBlocksPlaced(1) == placementThreshold) {
            giveReward(player, rewardForName("progress"));
        }
    }

    boolean allowPlacedBlock(Block block, Player player)
    {
        if (!block.getType().isSolid()) return false;
        if (placementBlacklist.contains(block.getType())) return false;
        if (BukkitExploits.getInstance().didRecentlyPlace(player, block, repeatInterval)) return false;
        return true;
    }
}
