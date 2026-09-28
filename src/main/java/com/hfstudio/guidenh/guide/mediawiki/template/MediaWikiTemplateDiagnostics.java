package com.hfstudio.guidenh.guide.mediawiki.template;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.minecraft.util.ResourceLocation;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

/**
 * Reports expansion problems once per kind and page, so a repeated mistake does not flood the log. Uses a
 * logger of its own rather than the shared debug logger, which resolves its appender through FML and is
 * therefore unusable before the game starts.
 */
public class MediaWikiTemplateDiagnostics {

    public static final String LOGGER_NAME = "GuideNH-MediaWikiTemplate";

    private static final Pattern LEGACY_CALL = Pattern
        .compile("\\{\\{\\{[^{}\\n]{1,80}\\}\\}\\}|\\{\\{[^{}\\n]{1,80}\\}\\}");

    private static final Logger LOGGER = LogManager.getLogger(LOGGER_NAME);

    private MediaWikiTemplateDiagnostics() {}

    public static void report(@Nullable ResourceLocation pageId, List<MediaWikiTemplateIssue> issues) {
        for (String line : formatIssues(pageId, issues)) {
            LOGGER.warn(line);
        }
    }

    public static void reportFailure(@Nullable ResourceLocation pageId, Throwable failure) {
        LOGGER.error(formatFailure(pageId, failure));
    }

    public static void reportCopyFailure(String nodeType, Throwable failure) {
        LOGGER.warn(
            "[GuideNH] [MediaWikiTemplate] Could not copy a {} node for a template call, rendering it as text: {}",
            nodeType,
            failure.toString());
    }

    public static void reportLegacySyntax(@Nullable ResourceLocation pageId, List<String> samples) {
        if (samples.isEmpty()) {
            return;
        }
        LOGGER.warn(
            "[GuideNH] [MediaWikiTemplate] {} still uses brace syntax, which is no longer a template call. "
                + "Write <Template name=\"...\" /> instead. Examples: {}",
            pageId,
            samples);
    }

    public static List<String> findLegacySyntax(String source, int limit) {
        if (source == null || source.isEmpty()) {
            return List.of();
        }
        List<String> found = new ArrayList<>();
        Matcher matcher = LEGACY_CALL.matcher(source);
        while (matcher.find() && found.size() < limit) {
            found.add(matcher.group());
        }
        return found;
    }

    public static List<String> formatIssues(@Nullable ResourceLocation pageId, List<MediaWikiTemplateIssue> issues) {
        if (issues.isEmpty()) {
            return List.of();
        }
        Set<String> seen = new LinkedHashSet<>();
        List<String> lines = new ArrayList<>();
        for (MediaWikiTemplateIssue issue : issues) {
            if (seen.add(issue.kind() + "\u0000" + issue.message())) {
                lines.add("[GuideNH] [MediaWikiTemplate] " + issue.kind() + " at " + pageId + ": " + issue.message());
            }
        }
        return lines;
    }

    public static String formatFailure(@Nullable ResourceLocation pageId, Throwable failure) {
        return "[GuideNH] [MediaWikiTemplate] Expansion failed for " + pageId + ": " + failure;
    }

    public static void reportRebuild(int changedTemplates, int affectedPages) {
        LOGGER.warn(
            "[GuideNH] [MediaWikiTemplate] {} template page(s) changed, {} page(s) will be rebuilt",
            changedTemplates,
            affectedPages);
    }
}
