package com.hfstudio.guidenh.guide.internal.search;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

import net.minecraft.util.EnumChatFormatting;

public class GuideTitleQuery implements Predicate<String> {

    private final List<List<String>> terms;

    public GuideTitleQuery(String query) {
        var parsedTerms = new ArrayList<List<String>>();
        for (String token : QueryStringSplitter.split(query.toLowerCase(Locale.ROOT))) {
            var fragments = new ArrayList<String>();
            for (String fragment : token.split("\\*")) {
                if (!fragment.isEmpty()) {
                    fragments.add(fragment);
                }
            }
            if (!fragments.isEmpty()) {
                parsedTerms.add(List.copyOf(fragments));
            }
        }
        terms = List.copyOf(parsedTerms);
    }

    public boolean isEmpty() {
        return terms.isEmpty();
    }

    @Override
    public boolean test(String title) {
        if (terms.isEmpty()) {
            return true;
        }
        String plainTitle = title.indexOf('§') >= 0 ? EnumChatFormatting.getTextWithoutFormattingCodes(title) : title;
        String normalizedTitle = plainTitle.toLowerCase(Locale.ROOT);
        for (List<String> fragments : terms) {
            int offset = 0;
            for (String fragment : fragments) {
                int match = normalizedTitle.indexOf(fragment, offset);
                if (match < 0) {
                    return false;
                }
                offset = match + fragment.length();
            }
        }
        return true;
    }
}
