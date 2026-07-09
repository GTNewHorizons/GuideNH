package com.hfstudio.guidenh.guide.document.interaction;

import net.minecraft.item.ItemStack;

import com.hfstudio.guidenh.guide.siteexport.ResourceExporter;

import lombok.Getter;

@Getter
public class ItemTooltip implements GuideTooltip {

    private final ItemStack stack;

    public ItemTooltip(ItemStack stack) {
        this.stack = stack;
    }

    @Override
    public void exportResources(ResourceExporter exporter) {
        exporter.referenceItemStack(stack);
    }
}
