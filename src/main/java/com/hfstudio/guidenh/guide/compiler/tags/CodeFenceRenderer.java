package com.hfstudio.guidenh.guide.compiler.tags;

import java.util.Set;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.document.block.LytBlock;
import com.hfstudio.guidenh.guide.extensions.Extension;
import com.hfstudio.guidenh.guide.extensions.ExtensionPoint;

public interface CodeFenceRenderer extends Extension {

    ExtensionPoint<CodeFenceRenderer> EXTENSION_POINT = new ExtensionPoint<>(CodeFenceRenderer.class);

    Set<String> getFenceNames();

    @Nullable
    LytBlock renderFence(PageCompiler compiler, String fenceName, String codeText, @Nullable String meta);

    /**
     * The HTML a fence body becomes on the exported site, where a fence this renderer owns would otherwise be shown.
     */
    @Nullable
    default String renderSiteFence(String fenceName, String codeText, @Nullable String meta) {
        return null;
    }
}
