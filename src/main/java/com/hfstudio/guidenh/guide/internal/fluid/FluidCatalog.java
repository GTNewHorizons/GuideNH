package com.hfstudio.guidenh.guide.internal.fluid;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraftforge.fluids.FluidContainerRegistry;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import com.hfstudio.guidenh.client.GuideClientTasks;

public class FluidCatalog implements IResourceManagerReloadListener {

    public static final FluidCatalog INSTANCE = new FluidCatalog();
    private static final long REFRESH_INTERVAL_NANOS = 60_000_000_000L;

    private volatile List<Entry> entries = List.of();
    private volatile long resourceRevision;
    private String language;
    private long refreshAfter;

    public record Entry(String id, String name, String searchText) {}

    public List<Entry> entries() {
        return GuideClientTasks.call(this::refreshIfNeeded);
    }

    public long resourceRevision() {
        return resourceRevision;
    }

    public static String language() {
        Minecraft minecraft = Minecraft.getMinecraft();
        return minecraft.getLanguageManager()
            .getCurrentLanguage()
            .getLanguageCode();
    }

    private List<Entry> refreshIfNeeded() {
        String currentLanguage = language();
        long now = System.nanoTime();
        if (now < refreshAfter && Objects.equals(language, currentLanguage)) return entries;
        var updated = new ArrayList<Entry>();
        for (var registered : FluidRegistry.getRegisteredFluids()
            .entrySet()) {
            String id = registered.getKey();
            FluidStack stack = new FluidStack(registered.getValue(), FluidContainerRegistry.BUCKET_VOLUME);
            String name = FluidStacks.displayName(stack);
            updated.add(new Entry(id, name, (id + " " + name).toLowerCase(Locale.ROOT)));
        }
        updated.sort(Comparator.comparing(Entry::id));
        entries = List.copyOf(updated);
        language = currentLanguage;
        refreshAfter = now + REFRESH_INTERVAL_NANOS;
        return entries;
    }

    @Override
    public void onResourceManagerReload(IResourceManager resourceManager) {
        resourceRevision++;
        refreshAfter = 0;
        language = null;
    }
}
