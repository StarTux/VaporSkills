package com.winthier.skills;

import com.winthier.skills.event.SkillsRewardEvent;
import com.winthier.skills.sql.SQLPlayerSetting;
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
import org.bukkit.potion.PotionEffect;

@Getter
public abstract class Skill {
    private boolean enabled = true;
    private String displayName;
    private String shorthand;
    private String description;

    /**
     * Called before onEnable() and on every command triggered
     * reload.
     */
    final void configureSkill() {
        enabled = getConfig().getBoolean("Enabled", true);
        displayName = getConfig().getString("DisplayName", getKey());
        shorthand = getConfig().getString("Shorthand", getKey());
        description = getConfig().getString("Description", "This is a default skill description. Slap StarTux around so he will finally implement proper skill descriptions and not this dribble.");
    }

    abstract SkillType getSkillType();
    void configure() { };
    void onEnable() { };
    void onDisable() { };

    final String getPermissionNode() {
        return "skills.skill." + getKey();
    }

    final boolean allowPlayer(Player player) {
        if (player == null) return false;
        if (player.getGameMode() != GameMode.SURVIVAL) return false;
        if (!player.hasPermission("skills.skill.*") && !player.hasPermission(getPermissionNode())) return false;
        return true;
    }

    public final String getKey() {
        return getSkillType().name().toLowerCase();
    }

    final Reward rewardForBlock(Block block) {
        @SuppressWarnings("deprecation")
        int blockType = block.getType().getId();
        @SuppressWarnings("deprecation")
        int blockData = (int)block.getData();
        return SkillsPlugin.getInstance().getScore().rewardForBlock(this, blockType, blockData);
    }

    final Reward rewardForBlockNamed(Block block, String name) {
        @SuppressWarnings("deprecation")
        int blockType = block.getType().getId();
        @SuppressWarnings("deprecation")
        int blockData = (int)block.getData();
        return SkillsPlugin.getInstance().getScore().rewardForBlockNamed(this, blockType, blockData, name);
    }

    final Reward rewardForItem(ItemStack item) {
        @SuppressWarnings("deprecation")
        int itemType = item.getType().getId();
        @SuppressWarnings("deprecation")
        int itemData = (int)item.getDurability();
        return SkillsPlugin.getInstance().getScore().rewardForItem(this, itemType, itemData);
    }

    final Reward rewardForPotionEffect(PotionEffect effect) {
        @SuppressWarnings("deprecation")
        int potionType = effect.getType().getId();
        @SuppressWarnings("deprecation")
        int potionData = effect.getAmplifier();
        return SkillsPlugin.getInstance().getScore().rewardForPotionEffect(this, potionType, potionData);
    }

    final Reward rewardForEnchantment(Enchantment enchant, int level) {
        @SuppressWarnings("deprecation")
        int enchantType = enchant.getId();
        return SkillsPlugin.getInstance().getScore().rewardForEnchantment(this, enchantType, level);
    }

    final Reward rewardForEntity(Entity e) {
        return SkillsPlugin.getInstance().getScore().rewardForEntity(this, Entities.name(e));
    }

    final Reward rewardForEntityType(EntityType e) {
        return SkillsPlugin.getInstance().getScore().rewardForEntity(this, Entities.name(e));
    }

    final Reward rewardForName(String name) {
        return SkillsPlugin.getInstance().getScore().rewardForName(this, name);
    }

    final Reward rewardForName(String name, int data) {
        return SkillsPlugin.getInstance().getScore().rewardForName(this, name, data);
    }

    final Reward rewardForNameAndMaximum(String name, int dataMax) {
        return SkillsPlugin.getInstance().getScore().rewardForNameAndMaximum(this, name, dataMax);
    }

    private void giveSkillPoints(Player player, double skillPoints) {
        if (skillPoints < 0.01) return;
        SkillsPlugin.getInstance().getScore().giveSkillPoints(player.getUniqueId(), this, skillPoints);
    }

    private void giveMoney(Player player, double money) {
        if (money < 0.01) return;
        SkillsPlugin.getInstance().giveMoney(player, money);
    }

    private void giveExp(Player player, double exp) {
        if (exp < 0.01) return;
        SkillsPlugin.getInstance().giveExp(player, exp);
    }

    final void giveReward(@NonNull Player player, Reward reward, double factor) {
        int level = SkillsPlugin.getInstance().getScore().getSkillLevel(player.getUniqueId(), this);
        double bonusFactor = 1.0 + (double)(level / 10) / 100.0;
        if (reward == null) return;
        double skillPoints = reward.getSkillPoints() * factor;
        double money       = reward.getMoney()       * factor * bonusFactor;
        double exp         = reward.getExp()         * factor;
        if (skillPoints < 0.01 && money < 0.01 && exp < 0.01) return;
        if (SkillsPlugin.getInstance().hasDebugMode(player)) {
            StoredReward br = StoredReward.of(reward);
            Msg.msg(player, "[sk] &e%s &8%s &e%s %s&8:&e%s &8\"&e%s&8\" &6%.2f&8sp &6%.2f&8mo &6%.2f&8xp",
                           getShorthand(),
                           Strings.camelCase(br.key.getTarget().name()),
                           Strings.camelCase(br.key.typeAsPrettyString()),
                           br.key.typeAsString(),
                           br.key.dataAsString(),
                           br.key.nameAsString(),
                           skillPoints, money, exp);
        }
        giveSkillPoints(player, skillPoints);
        giveMoney(player, money);
        giveExp(player, exp);
        Reward outcome = new CustomReward((float)skillPoints, (float)money, (float)exp);
        Session.of(player).onReward(player, this, outcome);
        SkillsPlugin.getInstance().getScore().logReward(reward, player.getUniqueId(), outcome);
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
        return new File(SkillsPlugin.getInstance().getDataFolder(), getKey() + ".yml");
    }

    final ConfigurationSection getConfig() {
        ConfigurationSection result = SkillsPlugin.getInstance().getConfig().getConfigurationSection(getKey());
        if (result == null) result = SkillsPlugin.getInstance().getConfig().createSection(getKey());
        return result;
    }

    final String getPlayerSettingString(UUID uuid, String key, String dfl) {
        String result = SQLPlayerSetting.getString(uuid, getKey(), key);
        return result != null ? result : dfl;
    }

    final int getPlayerSettingInt(UUID uuid, String key, int dfl) {
        Integer result = SQLPlayerSetting.getInt(uuid, getKey(), key);
        return result != null ? result : dfl;
    }

    final double getPlayerSettingDouble(UUID uuid, String key, double dfl) {
        Double result = SQLPlayerSetting.getDouble(uuid, getKey(), key);
        return result != null ? result : dfl;
    }

    final Location getPlayerSettingLocation(UUID uuid, String key, Location dfl) {
        String serial = SQLPlayerSetting.getString(uuid, getKey(), key);
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
        SQLPlayerSetting.set(uuid, getKey(), key, value);
    }
}
