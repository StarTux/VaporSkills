package com.winthier.skills.bukkit;

import com.winthier.skills.Reward;
import com.winthier.skills.Skill;
import lombok.Getter;
import lombok.NonNull;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

/**
 * Abstract class which implements Skill in a Bukkit-y manner.
 */
abstract class BukkitSkill implements Skill
{
    BukkitSkillsPlugin getPlugin()
    {
	return BukkitSkillsPlugin.instance;
    }

    BukkitSkills getSkills()
    {
	return BukkitSkills.instance;
    }

    abstract BukkitSkillType getSkillType();
    void onEnable() {};
    
    String getPermissionNode()
    {
	return "skills.skill" + getKey();
    }

    boolean allowPlayer(Player player)
    {
	return player.hasPermission(getPermissionNode());
    }

    @Override
    public String getKey()
    {
        return getSkillType().name().toLowerCase();
    }

    Reward rewardForBlock(Block block)
    {
        @SuppressWarnings("deprecation")
	int blockType = block.getType().getId();
        @SuppressWarnings("deprecation")
	int blockData = (int)block.getData();
        return getSkills().getScore().rewardForBlock(this, blockType, blockData);
    }

    Reward rewardForEntity(Entity e)
    {
        return getSkills().getScore().rewardForEntity(this, BukkitEntities.name(e));
    }

    private void giveSkillPoints(Player player, double skillPoints)
    {
        if (skillPoints < 0.01) return;
	getSkills().getScore().giveSkillPoints(player.getUniqueId(), this, skillPoints);
    }

    private void giveMoney(Player player, double money)
    {
        if (money < 0.01) return;
	getPlugin().getEconomy().depositPlayer(player, money);
    }

    private void giveExp(Player player, double exp) {
        if (exp < 1.0) return;
        player.giveExp((int)exp);
    }

    void giveReward(@NonNull Player player, Reward reward, double factor)
    {
        if (reward == null) return;
        if (factor < 0.01) return;
        giveSkillPoints(player, reward.getSkillPoints() * factor);
        giveMoney(player, reward.getMoney() * factor);
        giveExp(player, reward.getExp() * factor);
    }

    void giveReward(Player player, Reward reward)
    {
        giveReward(player, reward, 1.0);
    }
    
    Player getNearestPlayer(Entity e, double max)
    {
        Location loc = e.getLocation();
        Player result = null;
        double dist = 0.0;
        for (Entity o : e.getNearbyEntities(max, max, max)) {
            if (!(o instanceof Player)) continue;
            if (result == null) {
                result = (Player)o;
            } else {
                double newDist = loc.distanceSquared(o.getLocation());
                if (newDist < dist) {
                    dist = newDist;
                    result = (Player)o;
                }
            }
        }
        return result;
    }
}
