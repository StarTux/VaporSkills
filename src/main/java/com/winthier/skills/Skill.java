package com.winthier.skills;

import java.io.File;
import lombok.Getter;
import lombok.NonNull;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.metadata.Metadatable;
import org.bukkit.potion.PotionEffect;

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

    final Reward rewardForBlock(Block block) {
        @SuppressWarnings("deprecation")
        int blockType = block.getType().getId();
        @SuppressWarnings("deprecation")
        int blockData = (int)block.getData();
        return plugin.getScore().rewardForBlock(skillType, blockType, blockData);
    }

    final Reward rewardForBlockNamed(Block block, String name) {
        @SuppressWarnings("deprecation")
        int blockType = block.getType().getId();
        @SuppressWarnings("deprecation")
        int blockData = (int)block.getData();
        return plugin.getScore().rewardForBlockNamed(skillType, blockType, blockData, name);
    }

    final Reward rewardForItem(ItemStack item) {
        @SuppressWarnings("deprecation")
        int itemType = item.getType().getId();
        @SuppressWarnings("deprecation")
        int itemData = (int)item.getDurability();
        return plugin.getScore().rewardForItem(skillType, itemType, itemData);
    }

    final Reward rewardForPotionEffect(PotionEffect effect) {
        @SuppressWarnings("deprecation")
        int potionType = effect.getType().getId();
        @SuppressWarnings("deprecation")
        int potionData = effect.getAmplifier();
        return plugin.getScore().rewardForPotionEffect(skillType, potionType, potionData);
    }

    final Reward rewardForEnchantment(Enchantment enchant, int level) {
        @SuppressWarnings("deprecation")
        int enchantType = enchant.getId();
        return plugin.getScore().rewardForEnchantment(skillType, enchantType, level);
    }

    final Reward rewardForEntity(Entity e) {
        return plugin.getScore().rewardForEntity(skillType, Entities.name(e));
    }

    final Reward rewardForEntityType(EntityType e) {
        return plugin.getScore().rewardForEntity(skillType, Entities.name(e));
    }

    final Reward rewardForName(String name) {
        return plugin.getScore().rewardForName(skillType, name);
    }

    final Reward rewardForName(String name, int data) {
        return plugin.getScore().rewardForName(skillType, name, data);
    }

    private void giveSkillPoints(Player player, double skillPoints) {
        if (skillPoints < 0.01) return;
        plugin.getScore().giveSkillPoints(player.getUniqueId(), skillType, skillPoints);
    }

    final void giveReward(@NonNull Player player, Reward reward, double factor) {
        int level = plugin.getScore().getSkillLevel(player.getUniqueId(), skillType);
        if (reward == null) return;
        double skillPoints = reward.getSkillPoints() * factor;
        double exp         = reward.getExp()         * factor;
        if (skillPoints < 0.01 && exp < 0.01) return;
        if (plugin.hasDebugMode(player)) {
            Msg.msg(player, "[sk] &e%s &8%s &e%s %s&8:&e%s &8\"&e%s&8\" &6%.2f&8sp &6%.2f&8xp",
                           getShorthand(),
                           Msg.camelCase(reward.key.getTarget().name()),
                           Msg.camelCase(reward.key.typeAsPrettyString()),
                           reward.key.typeAsString(),
                           reward.key.dataAsString(),
                           reward.key.nameAsString(),
                           skillPoints, exp);
        }
        giveSkillPoints(player, skillPoints);
        plugin.giveExp(player, exp);
        plugin.getSession(player).onReward(player, this, skillPoints);
    }

    final void giveReward(Player player, Reward reward) {
        giveReward(player, reward, 1.0);
    }

    final Player getNearestPlayer(Location loc, double max) {
        Player result = null;
        double maxs = max * max;
        double dist = maxs;
        for (Player player : loc.getWorld().getPlayers()) {
            if (!allowPlayer(player)) continue;
            Location ploc = player.getLocation();
            if (!ploc.getWorld().equals(loc.getWorld())) continue;
            double newDist = loc.distanceSquared(ploc);
            if (newDist > maxs) continue;
            if (result == null || newDist < dist) {
                result = player;
                dist = newDist;
            }
        }
        return result;
    }

    final Player getNearestPlayer(Entity e, double max) {
        return getNearestPlayer(e.getLocation(), max);
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
