package com.winthier.skills.bukkit;

import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

class BukkitSkillDig extends BukkitSkillAbstractBlockBreak
{
    @lombok.Getter final BukkitSkillType skillType = BukkitSkillType.DIG;
}
