package com.hfstudio.guidenh.guide.siteexport.site;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.scene.annotation.InWorldOutlineAnnotation;

/**
 * Captures the target geometry of a group of outline annotations that share one style. The site viewer
 * rasterizes these meshes into its own outline mask, so the web contour follows the same silhouettes.
 */
@FunctionalInterface
public interface GuideSiteOutlineMeshExporter {

    GuideSiteOutlineMeshExporter NONE = group -> Meshes.EMPTY;

    Meshes export(List<InWorldOutlineAnnotation> group) throws Exception;

    record Meshes(@Nullable String geometryPath, @Nullable String cutoutPath) {

        public static final Meshes EMPTY = new Meshes(null, null);
    }
}
