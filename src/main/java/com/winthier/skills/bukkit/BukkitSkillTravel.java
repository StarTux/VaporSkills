package com.winthier.skills.bukkit;

import com.winthier.skills.Reward;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

class BukkitSkillTravel extends BukkitSkill implements Listener
{
    @lombok.Getter final BukkitSkillType skillType = BukkitSkillType.TRAVEL;
    final double DISTANCE_STEP = 10.0;
    final double MIN_DISTANCE = 50.0;
    final double MAX_TELEPORT_DISTANCE = 128.0;
    final Map<UUID, Data> players = new HashMap<>();

    @RequiredArgsConstructor
    class Data {
        final static String ANCHOR = "anchor";
        final static String DISTANCE = "distance";
        final UUID uuid;
        Location anchor;
        double distance;
        void store() {
            setPlayerSetting(uuid, ANCHOR, anchor);
            setPlayerSetting(uuid, DISTANCE, distance);
        }
        void load(Player player) {
            anchor = getPlayerSettingLocation(uuid, ANCHOR, player.getLocation());
            distance = getPlayerSettingDouble(uuid, DISTANCE, 0);
        }
        boolean didProgress(Player player) {
            Location loc = player.getLocation();
            if (!anchor.getWorld().equals(loc.getWorld())) {
                // If the world changed, reset.
                reset(player);
                return false;
            } else {
                double newDistance = anchor.distance(loc);
                if (distance < MIN_DISTANCE && newDistance >= MIN_DISTANCE) {
                    // Breach min distance
                    distance = newDistance;
                    return true;
                } else if (newDistance - distance > DISTANCE_STEP) {
                    distance = newDistance;
                    return true;
                } else {
                    return false;
                }
            }
        }
        void reset(Player player) {
            anchor = player.getLocation();
            distance = 0.0;
            if (getSkills().hasDebugMode(player)) {
                BukkitUtil.msg(player, "&e%s Reset %d %d %d", getTitle(), anchor.getBlockX(), anchor.getBlockY(), anchor.getBlockZ());
            }
        }
    }

    void storeAll()
    {
        for (Data data : players.values()) data.store();
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

    @EventHandler
    void onPlayerMove(PlayerMoveEvent event)
    {
        if (event instanceof PlayerTeleportEvent) return;
        Player player = event.getPlayer();
        if (!allowPlayer(player)) return;
        Location loc = player.getLocation();
        if (getData(player).didProgress(player)) {
            giveReward(player, rewardForName("progress"));
        }
    }

    @EventHandler
    void onPlayerTeleport(PlayerTeleportEvent event)
    {
        Player player = event.getPlayer();
        if (!event.getFrom().getWorld().equals(event.getTo().getWorld())) {
            getData(player).reset(player);
        } else if (event.getFrom().distanceSquared(event.getTo()) > MAX_TELEPORT_DISTANCE * MAX_TELEPORT_DISTANCE) {
            getData(player).reset(player);
        }
    }

    @Override
    void onDisable()
    {
        storeAll();
        players.clear();
    }
}
