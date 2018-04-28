package com.winthier.skills;

import java.io.File;
import java.util.UUID;
import lombok.Getter;
import lombok.NonNull;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
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

    final Reward rewardForNameAndMaximum(String name, int dataMax) {
        return plugin.getScore().rewardForNameAndMaximum(skillType, name, dataMax);
    }

    private void giveSkillPoints(Player player, double skillPoints) {
        if (skillPoints < 0.01) return;
        plugin.getScore().giveSkillPoints(player.getUniqueId(), skillType, skillPoints);
    }

    private void giveMoney(Player player, double money) {
        if (money < 0.01) return;
        plugin.giveMoney(player, money);
    }

    private void giveExp(Player player, double exp) {
        if (exp < 0.01) return;
        plugin.giveExp(player, exp);
    }

    final void giveReward(@NonNull Player player, Reward reward, double factor) {
        int level = plugin.getScore().getSkillLevel(player.getUniqueId(), skillType);
        double bonusFactor = 1.0 + (double)(level / 10) / 100.0;
        if (reward == null) return;
        double skillPoints = reward.getSkillPoints() * factor;
        double money       = reward.getMoney()       * factor * bonusFactor;
        double exp         = reward.getExp()         * factor;
        if (skillPoints < 0.01 && money < 0.01 && exp < 0.01) return;
        if (plugin.hasDebugMode(player)) {
            StoredReward br = StoredReward.of(reward);
            Msg.msg(player, "[sk] &e%s &8%s &e%s %s&8:&e%s &8\"&e%s&8\" &6%.2f&8sp &6%.2f&8mo &6%.2f&8xp",
                           getShorthand(),
                           Msg.camelCase(br.key.getTarget().name()),
                           Msg.camelCase(br.key.typeAsPrettyString()),
                           br.key.typeAsString(),
                           br.key.dataAsString(),
                           br.key.nameAsString(),
                           skillPoints, money, exp);
        }
        giveSkillPoints(player, skillPoints);
        giveMoney(player, money);
        giveExp(player, exp);
        Reward outcome = new CustomReward((float)skillPoints, (float)money, (float)exp);
        plugin.getSession(player).onReward(player, this, outcome);
        plugin.getScore().logReward(reward, player.getUniqueId(), outcome);
        Bukkit.getServer().getPluginManager().callEvent(new SkillsRewardEvent(player, this, outcome));
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

    final String getPlayerSettingString(UUID uuid, String key, String dfl) {
        String result = SQLPlayerSetting.getString(uuid, skillType.key, key);
        return result != null ? result : dfl;
    }

    final int getPlayerSettingInt(UUID uuid, String key, int dfl) {
        Integer result = SQLPlayerSetting.getInt(uuid, skillType.key, key);
        return result != null ? result : dfl;
    }

    final double getPlayerSettingDouble(UUID uuid, String key, double dfl) {
        Double result = SQLPlayerSetting.getDouble(uuid, skillType.key, key);
        return result != null ? result : dfl;
    }

    final Location getPlayerSettingLocation(UUID uuid, String key, Location dfl) {
        String serial = SQLPlayerSetting.getString(uuid, skillType.key, key);
        if (serial == null) return dfl;
        String[] tokens = serial.split(",");
        if (tokens.length != 6) return dfl;
        World world = Bukkit.getServer().getWorld(tokens[0]);
        if (world == null) return dfl;
        try {
            return new Location(world,
                                Double.parseDouble(tokens[1]),
                                Double.parseDouble(tokens[2]),
                                Double.parseDouble(tokens[3]),
                                Float.parseFloat(tokens[4]),
                                Float.parseFloat(tokens[5]));
        } catch (NumberFormatException nfe) {
            return dfl;
        }
    }

    final void setPlayerSetting(UUID uuid, String key, Object value) {
        if (value instanceof Location) {
            Location loc = (Location)value;
            value = String.format("%s,%f,%f,%f,%f,%f", loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
        }
        SQLPlayerSetting.set(uuid, skillType.key, key, value);
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
