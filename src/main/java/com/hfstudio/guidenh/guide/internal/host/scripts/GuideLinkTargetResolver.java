package com.hfstudio.guidenh.guide.internal.host.scripts;

import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.GuideAnchor;
import com.hfstudio.guidenh.guide.PageAnchor;
import com.hfstudio.guidenh.guide.compiler.IdUtils;
import com.hfstudio.guidenh.guide.internal.GuideRegistry;
import com.hfstudio.guidenh.guide.internal.host.ScriptContext;

public class GuideLinkTargetResolver {

    protected GuideLinkTargetResolver() {}

    public static @Nullable GuideAnchor resolve(String linksTo, @Nullable String currentPage,
        @Nullable String currentGuide, ScriptContext context) {
        if (currentPage == null || linksTo == null
            || linksTo.trim()
                .isEmpty())
            return null;
        try {
            ResourceLocation page = new ResourceLocation(currentPage);
            ResourceLocation guide = currentGuide == null ? null : new ResourceLocation(currentGuide);
            String target = linksTo.trim();
            int separator = target.indexOf('#');
            String pagePart = separator < 0 ? target : target.substring(0, separator);
            String anchor = separator < 0 ? null : target.substring(separator + 1);
            ResourceLocation pageId = pagePart.isEmpty() ? page : IdUtils.resolveLink(pagePart, page);
            ResourceLocation guideId = guide != null && pageId.getResourceDomain()
                .equals(page.getResourceDomain())
                    ? guide
                    : new ResourceLocation(
                        pageId.getResourceDomain(),
                        guide == null ? "guidenh" : guide.getResourcePath());
            var collection = context.getPageCollection();
            boolean exists = collection != null && guideId.equals(collection.getId()) ? collection.pageExists(pageId)
                : GuideRegistry.getById(guideId) != null && GuideRegistry.getById(guideId)
                    .pageExists(pageId);
            return exists ? new GuideAnchor(guideId, new PageAnchor(pageId, anchor)) : null;
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}
