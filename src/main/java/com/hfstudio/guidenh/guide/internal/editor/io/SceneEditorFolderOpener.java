package com.hfstudio.guidenh.guide.internal.editor.io;

import java.awt.Desktop;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.hfstudio.guidenh.guide.scene.support.GuideDebugLog;

/** Opens a saved Scene Editor file's directory using the host operating system. */
public class SceneEditorFolderOpener {

    public static void open(Path directory) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop()
                .isSupported(Desktop.Action.OPEN)) {
                Desktop.getDesktop()
                    .open(directory.toFile());
                return;
            }
        } catch (Exception exception) {
            GuideDebugLog.warnAlways("Failed to open scene export directory {}", directory, exception);
        }
        String osName = System.getProperty("os.name", "")
            .toLowerCase(Locale.ROOT);
        List<String> command = new ArrayList<>();
        if (osName.contains("win")) command.add("explorer");
        else if (osName.contains("mac")) command.add("open");
        else if (osName.contains("nux") || osName.contains("nix") || osName.contains("aix")) command.add("xdg-open");
        else return;
        command.add(
            directory.toAbsolutePath()
                .toString());
        try {
            new ProcessBuilder(command).start();
        } catch (Exception exception) {
            GuideDebugLog.warnAlways("Failed to open scene export directory {}", directory, exception);
        }
    }
}
