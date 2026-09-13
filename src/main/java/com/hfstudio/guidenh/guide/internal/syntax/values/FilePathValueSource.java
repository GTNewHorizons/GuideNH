package com.hfstudio.guidenh.guide.internal.syntax.values;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.client.resources.ResourcePackRepository;
import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.Guide;
import com.hfstudio.guidenh.guide.compiler.ParsedGuidePage;
import com.hfstudio.guidenh.guide.internal.datadriven.DataDrivenGuideLoader;
import com.hfstudio.guidenh.guide.syntax.SyntaxEnvironment;
import com.hfstudio.guidenh.guide.syntax.SyntaxEnvironmentAware;
import com.hfstudio.guidenh.guide.syntax.SyntaxSuggestion;
import com.hfstudio.guidenh.guide.syntax.SyntaxValueKind;
import com.hfstudio.guidenh.guide.syntax.SyntaxValueRequest;
import com.hfstudio.guidenh.guide.syntax.SyntaxValueSource;
import com.hfstudio.guidenh.mixins.early.fml.AccessorFMLClientHandler;

import cpw.mods.fml.client.FMLClientHandler;

public class FilePathValueSource implements SyntaxValueSource, SyntaxEnvironmentAware {

    private static final String[] EXTENSIONS = { ".png", ".jpg", ".jpeg", ".gif", ".snbt", ".nbt", ".csv", ".json",
        ".mmd", ".md" };

    @Nullable
    private List<String> candidatePaths;
    private NameSnapshot snapshot = new NameSnapshot(() -> List.of());
    /**
     * The guide the scan was made for. A guide is replaced when the pack is reloaded and the editor can be
     * pointed at another one, so this cannot be a plain "already scanned" flag: that would keep suggesting
     * the first guide's files for the rest of the session.
     */
    @Nullable
    private Object scannedGuide;

    @Override
    public Set<SyntaxValueKind> kinds() {
        return Set.of(SyntaxValueKind.FILE_PATH);
    }

    @Override
    public void prepare(SyntaxEnvironment environment) {
        Guide guide = environment.guide();
        if (guide == null || guide == scannedGuide) {
            return;
        }

        List<File> dirs = new ArrayList<>();

        List<IResourcePack> packs = new ArrayList<>();
        AccessorFMLClientHandler fmlAccessor = (AccessorFMLClientHandler) FMLClientHandler.instance();
        List<IResourcePack> basePacks = fmlAccessor.guidenh$getResourcePackList();
        if (basePacks != null) {
            packs.addAll(basePacks);
        }
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.getResourcePackRepository() != null) {
            for (ResourcePackRepository.Entry entry : minecraft.getResourcePackRepository()
                .getRepositoryEntries()) {
                IResourcePack resourcePack = entry.getResourcePack();
                if (resourcePack != null) {
                    packs.add(resourcePack);
                }
            }
        }

        for (ParsedGuidePage page : guide.getPages()) {
            ResourceLocation id = page.getId();
            String namespace = id.getResourceDomain();
            String path = id.getResourcePath();

            int slashIndex = path.lastIndexOf('/');
            String dirPath = slashIndex > 0 ? path.substring(0, slashIndex) : "";

            for (IResourcePack resourcePack : packs) {
                File packFile = DataDrivenGuideLoader.getResourcePackFile(resourcePack);
                if (packFile == null) {
                    continue;
                }
                File assetsDir = new File(packFile, "assets/" + namespace + "/" + dirPath);
                if (assetsDir.isDirectory() && !dirs.contains(assetsDir)) {
                    dirs.add(assetsDir);
                }
            }
        }

        candidatePaths = buildCandidatePaths(dirs);
        scannedGuide = guide;
        snapshot = new NameSnapshot(() -> candidatePaths != null ? candidatePaths : List.of());
    }

    @Override
    public List<SyntaxSuggestion> suggest(SyntaxValueRequest request, int limit) {
        return snapshot.suggestions(request.partialText(), limit);
    }

    private static List<String> buildCandidatePaths(List<File> dirs) {
        if (dirs.isEmpty()) {
            return List.of();
        }
        List<String> paths = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (File dir : dirs) {
            scanDir(dir, "", paths, seen);
        }
        return Collections.unmodifiableList(paths);
    }

    private static void scanDir(File dir, String prefix, List<String> paths, Set<String> seen) {
        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            String name = file.getName();
            if (name.startsWith(".")) {
                continue;
            }
            String relativePath = prefix.isEmpty() ? name : prefix + "/" + name;
            if (file.isDirectory()) {
                scanDir(file, relativePath, paths, seen);
            } else if (matchesExtension(name) && seen.add(relativePath)) {
                paths.add(relativePath);
            }
        }
    }

    private static boolean matchesExtension(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        for (String extension : EXTENSIONS) {
            if (lower.endsWith(extension)) {
                return true;
            }
        }
        return false;
    }
}
