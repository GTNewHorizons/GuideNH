package com.hfstudio.guidenh.guide.internal.welcome;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;

import com.hfstudio.guidenh.guide.scene.support.GuideDebugLog;

public class GuideWelcomeState {

    private static final String MARKER_DIR = "guidenh/welcome_seen";

    private final File markerFile;

    private GuideWelcomeState(File markerFile) {
        this.markerFile = markerFile;
    }

    public static GuideWelcomeState current() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.theWorld == null || mc.thePlayer == null) {
            return null;
        }

        UUID playerId = mc.thePlayer.getUniqueID();
        if (playerId == null) {
            return null;
        }

        String markerName = sha256(worldKey(mc) + "|" + playerId) + ".seen";
        File markerDir = new File(mc.mcDataDir, "config/" + MARKER_DIR);
        return new GuideWelcomeState(new File(markerDir, markerName));
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

    private static String worldKey(Minecraft mc) {
        ServerData serverData = mc.func_147104_D();
        if (serverData != null && serverData.serverIP != null
            && !serverData.serverIP.trim()
                .isEmpty()) {
            return "server:" + serverData.serverIP.trim()
                .toLowerCase();
        }
        try {
            if (mc.isIntegratedServerRunning() && mc.getIntegratedServer() != null) {
                return "singleplayer:" + mc.getIntegratedServer()
                    .getFolderName();
            }
        } catch (RuntimeException ignored) {
            // Fall through to the dimension fallback.
        }
        return "world:" + mc.theWorld.provider.dimensionId;
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                result.append(String.format("%02x", b & 0xFF));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException e) {
            return Integer.toHexString(value.hashCode());
        }
    }
}
