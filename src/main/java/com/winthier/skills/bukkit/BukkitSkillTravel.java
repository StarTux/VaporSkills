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
            if (section.hasKey("Anchor")) anchor = Location.deserialize(section.get("Anchor"));
            distance = section.getDouble("Distance", 0.0);
        }
        double currentDistance(Location loc)
        {
            if (anchor == null || !anchor.getWorld().equals(loc.getWorld())) {
                anchor = loc;
                distance = 0.0;
                return 0.0;
            } else {
                double squared = anchor.distanceSquared(loc);
                if (squared > MIN_DISTANCE * MIN_DISTANCE) {
                    
                }
                return 0.0;
            }
        }
    }

    final BukkitSkillType skillType = BukkitSkillType.TRAVEL;
    final String title = "Traveling";
    final String verb = "travel";
    final String personName = "traveler";
    final String activityName = "traveling";
    final double MIN_DISTANCE = 10.0;
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
    onPlayerMove(PlayerMoveEvent event)
    {
        
    }

    @Override
    void onDisable()
    {
        saveConfig();
    }
}
