package com.winthier.skills;

import java.util.Arrays;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.potion.PotionEffectType;

@Data
@AllArgsConstructor
@RequiredArgsConstructor
class Reward {
    public enum Target {
        NAME, BLOCK, ITEM, ENTITY, ENCHANTMENT, POTION_EFFECT;
    }

    @Value
    static class Key {
        @NonNull final SkillType skill;
        @NonNull final Target target;
        final Integer type;
        final Integer data;
        final String name;

        static Integer parseInt(String arg) {
            if (arg.equals("-")) return null;
            return Integer.parseInt(arg);
        }

        @SuppressWarnings("deprecation") static Integer parseType(Target target, String arg) {
            if (arg.equals("-")) return null;
            try {
                switch (target) {
                case BLOCK:
                case ITEM:
                    return Material.valueOf(arg.toUpperCase()).getId();
                case ENCHANTMENT:
                    return Enchantment.getByName(arg.toUpperCase()).getId();
                case POTION_EFFECT:
                    return PotionEffectType.getByName(arg.toUpperCase()).getId();
                default:
                    return Integer.parseInt(arg);
                }
            } catch (IllegalArgumentException iae) {
                iae.printStackTrace();
            }
            return 0;
        }

        static String parseName(String arg) {
            if (arg.equals("-")) return null;
            return arg;
        }

        static Key parse(String[] tokens) {
            if (tokens.length != 5) throw new IllegalArgumentException("5 items required");
            String skillTypeArg = tokens[0];
            String targetArg = tokens[1];
            String typeArg = tokens[2];
            String dataArg = tokens[3];
            String nameArg = tokens[4];
            Skill skill = SkillsPlugin.getInstance().skillByName(skillTypeArg);
            if (skill == null) throw new IllegalArgumentException("Skill not found: " + skillTypeArg);
            Target target = Target.valueOf(targetArg.toUpperCase());
            Integer type = parseType(target, typeArg);
            Integer data = parseInt(dataArg);
            String name = parseName(nameArg);
            return new Key(skill.getSkillType(), target, type, data, name);
        }

        @SuppressWarnings("deprecation")
        String typeAsPrettyString() {
            if (type == null) return "-";
            try {
                switch (target) {
                case BLOCK:
                case ITEM:
                    return Material.getMaterial(type).name();
                case ENCHANTMENT:
                    return Enchantment.getById(type).getName();
                case POTION_EFFECT:
                    return PotionEffectType.getById(type).getName();
                default:
                    return "?";
                }
            } catch (IllegalArgumentException iae) {
                iae.printStackTrace();
            }
            return "?";
        }

        String typeAsString() {
            if (type == null) return "-";
            return "" + type;
        }

        String dataAsString() {
            if (data == null) return "-";
            return "" + data;
        }

        String nameAsString() {
            if (name == null) return "-";
            return name;
        }

        @Override public String toString() {
            return String.format("%s %s (%s) %s:%s {%s}", Msg.camelCase(skill.name()), Msg.camelCase(target.name()), typeAsPrettyString(), typeAsString(), dataAsString(), nameAsString());
        }
    }

    @Getter final Key key;
    @Getter private float skillPoints = 0;
    @Getter private float money = 0;
    @Getter private float exp = 0;

    static Reward parse(String[] tokens) {
        if (tokens.length != 8 && tokens.length != 9) throw new IllegalArgumentException("8 or 9 items required");
        String skillPointsArg = tokens[5];
        String moneyArg = tokens[6];
        String expArg = tokens[7];
        Key key = Key.parse(Arrays.copyOfRange(tokens, 0, 5));
        float skillPoints = Float.parseFloat(skillPointsArg);
        float money = Float.parseFloat(moneyArg);
        float exp = Float.parseFloat(expArg);
        if (tokens.length >= 9) {
            float factor = Float.parseFloat(tokens[8]);
            skillPoints *= factor;
            money *= factor;
            exp *= factor;
        }
        return new Reward(key, skillPoints, money, exp);
    }

    @Override
    public String toString() {
        return String.format("%s %.2f %.2f %.2f", key, skillPoints, money, exp);
    }
}
