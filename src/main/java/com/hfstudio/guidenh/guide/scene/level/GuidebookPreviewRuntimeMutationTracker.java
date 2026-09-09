package com.hfstudio.guidenh.guide.scene.level;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import net.minecraftforge.common.util.ForgeDirection;

import it.unimi.dsi.fastutil.ints.Int2ByteOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

/**
 * Tracks positions whose imported preview state has been superseded by runtime scene mutations.
 * Integrations use a namespaced {@link MutationKind} to query the state they understand, while
 * scene systems use a {@link MutationLayer} to clear only mutations owned by their lifecycle.
 * Instances are owned by one {@link GuidebookLevel}: construction happens before synchronized
 * page publication, and subsequent access is confined to the client thread.
 */
public class GuidebookPreviewRuntimeMutationTracker {

    private final GuidebookLevel level;

    /**
     * Large block operations are uncommon, while looking up a mutation during preview preparation
     * is cheap. Above this size, retain one region instead of materializing every marked position
     * in both the layer and global indexes.
     */
    private static final long MAX_SPARSE_BOX_AROUND_MARKS = 16_384L;

    /** Runtime mutation key for connection or adjacency dependent render state. */
    public static final MutationKind BLOCK_TOPOLOGY = new MutationKind("guidenh:block_topology");

    /** Runtime mutations applied by static GuideScene operations. */
    public static final MutationLayer SCENE_OPERATIONS = new MutationLayer("guidenh:scene_operations");

    /** Runtime mutations applied by the current Ponder timeline state. */
    public static final MutationLayer PONDER_TIMELINE = new MutationLayer("guidenh:ponder_timeline");

    private final Object2ObjectOpenHashMap<MutationLayer, Object2ObjectOpenHashMap<MutationKind, MutationMarks>> marksByLayer = new Object2ObjectOpenHashMap<>();
    private final Object2ObjectOpenHashMap<MutationKind, GuidebookBlockPosSet> markedPositionsByKind = new Object2ObjectOpenHashMap<>();
    private final Object2ObjectOpenHashMap<MutationKind, GuidebookBlockPosByteMap> additionalMarksByKind = new Object2ObjectOpenHashMap<>();

    public GuidebookPreviewRuntimeMutationTracker(GuidebookLevel level) {
        this.level = Objects.requireNonNull(level, "level");
    }

    /** Marks one position as having runtime state that supersedes imported preview data. */
    public void mark(MutationKind kind, MutationLayer layer, int x, int y, int z) {
        markPosition(kind, marksFor(kind, layer).positions, x, y, z);
    }

    /** Marks a position and all direct neighbours for adjacency dependent preview state. */
    public void markAround(MutationKind kind, MutationLayer layer, int x, int y, int z) {
        MutationMarks marks = marksFor(kind, layer);
        markPosition(kind, marks.positions, x, y, z);
        for (ForgeDirection direction : ForgeDirection.VALID_DIRECTIONS) {
            markPosition(kind, marks.positions, x + direction.offsetX, y + direction.offsetY, z + direction.offsetZ);
        }
    }

    /**
     * Marks every position in an exclusive axis-aligned box and its six direct neighbour faces.
     * This produces the same state as calling {@link #markAround(MutationKind, MutationLayer, int, int, int)}
     * for every position in the box, without repeatedly hashing shared interior positions.
     */
    public void markBoxAround(MutationKind kind, MutationLayer layer, int minX, int minY, int minZ, int maxX, int maxY,
        int maxZ) {
        int clampedMinY = Math.max(level.getMinBuildHeight(), minY);
        int clampedMaxY = Math.min(level.getMaxBuildHeightExclusive(), maxY);
        if (minX >= maxX || clampedMinY >= clampedMaxY || minZ >= maxZ) {
            return;
        }
        MutationMarks marks = marksFor(kind, layer);
        if (requiresRegionMark(minX, clampedMinY, minZ, maxX, clampedMaxY, maxZ)) {
            marks.regions.add(new AroundBox(minX, clampedMinY, minZ, maxX, clampedMaxY, maxZ));
            return;
        }
        markBox(kind, marks.positions, minX, clampedMinY, minZ, maxX, clampedMaxY, maxZ);
        markBox(kind, marks.positions, minX - 1, clampedMinY, minZ, minX, clampedMaxY, maxZ);
        markBox(kind, marks.positions, maxX, clampedMinY, minZ, maxX + 1, clampedMaxY, maxZ);
        markBox(kind, marks.positions, minX, clampedMinY - 1, minZ, maxX, clampedMinY, maxZ);
        markBox(kind, marks.positions, minX, clampedMaxY, minZ, maxX, clampedMaxY + 1, maxZ);
        markBox(kind, marks.positions, minX, clampedMinY, minZ - 1, maxX, clampedMaxY, minZ);
        markBox(kind, marks.positions, minX, clampedMinY, maxZ, maxX, clampedMaxY, maxZ + 1);
    }

    /** Returns whether any layer has marked this position for the requested mutation key. */
    public boolean isMarked(MutationKind kind, int x, int y, int z) {
        if (!level.isValidBuildHeight(y)) {
            return false;
        }
        GuidebookBlockPosSet markedPositions = markedPositionsByKind.get(kind);
        if (markedPositions != null && markedPositions.contains(x, y, z)) {
            return true;
        }
        for (Object2ObjectOpenHashMap<MutationKind, MutationMarks> marksByKind : marksByLayer.values()) {
            MutationMarks marks = marksByKind.get(kind);
            if (marks != null && marks.containsRegion(x, y, z)) {
                return true;
            }
        }
        return false;
    }

    /** Returns whether one lifecycle layer has marked this position for the requested mutation kind. */
    public boolean isMarked(MutationKind kind, MutationLayer layer, int x, int y, int z) {
        if (!level.isValidBuildHeight(y)) {
            return false;
        }
        Object2ObjectOpenHashMap<MutationKind, MutationMarks> marksByKind = marksByLayer.get(layer);
        if (marksByKind == null) {
            return false;
        }
        MutationMarks marks = marksByKind.get(kind);
        return marks != null && (marks.positions.contains(x, y, z) || marks.containsRegion(x, y, z));
    }

    /** Clears mutations applied by one lifecycle layer without affecting other scene operations. */
    public void clearLayer(MutationLayer layer) {
        Object2ObjectOpenHashMap<MutationKind, MutationMarks> marksByKind = marksByLayer.remove(layer);
        if (marksByKind == null) {
            return;
        }
        for (var entry : marksByKind.object2ObjectEntrySet()) {
            removePositions(entry.getKey(), entry.getValue().positions);
        }
    }

    /** Clears one mutation kind from a lifecycle layer while retaining the layer's other state. */
    public void clear(MutationKind kind, MutationLayer layer) {
        Object2ObjectOpenHashMap<MutationKind, MutationMarks> marksByKind = marksByLayer.get(layer);
        if (marksByKind == null) {
            return;
        }
        MutationMarks marks = marksByKind.remove(kind);
        if (marks == null) {
            return;
        }
        removePositions(kind, marks.positions);
        if (marksByKind.isEmpty()) {
            marksByLayer.remove(layer);
        }
    }

    /** Clears all runtime mutation state when the preview level is rebuilt or discarded. */
    public void clear() {
        marksByLayer.clear();
        markedPositionsByKind.clear();
        additionalMarksByKind.clear();
    }

    private void markPosition(MutationKind kind, GuidebookBlockPosSet layerPositions, int x, int y, int z) {
        if (!level.isValidBuildHeight(y)) {
            return;
        }
        if (!layerPositions.add(x, y, z)) {
            return;
        }
        GuidebookBlockPosSet markedPositions = markedPositionsByKind.get(kind);
        if (markedPositions == null) {
            markedPositions = new GuidebookBlockPosSet();
            markedPositionsByKind.put(kind, markedPositions);
        }
        if (!markedPositions.add(x, y, z)) {
            GuidebookBlockPosByteMap additionalMarks = additionalMarksByKind.get(kind);
            if (additionalMarks == null) {
                additionalMarks = new GuidebookBlockPosByteMap();
                additionalMarksByKind.put(kind, additionalMarks);
            }
            int marks = additionalMarks.get(x, y, z);
            if (marks == Byte.MAX_VALUE) {
                throw new IllegalStateException("Too many mutation layers for " + kind.getId());
            }
            additionalMarks.put(x, y, z, (byte) (marks + 1));
        }
    }

    private void markBox(MutationKind kind, GuidebookBlockPosSet layerPositions, int minX, int minY, int minZ, int maxX,
        int maxY, int maxZ) {
        int validMinY = Math.max(level.getMinBuildHeight(), minY);
        int validMaxY = Math.min(level.getMaxBuildHeightExclusive(), maxY);
        if (validMinY >= validMaxY) {
            return;
        }
        for (int x = minX; x < maxX; x++) {
            for (int y = validMinY; y < validMaxY; y++) {
                for (int z = minZ; z < maxZ; z++) {
                    markPosition(kind, layerPositions, x, y, z);
                }
            }
        }
    }

    private MutationMarks marksFor(MutationKind kind, MutationLayer layer) {
        Object2ObjectOpenHashMap<MutationKind, MutationMarks> marksByKind = marksByLayer.get(layer);
        if (marksByKind == null) {
            marksByKind = new Object2ObjectOpenHashMap<>();
            marksByLayer.put(layer, marksByKind);
        }
        MutationMarks marks = marksByKind.get(kind);
        if (marks == null) {
            marks = new MutationMarks();
            marksByKind.put(kind, marks);
        }
        return marks;
    }

    private void removePositions(MutationKind kind, GuidebookBlockPosSet positions) {
        GuidebookBlockPosSet markedPositions = markedPositionsByKind.get(kind);
        if (markedPositions == null) {
            return;
        }
        GuidebookBlockPosByteMap additionalMarks = additionalMarksByKind.get(kind);
        positions.forEach((x, y, z) -> {
            int additionalMarkCount = additionalMarks != null ? additionalMarks.get(x, y, z) : 0;
            if (additionalMarkCount == 0) {
                markedPositions.remove(x, y, z);
            } else if (additionalMarkCount == 1) {
                additionalMarks.remove(x, y, z);
            } else {
                additionalMarks.put(x, y, z, (byte) (additionalMarkCount - 1));
            }
        });
        if (markedPositions.isEmpty()) {
            markedPositionsByKind.remove(kind);
            additionalMarksByKind.remove(kind);
        } else if (additionalMarks != null && additionalMarks.isEmpty()) {
            additionalMarksByKind.remove(kind);
        }
    }

    private static boolean requiresRegionMark(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        long width = (long) maxX - minX;
        long height = (long) maxY - minY;
        long depth = (long) maxZ - minZ;
        if (width > MAX_SPARSE_BOX_AROUND_MARKS || height > MAX_SPARSE_BOX_AROUND_MARKS
            || depth > MAX_SPARSE_BOX_AROUND_MARKS) {
            return true;
        }
        long coreVolume = width * height * depth;
        if (coreVolume > MAX_SPARSE_BOX_AROUND_MARKS) {
            return true;
        }
        long faceVolume = 2L * (width * height + width * depth + height * depth);
        return coreVolume + faceVolume > MAX_SPARSE_BOX_AROUND_MARKS;
    }

    private static class MutationMarks {

        private final GuidebookBlockPosSet positions = new GuidebookBlockPosSet();
        private final List<AroundBox> regions = new ArrayList<>();

        private boolean containsRegion(int x, int y, int z) {
            for (AroundBox region : regions) {
                if (region.contains(x, y, z)) {
                    return true;
                }
            }
            return false;
        }
    }

    /** Exact primitive coordinate set, partitioned by X/Z so Y keeps its full int range. */
    private static class GuidebookBlockPosSet {

        private final Long2ObjectOpenHashMap<IntOpenHashSet> yByColumn = new Long2ObjectOpenHashMap<>();
        private int size;

        private boolean add(int x, int y, int z) {
            long column = packColumn(x, z);
            IntOpenHashSet ys = yByColumn.get(column);
            if (ys == null) {
                ys = new IntOpenHashSet();
                yByColumn.put(column, ys);
            }
            if (!ys.add(y)) {
                return false;
            }
            size++;
            return true;
        }

        private boolean contains(int x, int y, int z) {
            IntOpenHashSet ys = yByColumn.get(packColumn(x, z));
            return ys != null && ys.contains(y);
        }

        private boolean remove(int x, int y, int z) {
            long column = packColumn(x, z);
            IntOpenHashSet ys = yByColumn.get(column);
            if (ys == null || !ys.remove(y)) {
                return false;
            }
            size--;
            if (ys.isEmpty()) {
                yByColumn.remove(column);
            }
            return true;
        }

        private boolean isEmpty() {
            return size == 0;
        }

        private void forEach(PositionConsumer consumer) {
            for (var entry : yByColumn.long2ObjectEntrySet()) {
                long column = entry.getLongKey();
                int x = unpackColumnX(column);
                int z = unpackColumnZ(column);
                for (int y : entry.getValue()) {
                    consumer.accept(x, y, z);
                }
            }
        }
    }

    /** Sparse multiplicity map paired with {@link GuidebookBlockPosSet}. */
    private static class GuidebookBlockPosByteMap {

        private final Long2ObjectOpenHashMap<Int2ByteOpenHashMap> valuesByColumn = new Long2ObjectOpenHashMap<>();
        private int size;

        private int get(int x, int y, int z) {
            Int2ByteOpenHashMap values = valuesByColumn.get(packColumn(x, z));
            return values != null ? values.get(y) : 0;
        }

        private void put(int x, int y, int z, byte value) {
            long column = packColumn(x, z);
            Int2ByteOpenHashMap values = valuesByColumn.get(column);
            if (values == null) {
                values = new Int2ByteOpenHashMap();
                valuesByColumn.put(column, values);
            }
            if (!values.containsKey(y)) {
                size++;
            }
            values.put(y, value);
        }

        private void remove(int x, int y, int z) {
            long column = packColumn(x, z);
            Int2ByteOpenHashMap values = valuesByColumn.get(column);
            if (values != null && values.remove(y) != 0) {
                size--;
                if (values.isEmpty()) {
                    valuesByColumn.remove(column);
                }
            }
        }

        private boolean isEmpty() {
            return size == 0;
        }
    }

    @FunctionalInterface
    private interface PositionConsumer {

        void accept(int x, int y, int z);
    }

    private static long packColumn(int x, int z) {
        return ((long) x << 32) | (z & 0xFFFFFFFFL);
    }

    private static int unpackColumnX(long column) {
        return (int) (column >> 32);
    }

    private static int unpackColumnZ(long column) {
        return (int) column;
    }

    /** An exclusive AABB plus its six direct-neighbour faces. */
    private static class AroundBox {

        private final int minX;
        private final int minY;
        private final int minZ;
        private final int maxX;
        private final int maxY;
        private final int maxZ;

        private AroundBox(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
            this.minX = minX;
            this.minY = minY;
            this.minZ = minZ;
            this.maxX = maxX;
            this.maxY = maxY;
            this.maxZ = maxZ;
        }

        private boolean contains(int x, int y, int z) {
            int outsideAxes = 0;
            if (x < minX) {
                if ((long) minX - x > 1L) return false;
                outsideAxes++;
            } else if (x >= maxX) {
                if ((long) x - maxX > 0L) return false;
                outsideAxes++;
            }
            if (y < minY) {
                if ((long) minY - y > 1L) return false;
                outsideAxes++;
            } else if (y >= maxY) {
                if ((long) y - maxY > 0L) return false;
                outsideAxes++;
            }
            if (z < minZ) {
                if ((long) minZ - z > 1L) return false;
                outsideAxes++;
            } else if (z >= maxZ) {
                if ((long) z - maxZ > 0L) return false;
                outsideAxes++;
            }
            return outsideAxes <= 1;
        }
    }

    /**
     * Identifies one integration-specific category of runtime preview mutation. IDs must be
     * namespaced, for example {@code examplemod:pipe_connections}.
     */
    public static class MutationKind {

        private final String id;

        public MutationKind(String id) {
            this.id = requireNamespacedId(id, "mutation kind");
        }

        public String getId() {
            return id;
        }

        @Override
        public boolean equals(Object other) {
            return this == other || other instanceof MutationKind kind && id.equals(kind.id);
        }

        @Override
        public int hashCode() {
            return id.hashCode();
        }
    }

    /**
     * Identifies the lifecycle that owns a set of runtime preview mutations. IDs must be
     * namespaced, for example {@code examplemod:animation}.
     */
    public static class MutationLayer {

        private final String id;

        public MutationLayer(String id) {
            this.id = requireNamespacedId(id, "mutation layer");
        }

        public String getId() {
            return id;
        }

        @Override
        public boolean equals(Object other) {
            return this == other || other instanceof MutationLayer layer && id.equals(layer.id);
        }

        @Override
        public int hashCode() {
            return id.hashCode();
        }
    }

    private static String requireNamespacedId(String id, String description) {
        String resolved = Objects.requireNonNull(id, description + " id")
            .trim();
        if (resolved.isEmpty() || resolved.indexOf(':') <= 0 || resolved.endsWith(":")) {
            throw new IllegalArgumentException(description + " id must use the namespace:path form: " + id);
        }
        return resolved;
    }
}
