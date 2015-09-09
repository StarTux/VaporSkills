package com.winthier.skills;

import lombok.Value;

@Value
public class CustomReward implements Reward
{
    float skillPoints, money, exp;
}
