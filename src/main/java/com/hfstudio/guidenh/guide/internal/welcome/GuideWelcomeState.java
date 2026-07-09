package com.hfstudio.guidenh.guide.internal.welcome;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import net.minecraft.client.Minecraft;

import com.hfstudio.guidenh.guide.scene.support.GuideDebugLog;

public class GuideWelcomeState {

    private static final String MARKER_DIR = "guidenh/welcome_seen";
    private static final String MARKER_FILE = "global.seen";

    private final File markerFile;

    private GuideWelcomeState(File markerFile) {
        this.markerFile = markerFile;
    }

    public static GuideWelcomeState current() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.mcDataDir == null) {
            return null;
        }

        File markerDir = new File(mc.mcDataDir, "config/" + MARKER_DIR);
        return new GuideWelcomeState(new File(markerDir, MARKER_FILE));
    }

    public boolean isSeen() {
        return markerFile.isFile();
    }

    public void markSeen() {
        try {
            Files.createDirectories(
                markerFile.toPath()
                    .getParent());
            Files.write(markerFile.toPath(), "GuideNH welcome popup seen\n".getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            GuideDebugLog.warnAlways("[GuideNH] Failed to write welcome popup marker {}", markerFile, e);
        }
    }
}
