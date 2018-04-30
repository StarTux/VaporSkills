package com.winthier.skills;

import java.io.File;
import lombok.Getter;
import lombok.NonNull;
import org.bukkit.GameMode;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.metadata.Metadatable;

@Getter
public abstract class Skill {
    protected final SkillsPlugin plugin;
    public final SkillType skillType;
    private boolean enabled = true;
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
        enabled = getConfig().getBoolean("Enabled", true);
        displayName = getConfig().getString("DisplayName", skillType.key);
        shorthand = getConfig().getString("Shorthand", skillType.key);
        description = getConfig().getString("Description", "This is a default skill description. Slap StarTux around so he will finally implement proper skill descriptions and not this dribble.");
    }

    void configure() { };
    void onEnable() { };
    void onDisable() { };

    final String getPermissionNode() {
        return "skills.skill." + skillType.key;
    }

    final boolean allowPlayer(Player player) {
        if (player == null) return false;
        if (player.getGameMode() != GameMode.SURVIVAL) return false;
        if (!player.hasPermission("skills.skill.*") && !player.hasPermission(getPermissionNode())) return false;
        return true;
    }

    final void giveReward(@NonNull Player player, Reward reward, double factor) {
        if (reward == null) return;
        int level = plugin.getScore().getSkillLevel(player.getUniqueId(), skillType);
        double skillPoints = reward.getSkillPoints() * factor;
        double exp         = reward.getExp()         * factor;
        if (skillPoints < 0.01 && exp < 0.01) return;
        if (plugin.hasDebugMode(player)) Msg.msg(player, "[sk]&e %s * %.2f", reward, factor);
        plugin.getScore().giveSkillPoints(player.getUniqueId(), skillType, skillPoints);
        plugin.giveExp(player, exp);
        plugin.getSession(player).onReward(player, this, skillPoints);
    }

    final void giveReward(Player player, Reward reward) {
        giveReward(player, reward, 1.0);
    }

    final Reward getReward(Reward.Category category, String name, Integer data, String extra) {
        return plugin.getScore().getRewards().get(new Reward.Key(skillType, category, name, data, extra));
    }

    final File getConfigFile() {
        return new File(plugin.getDataFolder(), skillType.key + ".yml");
    }

    final ConfigurationSection getConfig() {
        ConfigurationSection result = plugin.getConfig().getConfigurationSection(skillType.key);
        if (result == null) result = plugin.getConfig().createSection(skillType.key);
        return result;
    }

    static final double linearSkillBonus(double max, int skillLevel) {
        return Math.min(max, (double)skillLevel * max / 100.0);
    }

    final void setMetadata(Metadatable metadatable, String key, Object value) {
        metadatable.setMetadata(key, new FixedMetadataValue(plugin, value));
    }

    final void removeMetadata(Metadatable metadatable, String key) {
        metadatable.removeMetadata(key, plugin);
    }

    final MetadataValue getMetadata(Metadatable metadatable, String key) {
        for (MetadataValue value: metadatable.getMetadata(key)) {
            if (value.getOwningPlugin() == plugin) return value;
        }
        return null;
    }
}
