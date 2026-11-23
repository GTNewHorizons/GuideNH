package com.hfstudio.guidenh.capture.server;

import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.tileentity.TileEntity;

import com.hfstudio.guidenh.capture.server.api.CaptureServerGuiProvider;

public class CaptureServerRegistry {

    private final Map<String, CaptureServerGuiProvider> providers = new LinkedHashMap<>();

    public void register(CaptureServerGuiProvider provider) {
        if (providers.putIfAbsent(provider.id(), provider) != null)
            throw new IllegalArgumentException("Duplicate server capture provider: " + provider.id());
    }

    public CaptureServerGuiProvider provider(String id) {
        return providers.get(id);
    }

    public CaptureServerGuiProvider provider(TileEntity tile) {
        for (CaptureServerGuiProvider provider : providers.values()) {
            if (provider.supports(tile)) return provider;
        }
        return null;
    }
}
