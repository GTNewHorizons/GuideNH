package com.hfstudio.guidenh.guide.internal.editor.io;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.internal.editor.SceneEditorSession;
import com.hfstudio.guidenh.guide.internal.resource.GuideLocalPathPolicy;
import com.hfstudio.guidenh.guide.internal.structure.GuideStructureFileReader;

public class SceneEditorStructureCache {

    private final Path workingRoot;

    public SceneEditorStructureCache(Path workingRoot) {
        this.workingRoot = workingRoot;
    }

    public static SceneEditorStructureCache createDefault() {
        return new SceneEditorStructureCache(Paths.get(""));
    }

    public String createStructureSource() {
        return Paths.get("config", "guidenh", "scene-editor", UUID.randomUUID() + ".snbt")
            .toString()
            .replace('\\', '/');
    }

    public Path getStructureDirectory() {
        return workingRoot.resolve(Paths.get("config", "guidenh", "structures"));
    }

    public static String readSceneStructure(Path workingRoot, String structureSource) throws IOException {
        Path path = resolveSceneStructurePath(workingRoot, structureSource)
            .orElseThrow(() -> new IOException("Scene structure path leaves the owned directories"));
        return new GuideStructureFileReader(path.getParent(), GuideStructureFileReader.DEFAULT_MAX_BYTES).read(path);
    }

    public Optional<Path> resolveStructureCachePath(SceneEditorSession session) {
        return resolveStructureCachePath(
            session.getSceneModel()
                .getStructureSource());
    }

    public Optional<Path> resolveStructureCachePath(@Nullable String structureSource) {
        return resolveSceneStructurePath(workingRoot, structureSource);
    }

    public void writeStructureCache(Path path, String snbtText) throws IOException {
        path = resolveStructureCachePath(
            path.toAbsolutePath()
                .toString()).orElseThrow(() -> new IOException("Structure cache path leaves the owned directories"));
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        path = resolveStructureCachePath(path.toString())
            .orElseThrow(() -> new IOException("Structure cache path changed during directory creation"));
        Files.writeString(
            path,
            snbtText,
            StandardCharsets.UTF_8,
            StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING,
            LinkOption.NOFOLLOW_LINKS);
    }

    public static Optional<Path> resolveSceneStructurePath(Path workingRoot, @Nullable String structureSource) {
        if (structureSource == null || structureSource.trim()
            .isEmpty()) {
            return Optional.empty();
        }

        String source = structureSource.trim();
        if (source.replace('\\', '/')
            .startsWith("//")
            || !source.toLowerCase(Locale.ROOT)
                .endsWith(".snbt")) {
            return Optional.empty();
        }
        try {
            Path path = Paths.get(source);
            Path root = workingRoot.toAbsolutePath()
                .normalize();
            Path resolved = (path.isAbsolute() ? path : root.resolve(path)).normalize();
            Path cacheRoot = root.resolve(Paths.get("config", "guidenh", "scene-editor"));
            Path structureRoot = root.resolve(Paths.get("config", "guidenh", "structures"));
            Path owner = resolved.startsWith(cacheRoot) ? cacheRoot : structureRoot;
            return Optional.of(new GuideLocalPathPolicy(owner).resolveForWrite(resolved));
        } catch (IOException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
