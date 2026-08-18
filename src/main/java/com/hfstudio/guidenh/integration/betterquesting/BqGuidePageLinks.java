package com.hfstudio.guidenh.integration.betterquesting;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.minecraft.client.resources.I18n;
import net.minecraft.util.EnumChatFormatting;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.PageAnchor;
import com.hfstudio.guidenh.guide.internal.GuideRegistry;
import com.hfstudio.guidenh.guide.internal.util.LangUtil;

public class BqGuidePageLinks {

    public static final String URI_SCHEME = "guidenh";

    private static final int MAX_CACHE_ENTRIES = 512;
    private static final int MAX_CACHEABLE_TEXT_LENGTH = 32768;
    private static final Pattern GUIDE_TAG = Pattern
        .compile("\\[guide(?: page=([^\\] ]+))?](.*?)\\[/guide]", Pattern.DOTALL);
    private static final ConcurrentMap<String, String> TEXT_CACHE = new ConcurrentHashMap<>();
    private static final ConcurrentMap<String, Optional<PageAnchor>> PAGE_SPEC_CACHE = new ConcurrentHashMap<>();
    private static final ConcurrentMap<String, Optional<PageAnchor>> URI_CACHE = new ConcurrentHashMap<>();
    private static final ConcurrentMap<String, List<String>> TOOLTIP_CACHE = new ConcurrentHashMap<>();
    private static volatile long cacheRevision = Long.MIN_VALUE;
    private static volatile String cacheLanguage = "";

    private BqGuidePageLinks() {}

    public static String replaceGuideTags(String text) {
        if (text == null || !text.contains("[guide")) {
            return text;
        }

        clearCachesIfStale();
        if (text.length() > MAX_CACHEABLE_TEXT_LENGTH) {
            return replaceGuideTagsUncached(text, BqGuidePageLinks::createUriLink);
        }

        String cached = TEXT_CACHE.get(text);
        if (cached != null) {
            return cached;
        }

        String converted = replaceGuideTagsUncached(text, BqGuidePageLinks::createUriLink);
        return putBounded(TEXT_CACHE, text, converted);
    }

    public static String replaceGuideTags(String text, BiFunction<String, String, String> interactiveTextFactory) {
        if (text == null || !text.contains("[guide")) {
            return text;
        }

        clearCachesIfStale();
        return replaceGuideTagsUncached(
            text,
            (anchor, label) -> interactiveTextFactory.apply(anchor.toString(), label));
    }

    public static boolean isGuideUri(@Nullable String url) {
        return url != null && url.startsWith(URI_SCHEME + ":");
    }

    public static @Nullable PageAnchor parseUri(URI uri) {
        if (!URI_SCHEME.equals(uri.getScheme())) {
            return null;
        }

        clearCachesIfStale();
        String raw = uri.getRawSchemeSpecificPart();
        if (raw == null || raw.isEmpty()) {
            return null;
        }
        while (raw.startsWith("//")) {
            raw = raw.substring(2);
        }
        if (uri.getRawFragment() != null) {
            raw = raw + "#" + uri.getRawFragment();
        }
        return parsePageSpec(raw);
    }

    public static @Nullable PageAnchor parseUriString(@Nullable String url) {
        if (!isGuideUri(url)) {
            return null;
        }

        clearCachesIfStale();
        Optional<PageAnchor> cached = URI_CACHE.get(url);
        if (cached != null) {
            return cached.orElse(null);
        }

        Optional<PageAnchor> parsed = parseUriStringUncached(url);
        return putBounded(URI_CACHE, url, parsed).orElse(null);
    }

    public static @Nullable List<String> getTooltip(@Nullable String url) {
        if (!isGuideUri(url)) {
            return null;
        }

        clearCachesIfStale();
        List<String> cached = TOOLTIP_CACHE.get(url);
        if (cached != null) {
            return cached;
        }

        PageAnchor anchor = parseUriString(url);
        if (anchor == null) {
            return null;
        }
        return putBounded(TOOLTIP_CACHE, url, getTooltip(anchor));
    }

    public static List<String> getTooltip(PageAnchor anchor) {
        GuidePageLinkTarget target = GuidePageLinkTarget.resolve(anchor);
        List<String> tooltip = List.of(
            EnumChatFormatting.AQUA + I18n.format("guidenh.compat.bq.open_guide_page"),
            EnumChatFormatting.GRAY + target.title());
        return tooltip;
    }

    private static String replaceGuideTagsUncached(String text,
        BiFunction<PageAnchor, String, String> interactiveTextFactory) {
        Matcher matcher = GUIDE_TAG.matcher(text);
        StringBuffer result = new StringBuffer(text.length());
        while (matcher.find()) {
            String pageSpec = firstNonBlank(matcher.group(1), matcher.group(2));
            if (pageSpec == null) {
                matcher.appendReplacement(result, Matcher.quoteReplacement(matcher.group()));
                continue;
            }

            PageAnchor anchor = parsePageSpec(pageSpec.trim());
            if (anchor == null) {
                matcher.appendReplacement(result, Matcher.quoteReplacement(matcher.group()));
                continue;
            }

            String explicitText = matcher.group(1) != null ? matcher.group(2) : null;
            String label = explicitText != null && !explicitText.isEmpty() ? explicitText
                : GuidePageLinkTarget.resolve(anchor)
                    .title();
            matcher.appendReplacement(result, Matcher.quoteReplacement(interactiveTextFactory.apply(anchor, label)));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    public static @Nullable PageAnchor parsePageSpec(String pageSpec) {
        Optional<PageAnchor> cached = PAGE_SPEC_CACHE.get(pageSpec);
        if (cached != null) {
            return cached.orElse(null);
        }

        Optional<PageAnchor> parsed;
        try {
            parsed = Optional.of(PageAnchor.parse(pageSpec));
        } catch (RuntimeException e) {
            parsed = Optional.empty();
        }
        return putBounded(PAGE_SPEC_CACHE, pageSpec, parsed).orElse(null);
    }

    private static Optional<PageAnchor> parseUriStringUncached(String url) {
        String raw = url.substring((URI_SCHEME + ":").length());
        if (raw.isEmpty()) {
            return Optional.empty();
        }
        while (raw.startsWith("//")) {
            raw = raw.substring(2);
        }
        return Optional.ofNullable(parsePageSpec(raw));
    }

    private static <T> T putBounded(ConcurrentMap<String, T> cache, String key, T value) {
        if (cache.size() >= MAX_CACHE_ENTRIES) {
            cache.clear();
        }
        T existing = cache.putIfAbsent(key, value);
        return existing != null ? existing : value;
    }

    private static void clearCachesIfStale() {
        long revision = GuideRegistry.getNavigationRevision();
        String language = LangUtil.getCurrentLanguage();
        if (cacheRevision == revision && language.equals(cacheLanguage)) {
            return;
        }
        synchronized (BqGuidePageLinks.class) {
            if (cacheRevision != revision || !language.equals(cacheLanguage)) {
                TEXT_CACHE.clear();
                PAGE_SPEC_CACHE.clear();
                URI_CACHE.clear();
                TOOLTIP_CACHE.clear();
                cacheRevision = revision;
                cacheLanguage = language;
            }
        }
    }

    private static String createUriLink(PageAnchor anchor, String label) {
        return "[url link=" + URI_SCHEME + ":" + anchor + "]" + label + "[/url]";
    }

    private static @Nullable String firstNonBlank(@Nullable String first, @Nullable String second) {
        if (first != null && !first.trim()
            .isEmpty()) {
            return first;
        }
        if (second != null && !second.trim()
            .isEmpty()) {
            return second;
        }
        return null;
    }
}
