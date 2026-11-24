package com.hfstudio.guidenh.guide.internal.resource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.compiler.IdUtils;
import com.hfstudio.guidenh.guide.internal.GuideDevelopmentResourcePacks;
import com.hfstudio.guidenh.guide.internal.datadriven.DataDrivenGuideLoader;

public class GuideResourceAccess {

    public static final int MAX_RESOURCE_BYTES = 32 * 1024 * 1024;

    private GuideResourceAccess() {}

    public static void clearCache() {
        // Asset index is owned by DataDrivenGuideLoader; no separate negative cache to clear.
    }

    public static @Nullable byte[] readBytes(IResourceManager resourceManager, ResourceLocation id) {
        if (!isSafeResourceId(id)) {
            return null;
        }
        byte[] developmentBytes = GuideDevelopmentResourcePacks.readBytes(id);
        if (developmentBytes != null) {
            return developmentBytes;
        }

        try {
            IResource resource = resourceManager.getResource(id);
            try (var input = resource.getInputStream()) {
                return readFully(input);
            }
        } catch (IOException ignored) {}

        var candidates = DataDrivenGuideLoader.getAssetCandidatesFor(id);
        if (candidates != null) {
            for (var candidate : candidates) {
                byte[] bytes = DataDrivenGuideLoader.readBytes(candidate.pack(), candidate.resourceLocation());
                if (bytes != null) {
                    return bytes;
                }
            }
        }

        if (DataDrivenGuideLoader.isIndexPopulated()) {
            return null;
        }

        DataDrivenGuideLoader.ensureIndexReady(resourceManager);
        return readBytes(resourceManager, id);
    }

    public static @Nullable InputStream openStream(IResourceManager resourceManager, ResourceLocation id) {
        var bytes = readBytes(resourceManager, id);
        return bytes != null ? new ByteArrayInputStream(bytes) : null;
    }

    public static byte[] readFully(InputStream input) throws IOException {
        byte[] bytes = input.readNBytes(MAX_RESOURCE_BYTES + 1);
        if (bytes.length > MAX_RESOURCE_BYTES) {
            throw new IOException("Guide resource exceeds " + MAX_RESOURCE_BYTES + " bytes");
        }
        return bytes;
    }

    public static boolean isSafeResourceId(ResourceLocation id) {
        String namespace = id.getResourceDomain();
        if (namespace.isEmpty() || ".".equals(namespace) || "..".equals(namespace)) {
            return false;
        }
        for (int index = 0; index < namespace.length(); index++) {
            char value = namespace.charAt(index);
            if (!(value >= 'a' && value <= 'z' || value >= '0' && value <= '9'
                || value == '_'
                || value == '-'
                || value == '.')) {
                return false;
            }
        }
        String path = id.getResourcePath();
        return !path.startsWith("/") && path.indexOf(':') < 0 && IdUtils.normalizeAssetPath(path) != null;
    }
}
