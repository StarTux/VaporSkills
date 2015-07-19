package com.winthier.skills.util;

import java.util.List;
import lombok.NonNull;

public class Strings
{
    public static String camelCase(@NonNull String input)
    {
        return input.substring(0, 1).toUpperCase() + input.substring(1, input.length()).toLowerCase();
    }

    public static String fold(@NonNull String[] list,
			      @NonNull String delim)
    {
	if (list.length == 0) return "";
	StringBuilder sb = new StringBuilder(list[0]);
	for (int i = 1; i < list.length; ++i) {
	    sb.append(delim).append(list[i]);
	}
	return sb.toString();
    }

    public static String fold(@NonNull List<String> list,
			      @NonNull String delim)
    {
	return fold(list.toArray(new String[0]), delim);
    }
}
