package com.hfstudio.guidenh.integration.structurelib;

import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.world.World;

import com.gtnewhorizon.structurelib.StructureEvent.StructureElementVisitedEvent;
import com.gtnewhorizon.structurelib.structure.IStructureElement;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;

public class StructureLibVisitedElementCollector {

    public final Object instrumentId;
    public final World world;
    public final Map<Long, IStructureElement<?>> elementsByPosition = new LinkedHashMap<>();

    public StructureLibVisitedElementCollector(Object instrumentId, World world) {
        this.instrumentId = instrumentId;
        this.world = world;
    }

    @SubscribeEvent
    public void onStructureElementVisited(StructureElementVisitedEvent event) {
        if (event == null || event.getWorld() != world
            || event.getElement() == null
            || !instrumentId.equals(event.getInstrumentIdentifier())) {
            return;
        }
        elementsByPosition
            .put(StructureLibSceneMetadata.packBlockPos(event.getX(), event.getY(), event.getZ()), event.getElement());
    }

    public Map<Long, IStructureElement<?>> snapshot() {
        return elementsByPosition.isEmpty() ? Map.of() : Map.copyOf(elementsByPosition);
    }
}
