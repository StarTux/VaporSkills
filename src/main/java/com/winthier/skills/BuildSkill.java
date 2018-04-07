package com.winthier.skills;

import com.winthier.exploits.bukkit.BukkitExploits;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.Data;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

class BuildSkill extends Skill implements Listener {
    @Getter private final SkillType skillType = SkillType.BUILD;
    private long repeatInterval = 60 * 60;
    private int placementThreshold = 5;
    private long placementInterval = 10;
    private final Set<Material> placementBlacklist = EnumSet.noneOf(Material.class);
    private Map<UUID, PlayerData> players = new HashMap<>();

    @Data
    class PlayerData {
        private final UUID uuid;
        private long start;
        private int blocksPlaced = 0;
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

    PlayerData getPlayerData(UUID uuid) {
        PlayerData result = players.get(uuid);
        if (result == null) {
            result = new PlayerData(uuid);
            result.reset();
            players.put(uuid, result);
        }
        return result;
    }

    PlayerData getPlayerData(Player player) {
        return getPlayerData(player.getUniqueId());
    }

    @Override
    void configure() {
        placementThreshold = getConfig().getInt("PlacementThreshold", 5);
        placementInterval = getConfig().getLong("PlacementInterval", 10);
        repeatInterval = getConfig().getLong("RepeatInterval", 60 * 60);
        placementBlacklist.clear();
        for (String i : getConfig().getStringList("PlacementBlacklist")) {
            try {
                Material mat = Material.valueOf(i.toUpperCase());
                placementBlacklist.add(mat);
            } catch (IllegalArgumentException iae) {
                SkillsPlugin.getInstance().getLogger().warning("Build configure: Material not found: " + i);
            }
        }
    }

    @Override
    public void onDisable() {
        players.clear();
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (!allowPlayer(player)) return;
        if (!allowPlacedBlock(event.getBlock(), player)) return;
        PlayerData data = getPlayerData(player);
        if (data.age() >= placementInterval * 1000L) data.reset();
        if (data.addBlocksPlaced(1) == placementThreshold) {
            giveReward(player, rewardForName("progress"));
        }
    }

    boolean allowPlacedBlock(Block block, Player player) {
        if (!block.getType().isSolid()) return false;
        if (placementBlacklist.contains(block.getType())) return false;
        if (BukkitExploits.getInstance().didRecentlyPlace(player, block, repeatInterval)) return false;
        return true;
    }
}
