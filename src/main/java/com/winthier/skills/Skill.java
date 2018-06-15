package com.winthier.skills;

import lombok.Getter;
import lombok.NonNull;
import org.bukkit.GameMode;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.event.Listener;

@Getter
public abstract class Skill implements Listener {
    protected final SkillsPlugin plugin;
    public final SkillType skillType;
    private String displayName;
    private String shorthand;
    private String description;

    Skill(SkillsPlugin plugin, SkillType skillType) {
        this.plugin = plugin;
        this.skillType = skillType;
    }

    /**
     * Called before onEnable() and on every command triggered
     * reload.
     */
    final void configureSkill() {
        displayName = getConfig().getString("DisplayName");
        shorthand = getConfig().getString("Shorthand");
        description = getConfig().getString("Description");
        configure();
    }

    void configure() { };
    void onEnable() { };
    void onDisable() { };

    final String getPermissionNode() {
        return "skills.skill." + skillType.key;
    }

    final boolean allowPlayer(Player player) {
        if (player == null) return false;
        if (player.getGameMode() != GameMode.SURVIVAL && player.getGameMode() != GameMode.ADVENTURE) return false;
        if (!player.hasPermission("skills.skill.*") && !player.hasPermission(getPermissionNode())) return false;
        return true;
    }

    final void giveReward(@NonNull Player player, Reward reward, double factor) {
        if (reward == null) return;
        int level = plugin.getScore().getSkillLevel(player.getUniqueId(), skillType);
        double skillPoints = reward.getSkillPoints() * factor;
        if (skillPoints < 0.01) return;
        if (plugin.hasDebugMode(player)) Msg.msg(player, "[sk]&e %s * %.2f", reward, factor);
        plugin.getScore().giveSkillPoints(player.getUniqueId(), skillType, skillPoints);
        plugin.getSession(player).onReward(player, this, skillPoints);
    }

    final void giveReward(Player player, Reward reward) {
        giveReward(player, reward, 1.0);
    }

    final Reward getReward(Reward.Category category, String name, Integer data, String extra) {
        return plugin.getScore().getRewards().get(new Reward.Key(skillType, category, name, data, extra));
    }

    final ConfigurationSection getConfig() {
        if (!plugin.getConfig().isSet(skillType.key)) {
            return plugin.getConfig().getDefaultSection().getConfigurationSection(skillType.key);
        } else {
            return plugin.getConfig().getConfigurationSection(skillType.key);
        }
    }

    static final double linearSkillBonus(double max, int skillLevel) {
        return Math.min(max, (double)skillLevel * max / 100.0);
    }

    static boolean isViableAttackTarget(Entity entity, Player player) {
        if (!(entity instanceof LivingEntity)) return false;
        if (entity instanceof ArmorStand) return false; // TODO armor stand could be custom entity?
        if (entity instanceof Player) return false; // TODO pvp options?
        if (entity.isInvulnerable()) return false;
        if (entity instanceof Tameable) {
            Tameable pet = (Tameable)entity;
            if (pet.isTamed() && pet.getOwner() instanceof OfflinePlayer) return false; // TODO pvp option?
        }
        return true;
    }
}
