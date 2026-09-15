package com.hfstudio.guidenh.mixins.late.compat.neicustomdiagram;

import java.util.function.Consumer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import com.github.dcysteine.neicustomdiagram.api.diagram.interactable.CustomInteractable;
import com.github.dcysteine.neicustomdiagram.api.draw.Point;

@Mixin(value = CustomInteractable.class, remap = false)
public interface AccessorCustomInteractable {

    @Accessor("drawBackground")
    Consumer<Point> getDrawBackground();

    @Accessor("drawForeground")
    Consumer<Point> getDrawForeground();
}
