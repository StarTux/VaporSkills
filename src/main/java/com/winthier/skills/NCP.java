package com.winthier.skills;

import fr.neatmonster.nocheatplus.checks.CheckType;
import fr.neatmonster.nocheatplus.hooks.NCPExemptionManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

final class NCP {
    private NCP() { }

    static final int MOVING = 1;
    static final int FIGHT = 2;

    private static CheckType checkTypeOf(int checkType) {
        switch (checkType) {
        case MOVING: return CheckType.MOVING;
        case FIGHT: return CheckType.FIGHT;
        default:
            return null;
        }
    }

    static void exempt(Player player, int... checkType) {
        Plugin plugin = Bukkit.getServer().getPluginManager().getPlugin("NoCheatPlus");
        if (plugin == null || !plugin.isEnabled()) return;
        for (int i: checkType) {
            CheckType ncpCheckType = checkTypeOf(i);
            if (ncpCheckType == null) {
                System.err.println("Unknown check type: " + checkType);
                return;
            }
            NCPExemptionManager.exemptPermanently(player.getUniqueId(), ncpCheckType);
        }
    }

    static void unexempt(Player player, int... checkType) {
        Plugin plugin = Bukkit.getServer().getPluginManager().getPlugin("NoCheatPlus");
        if (plugin == null || !plugin.isEnabled()) return;
        for (int i: checkType) {
            CheckType ncpCheckType = checkTypeOf(i);
            if (ncpCheckType == null) {
                System.err.println("Unknown check type: " + checkType);
                return;
            }
            NCPExemptionManager.unexempt(player.getUniqueId(), ncpCheckType);
        }
    }
}
