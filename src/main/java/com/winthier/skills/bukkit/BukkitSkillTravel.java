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
    final static String ANCHOR = "anchor";
    final static String DISTANCE = "distance";

    @RequiredArgsConstructor
    class Data {
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
        }
    }

    @lombok.Getter final BukkitSkillType skillType = BukkitSkillType.TRAVEL;
    final double DISTANCE_STEP = 10.0;
    final double MIN_DISTANCE = 50.0;
    final Map<UUID, Data> players = new HashMap<>();

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
        switch (event.getCause()) {
        case COMMAND:
        case END_PORTAL:
        case NETHER_PORTAL:
        case PLUGIN:
        case SPECTATE:
        case UNKNOWN:
            getData(event.getPlayer()).reset(event.getPlayer());
            break;
        }
    }

    @Override
    void onDisable()
    {
        storeAll();
        players.clear();
    }
}
