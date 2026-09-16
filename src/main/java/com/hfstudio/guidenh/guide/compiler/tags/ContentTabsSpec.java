package com.hfstudio.guidenh.guide.compiler.tags;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.color.ColorValue;
import com.hfstudio.guidenh.guide.document.block.LytBlock;
import com.hfstudio.guidenh.guide.internal.markdown.MarkdownRuntimeBlocks.QuoteIconSpec;
import com.hfstudio.guidenh.libs.unist.UnistNode;

public record ContentTabsSpec(@Nullable String title, @Nullable QuoteIconSpec icon, @Nullable String defaultTitle,
    @Nullable Integer defaultIndex, @Nullable ColorValue accentColor, List<TabEntry> tabs,
    List<ValidationIssue> issues) {

    public boolean hasRenderableTabs() {
        return !tabs.isEmpty();
    }

    public record TabEntry(String title, LytBlock body, UnistNode sourceNode) {}

    public record ValidationIssue(String message, UnistNode sourceNode) {}
}
