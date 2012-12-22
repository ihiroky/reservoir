package net.ihiroky.reservoir;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

/**
 * Maps keys to values like {@code java.util.Map}. A {@code Cache} can't contain duplicate keys.
 * {@code null} key or value are not supported.
 *
 * @author Hiroki Itoh
 */
public interface Cache<K, V> extends CacheMBean, Iterable<Map.Entry<K, V>> {

    /**
     * Returns the value to which the specified key is mapped, or null if this cache contains no mapping for the key.
     * If this cache contains a mapping from a key {@code k} to a value {@code v} such that {@code key.equals(k)},
     * then this method returns {@code v}; otherwise it returns null. The key must not be null.
     *
     * @param key a key whose associated value is to be returned
     * @return a value to which the specified key is mapped, or null if this cache contains no mapping for the key
     */
    V get(K key);

    /**
     * Returns the values to which the specified keys are mapped, or an empty map if this cache contains no mapping
     * for the keys. If this cache contains a mapping from a keys {@code k} to a values {@code v} such that
     * {@code keys.contains(k)}, then this method returns {@code v}; otherwise it returns an empty map.
     * The keys must not be null.
     *
     * @param keys keys whose associated values are to be returned
     * @return values to which the specified keys are mapped, or null if this cache contains no mapping for the keys
     */
    Map<K, V> get(Collection<K> keys);

    /**
     * Associates the specified value with the specified key in this cache. If the cache previously contained a mapping
     * for the key, the old value is replaced by the specified value.
     *
     * @param key key with which the specified value is to be associated
     * @param value value to be associated with the specified key
     */
    void put(K key, V value);

    void put(Map<K, V> keyValues);

    void remove(K key);

    void remove(Collection<K> keys);

    V poll(K key);

    Map<K, V> poll(Collection<K> keys);

    boolean containsKey(K key);

    /**
     * Returns an iterator over the entries in this cache. The entries are returned in no particular order.
     * @return an iterator over the entries in this cache
     */
    Iterator<Map.Entry<K, V>> iterator();

    /**
     * Removes all of the mappings from this cache. The cache will be empty after this call returns.
     */
    void clear();

    /**
     * Invalidates this cache. Values and their stores is released.
     */
    void dispose();

    /**
     * Adds {@link net.ihiroky.reservoir.CacheEventListener} to receive cache events from this cache.
     * @param eventListener the event listener. If null, do nothing.
     */
    void addEventListener(CacheEventListener<K, V> eventListener);

    /**
     * Removes {@link net.ihiroky.reservoir.CacheEventListener} managed in this cache.
     * @param eventListener the event listener.
     */
    void removeEventListener(CacheEventListener<K, V> eventListener);

    /**
     * Sets {@link net.ihiroky.reservoir.StringResolver} used by MBean methods.
     * @param resolver {@link net.ihiroky.reservoir.StringResolver} used by MBean methods.
     */
    void setStringKeyResolver(StringResolver<K> resolver);

    /**
     * Returns the number of entries in this cache.
     * @return the number of entries.
     */
    int size();

    /**
     * Gets a name of this cache.
     * @return a name of this cache.
     */
    String getName();

    /**
     * Writes entries held by this cache to a {@code outputStream} using a {@code coder}.
     * @param outputStream {@code OutputStream} to write to.
     * @param coder {@link net.ihiroky.reservoir.StreamingCoder} that defines a serialization.
     * @throws Exception if aborted.
     */
    void writeTo(OutputStream outputStream, StreamingCoder<K, V> coder) throws Exception;

    /**
     * Read entries from {@code inputStream} using a {@code coder}
     * @param inputStream {@code InputStream} to read from.
     * @param coder {@link net.ihiroky.reservoir.StreamingCoder} that defines a de-serialization.
     * @throws Exception if aborted.
     */
    void readFrom(InputStream inputStream, StreamingCoder<K, V> coder) throws Exception;
}
