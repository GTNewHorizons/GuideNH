package com.hfstudio.guidenh.guide.document.block;

import java.util.Optional;

import com.hfstudio.guidenh.guide.color.SymbolicColor;
import com.hfstudio.guidenh.guide.document.interaction.GuideTooltip;
import com.hfstudio.guidenh.guide.document.interaction.InteractiveElement;
import com.hfstudio.guidenh.guide.internal.mermaid.flowchart.FlowchartDocument;
import com.hfstudio.guidenh.guide.style.BorderStyle;
import com.hfstudio.guidenh.guide.style.WhiteSpaceMode;
import com.hfstudio.guidenh.guide.ui.GuideUiHost;
import lombok.Getter;

public class LytMermaidFlowchart extends LytVBox implements InteractiveElement {

    @Getter
    private final FlowchartDocument flowchart;
    @Getter
    private final String sourceText;
    @Getter
    private final LytCodeBlockToolbar toolbar = new LytCodeBlockToolbar();
    private final LytParagraph body = new LytParagraph();

    public LytMermaidFlowchart(FlowchartDocument flowchart, String sourceText) {
        this.flowchart = flowchart;
        this.sourceText = sourceText != null ? sourceText : "";

        setPadding(6);
        setGap(4);
        setBackgroundColor(SymbolicColor.BLOCKQUOTE_BACKGROUND);
        setBorder(new BorderStyle(SymbolicColor.TABLE_BORDER, 1));

        toolbar.setLanguageDisplayName("Flowchart (stub)");
        toolbar.setCopyText(this.sourceText);
        toolbar.setCopyButtonVisible(true);

        body.modifyStyle(
            style -> style.whiteSpace(WhiteSpaceMode.PRE_WRAP));
        body.appendText(this.sourceText);

        append(toolbar);
        append(body);
    }

    public void setPreferredSize(int width, int height) {
        toolbar.setPreferredWidth(width);
    }

    @Override
    public boolean mouseClicked(GuideUiHost screen, int x, int y, int button, boolean doubleClick) {
        return toolbar.mouseClicked(screen, x, y, button, doubleClick);
    }

    @Override
    public Optional<GuideTooltip> getTooltip(float x, float y) {
        return toolbar.getTooltip(x, y);
    }
}
