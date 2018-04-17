package com.winthier.skills;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

@RequiredArgsConstructor
final class LevelUpEffect extends BukkitRunnable {
    private final SkillsPlugin plugin;
    private final UUID uuid;
    private final Skill skill;
    private final int level;
    private final boolean special;
    private int tickCounter = 0;
    private static final int MAX_TICKS = 20 * 8;

    static void launch(SkillsPlugin plugin, Player player, Skill skill, int level) {
        boolean special = level > 100 || (level > 50 && level % 5 == 0) || level % 10 == 0;
        new LevelUpEffect(plugin, player.getUniqueId(), skill, level, special).runTaskTimer(plugin, 0, 1);
    }

    @Override
    public void run() {
        int ticks = this.tickCounter++;
        if (ticks > MAX_TICKS) {
            cancel();
            return;
        }
        Player player = Bukkit.getServer().getPlayer(uuid);
        if (player == null || !player.isValid()) {
            cancel();
            return;
        }
        if (special) {
            if (ticks == 0) {
                announceLevelUp(player);
                showLevelUpTitle(player);
            }
            if (ticks % 10 == 0) colorful(player, ticks, 3 * (double)ticks / (double)MAX_TICKS);
            if (ticks == 20 * 2) player.getWorld().playSound(player.getEyeLocation(), Sound.ENTITY_FIREWORK_TWINKLE, .6f, 1);
            if (ticks == 20 * 4) player.getWorld().playSound(player.getEyeLocation(), Sound.ENTITY_FIREWORK_TWINKLE, .5f, 1);
            if (ticks == 20 * 6) player.getWorld().playSound(player.getEyeLocation(), Sound.ENTITY_FIREWORK_TWINKLE_FAR, .6f, 1);
            if (ticks == 20 * 8) player.getWorld().playSound(player.getEyeLocation(), Sound.ENTITY_FIREWORK_TWINKLE_FAR, .5f, 1);
        } else {
            if (ticks == 0) {
                informLevelUp(player);
                showLevelUpSubtitle(player, 0);
            } else if (ticks == 40) {
                showLevelUpSubtitle(player, 1);
            }
        }
        if (ticks == 0) player.getWorld().playSound(player.getEyeLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1, 1);
        if (ticks % 20 == 10) player.getWorld().playSound(player.getEyeLocation(), Sound.ENTITY_ENDERMEN_TELEPORT, .1f, .65f);
        spiral(player, ticks);
    }


    void showLevelUpTitle(Player player) {
        Msg.title(player, "&a" + skill.getDisplayName(), "&aLevel " + level);
    }

    void showLevelUpSubtitle(Player player, int n) {
        if (n == 0) {
            Msg.title(player, "", "&a" + skill.getDisplayName());
        } else if (n == 1) {
            Msg.title(player, "", "&aLevel " + level);
        }
    }

    void announceLevelUp(Player player) {
        Msg.announceRaw(
            Msg.format("&f%s reached level %d in ", player.getName(), level),
            Msg.button(
                "&a[" + skill.getDisplayName() + "]",
                "/sk " + skill.getShorthand(),
                "&a" + skill.getDisplayName(),
                "&f&oSkill",
                "&r" + Msg.wrap(skill.getDescription(), 32)));
    }

    void informLevelUp(Player player) {
        Msg.raw(
            player,
            Msg.format("&fYou reached level %d in ", level),
            Msg.button(
                "&a[" + skill.getDisplayName() + "]",
                "/sk " + skill.getShorthand(),
                "&a" + skill.getDisplayName(),
                "&f&oSkill",
                "&r" + Msg.wrap(skill.getDescription(), 32)));
        Location la = player.getLocation();
        for (Player nearby: player.getWorld().getPlayers()) {
            if (nearby.equals(player)) continue;
            Location lb = nearby.getLocation();
            int dx = la.getBlockX() - lb.getBlockX();
            int dy = la.getBlockY() - lb.getBlockY();
            int dz = la.getBlockZ() - lb.getBlockZ();
            if (dx * dx + dy * dy + dz * dz > 64 * 64) continue;
            Msg.raw(nearby,
                           Msg.format("&f%s reached level %d in ", player.getName(), level),
                           Msg.button("&a[" + skill.getDisplayName() + "]",
                                             "/sk " + skill.getShorthand(),
                                             "&a" + skill.getDisplayName(),
                                             "&f&oSkill",
                                             "&r" + Msg.wrap(skill.getDescription(), 32)));
        }
    }

    void colorful(Player player, int ticks, double height) {
        World world = player.getWorld();
        world.spawnParticle(Particle.SPELL_MOB,
                            player.getLocation().add(0, height, 0),
                            Math.min(50, level / 10 + 5), // count
                            .35f, 0f, .35f, // offset
                            -1f); // extra (color?)
    }

    void spiral(Player player, int ticks) {
        World world = player.getWorld();
        double frac = Math.PI * (double)ticks / (double)13;
        world.spawnParticle(Particle.FIREWORKS_SPARK,
                            player.getLocation().add(Math.cos(frac) * 2,
                                                     2.3 * (double)ticks / (double)MAX_TICKS,
                                                     Math.sin(frac) * 2),
                            1, // count
                            0f, 0f, 0f, // offset
                            .0001f); // extra (speed?)
    }
}
