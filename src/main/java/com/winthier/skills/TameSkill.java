package com.winthier.skills;

import java.util.UUID;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityTameEvent;

final class TameSkill extends Skill {
    TameSkill(SkillsPlugin plugin) {
        super(plugin, SkillType.TAME);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onEntityTame(EntityTameEvent event) {
        Player player = event.getOwner() instanceof Player ? (Player)event.getOwner() : null;
        if (player == null || !allowPlayer(player)) return;
        final UUID uuid = player.getUniqueId();
        // Perks
        final int skillLevel;
        switch (event.getEntity().getType()) {
        case WOLF:
            final Wolf wolf = (Wolf)event.getEntity();
            skillLevel = plugin.getScore().getSkillLevel(uuid, skillType);
            if (plugin.getScore().hasPerk(uuid, Perk.TAME_DOG_ATTACK_DAMAGE)) {
                wolf.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).addModifier(new AttributeModifier(new UUID(1, 1), Perk.TAME_DOG_ATTACK_DAMAGE.key, 1.0, AttributeModifier.Operation.MULTIPLY_SCALAR_1));
            }
            if (plugin.getScore().hasPerk(uuid, Perk.TAME_DOG_MOVEMENT_SPEED)) {
                wolf.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).addModifier(new AttributeModifier(new UUID(1, 1), Perk.TAME_DOG_MOVEMENT_SPEED.key, 1.0, AttributeModifier.Operation.MULTIPLY_SCALAR_1));
            }
            if (plugin.getScore().hasPerk(uuid, Perk.TAME_DOG_HEALTH)) {
                wolf.getAttribute(Attribute.GENERIC_MAX_HEALTH).addModifier(new AttributeModifier(new UUID(1, 1), Perk.TAME_DOG_HEALTH.key, 1.0, AttributeModifier.Operation.MULTIPLY_SCALAR_1));
            }
            break;
        default:
            break;
        }
        // Give reward
        Reward reward = getReward(Reward.Category.TAME_ENTITY, event.getEntity().getType().name(), null, null);
        giveReward(player, reward);
    }
}
