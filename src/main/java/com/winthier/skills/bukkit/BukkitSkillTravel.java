package com.winthier.skills.bukkit;

import com.winthier.skills.Reward;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

@Getter
class BukkitSkillTravel extends BukkitSkill implements Listener
{
    @RequiredArgsConstructor
    class Data {
        final UUID uuid;
        Location anchor;
        double distance;
        void store() {
            ConfigurationSection section = getConfig().getConfigurationSection(uuid.toString());
            if (section == null) section = getConfig().createSection(uuid.toString());
            if (anchor != null) section.createSection("Anchor", anchor.serialize());
            section.set("Distance", distance);
        }
        void load() {
            ConfigurationSection section = getConfig().getConfigurationSection(uuid.toString());
            if (section == null) return;
            if (section.isSet("Anchor")) anchor = Location.deserialize((Map<String,Object>)section.get("Anchor"));
            distance = section.getDouble("Distance", 0.0);
        }
        boolean didProgress(Location loc) {
            if (anchor == null || !anchor.getWorld().equals(loc.getWorld())) {
                // If there is no prior location, or it is in
                // another world, copy the given location.
                anchor = loc;
                distance = 0.0;
                return false;
            } else {
                double newDistance = anchor.distance(loc);
                if (distance < MIN_DISTANCE && newDistance >= MIN_DISTANCE) {
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
        void reset(Location loc) {
            anchor = loc;
            distance = 0.0;
        }
    }

    final BukkitSkillType skillType = BukkitSkillType.TRAVEL;
    final String title = "Traveling";
    final String verb = "travel";
    final String personName = "traveler";
    final String activityName = "traveling";
    final double DISTANCE_STEP = 10.0;
    final double MIN_DISTANCE = 50.0;
    final Map<UUID, Data> players = new HashMap<>();

    void storeAll()
    {
        for (Data data : players.values()) data.store();
    }

    Data getData(UUID uuid)
    {
        Data result = players.get(uuid);
        if (result == null) {
            result = new Data(uuid);
            result.load();
            players.put(uuid, result);
        }
        return result;
    }

    Data getData(Player player)
    {
        return getData(player.getUniqueId());
    }

    @EventHandler
    void onPlayerMove(PlayerMoveEvent event)
    {
        Player player = event.getPlayer();
        if (!allowPlayer(player)) return;
        Location loc = player.getLocation();
        if (getData(player).didProgress(player.getLocation())) {
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
            getData(event.getPlayer()).reset(event.getPlayer().getLocation());
            break;
        }
    }

    @Override
    void onDisable()
    {
        storeAll();
        players.clear();
        saveConfig();
    }
}
