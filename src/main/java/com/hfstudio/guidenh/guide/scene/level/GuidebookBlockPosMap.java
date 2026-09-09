package com.hfstudio.guidenh.guide.scene.level;

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

/**
 * Exact, allocation-free lookup map for block coordinates. A long cannot encode all three
 * signed-int axes, so this partitions by the exact X/Z column and uses the exact Y as its inner
 * primitive key. It is deliberately sparse for high-world scenes.
 *
 * <p>
 * This is deliberately not thread-safe. A preview level is fully constructed by the compile
 * worker, published through the worker's synchronization boundary, then mutated and rendered on
 * the client thread. Concurrent access would also be unsafe for the TileEntity values stored by
 * callers, so a concurrent map would not establish a valid ownership model.
 * </p>
 */
public class GuidebookBlockPosMap<V> {

    private final Long2ObjectOpenHashMap<Int2ObjectOpenHashMap<V>> valuesByColumn = new Long2ObjectOpenHashMap<>();
    private final Collection<V> valuesView = Collections.unmodifiableCollection(new ValuesView());
    private int size;

    public V get(int x, int y, int z) {
        Int2ObjectOpenHashMap<V> values = valuesByColumn.get(packColumn(x, z));
        return values != null ? values.get(y) : null;
    }

    public boolean containsKey(int x, int y, int z) {
        Int2ObjectOpenHashMap<V> values = valuesByColumn.get(packColumn(x, z));
        return values != null && values.containsKey(y);
    }

    public V put(int x, int y, int z, V value) {
        long column = packColumn(x, z);
        Int2ObjectOpenHashMap<V> values = valuesByColumn.get(column);
        if (values == null) {
            values = new Int2ObjectOpenHashMap<>();
            valuesByColumn.put(column, values);
        }
        boolean hadValue = values.containsKey(y);
        V previous = values.put(y, value);
        if (!hadValue) {
            size++;
        }
        return previous;
    }

    public V remove(int x, int y, int z) {
        long column = packColumn(x, z);
        Int2ObjectOpenHashMap<V> values = valuesByColumn.get(column);
        if (values == null || !values.containsKey(y)) {
            return null;
        }
        V previous = values.remove(y);
        size--;
        if (values.isEmpty()) {
            valuesByColumn.remove(column);
        }
        return previous;
    }

    public void clear() {
        valuesByColumn.clear();
        size = 0;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public int size() {
        return size;
    }

    public Collection<V> values() {
        return valuesView;
    }

    public void forEach(PositionValueConsumer<V> consumer) {
        for (var columnEntry : valuesByColumn.long2ObjectEntrySet()) {
            long column = columnEntry.getLongKey();
            int x = unpackColumnX(column);
            int z = unpackColumnZ(column);
            for (var valueEntry : columnEntry.getValue()
                .int2ObjectEntrySet()) {
                consumer.accept(x, valueEntry.getIntKey(), z, valueEntry.getValue());
            }
        }
    }

    @FunctionalInterface
    public interface PositionValueConsumer<V> {

        void accept(int x, int y, int z, V value);
    }

    private class ValuesView extends AbstractCollection<V> {

        @Override
        public Iterator<V> iterator() {
            return new Iterator<>() {

                private final Iterator<Int2ObjectOpenHashMap<V>> columns = valuesByColumn.values()
                    .iterator();
                private Iterator<V> currentValues = Collections.emptyIterator();

                @Override
                public boolean hasNext() {
                    while (!currentValues.hasNext() && columns.hasNext()) {
                        currentValues = columns.next()
                            .values()
                            .iterator();
                    }
                    return currentValues.hasNext();
                }

                @Override
                public V next() {
                    if (!hasNext()) {
                        throw new NoSuchElementException();
                    }
                    return currentValues.next();
                }
            };
        }

        @Override
        public int size() {
            return GuidebookBlockPosMap.this.size;
        }
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
}
