package com.hfstudio.guidenh.guide.indices;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.compiler.IdUtils;

public class ItemIdExpression {

    private final List<List<Predicate<ItemStack>>> alternatives;

    private ItemIdExpression(List<List<Predicate<ItemStack>>> alternatives) {
        this.alternatives = alternatives;
    }

    @Nullable
    public static ItemIdExpression parse(@Nullable String source) {
        if (source == null || source.trim()
            .isEmpty()) {
            return null;
        }

        var alternatives = new ArrayList<List<Predicate<ItemStack>>>();
        for (String part : source.trim()
            .split("\\s*\\|\\s*")) {
            var terms = new ArrayList<Predicate<ItemStack>>();
            for (String token : part.trim()
                .split("\\s+")) {
                Predicate<ItemStack> term = parseToken(token);
                if (term != null) {
                    terms.add(term);
                }
            }
            if (!terms.isEmpty()) {
                alternatives.add(List.copyOf(terms));
            }
        }
        return alternatives.isEmpty() ? null : new ItemIdExpression(List.copyOf(alternatives));
    }

    public boolean matches(@Nullable ItemStack stack) {
        if (stack == null || stack.getItem() == null) {
            return false;
        }
        for (List<Predicate<ItemStack>> alternative : alternatives) {
            boolean matches = true;
            for (Predicate<ItemStack> term : alternative) {
                if (!term.test(stack)) {
                    matches = false;
                    break;
                }
            }
            if (matches) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    private static Predicate<ItemStack> parseToken(String token) {
        var includes = new ArrayList<Predicate<ItemStack>>();
        var excludes = new ArrayList<Predicate<ItemStack>>();
        for (String rawRule : token.split(",")) {
            boolean excluded = rawRule.startsWith("!");
            String rule = excluded ? rawRule.substring(1) : rawRule;
            Predicate<ItemStack> matcher = parseRule(rule);
            if (matcher == null) {
                continue;
            }
            (excluded ? excludes : includes).add(matcher);
        }
        if (includes.isEmpty() && excludes.isEmpty()) {
            return null;
        }
        return stack -> (includes.isEmpty() || matchesAny(includes, stack)) && !matchesAny(excludes, stack);
    }

    private static boolean matchesAny(List<Predicate<ItemStack>> matchers, ItemStack stack) {
        for (Predicate<ItemStack> matcher : matchers) {
            if (matcher.test(stack)) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    private static Predicate<ItemStack> parseRule(String rule) {
        if (isDamageRule(rule)) {
            return damageMatcher(rule);
        }
        if (rule.startsWith("<") && rule.endsWith(">")) {
            return strictItemMatcher(rule.substring(1, rule.length() - 1));
        }
        Predicate<ItemStack> explicitItemMatcher = explicitItemMatcher(rule);
        if (explicitItemMatcher != null) {
            return explicitItemMatcher;
        }
        Predicate<String> matcher = stringMatcher(rule);
        if (matcher == null) {
            return null;
        }
        return stack -> {
            Object registryName = Item.itemRegistry.getNameForObject(stack.getItem());
            return registryName != null && matcher.test(registryName.toString());
        };
    }

    private static boolean isDamageRule(String rule) {
        int separator = rule.indexOf('-');
        if (separator < 0) {
            return IdUtils.isNonNegativeInt(rule);
        }
        return rule.indexOf('-', separator + 1) < 0 && IdUtils.isNonNegativeInt(rule.substring(0, separator))
            && IdUtils.isNonNegativeInt(rule.substring(separator + 1));
    }

    @Nullable
    private static Predicate<ItemStack> damageMatcher(String rule) {
        try {
            int separator = rule.indexOf('-');
            if (separator < 0) {
                int meta = Integer.parseInt(rule);
                return stack -> stack.getItemDamage() == meta;
            }
            int lower = Integer.parseInt(rule.substring(0, separator));
            int upper = Integer.parseInt(rule.substring(separator + 1));
            return stack -> stack.getItemDamage() >= lower && stack.getItemDamage() <= upper;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    @Nullable
    private static Predicate<ItemStack> strictItemMatcher(String rule) {
        if (!rule.contains(":")) {
            return null;
        }
        try {
            IdUtils.ParsedItemRef reference = IdUtils.parseItemRef(rule, "minecraft");
            if (reference == null) {
                return null;
            }
            Item item = (Item) Item.itemRegistry.getObject(reference.rawKey());
            if (item == null) {
                return null;
            }
            int meta = reference.meta();
            return stack -> stack.getItem() == item
                && (meta == OreDictionary.WILDCARD_VALUE || stack.getItemDamage() == meta);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    @Nullable
    private static Predicate<ItemStack> explicitItemMatcher(String rule) {
        try {
            IdUtils.ParsedItemRef reference = IdUtils.parseItemRef(rule, "minecraft");
            return reference != null && reference.hasExplicitMeta() ? strictItemMatcher(rule) : null;
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    @Nullable
    private static Predicate<String> stringMatcher(String rule) {
        if (rule.length() >= 3 && rule.startsWith("r/") && rule.endsWith("/")) {
            try {
                Pattern pattern = Pattern.compile(
                    rule.substring(2, rule.length() - 1),
                    Pattern.MULTILINE | Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
                return value -> pattern.matcher(value)
                    .find();
            } catch (PatternSyntaxException ignored) {
                return null;
            }
        }
        if (rule.isEmpty()) {
            return null;
        }
        String normalized = rule.toLowerCase(Locale.ROOT);
        return value -> value.toLowerCase(Locale.ROOT)
            .contains(normalized);
    }
}
