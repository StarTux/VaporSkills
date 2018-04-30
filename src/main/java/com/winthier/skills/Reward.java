package com.winthier.skills;

import java.util.Arrays;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Getter @RequiredArgsConstructor
class Reward {
    final Key key;
    final double skillPoints;
    final double exp;

    public enum Category {
        BREAK_BLOCK,
        BREED_ENTITY,
        DAMAGE_ENTITY,
        EAT_ITEM,
        FISH_ITEM,
        INGREDIENT,
        KILL_ENTITY,
        SHEAR_ENTITY,
        SMELT_ITEM,
        SPEND_LEVELS,
        TAME_ENTITY;
    }

    @Getter @RequiredArgsConstructor @EqualsAndHashCode
    static class Key {
        @NonNull final SkillType skill;
        @NonNull final Category category;
        final String name;
        final Integer data;
        final String extra;

        static Key parse(String[] tokens) {
            if (tokens.length != 5) throw new IllegalArgumentException("5 items required");
            String skillTypeArg = tokens[0];
            String categoryArg = tokens[1];
            String nameArg = tokens[2];
            String dataArg = tokens[3];
            String extraArg = tokens[4];
            Skill skill = SkillsPlugin.getInstance().skillByName(skillTypeArg);
            if (skill == null) throw new IllegalArgumentException("Skill not found: " + skillTypeArg);
            Category category = Category.valueOf(categoryArg.toUpperCase());
            String name = "-".equals(nameArg) ? null : nameArg.toUpperCase();
            Integer data = "-".equals(dataArg) ? null : Integer.parseInt(dataArg);
            String extra = "-".equals(extraArg) ? null : extraArg;
            return new Key(skill.getSkillType(), category, name, data, extra);
        }

        @Override public String toString() {
            return String.format("%s %s %s %s %s", skill.name(), category.name(), name == null ? "-" : name, data == null ? "-" : data.toString(), extra == null ? "-" : extra);
        }
    }

    static Reward parse(String[] tokens) {
        if (tokens.length != 7 && tokens.length != 8) throw new IllegalArgumentException("7 or 8 items required");
        String skillPointsArg = tokens[5];
        String expArg = tokens[6];
        Key key = Key.parse(Arrays.copyOfRange(tokens, 0, 5));
        double skillPoints = Double.parseDouble(skillPointsArg);
        double exp = Double.parseDouble(expArg);
        if (tokens.length >= 9) {
            double factor = Double.parseDouble(tokens[7]);
            skillPoints *= factor;
            exp *= factor;
        }
        return new Reward(key, skillPoints, exp);
    }

    @Override
    public String toString() {
        return String.format("%s %.2f %.2f", key, skillPoints, exp);
    }
}
