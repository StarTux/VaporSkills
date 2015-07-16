package com.winthier.skills;

public enum SkillType
{
    ARCHER("archery", "archer"),
    BRAWL("brawling", "brawling"),
    BREED("breeding", "breeder"),
    BREW("brewing", "brewer"),
    BUILD("building", "builder"),
    BUTCHER("butchering", "butcher"),
    COOK("cooking", "chef"),
    DIG("digging", "digger"),
    EAT("eating", "eater"),
    ENCHANT("enchanting", "enchanter"),
    EXPLORE("exploration", "explorer"),
    FISH("fishing", "fisher"),
    GARDEN("gardening", "gardener"),
    HARVEST("harvesting", "harvester"),
    MINE("mining", "miner"),
    SACRIFICE("sacrifice", "sacrificer"),
    SMELT("smelting", "smelter"),
    TRAVEL("traveling", "traveler"),
    ;

    public final String gerund, demonym;
    SkillType(String gerund, String demonym)
    {
        this.gerund = gerund;
        this.demonym = demonym;
    }
}
