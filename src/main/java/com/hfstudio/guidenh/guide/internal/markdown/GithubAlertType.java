package com.hfstudio.guidenh.guide.internal.markdown;

import com.hfstudio.guidenh.guide.color.ColorUtils;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.color.ConstantColor;
import com.hfstudio.guidenh.guide.internal.GuidebookText;

public enum GithubAlertType {

    NOTE(GuidebookText.MarkdownAlertNote, "ⓘ", new ConstantColor(ColorUtils.ARGB_FF638EF1.getColor())),
    TIP(GuidebookText.MarkdownAlertTip, "✦", new ConstantColor(ColorUtils.ARGB_FF61B75D.getColor())),
    IMPORTANT(GuidebookText.MarkdownAlertImportant, "➤", new ConstantColor(ColorUtils.ARGB_FF8755DD.getColor())),
    WARNING(GuidebookText.MarkdownAlertWarning, "⚠", new ConstantColor(ColorUtils.ARGB_FFC79D3E.getColor())),
    CAUTION(GuidebookText.MarkdownAlertCaution, "☢", new ConstantColor(ColorUtils.ARGB_FFE46150.getColor()));

    private final GuidebookText label;
    private final String symbol;
    private final ConstantColor accentColor;

    GithubAlertType(GuidebookText label, String symbol, ConstantColor accentColor) {
        this.label = label;
        this.symbol = symbol;
        this.accentColor = accentColor;
    }

    public String displayText() {
        return label.text();
    }

    public String symbol() {
        return symbol;
    }

    public ConstantColor accentColor() {
        return accentColor;
    }

    public static @Nullable GithubAlertType fromDirective(String text) {
        if (text == null) {
            return null;
        }
        String normalized = text.trim()
            .toUpperCase();
        if (normalized.startsWith("[!NOTE]") || normalized.startsWith("!NOTE") || normalized.startsWith("NOTE")) {
            return NOTE;
        }
        if (normalized.startsWith("[!TIP]") || normalized.startsWith("!TIP") || normalized.startsWith("TIP")) {
            return TIP;
        }
        if (normalized.startsWith("[!IMPORTANT]") || normalized.startsWith("!IMPORTANT")
            || normalized.startsWith("IMPORTANT")) {
            return IMPORTANT;
        }
        if (normalized.startsWith("[!WARNING]") || normalized.startsWith("!WARNING")
            || normalized.startsWith("WARNING")) {
            return WARNING;
        }
        if (normalized.startsWith("[!CAUTION]") || normalized.startsWith("!CAUTION")
            || normalized.startsWith("CAUTION")) {
            return CAUTION;
        }
        return null;
    }
}
