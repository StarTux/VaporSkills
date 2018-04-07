package com.winthier.skills;

import lombok.Value;

@Value
public class CustomReward implements Reward {
    private float skillPoints, money, exp;
}
