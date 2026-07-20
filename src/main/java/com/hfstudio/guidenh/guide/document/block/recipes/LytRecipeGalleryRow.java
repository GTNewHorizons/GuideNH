package com.hfstudio.guidenh.guide.document.block.recipes;

import com.hfstudio.guidenh.guide.document.block.LytBlock;
import com.hfstudio.guidenh.guide.document.block.LytHBox;
import com.hfstudio.guidenh.guide.internal.recipe.LytNeiRecipeBox;

/**
 * A wrapping horizontal row that groups consecutive recipe boxes into a
 * "gallery" so multiple recipes share a row when the available width allows.
 * Created by {@code LytDocument}'s recipe-gallery grouping pass; also the
 * marker type used to recognize (and extend) existing galleries.
 */
public class LytRecipeGalleryRow extends LytHBox {

    /** Matches {@code RecipeCompiler.MULTI_GAP} (kept local to avoid a compiler dependency). */
    public static final int GAP = 4;

    public LytRecipeGalleryRow() {
        setWrap(true);
        setGap(GAP);
        // Full width so the flex row wraps at the document's content edge.
        setFullWidth(true);
    }

    /** Recipe box block types eligible for gallery grouping. */
    public static boolean isRecipeBox(LytBlock block) {
        return block instanceof LytNeiRecipeBox || block instanceof LytStandardRecipeBox
            || block instanceof LytGenericRecipeBox;
    }
}
