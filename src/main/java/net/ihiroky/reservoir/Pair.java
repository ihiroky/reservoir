package net.ihiroky.reservoir;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created on 12/10/05, 14:28
 *
 * @author Hiroki Itoh
 */
public class Pair {

    public static <K, V> Map.Entry<K, V> newImmutableEntry(Map.Entry<K, V> entry) {
        return new AbstractMap.SimpleImmutableEntry<K, V>(entry.getKey(), entry.getValue());
    }

    public static <K, V> Map.Entry<K, V> newImmutableEntry(K key, V value) {
        return new AbstractMap.SimpleImmutableEntry<K, V>(key, value);
    }

    @SuppressWarnings("unchecked")
    public static <K, V> List<Map.Entry<K, V>> newImmutableEntries(Map.Entry<K, V> entry, Map.Entry<K, V>... entries) {
        int length = entries.length;
        List<Map.Entry<K, V>> entryList = new ArrayList<Map.Entry<K, V>>(length + 1);
        entryList.add(newImmutableEntry(entry));
        for (int i = 0; i < length; i++) {
            entryList.add(newImmutableEntry(entries[i]));
        }
        return entryList;
    }

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
