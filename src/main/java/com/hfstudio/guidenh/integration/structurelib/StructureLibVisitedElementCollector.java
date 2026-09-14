package com.hfstudio.guidenh.integration.structurelib;

import net.minecraft.world.World;

import com.gtnewhorizon.structurelib.StructureEvent.StructureElementVisitedEvent;
import com.gtnewhorizon.structurelib.structure.IStructureElement;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMaps;

public class StructureLibVisitedElementCollector {

    public final Object instrumentId;
    public final World world;
    private final Long2ObjectLinkedOpenHashMap<IStructureElement<?>> elementsByPosition = new Long2ObjectLinkedOpenHashMap<>();

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

    public Long2ObjectMap<IStructureElement<?>> snapshot() {
        return elementsByPosition.isEmpty() ? Long2ObjectMaps.emptyMap()
            : Long2ObjectMaps.unmodifiable(new Long2ObjectLinkedOpenHashMap<>(elementsByPosition));
    }
}
