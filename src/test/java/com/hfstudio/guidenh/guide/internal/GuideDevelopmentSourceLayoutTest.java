package com.hfstudio.guidenh.guide.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class GuideDevelopmentSourceLayoutTest {

    @TempDir
    private Path temporaryDirectory;

    @Test
    public void contentRootsRetainLocalizedAndNeutralPagesBesideSharedAssets() throws IOException {
        Path localized = temporaryDirectory.resolve("localized");
        Files.createDirectories(localized.resolve("assets"));
        Files.createDirectories(localized.resolve("_zh_cn"));
        assertEquals(
            GuideDevelopmentSourceLayout.CONTENT_ROOT,
            GuideDevelopmentSourceLayout.detect(localized, "guidenh"));

        Path neutral = temporaryDirectory.resolve("neutral");
        Files.createDirectories(neutral.resolve("assets"));
        Files.createFile(neutral.resolve("index.md"));
        assertEquals(
            GuideDevelopmentSourceLayout.CONTENT_ROOT,
            GuideDevelopmentSourceLayout.detect(neutral, "guidenh"));
    }

    @Test
    public void resourcePackRootsRetainNamespaceAndEmptyPackLayouts() throws IOException {
        Path populated = temporaryDirectory.resolve("populated");
        Files.createDirectories(populated.resolve("assets/gregtech/guidenh/_en_us"));
        assertEquals(
            GuideDevelopmentSourceLayout.RESOURCE_PACK_ROOT,
            GuideDevelopmentSourceLayout.detect(populated, "guidenh"));

        Path empty = temporaryDirectory.resolve("empty");
        Files.createDirectories(empty.resolve("assets"));
        assertEquals(
            GuideDevelopmentSourceLayout.RESOURCE_PACK_ROOT,
            GuideDevelopmentSourceLayout.detect(empty, "guidenh"));

        Files.createFile(empty.resolve("pack.mcmeta"));
        Files.createFile(empty.resolve("README.md"));
        assertEquals(
            GuideDevelopmentSourceLayout.RESOURCE_PACK_ROOT,
            GuideDevelopmentSourceLayout.detect(empty, "guidenh"));
    }

    @Test
    public void assetsRootsRetainTheirNamespaceLayout() throws IOException {
        Files.createDirectories(temporaryDirectory.resolve("guidenh/guidenh/_en_us"));
        assertEquals(
            GuideDevelopmentSourceLayout.ASSETS_ROOT,
            GuideDevelopmentSourceLayout.detect(temporaryDirectory, "guidenh"));
    }
}
