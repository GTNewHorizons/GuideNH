package com.hfstudio.guidenh.guide.internal;

import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.util.ResourceLocation;

public class GuideDevelopmentResourcePacks {

    private static final List<DirectoryResourcePack> CONFIGURED_PACKS = loadConfiguredPacks();

    private GuideDevelopmentResourcePacks() {}

    public static List<DirectoryResourcePack> getConfiguredPacks() {
        return CONFIGURED_PACKS;
    }

    public static boolean hasConfiguredPacks() {
        return !getConfiguredPacks().isEmpty();
    }

    public static byte[] readBytes(ResourceLocation resourceLocation) {
        return readBytesFromPacks(getConfiguredPacks(), resourceLocation);
    }

    static byte[] readBytesFromPacks(List<DirectoryResourcePack> packs, ResourceLocation resourceLocation) {
        for (DirectoryResourcePack pack : packs) {
            byte[] bytes = pack.readBytes(resourceLocation);
            if (bytes != null) {
                return bytes;
            }
        }
        return null;
    }

    private static List<DirectoryResourcePack> loadConfiguredPacks() {
        var packs = new ArrayList<DirectoryResourcePack>();
        for (Path root : parseConfiguredRoots()) {
            if (Files.isDirectory(root)) {
                packs.add(new DirectoryResourcePack("GuideNH Development Resources (" + root + ")", root));
            }
        }
        return List.copyOf(packs);
    }

    private static List<Path> parseConfiguredRoots() {
        return GuideDevelopmentSourceArguments.parseConfiguredResourcePackRoots(
            ManagementFactory.getRuntimeMXBean()
                .getInputArguments());
    }
}
