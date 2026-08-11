package com.hfstudio.guidenh.integration.betterquesting;

import java.util.List;

import com.hfstudio.guidenh.guide.GuideBuilder;
import com.hfstudio.guidenh.guide.compiler.TagCompiler;
import com.hfstudio.guidenh.integration.betterquesting.compiler.QuestCardCompiler;
import com.hfstudio.guidenh.integration.betterquesting.compiler.QuestLinkCompiler;

/**
 * BetterQuesting integration entry point.
 * <p/>
 * This class never references BetterQuesting types directly; all such access is funneled
 * through {@link BqHelpers}. That keeps {@code BqCompat} safe to load even when BetterQuesting
 * is missing from the classpath, so it can be statically referenced from the rest of the mod.
 */
public class BqCompat {

    /**
     * Attaches the {@link QuestIndex} to the given guide builder. Safe to call when BQ is
     * absent because {@link QuestIndex} only depends on standard library types.
     */
    public static void attachQuestIndex(GuideBuilder builder) {
        builder.index(new QuestIndex());
    }

    /**
     * Appends BetterQuesting-aware tag compilers to the given mutable list of tag compilers.
     * The compilers themselves do not reference BQ types; they delegate runtime work to
     * {@link BqHelpers}.
     */
    public static void appendCompilers(List<TagCompiler> compilers) {
        compilers.add(new QuestLinkCompiler());
        compilers.add(new QuestCardCompiler());
    }

}
