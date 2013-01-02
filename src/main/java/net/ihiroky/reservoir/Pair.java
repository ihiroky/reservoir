package net.ihiroky.reservoir;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Provides for factory methods to create immutable {@code java.util.Map.Entry}.
 * A immutable {@code Map.Entry} does not support {@code setValue} method.
 *
 * @author Hiroki Itoh
 */
public class Pair {

    /**
     * Creates a immutable {@code Map.Entry} with a specified {@code Map.Entry}.
     *
     * @param entry an entry to copy
     * @param <K> a type of an entry's key
     * @param <V> a type of an entry's value
     * @return a immutable {@code Map.Entry}
     */
    public static <K, V> Map.Entry<K, V> newImmutableEntry(Map.Entry<K, V> entry) {
        return new AbstractMap.SimpleImmutableEntry<K, V>(entry.getKey(), entry.getValue());
    }

    /**
     * Creates a immutale {@code Map.Entry} with specified key and value.
     *
     * @param key a key represented by this entry
     * @param value a value represented by this entry
     * @param <K> a type of a key
     * @param <V> a type of a value
     * @return a immutable {@code Map.Entry}
     */
    public static <K, V> Map.Entry<K, V> newImmutableEntry(K key, V value) {
        return new AbstractMap.SimpleImmutableEntry<K, V>(key, value);
    }

    /**
     * Creates a list of immutable {@code Map.Entry}s with specified entries.
     *
     * @param entry a first entry to copy
     * @param entries left entries to copy
     * @param <K> a type of an entry's key
     * @param <V> a type of an entry's value
     * @return a list of immutable {@code Map.Entry}
     */
    @SuppressWarnings("unchecked")
    public static <K, V> List<Map.Entry<K, V>> newImmutableEntries(Map.Entry<K, V> entry, Map.Entry<K, V>... entries) {
        int length = entries.length;
        List<Map.Entry<K, V>> entryList = new ArrayList<Map.Entry<K, V>>(length + 1);
        entryList.add(newImmutableEntry(entry));
        for (Map.Entry<K, V> e : entries) {
            entryList.add(newImmutableEntry(e));
        }
        return entryList;
    }

    /**
     * Creates a list of immutable {@code Map.Entry}s with specified keys and values.
     *
     * @param key a first entry's key
     * @param value a first entry's value
     * @param kvs left keys and values
     * @param <K> a type of keys
     * @param <V> a type of values
     * @return a list of immutable {@code Map.Entry}
     */
    @SuppressWarnings("unchecked")
    public static <K, V> List<Map.Entry<K, V>> newImmutableEntries(K key, V value, Object... kvs) {
        if (kvs.length % 2 != 0) {
            throw new IllegalArgumentException();
        }
        int length = kvs.length;
        List<Map.Entry<K, V>> entryList = new ArrayList<Map.Entry<K, V>>(length / 2 + 1);
        entryList.add(newImmutableEntry(key, value));
        for (int i = 0; i < length; i += 2) {
            entryList.add(newImmutableEntry((K) kvs[i], (V) kvs[i + 1]));
        }
        return entryList;
    }
}
