package com.winthier.skills;

import com.winthier.custom.CustomPlugin;
import com.winthier.custom.entity.CustomEntity;
import com.winthier.custom.entity.EntityContext;
import com.winthier.custom.entity.EntityWatcher;
import com.winthier.custom.entity.TickableEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Cow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

@RequiredArgsConstructor
public final class LootEntity implements CustomEntity, TickableEntity {
    static final String CUSTOM_ID = "skills:loot";
    private final SkillsPlugin plugin;

    @Override
    public String getCustomId() {
        return CUSTOM_ID;
    }

    @Override
    public Entity spawnEntity(Location location) {
        Cow result = location.getWorld().spawn(location, Cow.class, (cow) -> {
                cow.setCustomName("Dinnerbone");
                cow.setAI(false);
                cow.setSilent(true);
            });
        return result;
    }

    @Override
    public EntityWatcher createEntityWatcher(Entity entity) {
        return new Watcher(this, entity);
    }

    @Override public void onTick(EntityWatcher watcher) {
        ((Watcher)watcher).onTick();
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEntityEvent event, EntityContext context) {
        Watcher watcher = (Watcher)context.getEntityWatcher();
        if (watcher.owners.isEmpty() || watcher.owners.contains(event.getPlayer().getUniqueId())) {
            event.getPlayer().openInventory(watcher.getInventory());
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event, EntityContext context) {
        event.setCancelled(true);
    }

    @RequiredArgsConstructor @Getter
    final static class Watcher implements EntityWatcher {
        final LootEntity customEntity;
        final Entity entity;
        @Setter String inventoryTitle = "Loot";
        @Setter int inventorySize = 18;
        @Setter List<UUID> owners = new ArrayList<>();
        private long age = 0;
        private Inventory inventory = null;

        void remove() {
            entity.remove();
            for (HumanEntity viewer: getInventory().getViewers()) {
                viewer.closeInventory();
            }
            inventory = null;
        }

        void onTick() {
            age += 1;
            if (age > 6000) {
                remove();
                return;
            }
            // Remove if empty and unwatched
            if (age < 20) return;
            if (inventory == null) {
                remove();
                return;
            }
            if (!getInventory().getViewers().isEmpty()) return;
            for (ItemStack item: getInventory().getContents()) {
                if (item != null && item.getType() != Material.AIR) return;
            }
            remove();
        }

        Inventory getInventory() {
            if (inventory == null) {
                inventory = Bukkit.getServer().createInventory(null, inventorySize, inventoryTitle);
            }
            return inventory;
        }

        @Override
        public void handleMessage(CommandSender sender, String[] args) {
            if (args.length == 0) return;
            switch (args[0]) {
            case "debug":
                sender.sendMessage(LootEntity.CUSTOM_ID + " " + Msg.capitalize(entity.getType().name()) + " received debug message");
                getInventory().addItem(new ItemStack(Material.STICK));
                break;
            case "title":
            case "name":
                if (args.length > 1) {
                    StringBuilder sb = new StringBuilder(args[1]);
                    for (int i = 2; i < args.length; i += 1) sb.append(" ").append(args[i]);
                    inventoryTitle = sb.toString();
                    sender.sendMessage(LootEntity.CUSTOM_ID + " " + Msg.capitalize(entity.getType().name()) + " setting inventory title to \"" + sb.toString() + "\"");
                }
            break;
            case "size":
                if (args.length == 2) {
                    try {
                        inventorySize = Integer.parseInt(args[1]);
                        sender.sendMessage(LootEntity.CUSTOM_ID + " " + Msg.capitalize(entity.getType().name()) + " setting inventory size to " + inventorySize);
                    } catch (NumberFormatException nfe) {
                        nfe.printStackTrace();
                    }
                }
                break;
            }
        }
    }
}
