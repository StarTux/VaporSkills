package com.winthier.skills.bukkit;

import com.winthier.skills.Reward;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

class BukkitSkillTravel extends BukkitSkill implements Listener
{
    @Getter final BukkitSkillType skillType = BukkitSkillType.TRAVEL;
    final static String ANCHOR = "anchor";
    final static String DISTANCE = "distance";
    final Map<UUID, Data> players = new HashMap<>();
    double distanceStep = 16;
    double maxTeleportDistance = 128;
    double maxDistanceNoProgress = 96;

    static double horizontalDistanceSquared(Location loc1, Location loc2)
    {
        double x = loc2.getX() - loc1.getX();
        double z = loc2.getZ() - loc1.getZ();
        return x*x + z*z;
    }

    static double horizontalDistance(Location loc1, Location loc2)
    {
        return Math.sqrt(horizontalDistanceSquared(loc1, loc2));
    }

    @RequiredArgsConstructor
    class Data {
        final UUID uuid;
        Location anchor, last;
        double distance;
        void store() {
            setPlayerSetting(uuid, ANCHOR, anchor);
            setPlayerSetting(uuid, DISTANCE, distance);
        }
        void load(Player player) {
            last = anchor = getPlayerSettingLocation(uuid, ANCHOR, player.getLocation());
            distance = getPlayerSettingDouble(uuid, DISTANCE, 0);
        }
        void onPlayerMove(Player player, Location loc) {
            if (!anchor.getWorld().equals(loc.getWorld())) {
                // If the world changed, reset.
                reset(player, loc);
                return;
            }
            double newDistance = horizontalDistance(loc, anchor);
            if (newDistance - distance > distanceStep) {
                last = loc;
                distance = newDistance;
                giveReward(player, rewardForNameAndMaximum("distance", (int)distance));
            } else if (!loc.getWorld().equals(last.getWorld()) ||
                       horizontalDistanceSquared(loc, last) > maxDistanceNoProgress*maxDistanceNoProgress) {
                reset(player, loc);
            }
        }
        void reset(Player player, Location loc) {
            last = anchor = loc;
            distance = 0;
            if (getSkills().hasDebugMode(player)) {
                BukkitUtil.msg(player, "&e%s Reset %d %d %d", getDisplayName(), anchor.getBlockX(), anchor.getBlockY(), anchor.getBlockZ());
            }
        }
    }

    @Override
    public void configure()
    {
        super.configure();
        distanceStep = getConfig().getDouble("DistanceStep", 16);
        maxTeleportDistance = getConfig().getDouble("MaxTeleportDistance", 128);
        maxDistanceNoProgress = getConfig().getDouble("MaxDistanceNoProgress", 96);
    }

    Data getData(Player player)
    {
        UUID uuid = player.getUniqueId();
        Data result = players.get(uuid);
        if (result == null) {
            result = new Data(uuid);
            result.load(player);
            players.put(uuid, result);
        }
        return result;
    }

    @Override
    boolean allowPlayer(Player player)
    {
        if (!super.allowPlayer(player)) return false;
        if (!player.isOnGround()) return false;
        if (player.isInsideVehicle()) {
            if (player.getVehicle().getType() == EntityType.HORSE ||
                player.getVehicle().getType() == EntityType.PIG) {
                if (player.getVehicle().isInsideVehicle()) return false;
            } else {
                return false;
            }
        }
        return true;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    void onPlayerMove(PlayerMoveEvent event)
    {
        if (event instanceof PlayerTeleportEvent) return;
        Player player = event.getPlayer();
        if (!allowPlayer(player)) return;
        getData(player).onPlayerMove(player, event.getTo());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    void onPlayerTeleport(PlayerTeleportEvent event)
    {
        final Player player = event.getPlayer();
        if (!allowPlayer(player)) return;
        if (!event.getFrom().getWorld().equals(event.getTo().getWorld()) ||
            horizontalDistanceSquared(event.getFrom(), event.getTo()) > maxTeleportDistance*maxTeleportDistance) {
            getData(player).reset(player, event.getTo());
        } else {
            getData(player).onPlayerMove(player, event.getTo());
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    void onPlayerRespawn(PlayerRespawnEvent event)
    {
        Player player = event.getPlayer();
        if (!allowPlayer(player)) return;
        getData(player).reset(player, event.getRespawnLocation());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    void onPlayerQuit(PlayerQuitEvent event)
    {
        Player player = event.getPlayer();
        if (!allowPlayer(player)) return;
        Data data = players.remove(player.getUniqueId());
        if (data != null) data.store();
    }
    
    @Override
    void onDisable()
    {
        for (Data data : players.values()) data.store();
        players.clear();
    }
}
