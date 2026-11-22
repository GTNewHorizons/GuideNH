package com.hfstudio.guidenh.config;

import com.gtnewhorizon.gtnhlib.config.Config.DefaultBoolean;
import com.gtnewhorizon.gtnhlib.config.Config.DefaultFloat;
import com.gtnewhorizon.gtnhlib.config.Config.DefaultInt;
import com.gtnewhorizon.gtnhlib.config.Config.DefaultString;
import com.gtnewhorizon.gtnhlib.config.Config.RangeFloat;
import com.gtnewhorizon.gtnhlib.config.Config.RangeInt;

public class CaptureConfig {

    @DefaultFloat(1)
    @RangeFloat(min = 1, max = 32)
    public float scale = 1;

    @DefaultBoolean(false)
    public boolean includeWorld;

    @DefaultBoolean(false)
    public boolean includeGuiBackdrop;

    @DefaultBoolean(false)
    public boolean includeTooltips;

    @DefaultBoolean(false)
    public boolean includeDebug;

    @DefaultBoolean(true)
    public boolean includeNei = true;

    @DefaultBoolean(true)
    public boolean snapping = true;

    @DefaultBoolean(true)
    public boolean snapSlots = true;

    @DefaultBoolean(true)
    public boolean snapComponents = true;

    @DefaultBoolean(true)
    public boolean snapWindows = true;

    @DefaultInt(6)
    @RangeInt(min = 1, max = 32)
    public int snapDistance = 6;

    @DefaultInt(10)
    @RangeInt(min = 1, max = 64)
    public int snapReleaseDistance = 10;

    @DefaultBoolean(true)
    public boolean showClassName = true;

    @DefaultBoolean(true)
    public boolean freezePreview = true;

    @DefaultBoolean(false)
    public boolean refreshBeforeSave;

    @DefaultBoolean(false)
    public boolean closeAfterSave;

    @DefaultBoolean(false)
    public boolean openFolderAfterSave;

    @DefaultInt(55)
    @RangeInt(min = 0, max = 90)
    public int maskOpacity = 55;

    @DefaultString("png")
    public String format = "png";

    @DefaultString("automatic")
    public String qualityPolicy = "automatic";

    @DefaultString("original")
    public String arrangement = "original";

    @DefaultInt(4)
    @RangeInt(min = 1, max = 32)
    public int columns = 4;

    @DefaultInt(4)
    @RangeInt(min = 0, max = 128)
    public int spacing = 4;

    @DefaultInt(0)
    public int backgroundArgb;

    @DefaultString("screenshots/guidenh")
    public String outputDirectory = "screenshots/guidenh";

    @DefaultInt(10)
    @RangeInt(min = 1, max = 60)
    public int fps = 10;

    @DefaultInt(5)
    @RangeInt(min = 0, max = 3600)
    public int recordingSeconds = 5;

    @DefaultInt(0)
    @RangeInt(min = 0)
    public int frameLimit;

    @DefaultInt(0)
    @RangeInt(min = 0, max = 65535)
    public int loopCount;

    @DefaultInt(128)
    @RangeInt(min = 0, max = 255)
    public int gifAlphaThreshold = 128;

    @DefaultBoolean(false)
    public boolean gifMatte;

    @DefaultBoolean(false)
    public boolean gifDither;

    @DefaultInt(4)
    @RangeInt(min = 1, max = 64)
    public int spriteColumns = 4;

    @DefaultInt(2)
    @RangeInt(min = 0, max = 128)
    public int spritePadding = 2;

    @DefaultInt(4096)
    @RangeInt(min = 256, max = 32768)
    public int spritePageSize = 4096;

    @DefaultBoolean(false)
    public boolean strictRecordingQueue;

    @DefaultString("")
    public String scheduledTime = "";

    @DefaultString("wait")
    public String unavailablePolicy = "wait";

    @DefaultInt(0)
    @RangeInt(min = 0, max = 86400)
    public int delaySeconds;

    @DefaultInt(5)
    @RangeInt(min = 1, max = 86400)
    public int intervalSeconds = 5;

    @DefaultInt(1)
    @RangeInt(min = 1, max = 10000)
    public int repeatCount = 1;

    @DefaultInt(256)
    @RangeInt(min = 32, max = 4096)
    public int memoryBudgetMiB = 256;

    @DefaultInt(2048)
    @RangeInt(min = 256, max = 8192)
    public int tileSize = 2048;

    @DefaultInt(4)
    @RangeInt(min = 1, max = 16)
    public int queueFrames = 4;

    @DefaultInt(4096)
    @RangeInt(min = 128, max = 65536)
    public int temporaryBudgetMiB = 4096;

    @DefaultBoolean(true)
    public boolean captureVisibleLayers = true;

    @DefaultBoolean(false)
    public boolean capturePages;

    @DefaultBoolean(false)
    public boolean captureReachableGuis;

    @DefaultBoolean(false)
    public boolean includePopups;

    @DefaultBoolean(false)
    public boolean includeVariants;

    @DefaultBoolean(true)
    public boolean restoreOriginal = true;

    @DefaultInt(2)
    @RangeInt(min = 1, max = 60)
    public int stableFrames = 2;

    @DefaultInt(5)
    @RangeInt(min = 1, max = 60)
    public int timeoutSeconds = 5;

    @DefaultInt(500)
    @RangeInt(min = 1, max = 100000)
    public int maxTargets = 500;
}
