package com.hfstudio.guidenh.integration.structurelib;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import com.hfstudio.guidenh.integration.Mods;

import cpw.mods.fml.common.Optional;

/**
 * Thin backward-compatible wrapper. New code should use {@link StructureLibBuildService} directly.
 */
public class StructureLibSceneImportService {

    private final StructureLibBuildService buildService;

    public StructureLibSceneImportService() {
        this(resolveBuildService());
    }

    public StructureLibSceneImportService(@Nullable StructureLibBuildService buildService) {
        this.buildService = buildService != null ? buildService : resolveBuildService();
    }

    public boolean isAvailable() {
        return Mods.StructureLib.isModLoaded();
    }

    @Nullable
    public StructureLibImportResult importScene(@Nullable StructureLibImportRequest request) {
        if (request == null) return null;
        StructureLibBuildRequest buildReq = convert(request);
        StructureLibBuildResult result = buildService.build(buildReq);
        return convert(result);
    }

    private static StructureLibBuildRequest convert(StructureLibImportRequest req) {
        return new StructureLibBuildRequest(
            req.getController(),
            req.getPiece(),
            req.getFacing(),
            req.getRotation(),
            req.getFlip(),
            req.getPreviewSelection() != null ? req.getPreviewSelection().getMasterTier() : 1,
            req.getPreviewSelection() != null ? req.getPreviewSelection().getChannelOverrides() : Map.of(),
            req.getPreviewSelection() != null ? req.getPreviewSelection().getIntegrationOptions() : Map.of());
    }

    @Nullable
    private static StructureLibImportResult convert(StructureLibBuildResult result) {
        if (result == null) return null;
        if (!result.success()) {
            return StructureLibImportResult.failure(result.error());
        }
        List<StructureLibBuildResult.PlacedBlock> src = result.blocks();
        List<StructureLibImportResult.PlacedBlock> dst = new ArrayList<>(src.size());
        for (StructureLibBuildResult.PlacedBlock pb : src) {
            dst.add(new StructureLibImportResult.PlacedBlock(
                pb.x(), pb.y(), pb.z(), pb.block(), pb.meta(), pb.tileTag(), pb.blockId()));
        }
        return StructureLibImportResult.success(dst, List.of(), null);
    }

    private static StructureLibBuildService resolveBuildService() {
        if (!Mods.StructureLib.isModLoaded()) return null;
        return createService();
    }

    @Optional.Method(modid = "structurelib")
    private static StructureLibBuildService createService() {
        return new StructureLibBuildService();
    }
}
