package com.winthier.skills.bukkit;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

@RequiredArgsConstructor
public class BukkitLevelUpEffect extends BukkitRunnable
{
    final UUID player;
    final int level;
    int ticks = 0;
    final int MAX_TICKS = 20*8;
    final int RADIUS = 64;

    static void launch(Player player, int level)
    {
        new BukkitLevelUpEffect(player.getUniqueId(), level).runTaskTimer(BukkitSkillsPlugin.getInstance(), 0, 1);
    }

    Player getPlayer()
    {
        return Bukkit.getServer().getPlayer(this.player);
    }

    @Override
    public void run()
    {
        int ticks = this.ticks++;
        if (ticks > MAX_TICKS) {
            cancel();
            return;
        }
        Player player = getPlayer();
        if (player == null) {
            cancel();
            return;
        }
        if (ticks % 8 == 0) colorful(player, ticks, 0.1);
        if (ticks % 8 == 4) colorful(player, ticks, 2.3);
        spiral(player, ticks);
    }

    void colorful(Player player, int ticks, double height) {
        World world = player.getWorld();
        world.spigot().playEffect(player.getLocation().add(0,
                                                           height,
                                                           0),
                                  Effect.POTION_SWIRL,
                                  0, 0,
                                  .35f, 0f, .35f,
                                  1,
                                  Math.min(55, level/10+5), RADIUS);
    }

    void spiral(Player player, int ticks) {
        World world = player.getWorld();
        double frac = Math.PI * (double)ticks / (double)13;
        world.spigot().playEffect(player.getLocation().add(Math.cos(frac),
                                                           2.3*(double)ticks/(double)MAX_TICKS,
                                                           Math.sin(frac)),
                                  Effect.FIREWORKS_SPARK,
                                  0, 0,
                                  0, 0, 0,
                                  .0001f,
                                  1, RADIUS);
    }
}
