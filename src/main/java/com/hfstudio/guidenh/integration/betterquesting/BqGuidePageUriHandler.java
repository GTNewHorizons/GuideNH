package com.hfstudio.guidenh.integration.betterquesting;

import java.net.URI;
import java.util.function.Predicate;

import net.minecraft.client.Minecraft;

import com.hfstudio.guidenh.guide.PageAnchor;
import com.hfstudio.guidenh.guide.internal.GuideScreen;

import betterquesting.api2.client.gui.misc.URIHandlers;

public class BqGuidePageUriHandler implements Predicate<URI> {

    private static boolean registered;

    public static synchronized void register() {
        if (registered || URIHandlers.get(BqGuidePageLinks.URI_SCHEME) != null) {
            registered = true;
            return;
        }
        URIHandlers.register(BqGuidePageLinks.URI_SCHEME, new BqGuidePageUriHandler());
        registered = true;
    }

    @Override
    public boolean test(URI uri) {
        PageAnchor anchor = BqGuidePageLinks.parseUri(uri);
        if (anchor == null) {
            return false;
        }

        GuidePageLinkTarget target = GuidePageLinkTarget.resolve(anchor);
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null) {
            return false;
        }

        GuideScreen.open(target.guideId(), target.anchor());
        return true;
    }
}
