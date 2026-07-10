package com.hfstudio.guidenh.guide.internal;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;

import net.minecraft.client.resources.IResourcePack;
import net.minecraft.client.resources.data.IMetadataSection;
import net.minecraft.client.resources.data.IMetadataSerializer;
import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.Nullable;

import lombok.Getter;

public class DirectoryResourcePack implements IResourcePack {

    private final String packName;
    @Getter
    private final Path root;
    private volatile DomainSnapshot resourceDomains;

    public DirectoryResourcePack(String packName, Path root) {
        this.packName = packName;
        this.root = root.toAbsolutePath()
            .normalize();
    }

    @Override
    public InputStream getInputStream(ResourceLocation resourceLocation) throws IOException {
        Path resourcePath = resolveResourcePath(resourceLocation);
        if (resourcePath == null) {
            throw new IOException("Resource does not exist: " + resourceLocation);
        }
        return Files.newInputStream(resourcePath);
    }

    @Override
    public boolean resourceExists(ResourceLocation resourceLocation) {
        return resolveResourcePath(resourceLocation) != null;
    }

    @Override
    public Set<String> getResourceDomains() {
        Path assetsRoot = root.resolve("assets");
        long rootStamp = readDirectoryStamp(root);
        long assetsStamp = readDirectoryStamp(assetsRoot);

        DomainSnapshot cached = resourceDomains;
        if (cached != null && cached.matches(rootStamp, assetsStamp)) {
            return cached.domains();
        }

        Set<String> discovered = Set.copyOf(discoverResourceDomains(root));
        resourceDomains = new DomainSnapshot(rootStamp, assetsStamp, discovered);
        return discovered;
    }

    @Override
    public BufferedImage getPackImage() {
        return null;
    }

    @Override
    public IMetadataSection getPackMetadata(IMetadataSerializer metadataSerializer, String metadataSectionName) {
        return null;
    }

    @Override
    public String getPackName() {
        return packName;
    }

    public byte[] readBytes(ResourceLocation resourceLocation) {
        Path resourcePath = resolveResourcePath(resourceLocation);
        if (resourcePath == null) {
            return null;
        }

        try {
            return Files.readAllBytes(resourcePath);
        } catch (IOException e) {
            return null;
        }
    }

    private @Nullable Path resolveResourcePath(ResourceLocation resourceLocation) {
        String namespace = resourceLocation.getResourceDomain();
        String path = resourceLocation.getResourcePath();

        Path assetPath = root.resolve("assets")
            .resolve(namespace)
            .resolve(path);
        if (Files.isRegularFile(assetPath)) {
            return assetPath;
        }

        Path nativePath = root.resolve(namespace)
            .resolve(path);
        return Files.isRegularFile(nativePath) ? nativePath : null;
    }

    private static LinkedHashSet<String> discoverResourceDomains(Path root) {
        var domains = new LinkedHashSet<String>();
        discoverResourceDomains(root.resolve("assets"), domains);
        discoverResourceDomains(root, domains);
        return domains;
    }

    private static void discoverResourceDomains(Path base, Set<String> domains) {
        if (!Files.isDirectory(base)) {
            return;
        }

        try (var children = Files.list(base)) {
            children.filter(Files::isDirectory)
                .filter(
                    child -> !"assets".equals(
                        child.getFileName()
                            .toString()))
                .forEach(
                    child -> domains.add(
                        child.getFileName()
                            .toString()));
        } catch (IOException ignored) {}
    }

    private static long readDirectoryStamp(Path directory) {
        if (!Files.isDirectory(directory)) {
            return Long.MIN_VALUE;
        }

        try {
            return Files.getLastModifiedTime(directory)
                .toMillis();
        } catch (IOException ignored) {
            return Long.MIN_VALUE;
        }
    }

    private record DomainSnapshot(long rootStamp, long assetsStamp, Set<String> domains) {

        private boolean matches(long currentRootStamp, long currentAssetsStamp) {
            return rootStamp == currentRootStamp && assetsStamp == currentAssetsStamp;
        }
    }
}
