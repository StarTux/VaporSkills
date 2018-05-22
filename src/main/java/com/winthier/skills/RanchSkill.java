package com.winthier.skills;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;

/**
 * The ranching skill works in conjunction with the RanchingEntity.
 */
final class RanchSkill extends Skill {
    List<String> maleNames, femaleNames;

    RanchSkill(SkillsPlugin plugin) {
        super(plugin, SkillType.RANCH);
    }

    @Override
    void configure() {
        maleNames = getConfig().getStringList("MaleNames");
        if (maleNames.isEmpty()) maleNames = Arrays.asList("Adam");
        femaleNames = getConfig().getStringList("FemaleNames");
        if (femaleNames.isEmpty()) femaleNames = Arrays.asList("Eve");
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
        if (doImprove) plugin.getRanchEntity().onBreed(event.getMother(), event.getFather(), entity, player);
        // Give Reward
        Reward reward = getReward(Reward.Category.BREED_ENTITY, entity.getType().name(), null, null);
        giveReward(player, reward);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        plugin.getRanchEntity().onPlayerDropItem(event);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        final Player player = event.getPlayer();
        final UUID uuid = player.getUniqueId();
        if (plugin.getScore().hasPerk(player.getUniqueId(), Perk.RANCH_CARRY)) {
            plugin.getRanchEntity().pickup(player, event.getRightClicked());
        }
    }
}
