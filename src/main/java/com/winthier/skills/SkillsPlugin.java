package com.winthier.skills;

import com.winthier.custom.event.CustomRegisterEvent;
import com.winthier.sql.SQLDatabase;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.Value;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.block.BrewingStand;
import org.bukkit.block.Furnace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.FurnaceInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.metadata.Metadatable;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

@Getter
public final class SkillsPlugin extends JavaPlugin implements Listener {
    // Constants
    static final String CONFIG_YML = "config.yml";
    static final String REWARDS_TXT = "rewards.txt";
    static final String PERKS_YML = "perks.yml";
    static final String ANVIL_RECIPES_YML = "anvil_recipes.yml";
    static final String SPAWN_REASON_TAG = "SpawnReason";
    static final String LAST_DAMAGE_CAUSE_KEY = "LastDamageCause";
    // Singleton
    @Getter private static SkillsPlugin instance;
    // External Data
    private SQLDatabase db;
    // Commands
    private final AdminCommand adminCommand = new AdminCommand(this);
    private final SkillsCommand skillsCommand = new SkillsCommand(this);
    private final HighscoreCommand highscoreCommand = new HighscoreCommand(this);
    // Internal Data
    private final Score score = new Score(this);
    private final Map<SkillType, Skill> skillMap = new EnumMap<>(SkillType.class);
    private final Map<Class<? extends Skill>, Skill> skillClassMap = new HashMap<>();
    private final Map<String, Skill> nameMap = new HashMap<>();
    private final Set<UUID> playersInDebugMode = new HashSet<>();
    private final Map<UUID, Session> sessions = new HashMap<>();
    private final List<AnvilRecipe> anvilRecipes = new ArrayList<>();
    private ConfigurationSection perksConfig = null;
    private RanchEntity ranchEntity = null;
    private GearItem gearItem = null;
    private LootEntity lootEntity = null;
    private Map<IngredientItem.Type, IngredientItem> ingredients = null;
    final Random random = new Random(System.currentTimeMillis());

    public SkillsPlugin() {
        instance = this;
    }

    static List<Class<?>> getDatabaseClasses() {
        return Arrays.asList(SQLScore.class,
                             SQLPerk.class,
                             SQLPerkProgress.class);
    }

    @Override
    public void onEnable() {
        // Files
        reloadConfig();
        // Database
        db = new SQLDatabase(this);
        for (Class<?> clazz: getDatabaseClasses()) db.registerTable(clazz);
        if (!db.createAllTables()) {
            getLogger().warning("Database setup failed. Disabling skills.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        // Skills
        List<Skill> skills = Arrays.asList(
            new BrawlSkill(this),
            new BrewSkill(this),
            new CookSkill(this),
            new DigSkill(this),
            new EnchantSkill(this),
            new FishSkill(this),
            new GardenSkill(this),
            new HuntSkill(this),
            new MineSkill(this),
            new RanchSkill(this),
            new SmithSkill(this),
            new TameSkill(this),
            new WoodcutSkill(this));
        for (Skill skill : skills) {
            SkillType type = skill.getSkillType();
            if (skillMap.containsKey(type)) {
                throw new IllegalStateException("Duplicate skill " + type.name() + ": " + skillMap.get(type).getClass().getName() + " and " + skill.getClass().getName());
            }
            skillMap.put(type, skill);
            skillClassMap.put(skill.getClass(), skill);
        }
        // Double check skills
        for (SkillType type : SkillType.values()) {
            Skill skill = skillMap.get(type);
            if (skill == null) {
                getLogger().warning("Missing skill: " + type.name());
            } else {
                getServer().getPluginManager().registerEvents(skill, this);
                skill.configureSkill();
                skill.onEnable();
            }
        }
        // Commands
        getCommand("skillsadmin").setExecutor(adminCommand);
        getCommand("skills").setExecutor(skillsCommand);
        getCommand("highscore").setExecutor(highscoreCommand);
        // Events
        getServer().getPluginManager().registerEvents(this, this);
        // Tasks
        new BukkitRunnable() {
            @Override public void run() {
                onTick();
            }
        }.runTaskTimer(this, 1, 1);
        // Cache
        buildNameMap();
        importRewards();
        importAnvilRecipes();
    }

    @Override
    public void onDisable() {
        for (Skill skill : getSkills()) {
            skill.onDisable();
        }
        for (Player player: getServer().getOnlinePlayers()) {
            getSession(player).onDisable();
        }
        sessions.clear();
    }

    void writeDefaultFiles(boolean force) {
        saveResource(CONFIG_YML, force);
        saveResource(REWARDS_TXT, force);
        saveResource(PERKS_YML, force);
        saveResource(ANVIL_RECIPES_YML, force);
    }

    public ConfigurationSection getPerksConfig() {
        if (perksConfig == null) {
            YamlConfiguration tmp;
            tmp = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "perks.yml"));
            tmp.setDefaults(YamlConfiguration.loadConfiguration(new InputStreamReader(getResource("perks.yml"))));
            this.perksConfig = tmp;
        }
        return perksConfig;
    }

    void reloadAll() {
        writeDefaultFiles(false);
        reloadConfig();
        perksConfig = null;
        for (Skill skill : getSkills()) skill.configure();
        buildNameMap();
        score.clear();
        importRewards();
        importAnvilRecipes();
        for (Player player: getServer().getOnlinePlayers()) {
            getSession(player).onDisable();
        }
        sessions.clear();
    }

    // Event Handlers

    /**
     * Remove player related data from the caches when they log out.
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        final UUID uuid = event.getPlayer().getUniqueId();
        sessions.remove(uuid);
        score.removePlayer(uuid);
    }

    @EventHandler
    public void onCustomRegister(CustomRegisterEvent event) {
        ranchEntity = new RanchEntity(this);
        lootEntity = new LootEntity(this);
        gearItem = new GearItem(this);
        event.addEntity(ranchEntity);
        event.addEntity(lootEntity);
        ingredients = new EnumMap<>(IngredientItem.Type.class);
        for (IngredientItem.Type type: IngredientItem.Type.values()) {
            IngredientItem item = new IngredientItem(this, type);
            event.addItem(item);
            ingredients.put(type, item);
        }
        event.addItem(gearItem);
    }

    @RequiredArgsConstructor @Getter
    static final class AnvilStore {
        static final String KEY = "Anvil";
        final Block anvilBlock;
        final UUID player;
        final ItemStack inputA, inputB;
        ItemStack output = null;
        boolean customRecipe = false;
        void setOutput(ItemStack o) {
            this.output = o;
            this.customRecipe = true;
        }
    }

    @Data
    private static final class FurnaceStore {
        static final String KEY = "Furnace";
        private UUID player;
        private Material material;
        private int amount;
    }

    @Value
    private static final class AnvilRecipe {
        @Value
        private static final class Item {
            final IngredientItem.Type type;
            final int amount;
        }
        private Item inputA, inputB, output;
    }

    // Anvil, Brewing Stand, Furnace

    /**
     * Take note when a player loads a furnace.
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getClickedInventory() == null) return;
        if (!(event.getWhoClicked() instanceof Player)) return;
        final Player player = (Player)event.getWhoClicked();
        final UUID uuid = player.getUniqueId();
        switch (event.getView().getType()) {
        case FURNACE:
            if (getMetadata(player, FurnaceStore.KEY) == null) {
                final int initialAmount;
                final Material material, targetMaterial;
                if (event.getClickedInventory().getType() == InventoryType.FURNACE) {
                    if (event.getSlotType() != InventoryType.SlotType.CRAFTING) return;
                    if (event.isShiftClick()) {
                        material = event.getCurrentItem().getType();
                    } else if (event.isRightClick()) {
                        material = event.getCurrentItem().getType();
                    } else {
                        material = event.getCursor().getType();
                    }
                } else {
                    if (!event.isShiftClick()) return;
                    material = event.getCurrentItem().getType();
                }
                if (material == Material.AIR) return;
                final FurnaceInventory furnaceInventory = (FurnaceInventory)event.getView().getTopInventory();
                if (furnaceInventory.getSmelting() == null) {
                    targetMaterial = null;
                    initialAmount = 0;
                } else {
                    targetMaterial = furnaceInventory.getSmelting().getType();
                    initialAmount = furnaceInventory.getSmelting().getAmount();
                }
                if (targetMaterial != null && targetMaterial != material) return;
                Furnace furnace = furnaceInventory.getHolder();
                if (furnace == null) return;
                final Block block = furnace.getBlock();
                FurnaceStore furnaceStore = (FurnaceStore)getMetadata(block, FurnaceStore.KEY);
                if (furnaceStore == null) {
                    furnaceStore = new FurnaceStore();
                    furnaceStore.player = uuid;
                    furnaceStore.material = material;
                    furnaceStore.amount = 0;
                } else {
                    removeMetadata(block, FurnaceStore.KEY);
                    if (!uuid.equals(furnaceStore.player)
                        || material != furnaceStore.material) {
                        furnaceStore.player = uuid;
                        furnaceStore.material = material;
                        furnaceStore.amount = 0;
                    }
                }
                final FurnaceStore finalFurnaceStore = furnaceStore;
                setMetadata(player, FurnaceStore.KEY, true);
                new BukkitRunnable() {
                    @Override public void run() {
                        removeMetadata(player, FurnaceStore.KEY);
                        final int finalAmount;
                        final ItemStack smelting = furnaceInventory.getSmelting();
                        if (smelting == null || smelting.getType() != material) {
                            finalAmount = 0;
                        } else {
                            finalAmount = smelting.getAmount();
                        }
                        final int difference = finalAmount - initialAmount;
                        finalFurnaceStore.amount = Math.min(64, Math.max(0, finalFurnaceStore.amount + difference));
                        setMetadata(block, FurnaceStore.KEY, finalFurnaceStore);
                    }
                }.runTask(this);
            }
            break;
        case BREWING:
            if (getMetadata(player, FurnaceStore.KEY) != null) {
                final int initialAmount;
                final Material material, targetMaterial;
                if (event.getClickedInventory().getType() == InventoryType.BREWING) {
                    // FUEL = Ingredient slot, for some reason
                    if (event.getSlotType() != InventoryType.SlotType.FUEL) return;
                    if (event.isShiftClick()) {
                        material = event.getCurrentItem().getType();
                    } else if (event.isRightClick()) {
                        material = event.getCurrentItem().getType();
                    } else {
                        material = event.getCursor().getType();
                    }
                } else {
                    if (!event.isShiftClick()) return;
                    material = event.getCurrentItem().getType();
                }
                if (material == Material.AIR) return;
                final BrewerInventory brewerInventory = (BrewerInventory)event.getView().getTopInventory();
                if (brewerInventory.getIngredient() == null) {
                    targetMaterial = null;
                    initialAmount = 0;
                } else {
                    targetMaterial = brewerInventory.getIngredient().getType();
                    initialAmount = brewerInventory.getIngredient().getAmount();
                }
                if (targetMaterial != null && material != targetMaterial) return;
                final BrewingStand brewingStand = brewerInventory.getHolder();
                if (brewingStand == null) return;
                final Block block = brewingStand.getBlock();
                FurnaceStore furnaceStore = (FurnaceStore)getMetadata(block, FurnaceStore.KEY);
                if (furnaceStore == null) {
                    furnaceStore = new FurnaceStore();
                    furnaceStore.player = uuid;
                    furnaceStore.material = material;
                    furnaceStore.amount = 0;
                } else {
                    removeMetadata(block, FurnaceStore.KEY);
                    if (!uuid.equals(furnaceStore.player)
                        || furnaceStore.material != material) {
                        furnaceStore.player = uuid;
                        furnaceStore.material = material;
                        furnaceStore.amount = 0;
                    }
                }
                FurnaceStore finalFurnaceStore = furnaceStore;
                setMetadata(player, FurnaceStore.KEY, true);
                new BukkitRunnable() {
                    @Override public void run() {
                        removeMetadata(player, FurnaceStore.KEY);
                        final int finalAmount;
                        final ItemStack ingredient = brewerInventory.getIngredient();
                        if (ingredient == null || ingredient.getType() != material) {
                            finalAmount = 0;
                        } else {
                            finalAmount = ingredient.getAmount();
                        }
                        final int difference = finalAmount - initialAmount;
                        finalFurnaceStore.amount = Math.min(1, Math.max(0, finalFurnaceStore.amount + difference));
                        setMetadata(block, FurnaceStore.KEY, finalFurnaceStore);
                    }
                }.runTask(this);
            }
            break;
        case ANVIL:
            // For customized anvil recipes, several checks and
            // actions are required.
            // The creation of the recipes is overseen by
            // onPrepareAnvil().
            if (event.getSlotType() == InventoryType.SlotType.RESULT) {
                // Left input slot and output slot must not be empty.
                final Inventory inventory = event.getInventory();
                ItemStack inputA = inventory.getItem(0);
                ItemStack inputB = inventory.getItem(1);
                // Find AnvilStore in block metadata store
                final Location anvilLoc = event.getInventory().getLocation();
                if (anvilLoc == null) return;
                final Block anvilBlock = anvilLoc.getBlock();
                AnvilStore anvilStore = (AnvilStore)getMetadata(anvilBlock, AnvilStore.KEY);
                removeMetadata(anvilBlock, AnvilStore.KEY);
                if (anvilStore == null) return;
                // Compare AnvilStore with AnvilInventory, except the
                // output, which tends to differ for some reason.
                if (!anvilStore.player.equals(player.getUniqueId())) return;
                if (anvilStore.inputA == null && inputA != null) return;
                if (anvilStore.inputA != null && inputA == null) return;
                if (!anvilStore.inputA.equals(inputA)) return;
                if (anvilStore.inputB == null && inputB != null) return;
                if (anvilStore.inputB != null && inputB == null) return;
                if (!anvilStore.inputB.equals(inputB)) return;
                // Dish out result item
                boolean didCraft = false;
                if (event.isShiftClick()) {
                    // Only fit for exactly one output item.
                    if (player.getInventory().addItem(anvilStore.output).isEmpty()) {
                        didCraft = true;
                    }
                } else if (event.getCursor() == null || event.getCursor().getType() == Material.AIR) {
                    event.getView().setCursor(anvilStore.output);
                    didCraft = true;
                }
                if (didCraft) {
                    event.getInventory().setItem(0, null);
                    event.getInventory().setItem(1, null);
                    event.getInventory().setItem(2, null);
                    anvilUseEffect(anvilBlock);
                }
            }
            break;
        default:
            break;
        }
    }

    void anvilUseEffect(Block block) {
        Location loc = block.getLocation().add(0.5, 1, 0.5);
        loc.getWorld().playSound(loc, Sound.BLOCK_ANVIL_USE, SoundCategory.BLOCKS, 1.0f, 0.8f);
        new BukkitRunnable() {
            private int ticks = 0;
            @Override public void run() {
                if (ticks < 60) {
                    if (ticks % 20 == 0) {
                        loc.getWorld().spawnParticle(Particle.LAVA, loc, 1, 0.25, 0.25, 0.25, 0);
                    }
                    if (ticks % 5 == 0) {
                        loc.getWorld().spawnParticle(Particle.FLAME, loc, 4, 0.25, 0.25, 0.25, 0);
                    }
                    if (ticks == 50) {
                        loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_BURN, SoundCategory.BLOCKS, 1.0f, 1.25f);
                    }
                    loc.getWorld().spawnParticle(Particle.SMOKE_LARGE, loc, 1, 0.25, 0.25, 0.25, 0);
                    ticks += 1;
                } else {
                    cancel();
                }
            }
        }.runTaskTimer(this, 1, 1);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        final Location anvilLoc = event.getInventory().getLocation();
        if (anvilLoc == null) return;
        final Block anvilBlock = anvilLoc.getBlock();
        if (!(event.getView().getPlayer() instanceof Player)) return;
        final Player player = (Player)event.getView().getPlayer();
        final ItemStack output = event.getInventory().getItem(2);
        final ItemStack inputA = event.getInventory().getItem(0);
        if (inputA == null || inputA.getAmount() == 0) return;
        final ItemStack inputB = event.getInventory().getItem(1);
        AnvilStore anvilStore = new AnvilStore(anvilBlock, player.getUniqueId(), inputA, inputB);
        IngredientItem.Type ingredientA = IngredientItem.Type.of(inputA);
        IngredientItem.Type ingredientB = IngredientItem.Type.of(inputB);
        if (ingredientA != null && (ingredientB == null) == (inputB == null || inputB.getAmount() == 0)) {
            for (AnvilRecipe anvilRecipe: anvilRecipes) {
                if (anvilRecipe.inputA.type == ingredientA && anvilRecipe.inputA.amount == inputA.getAmount()
                    && ((ingredientB == null && anvilRecipe.inputB == null) ||
                        (anvilRecipe.inputB.type == ingredientB && anvilRecipe.inputB.amount == inputB.getAmount()))) {
                    anvilStore.setOutput(anvilRecipe.output.type.spawn(anvilRecipe.output.amount));
                    break;
                }
            }
        }
        if (!anvilStore.isCustomRecipe()) {
            getSkill(SmithSkill.class).anvilRecipe(player, anvilStore);
        }
        if (anvilStore.isCustomRecipe()) {
            event.setResult(anvilStore.output);
            setMetadata(anvilBlock, AnvilStore.KEY, anvilStore);
        } else {
            event.setResult(null);
            removeMetadata(anvilBlock, AnvilStore.KEY);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onFurnaceSmelt(FurnaceSmeltEvent event) {
        final Block block = event.getBlock();
        final FurnaceStore furnaceStore = (FurnaceStore)getMetadata(block, FurnaceStore.KEY);
        if (furnaceStore == null) return;
        if (furnaceStore.amount <= 0) return;
        if (furnaceStore.material != event.getSource().getType()) return;
        furnaceStore.amount -= 1;
        Player player = getServer().getPlayer(furnaceStore.player);
        if (player == null) return;
        if (hasDebugMode(player)) player.sendMessage("SMELT " + furnaceStore.material + " " + furnaceStore.amount);
        getSkill(CookSkill.class).onItemSmelt(player, event.getSource(), event.getResult());
        getSkill(SmithSkill.class).onItemSmelt(player, event.getSource(), event.getResult());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onBrew(BrewEvent event) {
        final Block block = event.getBlock();
        final FurnaceStore furnaceStore = (FurnaceStore)getMetadata(block, FurnaceStore.KEY);
        if (furnaceStore == null) return;
        if (furnaceStore.amount <= 0) return;
        Player player = getServer().getPlayer(furnaceStore.player);
        if (player == null) return;
        if (hasDebugMode(player)) player.sendMessage("BREW " + event.getContents().getItem(0));
        ((BrewSkill)getSkill(SkillType.BREW)).onBrew(player, event.getContents());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        setScoreboardValue(event.getEntity(), SPAWN_REASON_TAG, event.getSpawnReason().name());
    }

    /**
     * Listen for entity deaths and figure out if they died for the
     * right reasons on behalf of Brawling and Hunting.
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onEntityDeath(EntityDeathEvent event) {
        final LivingEntity entity = event.getEntity();
        Player killer = entity.getKiller();
        if (killer == null) return;
        String reasonString = getScoreboardValue(entity, SPAWN_REASON_TAG);
        if (reasonString == null) return;
        SpawnReason reason;
        try {
            reason = SpawnReason.valueOf(reasonString.toUpperCase());
        } catch (IllegalArgumentException iae) {
            return;
        }
        switch (reason) {
        case SPAWNER:
        case SPAWNER_EGG:
            return;
        default: break;
        }
        String lastDamageCause = (String)getMetadata(entity, LAST_DAMAGE_CAUSE_KEY);
        if (lastDamageCause == null) {
            return;
        } else if (lastDamageCause.equals(SkillType.BRAWL.key)) {
            getSkill(BrawlSkill.class).onEntityKill(killer, entity);
        } else if (lastDamageCause.equals(SkillType.HUNT.key)) {
            getSkill(HuntSkill.class).onEntityKill(killer, entity);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager();
        if (!(event.getEntity() instanceof LivingEntity)) return;
        LivingEntity damagee = (LivingEntity)event.getEntity();
        if (damager instanceof Player) {
            setMetadata(damagee, LAST_DAMAGE_CAUSE_KEY, SkillType.BRAWL.key);
        } else if (damager instanceof Projectile) {
            Projectile projectile = (Projectile)damager;
            if (projectile.getShooter() instanceof Player) {
                setMetadata(damagee, LAST_DAMAGE_CAUSE_KEY, SkillType.HUNT.key);
            }
        }
    }

    void buildNameMap() {
        nameMap.clear();
        // Put all the names in the map
        for (Skill skill : skillMap.values()) {
            nameMap.put(skill.skillType.key, skill);
            nameMap.put(skill.getDisplayName().toLowerCase(), skill);
            nameMap.put(skill.getShorthand().toLowerCase(), skill);
        }
        // Bake the map
        for (Map.Entry<String, Skill> entry : new ArrayList<>(nameMap.entrySet())) {
            Skill skill = entry.getValue();
            for (String name = entry.getKey(); name.length() > 0; name = name.substring(0, name.length() - 1)) {
                if (!nameMap.containsKey(name)) nameMap.put(name, skill);
            }
        }
    }

    public void onLevelUp(UUID uuid, SkillType skillType, int level) {
        final Player player = Bukkit.getServer().getPlayer(uuid);
        if (player == null) return;
        LevelUpEffect.launch(this, player, skillType, level);
        Bukkit.getServer().getPluginManager().callEvent(new SkillsLevelUpEvent(player, skillType, level));
    }

    public Collection<? extends Skill> getSkills() {
        return skillMap.values();
    }

    public Skill skillByName(String name) {
        return nameMap.get(name.toLowerCase());
    }

    public Skill getSkill(SkillType type) {
        return skillMap.get(type);
    }

    public <E extends Skill> E getSkill(Class<E> skillClass) {
        return skillClass.cast(skillClassMap.get(skillClass));
    }

    boolean hasDebugMode(Player player) {
        return (playersInDebugMode.contains(player.getUniqueId()));
    }

    void setDebugMode(Player player, boolean debugMode) {
        if (debugMode) {
            playersInDebugMode.add(player.getUniqueId());
        } else {
            playersInDebugMode.remove(player.getUniqueId());
        }
    }

    Session getSession(UUID uuid) {
        Session result = sessions.get(uuid);
        if (result == null) {
            result = new Session(this, uuid);
            sessions.put(uuid, result);
        }
        return result;
    }

    Session getSession(Player player) {
        return getSession(player.getUniqueId());
    }

    void onTick() {
        for (Player player : Bukkit.getServer().getOnlinePlayers()) {
            getSession(player.getUniqueId()).onTick(player);
        }
        score.saveOneDirtyRow();
    }

    void importRewards() {
        int linum = 0;
        BufferedReader in = null;
        Reward reward = null;
        try {
            Map<Reward.Key, Reward> map = new HashMap<>();
            final File file = new File(getDataFolder(), REWARDS_TXT);
            if (file.exists()) {
                in = new BufferedReader(new FileReader(file));
            } else {
                in = new BufferedReader(new InputStreamReader(getResource(REWARDS_TXT)));
            }
            String line = null;
            while (null != (line = in.readLine())) {
                linum++;
                line = line.split("#")[0];
                if (line.isEmpty()) continue;
                String[] tokens = line.split("\\s+");
                try {
                    reward = Reward.parse(tokens);
                } catch (RuntimeException re) {
                    getLogger().warning("Skipping " + REWARDS_TXT + " line " + linum);
                    re.printStackTrace();
                    continue;
                }
                if (map.containsKey(reward.key)) getLogger().warning("Warning: Duplicate key '" + reward.key + "' in line " + linum);
                map.put(reward.key, reward);
                try {
                    reward.test();
                } catch (RuntimeException re) {
                    getLogger().warning("Error in " + REWARDS_TXT + " line " + linum + ": " + line);
                    re.printStackTrace();
                }
            }
            score.setRewards(map);
            getLogger().info("Imported " + map.size() + " rewards from " + REWARDS_TXT);
        } catch (IOException ioe) {
            getLogger().warning("Error reading " + REWARDS_TXT + ". See console.");
            ioe.printStackTrace();
        } catch (RuntimeException re) {
            getLogger().warning("Error parsing " + REWARDS_TXT + ", line " + linum + ". See console.");
            getLogger().warning("" + reward);
            re.printStackTrace();
        } finally {
            try {
                in.close();
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
    }

    void importAnvilRecipes() {
        anvilRecipes.clear();
        YamlConfiguration config = YamlConfiguration.loadConfiguration(new File(getDataFolder(), ANVIL_RECIPES_YML));
        config.setDefaults(YamlConfiguration.loadConfiguration(new InputStreamReader(getResource(ANVIL_RECIPES_YML))));
        for (Map<?, ?> map: config.getMapList("AnvilRecipes")) {
            try {
                ConfigurationSection section = config.createSection("tmp", map);
                List<AnvilRecipe.Item> items = new ArrayList<>();
                for (int i = 0; i < 3; i += 1) {
                    String key;
                    switch (i) {
                    case 0: key = "inputA"; break;
                    case 1: key = "inputB"; break;
                    default: key = "output"; break;
                    }
                    if (!section.isSet(key)) {
                        items.add(null);
                    } else if (section.isString(key)) {
                        IngredientItem.Type type = IngredientItem.Type.valueOf(section.getString(key).toUpperCase());
                        items.add(new AnvilRecipe.Item(type, 1));
                    } else if (section.isList(key)) {
                        @SuppressWarnings("unchecked")
                        List<Object> list = (List<Object>)section.getList(key);
                        IngredientItem.Type type = IngredientItem.Type.valueOf(((String)list.get(0)).toUpperCase());
                        int amount = ((Number)list.get(1)).intValue();
                        items.add(new AnvilRecipe.Item(type, amount));
                    }
                }
                anvilRecipes.add(new AnvilRecipe(items.get(0), items.get(1), items.get(2)));
            } catch (RuntimeException re) {
                re.printStackTrace();
            }
        }
    }

    // Utility for metadata

    void setMetadata(Metadatable metadatable, String key, Object value) {
        metadatable.setMetadata(key, new FixedMetadataValue(this, value));
    }

    void removeMetadata(Metadatable metadatable, String key) {
        metadatable.removeMetadata(key, this);
    }

    Object getMetadata(Metadatable metadatable, String key) {
        for (MetadataValue value: metadatable.getMetadata(key)) {
            if (value.getOwningPlugin() == this) return value.value();
        }
        return null;
    }

    // Utility for scoreboards

    void setScoreboardValue(Entity entity, String key, String value) {
        removeScoreboardKey(entity, key);
        entity.addScoreboardTag(key + "=" + value);
    }

    void removeScoreboardKey(Entity entity, String key) {
        key = key + "=";
        List<String> removes = null;
        for (String tag: entity.getScoreboardTags()) {
            if (tag.startsWith(key)) {
                if (removes == null) removes = new ArrayList<>();
                removes.add(tag);
            }
        }
        if (removes == null) return;
        for (String remove: removes) {
            entity.removeScoreboardTag(remove);
        }
    }

    String getScoreboardValue(Entity entity, String key) {
        key = key + "=";
        for (String tag: entity.getScoreboardTags()) {
            if (tag.startsWith(key)) {
                return tag.substring(key.length());
            }
        }
        return null;
    }
}
