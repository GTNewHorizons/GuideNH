package com.hfstudio.guidenh.guide.scene.render;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.joml.Matrix4f;

public class GuideSceneTranslucentBlockSorter {

    private static final int RADIX_SORT_THRESHOLD = 128;
    private static final long MAX_RADIX_SCRATCH_BYTES = 16L * 1024 * 1024;
    private static final int RADIX_BUCKET_COUNT = 256;
    private static final int SCRATCH_ARRAY_OVERHEAD_BYTES = 128;
    // All scene sorters on a render thread share bounded scratch, without retaining scene data.
    private static final ThreadLocal<RadixScratch> RADIX_SCRATCH = ThreadLocal.withInitial(RadixScratch::new);

    private final ArrayList<int[]> source = new ArrayList<>();
    private final ArrayList<int[]> sorted = new ArrayList<>();
    private final List<int[]> view = Collections.unmodifiableList(sorted);
    private final Comparator<int[]> comparator = (left, right) -> Float.compare(depth(right), depth(left));
    private int[] coordinates = new int[0];
    private float depthX;
    private float depthY;
    private float depthZ;
    private float depthOffset;
    private boolean valid;

    public List<int[]> sort(List<int[]> positions, Matrix4f matrix) {
        boolean sameDepth = valid && depthX == matrix.m02()
            && depthY == matrix.m12()
            && depthZ == matrix.m22()
            && depthOffset == matrix.m32();
        if (sameDepth && matches(positions)) {
            return view;
        }
        boolean cameraDepthChanged = valid && !sameDepth;
        valid = false;
        depthX = matrix.m02();
        depthY = matrix.m12();
        depthZ = matrix.m22();
        depthOffset = matrix.m32();
        int coordinateCount = Math.multiplyExact(positions.size(), 3);
        if (coordinates.length < coordinateCount) {
            coordinates = new int[coordinateCount];
        }
        source.clear();
        source.addAll(positions);
        for (int index = 0; index < positions.size(); index++) {
            int[] position = positions.get(index);
            int offset = index * 3;
            coordinates[offset] = position[0];
            coordinates[offset + 1] = position[1];
            coordinates[offset + 2] = position[2];
        }
        sorted.clear();
        sorted.addAll(source);
        // Static scenes keep the original initial-sort cost and never allocate unused radix workspace.
        if (sorted.size() > 1 && (!cameraDepthChanged || !sortWithRadix())) {
            sorted.sort(comparator);
        }
        valid = true;
        return view;
    }

    public void clear() {
        source.clear();
        sorted.clear();
        coordinates = new int[0];
        RADIX_SCRATCH.remove();
        valid = false;
    }

    private boolean matches(List<int[]> positions) {
        if (source.size() != positions.size()) {
            return false;
        }
        for (int index = 0; index < positions.size(); index++) {
            int[] position = positions.get(index);
            int offset = index * 3;
            if (source.get(index) != position || coordinates[offset] != position[0]
                || coordinates[offset + 1] != position[1]
                || coordinates[offset + 2] != position[2]) {
                return false;
            }
        }
        return true;
    }

    private float depth(int[] position) {
        return depthX * (position[0] + 0.5f) + depthY * (position[1] + 0.5f)
            + depthZ * (position[2] + 0.5f)
            + depthOffset;
    }

    private boolean sortWithRadix() {
        int size = source.size();
        long requiredBytes = 3L * Integer.BYTES * size + Integer.BYTES * RADIX_BUCKET_COUNT
            + SCRATCH_ARRAY_OVERHEAD_BYTES;
        if (requiredBytes > MAX_RADIX_SCRATCH_BYTES) {
            RADIX_SCRATCH.remove();
            return false;
        }
        if (size <= RADIX_SORT_THRESHOLD) {
            return false;
        }
        RadixScratch scratch = RADIX_SCRATCH.get();
        scratch.ensureCapacity(size);
        int[] keys = scratch.keys;
        int[] order = scratch.order;
        int[] work = scratch.work;
        int[] buckets = scratch.buckets;
        for (int index = 0; index < size; index++) {
            int bits = Float.floatToIntBits(depth(source.get(index)));
            int signedOrdered = bits ^ ((bits >> 31) & Integer.MAX_VALUE);
            // Unsigned ascending keys match Float.compare descending, including NaNs and signed zero.
            keys[index] = ~(signedOrdered ^ Integer.MIN_VALUE);
            order[index] = index;
        }
        for (int shift = 0; shift < Integer.SIZE; shift += Byte.SIZE) {
            Arrays.fill(buckets, 0);
            for (int index = 0; index < size; index++) {
                buckets[(keys[order[index]] >>> shift) & 0xff]++;
            }
            int offset = 0;
            for (int bucket = 0; bucket < buckets.length; bucket++) {
                int count = buckets[bucket];
                buckets[bucket] = offset;
                offset += count;
            }
            for (int index = 0; index < size; index++) {
                int sourceIndex = order[index];
                int bucket = (keys[sourceIndex] >>> shift) & 0xff;
                work[buckets[bucket]++] = sourceIndex;
            }
            int[] previousOrder = order;
            order = work;
            work = previousOrder;
        }
        for (int index = 0; index < size; index++) {
            sorted.set(index, source.get(order[index]));
        }
        return true;
    }

    private static class RadixScratch {

        private int[] keys = new int[0];
        private int[] order = new int[0];
        private int[] work = new int[0];
        private final int[] buckets = new int[RADIX_BUCKET_COUNT];

        private void ensureCapacity(int size) {
            if (keys.length < size) {
                keys = new int[size];
                order = new int[size];
                work = new int[size];
            }
        }
    }
}
