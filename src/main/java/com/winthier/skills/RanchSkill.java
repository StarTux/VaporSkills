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
import org.bukkit.event.entity.EntityBreedEvent;

/**
 * The ranching skill works in conjunction with the RanchingEntity.
 */
final class RanchSkill extends Skill {
    RanchSkill(SkillsPlugin plugin) {
        super(plugin, SkillType.RANCH);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onEntityBreed(EntityBreedEvent event) {
        if (!(event.getBreeder() instanceof Player)) return;
        final Player player = (Player)event.getBreeder();
        final LivingEntity entity = event.getEntity();
        // Give Reward
        Reward reward = getReward(Reward.Category.BREED_ENTITY, entity.getType().name(), null, null);
        giveReward(player, reward);
    }

}
