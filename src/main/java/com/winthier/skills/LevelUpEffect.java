package com.winthier.skills;

import java.util.Random;
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
    private final SkillType skillType;
    private final int level;
    private int tickCounter = 0;
    private static final int MAX_TICKS = 20 * 8;

    static void launch(SkillsPlugin plugin, Player player, SkillType skillType, int level) {
        new LevelUpEffect(plugin, player.getUniqueId(), skillType, level).runTaskTimer(plugin, 0, 1);
    }

    @Override
    public void run() {
        final Random random = plugin.random;
        int ticks = this.tickCounter++;
        Player player = Bukkit.getServer().getPlayer(uuid);
        if (ticks > MAX_TICKS || player == null || !player.isValid()) {
            cancel();
            return;
        }
        final World world = player.getWorld();
        if (skillType == SkillType.TOTAL) {
            if (ticks == 0) {
                Msg.announce("&f%s reached total skill level %d", player.getName(), level);
            }
            if (ticks % 20 == 10) world.playSound(player.getEyeLocation(), Sound.ENTITY_ENDERMEN_TELEPORT, .1f, .65f);
            if (level >= 10) {
                if (ticks == 20 * 2) world.playSound(player.getEyeLocation(), Sound.ENTITY_FIREWORK_TWINKLE, .6f, 1);
                if (ticks == 20 * 4) world.playSound(player.getEyeLocation(), Sound.ENTITY_FIREWORK_TWINKLE, .5f, 1);
                if (ticks == 20 * 6) world.playSound(player.getEyeLocation(), Sound.ENTITY_FIREWORK_TWINKLE_FAR, .6f, 1);
                if (ticks == 20 * 8) world.playSound(player.getEyeLocation(), Sound.ENTITY_FIREWORK_TWINKLE_FAR, .5f, 1);
            }
            // Spiral effect
            double frac = Math.PI * (double)ticks / (double)13;
            world.spawnParticle(Particle.FIREWORKS_SPARK,
                                player.getLocation().add(Math.cos(frac) * 2,
                                                         2.3 * (double)ticks / (double)MAX_TICKS,
                                                         Math.sin(frac) * 2),
                                1, // count
                                0f, 0f, 0f, // offset
                                .0001f); // extra (speed?)
        } else {
            Skill skill = plugin.getSkill(skillType);
            if (ticks % 10 == 0) {
                // Colorful particle effect
                double height = 3.0 * (double)ticks / (double)MAX_TICKS;
                double rnd = random.nextDouble() * Math.PI;
                world.spawnParticle(Particle.SPELL_MOB,
                                    player.getLocation().add(Math.cos(rnd) * 2.0, height, Math.sin(rnd) * 2.0),
                                    Math.min(50, level / 10 + 5), // count
                                    .5, .5f, .5f, // offset
                                    -1f); // extra (color?)
            }
            if (ticks == 0) {
                Msg.title(player, "", "&a" + skill.getDisplayName());
                world.playSound(player.getEyeLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1, 1);
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
            } else if (ticks == 40) {
                Msg.title(player, "", "&aLevel " + level);
            }
        }
    }
}
