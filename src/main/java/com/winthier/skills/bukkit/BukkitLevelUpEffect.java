package com.winthier.skills.bukkit;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.text.WordUtils;
import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Sound; 
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

@RequiredArgsConstructor
public class BukkitLevelUpEffect extends BukkitRunnable
{
    final UUID player;
    final BukkitSkill skill;
    final int level;
    int ticks = 0;
    final int MAX_TICKS = 20*8;
    final int RADIUS = 64;

    static void launch(Player player, BukkitSkill skill, int level)
    {
        new BukkitLevelUpEffect(player.getUniqueId(), skill, level).runTaskTimer(BukkitSkillsPlugin.getInstance(), 0, 1);
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
        if (ticks == 0) {
            showLevelUpTitle(player);
            if (level % 5 == 0) {
                announceLevelUp(player);
            } else {
                informLevelUp(player);
            }
        }
        if (level % 5 == 0) {
            if (ticks % 8 == 0) colorful(player, ticks, 0.1);
            if (ticks % 8 == 4) colorful(player, ticks, 2.3);
            if (ticks == 20*2) player.getWorld().playSound(player.getEyeLocation(), Sound.FIREWORK_TWINKLE, .6f, 1);
            if (ticks == 20*4) player.getWorld().playSound(player.getEyeLocation(), Sound.FIREWORK_TWINKLE, .5f, 1);
            if (ticks == 20*6) player.getWorld().playSound(player.getEyeLocation(), Sound.FIREWORK_TWINKLE2, .6f, 1);
            if (ticks == 20*8) player.getWorld().playSound(player.getEyeLocation(), Sound.FIREWORK_TWINKLE2, .5f, 1);
        }
        spiral(player, ticks);
        if (ticks == 0) player.getWorld().playSound(player.getEyeLocation(), Sound.LEVEL_UP, 1, 1);
        if (ticks % 20 == 10) player.getWorld().playSound(player.getEyeLocation(), Sound.ENDERMAN_TELEPORT, .1f, .65f);
    }


    void showLevelUpTitle(Player player)
    {
        BukkitUtil.title(player, "&b"+skill.getDisplayName(), "&bLevel " + level);
    }

    void announceLevelUp(Player player)
    {
        BukkitUtil.announceRaw(
            BukkitUtil.format("&f%s reached level %d in ", player.getName(), level),
            BukkitUtil.button(
                "&a[" + skill.getDisplayName() + "]",
                "/sk " + skill.getShorthand(),
                "&a" + skill.getDisplayName(),
                // TODO: Put something more interesting here?
                "&f&oSkill",
                "&r" + WordUtils.wrap(skill.getDescription(), 32)));
    }

    void informLevelUp(Player player)
    {
        BukkitUtil.raw(
            player,
            BukkitUtil.format("&fYou reached level %d in ", level),
            BukkitUtil.button(
                "&a[" + skill.getDisplayName() + "]",
                "/sk " + skill.getShorthand(),
                "&a" + skill.getDisplayName(),
                // TODO: Put something more interesting here?
                "&f&oSkill",
                "&r" + WordUtils.wrap(skill.getDescription(), 32)));
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
                                  Math.min(100, level/5+5), RADIUS);
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
