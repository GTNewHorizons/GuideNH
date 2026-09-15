package com.hfstudio.guidenh.mixins.late.compat.neicustomdiagram;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import com.github.dcysteine.neicustomdiagram.api.diagram.Diagram;
import com.github.dcysteine.neicustomdiagram.api.diagram.DiagramGroup;
import com.github.dcysteine.neicustomdiagram.api.diagram.DiagramState;
import com.google.common.collect.ImmutableList;

@Mixin(value = DiagramGroup.class, remap = false)
public interface AccessorDiagramGroup {

    @Accessor("diagrams")
    ImmutableList<Diagram> getDiagrams();

    @Accessor("diagramState")
    DiagramState getDiagramState();
}
