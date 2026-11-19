package com.hfstudio.guidenh.guide.internal.host.scripts;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.util.ResourceLocation;

import com.hfstudio.guidenh.guide.Guide;
import com.hfstudio.guidenh.guide.PageCollection;
import com.hfstudio.guidenh.guide.color.ColorUtils;
import com.hfstudio.guidenh.guide.color.ConstantColor;
import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.compiler.tags.MermaidCompiler.MermaidPlaceholder;
import com.hfstudio.guidenh.guide.document.block.LytBlock;
import com.hfstudio.guidenh.guide.document.block.LytCodeBlock;
import com.hfstudio.guidenh.guide.document.block.LytMermaidFlowchart;
import com.hfstudio.guidenh.guide.document.block.LytMermaidMindmap;
import com.hfstudio.guidenh.guide.document.block.LytNode;
import com.hfstudio.guidenh.guide.document.block.LytParagraph;
import com.hfstudio.guidenh.guide.document.flow.LytFlowContent;
import com.hfstudio.guidenh.guide.document.flow.LytFlowText;
import com.hfstudio.guidenh.guide.extensions.ExtensionCollection;
import com.hfstudio.guidenh.guide.internal.host.EventType;
import com.hfstudio.guidenh.guide.internal.host.LytEvent;
import com.hfstudio.guidenh.guide.internal.host.LytScript;
import com.hfstudio.guidenh.guide.internal.host.ScriptContext;
import com.hfstudio.guidenh.guide.internal.host.ScriptType;
import com.hfstudio.guidenh.guide.internal.mermaid.MermaidDiagramType;
import com.hfstudio.guidenh.guide.internal.mermaid.MermaidParser;
import com.hfstudio.guidenh.guide.internal.mermaid.MermaidSourceExtractor;
import com.hfstudio.guidenh.guide.internal.mermaid.flowchart.FlowchartNode;
import com.hfstudio.guidenh.guide.internal.mermaid.flowchart.FlowchartParser;
import com.hfstudio.guidenh.guide.internal.mermaid.mindmap.MindmapNode;
import com.hfstudio.guidenh.guide.internal.mermaid.mindmap.MindmapParser;
import com.hfstudio.guidenh.guide.scene.support.GuideDebugLog;
import com.hfstudio.guidenh.guide.style.TextAlignment;
import com.hfstudio.guidenh.guide.style.TextStyle;
import com.hfstudio.guidenh.guide.style.WhiteSpaceMode;

public class MermaidScript implements LytScript {

    @Override
    public ScriptType type() {
        return ScriptType.JAVA;
    }

    @Override
    public String styleClass() {
        return "Mermaid";
    }

    @Override
    public boolean isAsync() {
        return true;
    }

    @Override
    public void onEvent(Object node, LytEvent event, ScriptContext ctx) {
        if (event.type() != EventType.MOUNT) return;
        if (!(node instanceof MermaidPlaceholder ph)) return;

        String sourceText = ph.sourceText;
        if (sourceText == null && ph.src != null) {
            ResourceLocation srcId;
            try {
                srcId = new ResourceLocation(ph.src);
            } catch (Exception e) {
                replaceWithError(ctx, "Invalid source path: " + ph.src);
                return;
            }
            byte[] data = ctx.loadAsset(srcId);
            if (data != null) {
                sourceText = new String(data, StandardCharsets.UTF_8);
            }
        }

        if (sourceText != null) {
            sourceText = MermaidSourceExtractor.normalize(sourceText);
        }

        if (sourceText == null || sourceText.trim()
            .isEmpty()) {
            replaceWithError(ctx, "Source not found or empty");
            return;
        }

        MermaidDiagramType diagramType = ph.diagramType;
        if (diagramType == MermaidDiagramType.UNKNOWN) {
            diagramType = MermaidDiagramType.detect(sourceText);
        }

        try {
            switch (diagramType) {
                case MINDMAP -> renderMindmap(ctx, ph, sourceText);
                case FLOWCHART -> renderFlowchart(ctx, ph, sourceText);
                case UNKNOWN -> renderUnknown(ctx, sourceText, ph);
            }
        } catch (IllegalArgumentException e) {
            String message = diagramType == MermaidDiagramType.FLOWCHART ? "Failed to parse flowchart"
                : "Failed to parse";
            GuideDebugLog.error("[GuideNH] [MermaidScript] {}: {}", message, sourceText, e);
            replaceWithError(ctx, message + ": " + e.getMessage());
        }
    }

    private void renderMindmap(ScriptContext ctx, MermaidPlaceholder ph, String sourceText) {
        var document = MindmapParser.parse(sourceText);
        Map<String, LytBlock> nodeContentBlocks = new LinkedHashMap<>(
            ph.nodeContentBlocks != null ? ph.nodeContentBlocks : Collections.emptyMap());
        PageCompiler compiler = createRuntimeCompiler(ctx, ph);
        if (compiler != null) {
            collectRichMindmapLabels(compiler, document.getRoot(), nodeContentBlocks);
        }
        LytMermaidMindmap block = new LytMermaidMindmap(document, sourceText, nodeContentBlocks);
        if (ph.width > 0 || ph.height > 0) {
            block.setPreferredSize(ph.width, ph.height);
        }
        dispatchNodeContent(ctx, nodeContentBlocks);
        ctx.replace(block);
    }

    private void renderFlowchart(ScriptContext ctx, MermaidPlaceholder ph, String sourceText) {
        var document = FlowchartParser.parse(sourceText);
        Map<String, LytBlock> nodeContentBlocks = new LinkedHashMap<>(
            ph.nodeContentBlocks != null ? ph.nodeContentBlocks : Collections.emptyMap());
        PageCompiler compiler = createRuntimeCompiler(ctx, ph);
        if (compiler != null) {
            for (FlowchartNode node : document.getNodes()
                .values()) {
                if (nodeContentBlocks.containsKey(node.getId())) continue;
                String label = node.getLabelSource() != null
                    ? MermaidParser.normalizeMarkdownSource(node.getLabelSource())
                    : node.getLabel();
                LytBlock content = compileRichLabel(compiler, label);
                if (content != null) {
                    nodeContentBlocks.put(node.getId(), content);
                }
            }
        }
        LytMermaidFlowchart block = new LytMermaidFlowchart(document, sourceText, nodeContentBlocks);
        if (ph.width > 0 || ph.height > 0) {
            block.setPreferredSize(ph.width, ph.height);
        }
        dispatchNodeContent(ctx, nodeContentBlocks);
        ctx.replace(block);
    }

    private void renderUnknown(ScriptContext ctx, String sourceText, MermaidPlaceholder ph) {
        LytCodeBlock codeBlock = new LytCodeBlock();
        codeBlock.setCodeContent("mermaid", sourceText);
        codeBlock.setLanguageDisplayName("Mermaid (stub)");
        if (ph.width > 0 || ph.height > 0) {
            codeBlock.setPreferredBodyWidth(ph.width);
            if (ph.height > 0) {
                codeBlock.setForcedBodyHeight(ph.height);
            }
        }
        ctx.replace(codeBlock);
    }

    private void replaceWithError(ScriptContext ctx, String message) {
        ctx.replace(LytParagraph.error("[Mermaid] " + message));
    }

    private void collectRichMindmapLabels(PageCompiler compiler, MindmapNode node, Map<String, LytBlock> result) {
        if (!result.containsKey(node.getId())) {
            LytBlock content = compileRichLabel(compiler, MermaidParser.normalizeMarkdownSource(node.getLabelSource()));
            if (content != null) {
                result.put(node.getId(), content);
            }
        }
        for (MindmapNode child : node.getChildren()) {
            collectRichMindmapLabels(compiler, child, result);
        }
    }

    private PageCompiler createRuntimeCompiler(ScriptContext ctx, MermaidPlaceholder ph) {
        PageCollection pages = ctx.getPageCollection();
        if (pages == null) return null;
        ExtensionCollection extensions = pages instanceof Guide guide ? guide.getExtensions()
            : ExtensionCollection.EMPTY;
        return new PageCompiler(pages, extensions, ph.sourcePack, ph.language, new ResourceLocation(ph.pageId), "");
    }

    private LytBlock compileRichLabel(PageCompiler compiler, String normalized) {
        if (normalized.isEmpty()) return null;

        LytParagraph paragraph = new LytParagraph();
        paragraph.setStyle(
            TextStyle.builder()
                .color(new ConstantColor(ColorUtils.ARGB_FFD7DEE7.getColor()))
                .alignment(TextAlignment.CENTER)
                .whiteSpace(WhiteSpaceMode.PRE_LINE)
                .build());
        compiler.compileInlineMarkdown(normalized, paragraph);
        if (paragraph.isEmpty()) return null;

        StringBuilder rendered = new StringBuilder();
        for (LytFlowContent content : paragraph.getContent()) {
            if (!(content instanceof LytFlowText text)) return paragraph;
            rendered.append(text.getText());
        }
        return rendered.toString()
            .equals(MermaidParser.decodeEntities(normalized)) ? null : paragraph;
    }

    private void dispatchNodeContent(ScriptContext ctx, Map<String, LytBlock> nodeContentBlocks) {
        GuideDebugLog.debug("[MermaidDebug] Dispatching into {} NodeContent blocks", nodeContentBlocks.size());
        for (LytBlock contentBlock : nodeContentBlocks.values()) {
            if (contentBlock instanceof LytNode root) {
                ctx.dispatchSubtree(root);
            }
        }
    }
}
