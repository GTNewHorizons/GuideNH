package com.hfstudio.guidenh.guide.siteexport.site;

public class GuideSiteExportOptions {

    public static final GuideSiteExportOptions DEFAULT = new GuideSiteExportOptions(true);

    private final boolean exportPonderEveryTick;

    public GuideSiteExportOptions(boolean exportPonderEveryTick) {
        this.exportPonderEveryTick = exportPonderEveryTick;
    }

    public boolean exportPonderEveryTick() {
        return exportPonderEveryTick;
    }
}
