package com.hfstudio.guidenh.guide.scene.preview;

import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.MinecraftForge;

import com.google.common.collect.Iterables;
import com.gtnewhorizon.structurelib.StructureEvent;
import com.gtnewhorizon.structurelib.StructureLibAPI;
import com.gtnewhorizon.structurelib.alignment.constructable.IConstructable;
import com.gtnewhorizon.structurelib.alignment.constructable.IMultiblockInfoContainer;
import com.gtnewhorizon.structurelib.structure.IStructureElement;
import com.hfstudio.guidenh.guide.scene.support.GuideDebugLog;

import blockrenderer6343.client.utils.BRUtil;
import blockrenderer6343.client.utils.ConstructableData;
import blockrenderer6343.client.world.ObserverWorld;
import blockrenderer6343.integration.nei.StructureHacks;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;

/**
 * StructureLib container scan with per-container failure isolation.
 *
 * <p>
 * BlockRenderer6343's scanner publishes only after all containers finish. A single broken optional
 * integration can therefore discard every valid definition. This scanner keeps the same data collection
 * algorithm while allowing the remaining containers to publish.
 * </p>
 */
@SuppressWarnings("unchecked")
public class GuideStructureLibContainerScan implements Runnable {

    public static final String IDENTIFIER = "GuideNH-StructureLibContainerScan";

    public final Consumer<Long2ObjectMap<ObjectSet<IConstructable>>> resultCallback;
    public final Consumer<Object2ObjectMap<IConstructable, ItemStack>> stackCallback;
    public final Map<String, IMultiblockInfoContainer<TileEntity>> infoContainers;
    public final Long2ObjectMap<ObjectSet<IConstructable>> result = new Long2ObjectOpenHashMap<>();
    public final ObjectSet<IStructureElement<?>> checkedElements = new ObjectOpenHashSet<>();
    public final ObserverWorld world = new ObserverWorld();
    public IConstructable currentConstructable;
    public ConstructableData currentData = new ConstructableData();

    public GuideStructureLibContainerScan(Consumer<Long2ObjectMap<ObjectSet<IConstructable>>> resultCallback,
        Consumer<Object2ObjectMap<IConstructable, ItemStack>> stackCallback,
        Map<String, IMultiblockInfoContainer<?>> source) {
        this.resultCallback = resultCallback;
        this.stackCallback = stackCallback;
        this.infoContainers = source.entrySet()
            .stream()
            .collect(
                Collectors.toMap(Map.Entry::getKey, entry -> (IMultiblockInfoContainer<TileEntity>) entry.getValue()));
    }

    @Override
    public void run() {
        MinecraftForge.EVENT_BUS.register(this);
        boolean instrumentEnabled = false;
        try {
            if (!StructureLibAPI.isInstrumentEnabled()) {
                StructureLibAPI.enableInstrument(IDENTIFIER);
                instrumentEnabled = true;
            }

            Object2ObjectMap<IConstructable, ItemStack> stacks = new Object2ObjectOpenHashMap<>();
            Object2ObjectMap<IConstructable, ConstructableData> constructableData = new Object2ObjectOpenHashMap<>();
            for (Map.Entry<String, IMultiblockInfoContainer<TileEntity>> entry : infoContainers.entrySet()) {
                currentConstructable = null;
                currentData = new ConstructableData();
                checkedElements.clear();
                world.reset();
                try {
                    currentConstructable = world.getConstructableFromContainer(entry.getKey(), entry.getValue());
                    int tier = world.estimateTierFromInfoContainer(result, stacks, currentConstructable);
                    if (tier > 1) currentData.setMaxTier(tier, "");
                    if (currentData.hasData()) constructableData.put(currentConstructable, currentData);
                } catch (Throwable t) {
                    // ObserverWorld.reset() normally runs at the end of a successful tier pass. If a
                    // compatibility container throws halfway through construct(), that cleanup is skipped;
                    // clear the partial blocks before the next independent container is inspected.
                    world.reset();
                    GuideDebugLog.warn(
                        "[GuideNH] [StructureLib] Skipping invalid multiblock container {}: {}",
                        entry.getKey(),
                        t.toString());
                }
            }
            ConstructableData.addConstructableData(constructableData);
            stackCallback.accept(stacks);
            resultCallback.accept(result);
        } finally {
            currentConstructable = null;
            checkedElements.clear();
            if (instrumentEnabled && StructureLibAPI.isInstrumentEnabled()) {
                StructureLibAPI.disableInstrument();
            }
            MinecraftForge.EVENT_BUS.unregister(this);
        }
    }

    @SubscribeEvent
    @SuppressWarnings({ "unused", "unchecked" })
    public void onStructureEvent(StructureEvent.StructureElementVisitedEvent event) {
        if (!IDENTIFIER.equals(event.getInstrumentIdentifier()) || currentConstructable == null
            || !checkedElements.add(event.getElement())) {
            return;
        }
        try {
            TileEntity tile = world.getTileEntity(0, 64, 0);
            Iterable<ItemStack> candidates = StructureHacks
                .getStacksForElement(tile, (IStructureElement<Object>) event.getElement(), currentData);
            if (candidates == null || Iterables.isEmpty(candidates)) return;
            for (ItemStack stack : candidates) {
                if (!StructureHacks.isSafeStack(stack)) continue;
                result.computeIfAbsent(BRUtil.hashStack(stack), ignored -> new ObjectOpenHashSet<>())
                    .add(currentConstructable);
            }
        } catch (Throwable t) {
            GuideDebugLog.warn(
                "[GuideNH] [StructureLib] Skipping invalid structure element {}: {}",
                event.getElement()
                    .getClass()
                    .getName(),
                t.toString());
        }
    }
}
