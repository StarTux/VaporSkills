package com.winthier.skills.bukkit;

import com.winthier.exploits.bukkit.BukkitExploits;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.Data;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

class BukkitSkillBuild extends BukkitSkill implements Listener
{
    @Getter final BukkitSkillType skillType = BukkitSkillType.BUILD;
    final long REPEAT_CHECK_SECONDS = 60L * 60L;
    final int BLOCKS_THRESHOLD = 10;
    final long BUILD_INTERVAL = 1000 * (long)BLOCKS_THRESHOLD;
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
    public void onDisable()
    {
        players.clear();
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onBlockPlace(BlockPlaceEvent event)
    {
        Player player = event.getPlayer();
	if (!allowPlayer(player)) return;
	if (!allowPlacedBlock(event.getBlock(), player)) return;
        PlayerData data = getPlayerData(player);
        if (data.age() >= BUILD_INTERVAL) data.reset();
        if (data.addBlocksPlaced(1) == BLOCKS_THRESHOLD) {
            giveReward(player, rewardForName("progress"));
        }
    }

    boolean allowPlacedBlock(Block block, Player player)
    {
        if (BukkitExploits.getInstance().didRecentlyPlace(player, block, REPEAT_CHECK_SECONDS)) return false;
        return true;
    }
}
