package com.winthier.skills.bukkit;

import com.winthier.skills.Reward;
import com.winthier.skills.Skill;
import com.winthier.skills.sql.SQLPlayerSetting;
import com.winthier.skills.util.Strings;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
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
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

/**
 * Abstract class which implements Skill in a Bukkit-y manner.
 */
@Getter
abstract class BukkitSkill implements Skill
{
    double skillPointsFactor = 1.0;
    double moneyFactor = 1.0;
    double expFactor = 1.0;
    String displayName;
    String shorthand;
    String description;

    BukkitSkillsPlugin getPlugin()
    {
	return BukkitSkillsPlugin.instance;
    }

    BukkitSkills getSkills()
    {
	return BukkitSkills.instance;
    }

    /**
     * Called before onEnable() and on every command triggered
     * reload. Call super.configure() if you override this method!
     */
    void configure()
    {
        displayName = getConfig().getString("DisplayName", getKey());
        shorthand = getConfig().getString("Shorthand", getKey());
        description = getConfig().getString("Description", "This is a default skill description. Slap StarTux around so he will finally implement proper skill descriptions and not this dribble.");
        List<Double> factors = getConfig().getDoubleList("RewardFactors");
        skillPointsFactor = factors.size() >= 1 ? factors.get(0) : 1.0;
        moneyFactor       = factors.size() >= 2 ? factors.get(1) : 1.0;
        expFactor         = factors.size() >= 3 ? factors.get(2) : 1.0;
    }

    abstract BukkitSkillType getSkillType();

    void onEnable() {};
    void onDisable() {};
    
    String getPermissionNode()
    {
	return "skills.skill." + getKey();
    }

    boolean allowPlayer(Player player)
    {
        if (player.getGameMode() != GameMode.SURVIVAL) return false;
	if (!player.hasPermission("skills.skill.*") && !player.hasPermission(getPermissionNode())) return false;
        return true;
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

    Reward rewardForBlockNamed(Block block, String name)
    {
        @SuppressWarnings("deprecation")
	int blockType = block.getType().getId();
        @SuppressWarnings("deprecation")
	int blockData = (int)block.getData();
        return getSkills().getScore().rewardForBlockNamed(this, blockType, blockData, name);
    }
    
    Reward rewardForItem(ItemStack item)
    {
        @SuppressWarnings("deprecation")
	int itemType = item.getType().getId();
        @SuppressWarnings("deprecation")
        int itemData = (int)item.getDurability();
        return getSkills().getScore().rewardForItem(this, itemType, itemData);
    }

    Reward rewardForPotionEffect(PotionEffect effect)
    {
        @SuppressWarnings("deprecation")
        int potionType = effect.getType().getId();
        @SuppressWarnings("deprecation")
        int potionData = effect.getAmplifier();
        return getSkills().getScore().rewardForPotionEffect(this, potionType, potionData);
    }

    Reward rewardForEnchantment(Enchantment enchant, int level)
    {
        @SuppressWarnings("deprecation")
        int enchantType = enchant.getId();
        return getSkills().getScore().rewardForEnchantment(this, enchantType, level);
    }

    Reward rewardForEntity(Entity e)
    {
        return getSkills().getScore().rewardForEntity(this, BukkitEntities.name(e));
    }

    Reward rewardForName(String name)
    {
        return getSkills().getScore().rewardForName(this, name);
    }

    Reward rewardForName(String name, int data)
    {
        return getSkills().getScore().rewardForName(this, name, data);
    }
    
    private void giveSkillPoints(Player player, double skillPoints)
    {
        if (skillPoints < 0.01) return;
	getSkills().getScore().giveSkillPoints(player.getUniqueId(), this, skillPoints);
    }

    private void giveMoney(Player player, double money)
    {
        if (money < 0.01) return;
        getSkills().giveMoney(player, money);
    }

    private void giveExp(Player player, double exp) {
        if (exp < 1.0) return;
        player.giveExp((int)exp);
    }

    void giveReward(@NonNull Player player, Reward reward, double factor)
    {
        if (reward == null) return;
        double skillPoints = reward.getSkillPoints() * factor * getSkillPointsFactor() * getSkills().getSkillPointsFactor();
        double money       = reward.getMoney()       * factor * getMoneyFactor()       * getSkills().getMoneyFactor();
        double exp         = reward.getExp()         * factor * getExpFactor()         * getSkills().getExpFactor();
        if (skillPoints < 0.01 && money < 0.01 && exp < 0.01) return;
        if (getSkills().hasDebugMode(player)) {
            BukkitReward br = BukkitReward.of(reward);
            BukkitUtil.msg(player, "[sk] &e%s &8%s &e%s %s&8:&e%s &8\"&e%s&8\" &6%.2f&8sp &6%.2f&8mo &6%.2f&8xp",
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
        BukkitPlayer.of(player).onReward(this, skillPoints, money, exp);
    }

    void giveReward(Player player, Reward reward)
    {
        giveReward(player, reward, 1.0);
    }
    
    Player getNearestPlayer(Location loc, double max)
    {
        Player result = null;
        double dist = 0.0;
        for (Entity o : loc.getWorld().getNearbyEntities(loc, max, max, max)) {
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

    Player getNearestPlayer(Entity e, double max)
    {
        return getNearestPlayer(e.getLocation(), max);
    }

    File getConfigFile()
    {
        return new File(getPlugin().getDataFolder(), getKey() + ".yml");
    }

    ConfigurationSection getConfig()
    {
        ConfigurationSection result = getPlugin().getConfig().getConfigurationSection(getKey());
        if (result == null) result = getPlugin().getConfig().createSection(getKey());
        return result;
    }

    String getPlayerSettingString(UUID uuid, String key, String dfl)
    {
        String result = SQLPlayerSetting.getString(uuid, getKey(), key);
        return result != null ? result : dfl;
    }

    int getPlayerSettingInt(UUID uuid, String key, int dfl)
    {
        Integer result = SQLPlayerSetting.getInt(uuid, getKey(), key);
        return result != null ? result : dfl;
    }

    double getPlayerSettingDouble(UUID uuid, String key, double dfl)
    {
        Double result = SQLPlayerSetting.getDouble(uuid, getKey(), key);
        return result != null ? result : dfl;
    }

    Location getPlayerSettingLocation(UUID uuid, String key, Location dfl)
    {
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

    void setPlayerSetting(UUID uuid, String key, Object value)
    {
        if (value instanceof Location) {
            Location loc = (Location)value;
            value = String.format("%s,%f,%f,%f,%f,%f", loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
        }
        SQLPlayerSetting.set(uuid, getKey(), key, value);
    }
}
