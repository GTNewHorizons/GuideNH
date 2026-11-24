package com.hfstudio.guidenh.capture.server;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.tileentity.TileEntity;

import com.hfstudio.guidenh.capture.server.api.CaptureServerContext;
import com.hfstudio.guidenh.capture.server.api.CaptureServerGuiProvider;
import com.hfstudio.guidenh.capture.server.api.CaptureServerObjectProvider;
import com.hfstudio.guidenh.capture.server.api.CaptureServerObjectTarget;

public class CaptureServerRegistry {

    private final Map<String, CaptureServerGuiProvider> providers = new LinkedHashMap<>();
    private final Map<String, CaptureServerObjectProvider> objectProviders = new LinkedHashMap<>();

    public void registerObjects(CaptureServerObjectProvider provider) {
        if (objectProviders.putIfAbsent(provider.id(), provider) != null)
            throw new IllegalArgumentException("Duplicate server object provider: " + provider.id());
    }

    public List<CaptureServerObjectTarget> objects(CaptureServerContext context) {
        List<CaptureServerObjectTarget> targets = new ArrayList<>();
        for (CaptureServerObjectProvider provider : objectProviders.values()) {
            for (CaptureServerObjectTarget target : provider.enumerate(context)) {
                if (targets.size() >= context.query()
                    .maxTargets()) break;
                targets.add(target);
            }
            if (targets.size() >= context.query()
                .maxTargets()) break;
        }
        return List.copyOf(targets);
    }

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
