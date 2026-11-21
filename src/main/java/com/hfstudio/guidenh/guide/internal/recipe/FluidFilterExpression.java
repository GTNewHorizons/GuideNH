package com.hfstudio.guidenh.guide.internal.recipe;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

public record FluidFilterExpression(List<List<Term>> groups) {

    public static final FluidFilterExpression EMPTY = new FluidFilterExpression(List.of());

    public FluidFilterExpression {
        groups = groups.stream()
            .map(List::copyOf)
            .toList();
    }

    public record Term(String id, boolean negated) {}

    public static FluidFilterExpression parse(@Nullable String raw) {
        if (raw == null || raw.isBlank()) return EMPTY;
        var groups = new ArrayList<List<Term>>();
        for (String group : raw.split(",", -1)) {
            var terms = new ArrayList<Term>();
            for (String token : group.split("&", -1)) {
                String id = token.trim();
                boolean negated = id.startsWith("!");
                if (negated) id = id.substring(1)
                    .trim();
                if (id.isEmpty() || id.indexOf('!') >= 0) {
                    throw new IllegalArgumentException("Malformed fluid filter: " + token);
                }
                terms.add(new Term(id, negated));
            }
            groups.add(terms);
        }
        return new FluidFilterExpression(groups);
    }

    public boolean isEmpty() {
        return groups.isEmpty();
    }

    public boolean matches(Set<String> ids) {
        if (isEmpty()) return true;
        for (var group : groups) {
            boolean matched = true;
            for (var term : group) {
                if (ids.contains(term.id()) == term.negated()) {
                    matched = false;
                    break;
                }
            }
            if (matched) return true;
        }
        return false;
    }
}
