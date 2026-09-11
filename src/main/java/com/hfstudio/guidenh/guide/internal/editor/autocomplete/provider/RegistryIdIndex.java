package com.hfstudio.guidenh.guide.internal.editor.autocomplete.provider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.item.Item;

/**
 * Read-only views of the game registries shaped for completion queries: identifiers sorted, their
 * lowercase forms precomputed, and the distinct namespaces collected once.
 *
 * <p>
 * Completion runs on every keystroke, so a registry is scanned when it changes rather than per query.
 * Registries only grow while mods load, which the key count detects; a slow interval bounds how long a
 * change that keeps the count could stay invisible.
 */
public class RegistryIdIndex {

    /** One registry identifier with its lowercase form, so matching never allocates. */
    public record Entry(String id, String lower) {}

    /** A registry snapshot that can answer ranked identifier queries. */
    public record Snapshot(List<Entry> ids, List<Entry> namespaces) {

        /**
         * Identifiers matching {@code partial}, closest first: namespaces (so {@code minecraft:} is
         * one keystroke away), then identifiers whose path starts with the partial, then identifiers
         * that merely contain it.
         */
        public List<String> match(String partial, int limit) {
            int safeLimit = Math.max(0, limit);
            List<String> results = new ArrayList<>();
            if (safeLimit <= 0) {
                return results;
            }
            String lower = partial != null ? partial.toLowerCase(Locale.ROOT) : "";
            if (lower.indexOf(':') < 0) {
                addNamespaces(results, lower, safeLimit);
            }
            addIdentifierMatches(results, lower, safeLimit, true);
            addIdentifierMatches(results, lower, safeLimit, false);
            return results;
        }

        private void addNamespaces(List<String> results, String lower, int limit) {
            for (Entry namespace : namespaces) {
                if (results.size() >= limit) {
                    return;
                }
                if (lower.isEmpty() || namespace.lower()
                    .startsWith(lower)) {
                    results.add(namespace.id() + ":");
                }
            }
        }

        private void addIdentifierMatches(List<String> results, String lower, int limit, boolean pathPrefixOnly) {
            for (Entry entry : ids) {
                if (results.size() >= limit) {
                    return;
                }
                boolean pathPrefix = matchesPathPrefix(entry.lower(), lower);
                if (pathPrefix != pathPrefixOnly) {
                    continue;
                }
                if (pathPrefix || lower.isEmpty()
                    || entry.lower()
                        .contains(lower)) {
                    results.add(entry.id());
                }
            }
        }
    }

    /**
     * A registry only grows while mods load, which the key count detects, so a snapshot is rebuilt when
     * the count moves. The rebuild also happens after this interval, so an entry a mod replaces with one
     * of the same count cannot stay invisible forever.
     */
    private static final long REBUILD_INTERVAL_MILLIS = 60_000L;

    private static Snapshot itemSnapshot = build(Collections.emptySet());
    private static Snapshot blockSnapshot = build(Collections.emptySet());
    private static int cachedItemKeyCount = -1;
    private static int cachedBlockKeyCount = -1;
    private static long nextItemRebuildAtMillis;
    private static long nextBlockRebuildAtMillis;

    private RegistryIdIndex() {}

    public static Snapshot items() {
        Set<?> keysView = Item.itemRegistry.getKeys();
        if (keysView.size() != cachedItemKeyCount || System.currentTimeMillis() >= nextItemRebuildAtMillis) {
            itemSnapshot = build(keysView);
            cachedItemKeyCount = keysView.size();
            nextItemRebuildAtMillis = System.currentTimeMillis() + REBUILD_INTERVAL_MILLIS;
        }
        return itemSnapshot;
    }

    public static Snapshot blocks() {
        Set<?> keysView = Block.blockRegistry.getKeys();
        if (keysView.size() != cachedBlockKeyCount || System.currentTimeMillis() >= nextBlockRebuildAtMillis) {
            blockSnapshot = build(keysView);
            cachedBlockKeyCount = keysView.size();
            nextBlockRebuildAtMillis = System.currentTimeMillis() + REBUILD_INTERVAL_MILLIS;
        }
        return blockSnapshot;
    }

    /** True when the identifier or its path segment starts with the partial text. */
    private static boolean matchesPathPrefix(String lowerId, String lower) {
        if (lower.isEmpty()) {
            return false;
        }
        if (lowerId.startsWith(lower)) {
            return true;
        }
        int separator = lowerId.indexOf(':');
        return separator >= 0 && lowerId.startsWith(lower, separator + 1);
    }

    private static Snapshot build(Set<?> keysView) {
        List<Entry> entries = new ArrayList<>(keysView.size());
        Set<String> namespaceIds = new LinkedHashSet<>();
        for (Object key : keysView) {
            if (!(key instanceof String id)) {
                continue;
            }
            entries.add(new Entry(id, id.toLowerCase(Locale.ROOT)));
            int separator = id.indexOf(':');
            if (separator > 0) {
                namespaceIds.add(id.substring(0, separator));
            }
        }
        entries.sort(RegistryIdIndex::compareEntries);
        List<Entry> namespaces = new ArrayList<>(namespaceIds.size());
        for (String namespace : namespaceIds) {
            namespaces.add(new Entry(namespace, namespace.toLowerCase(Locale.ROOT)));
        }
        namespaces.sort(RegistryIdIndex::compareEntries);
        return new Snapshot(Collections.unmodifiableList(entries), Collections.unmodifiableList(namespaces));
    }

    private static int compareEntries(Entry left, Entry right) {
        int byLower = left.lower()
            .compareTo(right.lower());
        return byLower != 0 ? byLower
            : left.id()
                .compareTo(right.id());
    }
}
