package com.winthier.skills.bukkit.event;

import com.winthier.skills.Reward;
import com.winthier.skills.bukkit.BukkitSkill;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

@Getter
@RequiredArgsConstructor
public class SkillsRewardEvent extends Event {
    final Player player;
    final BukkitSkill skill;
    final Reward reward;
    private static HandlerList handlers = new HandlerList();

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }
}

