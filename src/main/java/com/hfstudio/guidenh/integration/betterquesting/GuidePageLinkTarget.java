package com.hfstudio.guidenh.integration.betterquesting;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import net.minecraft.util.ResourceLocation;

import com.github.bsideup.jabel.Desugar;
import com.hfstudio.guidenh.guide.PageAnchor;
import com.hfstudio.guidenh.guide.compiler.ParsedGuidePage;
import com.hfstudio.guidenh.guide.internal.GuideRegistry;
import com.hfstudio.guidenh.guide.internal.MutableGuide;
import com.hfstudio.guidenh.guide.mediawiki.MediaWikiPageTitleResolver;

@Desugar
public record GuidePageLinkTarget(ResourceLocation guideId, PageAnchor anchor, String title) {

    private static final ConcurrentMap<PageAnchor, GuidePageLinkTarget> CACHE = new ConcurrentHashMap<>();
    private static volatile long cacheRevision = Long.MIN_VALUE;

    public static GuidePageLinkTarget missing(PageAnchor anchor) {
        return new GuidePageLinkTarget(anchor.pageId(), anchor, anchor.toString());
    }

    public static GuidePageLinkTarget resolve(PageAnchor anchor) {
        clearCacheIfStale();
        return CACHE.computeIfAbsent(anchor, GuidePageLinkTarget::resolveUncached);
    }

    private static GuidePageLinkTarget resolveUncached(PageAnchor anchor) {
        GuidePageLinkTarget exact = resolveExact(anchor);
        if (exact != null) {
            return exact;
        }

        String path = anchor.pageId()
            .getResourcePath();
        if (!path.endsWith(".md")) {
            PageAnchor markdownAnchor = new PageAnchor(
                new ResourceLocation(
                    anchor.pageId()
                        .getResourceDomain(),
                    path + ".md"),
                anchor.anchor());
            GuidePageLinkTarget markdown = resolveExact(markdownAnchor);
            if (markdown != null) {
                return markdown;
            }
        }

        return missing(anchor);
    }

    private static GuidePageLinkTarget resolveExact(PageAnchor anchor) {
        for (MutableGuide guide : GuideRegistry.getAll()) {
            ParsedGuidePage page = guide.getParsedPage(anchor.pageId());
            if (page != null) {
                String title = MediaWikiPageTitleResolver.resolvePageTitle(guide, page);
                return new GuidePageLinkTarget(guide.getId(), anchor, title);
            }
        }
        return null;
    }

    private static void clearCacheIfStale() {
        long revision = GuideRegistry.getNavigationRevision();
        if (cacheRevision == revision) {
            return;
        }
        synchronized (CACHE) {
            if (cacheRevision != revision) {
                CACHE.clear();
                cacheRevision = revision;
            }
        }
    }
}
