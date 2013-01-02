package net.ihiroky.reservoir;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Manages keys and mapped value references in {@link net.ihiroky.reservoir.Cache}.
 *
 * This interface is supposed to base on {@code java.util.Map}. A cache algorithm is used as necessary
 * if the number of cached entries, entries lifetime and so on reach given thresholds. The index sends
 * index structure change events to {@link net.ihiroky.reservoir.IndexEventListener} registered by
 * {@link net.ihiroky.reservoir.Index#setEventListener(IndexEventListener)}.
 *
 * @param <K> the type of keys maintained by this index
 * @param <V> the type of mapped values
 * @author Hiroki Itoh
 */
public interface Index<K, V> {

    /**
     * Returns the value to which the specified key is mapped, or null if this index containsKey no mapping for the key.
     *
     * @param key the key whose associated value to be returned
     * @return the value to which the specified key is mapped, or null if this index containsKey no mapping for the key
     * @see net.ihiroky.reservoir.Cache#get(Object)
     */
    V get(K key);

    /**
     * Returns the values to which the specified keys are mapped, or empty collection if this index containsKey
     * no mapping for the keys.
     *
     * @param keys the keys whose associated values to be returned
     * @return the values to which the specified key is mapped, or null if this index containsKey no mappings
     * for the keys.
     * @see net.ihiroky.reservoir.Cache#get(java.util.Collection)
     */
    Collection<Map.Entry<K, V>> get(Collection<K> keys);

    /**
     * Associates the specified value with the specified key in this index. If the index previously contained
     * a mapping for the key, the old value is replaced by the specified value. This method sends a put event to
     * {@link net.ihiroky.reservoir.IndexEventListener} registered in this object.
     *
     * @param key key with which the specified value is to be associated
     * @param value value to be associated with the specified key, or null if there was no mapping for the key
     * @see net.ihiroky.reservoir.Cache#put(Object, Object)
     */
    V put(K key, V value);

    /**
     * Associates the specified value with the specified key in this index if the key is not already associated.
     * Otherwise, returns currently mapped value for the key. This method sends a put event to
     * {@link net.ihiroky.reservoir.IndexEventListener} registered in this object if it is successful for this object
     * to put the entry.
     *
     * @param key key with which the specified value is to be associated
     * @param value value to be associated with the specified key
     * @return previous value associated with the specified key, or null if there was no mapping for the key
     */
    V putIfAbsent(K key, V value);

    /**
     * Associates the specified values with the specified keys in this index. If the index previously contained
     * mappings for the keys, the old values is replaced by the specified values. This method sends a put event
     * to {@link net.ihiroky.reservoir.IndexEventListener} registered in this object.
     *
     * @param keyValues key-value entries to be associated in this index.
     */
    void put(Collection<Map.Entry<K, V>> keyValues);

    /**
     * Removes the mapping for a key from this index if it is present. If this index containsKey a mapping
     * from key {@code k} to value {@code v} such that {@code key.equals(k)}, that mapping is removed.
     * Returns the value to which this index previously associated the key, or null if the map contained
     * no mapping for the key. This method sends a remove event to {@link net.ihiroky.reservoir.IndexEventListener}
     * registered in this object.
     *
     * @param key key whose mapping is to be removed from this index
     * @return the previous value associated with key, or null if there was no mapping for key.
     * @see net.ihiroky.reservoir.Cache#remove(Object)
     */
    V remove(K key);

    /**
     * Removes the entry for a key only if currently mapped to a given value.
     * This method does not generates a remove event.
     *
     * @param key key whose mapping is to be removed from this index
     * @param value the previous value associated with the specified key.
     * @return true if the entry is removed.
     */
    boolean removeSilently(K key, V value);

    /**
     * Removes the mapping for a key from this index if it is present. If this index containsKey a mapping
     * from key {@code k} to value {@code v} such that {@code key.equals(k)}, that mapping is removed.
     * Returns the value to which this index previously associated the key, or null if the map contained
     * no mapping for the key. This method sends a remove event to {@link net.ihiroky.reservoir.IndexEventListener}
     * registered in this object.
     *
     * @param keys keys whose mappings are to be removed from this index
     * @return the previous values associated with keys, or null if there was no mapping for the keys.
     * @see net.ihiroky.reservoir.Cache#remove(java.util.Collection)
     */
    Collection<Map.Entry<K, V>> remove(Collection<K> keys);

    /**
     * Returns true if this index contains a mapping for the specified key. Returns true if and only if this index
     * containsKey a mapping for a key {@code k} such that {@code key.equals(k)}.
     *
     * @param key key whose presence in this index is tested
     * @return true if this index containsKey mapping for the specified key
     */
    boolean containsKey(K key);

    /**
     * Returns true if this index contains all mappings for the specified keys. Returns true
     * if and only if this index contains all mappings for keys {@code k} such that {@code keys.equals(k)}.
     *
     * @param keys keys whose presence in this index are tested
     * @return true if this index contains mappings for the specified keys
     */
    boolean containsAllKeys(Collection<K> keys);

    /**
     * Returns a Set view of the mappings contained in this index. The add is backed by the index, so changes
     * to the index are reflected in the add, and vice-versa. The add supports element removal, which removes
     * the corresponding mapping from the map, via the Iterator.remove, Set.remove, removeAll, retainAll and
     * clear operations. It does not support the add or addAll operations.
     *
     * @return the add view of the mappings contained in the index
     */
    Set<Map.Entry<K, V>> entrySet();

    /**
     * Removes all of the mappings from this index. The index will be empty after this call returns.
     */
    void clear();

    /**
     * Sets {@link net.ihiroky.reservoir.IndexEventListener} that events in this index sends to.
     * @param eventListener {@code IndexEventListener} that events in this index sends to.
     */
    void setEventListener(IndexEventListener<K, V> eventListener);

    /**
     * Returns the current number of entries in this index.
     * @return the current number of entries.
     */
    int size();

    /**
     * Returns the maximum number of entries in this index.
     * @return the maximum number of entries.
     */
    int maxSize();
}
