package net.ihiroky.reservoir;

/**
 * Provides for methods to receive {@link net.ihiroky.reservoir.Cache} events.
 * A class that is interested in {@link net.ihiroky.reservoir.Cache} events implements this interface and call
 * {@link net.ihiroky.reservoir.Cache#addEventListener(net.ihiroky.reservoir.CacheEventListener)} with the implemented
 * object.
 *
 * @author Hiroki Itoh
 */
public interface CacheEventListener<K, V> {

    /**
     * Invoked when a new entry is put to a target {@link net.ihiroky.reservoir.Cache}.
     *
     * @param cache a target {@code Cache}
     * @param key a key of a new entry
     * @param ref a {@link net.ihiroky.reservoir.Ref} holding a value of a new entry
     */
    void onPut(Cache<K, V> cache, K key, Ref<V> ref);

    /**
     * Invoked when a entry is removed from a target {@link net.ihiroky.reservoir.Cache}.
     * @param cache a target {@code Cache}
     * @param key a key of a removed entry
     * @param ref a {@link net.ihiroky.reservoir.Ref} holding a value of a removed entry
     */
    void onRemove(Cache<K, V> cache, K key, Ref<V> ref);

    /**
     * Invoked when a entry is removed from a target {@link net.ihiroky.reservoir.Cache} because of the number of
     * cache entry limitation.
     * @param cache a target {@code Cache}
     * @param key a key of a removed entry
     * @param ref a {@link net.ihiroky.reservoir.Ref} holding avalue of a removed entry
     */
    void onCacheOut(Cache<K, V> cache, K key, Ref<V> ref);
}
