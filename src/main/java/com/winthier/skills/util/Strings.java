package com.winthier.skills.Strings;

public class Strings
{
    static String camel(String input)
    {
        return input.substring(0, 1).toUpperCase() + input.substring(1, input.length()).toLowerCase();
    }
}
