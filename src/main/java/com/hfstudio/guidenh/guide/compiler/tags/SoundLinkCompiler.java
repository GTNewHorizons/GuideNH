package com.hfstudio.guidenh.guide.compiler.tags;

import java.util.Collections;
import java.util.Set;

import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.document.flow.LytFlowLink;
import com.hfstudio.guidenh.guide.document.flow.LytFlowParent;
import com.hfstudio.guidenh.guide.sound.GuideSoundParsers;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;

public class SoundLinkCompiler extends FlowTagCompiler {

    @Override
    public Set<String> getTagNames() {
        return Collections.singleton("SoundLink");
    }

    @Override
    protected void compile(PageCompiler compiler, LytFlowParent parent, MdxJsxElementFields el) {
        var sound = GuideSoundParsers.parseAttributes(compiler, parent, el);
        if (sound == null) {
            parent.appendError(compiler, "SoundLink requires a sound or src attribute.", el);
            return;
        }
        var link = new LytFlowLink();
        link.setStyleClass("SoundLink");
        link.setData("soundSpec", sound);
        var children = el.children();
        if (children.isEmpty()) {
            // Self-closing: synthesize visible label from attributes
            var title = el.getAttributeString("title", "");
            if (!title.isEmpty()) {
                link.appendText(title);
            } else {
                // Use sound id short name (last segment after final '.')
                String soundPath = sound.soundId().getResourcePath();
                int lastDot = soundPath.lastIndexOf('.');
                String shortName = lastDot >= 0 ? soundPath.substring(lastDot + 1) : soundPath;
                link.appendText(shortName);
            }
        } else {
            compiler.compileInlineFragment(children, link);
        }
        parent.append(link);
    }

}
