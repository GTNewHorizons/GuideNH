package com.hfstudio.guidenh.guide.document.block;

import com.hfstudio.guidenh.guide.color.ColorUtils;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import com.hfstudio.guidenh.guide.color.ColorValue;
import com.hfstudio.guidenh.guide.document.LytSize;
import com.hfstudio.guidenh.guide.document.interaction.GuideTooltip;
import com.hfstudio.guidenh.guide.document.interaction.InteractiveElement;
import com.hfstudio.guidenh.guide.internal.GuidebookText;
import com.hfstudio.guidenh.guide.internal.mermaid.flowchart.FlowchartDocument;
import com.hfstudio.guidenh.guide.style.BorderStyle;
import com.hfstudio.guidenh.guide.ui.GuideUiHost;

import lombok.Getter;

public class LytMermaidFlowchart extends LytVBox implements InteractiveElement {

    @Getter
    private final FlowchartDocument flowchart;
    @Getter
    private final String sourceText;
    @Getter
    private final LytCodeBlockToolbar toolbar = new LytCodeBlockToolbar();
    private final LytMermaidFlowchartCanvas canvas;

    public LytMermaidFlowchart(FlowchartDocument flowchart, String sourceText) {
        this(flowchart, sourceText, Collections.emptyMap());
    }

    public LytMermaidFlowchart(FlowchartDocument flowchart, String sourceText, Map<String, LytBlock> nodeContent) {
        this.flowchart = flowchart;
        this.sourceText = sourceText != null ? sourceText : "";
        this.canvas = new LytMermaidFlowchartCanvas(
            flowchart,
            nodeContent != null ? nodeContent : Collections.emptyMap());

        setPadding(6);
        setGap(4);
        setBackgroundColor(ColorUtils.BLOCKQUOTE_BACKGROUND);
        setBorder(new BorderStyle(ColorUtils.TABLE_BORDER, 1));

        toolbar.setLanguageDisplayName("Flowchart");
        toolbar.setCopyText(this.sourceText);
        toolbar.setCopyButtonVisible(true);
        String copyValue = flowchart.getCopyValue();
        if (copyValue != null && !copyValue.isEmpty()) {
            LytButton btn = new LytButton(LytCodeBlockToolbar.COPY_SPRITE, new LytSize(16, 16));
            btn.setOnClick(screen -> screen.copyCodeBlock(copyValue));
            btn.setTooltipFunction((pressed) -> {
                if (pressed) return GuidebookText.FlowchartCopyPlanSuccess.text();
                return GuidebookText.FlowchartCopyPlan.text();
            });
            btn.setHoverColor(ColorUtils.ICON_BUTTON_HOVER);
            toolbar.addButton(btn);
        }
        toolbar.addButton(createResetViewButton());

        append(toolbar);
        append(canvas);
    }

    private LytButton createResetViewButton() {
        LytButton button = new LytButton(LytCodeBlockToolbar.RESET_VIEW_SPRITE, new LytSize(16, 16));
        button.setOnClick(screen -> canvas.resetView());
        button.setTooltipText(GuidebookText.ResetView.text());
        button.setHoverColor(ColorUtils.ICON_BUTTON_HOVER);
        return button;
    }

    public void setPreferredSize(int width, int height) {
        canvas.setPreferredSize(width, height);
        toolbar.setPreferredWidth(width);
    }

    @Override
    public boolean mouseClicked(GuideUiHost screen, int x, int y, int button, boolean doubleClick) {
        return toolbar.mouseClicked(screen, x, y, button, doubleClick)
            || canvas.mouseClicked(screen, x, y, button, doubleClick);
    }

    @Override
    public Optional<GuideTooltip> getTooltip(float x, float y) {
        Optional<GuideTooltip> toolbarTooltip = toolbar.getTooltip(x, y);
        return toolbarTooltip.isPresent() ? toolbarTooltip : canvas.getTooltip(x, y);
    }
}
