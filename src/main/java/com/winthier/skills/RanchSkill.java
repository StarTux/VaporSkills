package com.winthier.skills;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityBreedEvent;

final class RanchSkill extends Skill implements Listener {
    static final String BREED_KEY = "winthier.skill.Ranch.breed";

    private static class Breed {
        int generation;
        UUID owner;
        List<String> quirks;
        List<String> hiddenQuirks;
    }

    RanchSkill(SkillsPlugin plugin) {
        super(plugin, SkillType.RANCH);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onEntityBreed(EntityBreedEvent event) {
        if (!(event.getBreeder() instanceof Player)) return;
        final Player player = (Player)event.getBreeder();
        final LivingEntity entity = event.getEntity();
        Reward reward = getReward(Reward.Category.BREED_ENTITY, entity.getType().name(), null, null);
        giveReward(player, reward);
        onBreed(player, event.getMother(), event.getFather(), entity);
    }

    static Map<String, Object> getStoredData(LivingEntity e, String key) {
        for (String s: e.getScoreboardTags()) {
            if (s.startsWith(key)) {
                String v = s.substring(key.length());
                Object o = Msg.fromJSONString(v);
                if (!(o instanceof Map)) return null;
                @SuppressWarnings("unchecked")
                Map<String, Object> result = (Map<String, Object>)o;
                return result;
            }
        }
        return null;
    }

    static int removeStoredData(LivingEntity e, String key) {
        List<String> rs = new ArrayList<>();
        for (String s: e.getScoreboardTags()) {
            if (s.startsWith(key)) rs.add(s);
        }
        for (String r: rs) {
            e.removeScoreboardTag(r);
        }
        return rs.size();
    }

    static void storeData(LivingEntity e, String key, Map<String, Object> d) {
        removeStoredData(e, key);
        e.addScoreboardTag(key + Msg.toJSONString(d));
    }

    private void onBreed(Player player, LivingEntity mother, LivingEntity father, LivingEntity baby) {
    }
}
