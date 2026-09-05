package com.hfstudio.guidenh.guide.document.block;

import com.hfstudio.guidenh.guide.color.ColorUtils;
import com.hfstudio.guidenh.guide.color.ConstantColor;
import com.hfstudio.guidenh.guide.document.DefaultStyles;
import com.hfstudio.guidenh.guide.document.flow.LytFlowSpan;
import com.hfstudio.guidenh.guide.document.flow.LytFlowText;
import com.hfstudio.guidenh.guide.internal.markdown.GithubAlertType;
import com.hfstudio.guidenh.guide.style.BorderStyle;

public class LytAlertBox extends LytVBox {

    private final LytParagraph titleParagraph = new LytParagraph();

    public LytAlertBox() {
        setPadding(6);
        setGap(4);
        setFullWidth(true);
        setBackgroundColor(ColorUtils.BLOCKQUOTE_BACKGROUND);
        setBorderLeft(new BorderStyle(new ConstantColor(ColorUtils.ARGB_FF4FA3FF.getColor()), 3));

        titleParagraph.setMarginTop(0);
        titleParagraph.setMarginBottom(0);
        titleParagraph.modifyStyle(
            style -> style.bold(true)
                .fontScale(DefaultStyles.HEADING5.fontScale())
                .color(new ConstantColor(ColorUtils.ARGB_FFD8E9FF.getColor())));
        append(titleParagraph);
    }

    public void setTitle(String title) {
        setTitle(title, null);
    }

    public void setTitle(String title, GithubAlertType type) {
        titleParagraph.clearContent();
        if (type != null) {
            setBorderLeft(new BorderStyle(type.accentColor(), 3));
            titleParagraph.modifyStyle(
                style -> style.bold(true)
                    .fontScale(DefaultStyles.HEADING5.fontScale())
                    .color(type.accentColor()));
            LytFlowSpan iconSpan = new LytFlowSpan();
            iconSpan.modifyStyle(style -> style.color(type.accentColor()));
            iconSpan.append(LytFlowText.of(type.symbol() + " "));
            titleParagraph.append(iconSpan);
        }
        titleParagraph.appendText(title != null ? title : "");
    }

    public void appendBody(LytBlock child) {
        append(child);
    }
}
