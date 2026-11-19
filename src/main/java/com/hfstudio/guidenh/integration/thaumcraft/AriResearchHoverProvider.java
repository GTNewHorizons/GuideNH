package com.hfstudio.guidenh.integration.thaumcraft;

import java.awt.Point;

import net.minecraft.client.gui.GuiScreen;

import org.jetbrains.annotations.Nullable;

import com.gtnewhorizons.aspectrecipeindex.nei.TemplateThaumHandler;
import com.gtnewhorizons.aspectrecipeindex.nei.TemplateThaumHandler.CachedThaumRecipe;
import com.gtnewhorizons.aspectrecipeindex.util.ARIConfig;
import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.integration.api.binding.BindingHoverTarget;
import com.hfstudio.guidenh.integration.api.binding.BindingKey;
import com.hfstudio.guidenh.integration.api.binding.GuideBindingHoverProvider;

import codechicken.nei.guihook.GuiContainerManager;
import codechicken.nei.recipe.GuiRecipe;

public class AriResearchHoverProvider implements GuideBindingHoverProvider {

    @Override
    @Nullable
    public BindingHoverTarget findHoveredBinding(GuiScreen screen, int x, int y) {
        if (!ARIConfig.showResearchKey || !(screen instanceof GuiRecipe<?>gui)
            || !GuiContainerManager.shouldShowTooltip(gui)
            || !(gui.getHandler() instanceof TemplateThaumHandler handler)) return null;
        var point = new Point(x, y);
        for (int index : gui.getRecipeIndices()) {
            if (index < 0 || index >= handler.arecipes.size()
                || !(handler.arecipes.get(index) instanceof CachedThaumRecipe recipe)) continue;
            for (var info : recipe.getResearchInfo()) {
                var rect = info.getRect(gui, index);
                if (!rect.contains(point)) continue;
                var item = info.getResearchItem();
                if (item != null) return new BindingHoverTarget(
                    new BindingKey("thaumcraft", item.key),
                    new LytRect(rect.x, rect.y, rect.width, rect.height));
            }
        }
        return null;
    }
}
