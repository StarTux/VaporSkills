package com.winthier.skills;

import com.winthier.custom.CustomPlugin;
import com.winthier.custom.entity.EntityWatcher;
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
        final UUID uuid = player.getUniqueId();
        final LivingEntity entity = event.getEntity();
        final boolean doImprove;
        switch (entity.getType()) {
        case COW:
            doImprove = plugin.getScore().hasPerk(uuid, Perk.RANCH_COW);
            break;
        case MUSHROOM_COW:
            doImprove = plugin.getScore().hasPerk(uuid, Perk.RANCH_MUSHROOM_COW);
            break;
        case PIG:
            doImprove = plugin.getScore().hasPerk(uuid, Perk.RANCH_PIG);
            break;
        case CHICKEN:
            doImprove = plugin.getScore().hasPerk(uuid, Perk.RANCH_CHICKEN);
            break;
        case SHEEP:
            doImprove = plugin.getScore().hasPerk(uuid, Perk.RANCH_SHEEP);
            break;
        case RABBIT:
            doImprove = plugin.getScore().hasPerk(uuid, Perk.RANCH_RABBIT);
            break;
        default:
            doImprove = false;
            break;
        }
        if (doImprove) {
            final RanchEntity.Watcher mother, father;
            EntityWatcher tmp;
            tmp = CustomPlugin.getInstance().getEntityManager().getEntityWatcher(event.getMother());
            if (tmp != null && tmp instanceof RanchEntity.Watcher) {
                mother = (RanchEntity.Watcher)tmp;
            } else {
                mother = null;
            }
            tmp = CustomPlugin.getInstance().getEntityManager().getEntityWatcher(event.getFather());
            if (tmp != null && tmp instanceof RanchEntity.Watcher) {
                father = (RanchEntity.Watcher)tmp;
            } else {
                father = null;
            }
            if (mother != null && father != null) {
                plugin.getRanchEntity().breed(mother, father, entity, player);
            } else {
                plugin.getRanchEntity().breed(entity, player);
            }
        }
        // Give Reward
        Reward reward = getReward(Reward.Category.BREED_ENTITY, entity.getType().name(), null, null);
        giveReward(player, reward);
    }

}
