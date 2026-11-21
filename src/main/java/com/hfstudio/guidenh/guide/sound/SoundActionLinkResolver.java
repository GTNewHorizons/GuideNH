package com.hfstudio.guidenh.guide.sound;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Set;

import com.hfstudio.guidenh.guide.compiler.ActionLinkResolver;
import com.hfstudio.guidenh.guide.internal.markdown.MarkdownActionLink;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxTextElement;

public class SoundActionLinkResolver implements ActionLinkResolver {

    @Override
    public Set<String> getSchemes() {
        return Set.of("sound", "sound-src");
    }

    @Override
    public MdxJsxTextElement resolve(String label, String destination, String defaultNamespace) {
        int schemeEnd = destination.indexOf(':');
        int queryStart = destination.indexOf('?', schemeEnd + 1);
        String path = destination.substring(schemeEnd + 1, queryStart < 0 ? destination.length() : queryStart);
        MdxJsxTextElement element = new MdxJsxTextElement("SoundLink", new ArrayList<>());
        element.addAttribute(
            "sound-src".equalsIgnoreCase(destination.substring(0, schemeEnd)) ? "src" : "sound",
            URLDecoder.decode(path, StandardCharsets.UTF_8));
        if (queryStart >= 0) {
            for (String pair : destination.substring(queryStart + 1)
                .split("&")) {
                int separator = pair.indexOf('=');
                if (separator <= 0) continue;
                String name = URLDecoder.decode(pair.substring(0, separator), StandardCharsets.UTF_8);
                String value = URLDecoder.decode(pair.substring(separator + 1), StandardCharsets.UTF_8);
                try {
                    if ("cooldown".equals(name)) {
                        element.setAttribute(name, Integer.toString(Integer.parseInt(value)));
                    } else if (Set.of("volume", "pitch", "radius", "minVolume")
                        .contains(name)) {
                            element.setAttribute(name, Float.toString(Float.parseFloat(value)));
                        }
                } catch (NumberFormatException ignored) {
                    // Legacy sound actions ignore malformed numeric parameters.
                }
            }
        }
        element.addChild(MarkdownActionLink.plainLabel(label));
        return element;
    }
}
