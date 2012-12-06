package net.ihiroky.reservoir;

/**
 * Created on 12/09/18, 20:17
 *
 * @author Hiroki Itoh
 */
public interface CacheEventListener<K, V> {

    void onPut(Cache<K, V> cache, K key, Ref<V> ref);

    void onRemove(Cache<K, V> cache, K key, Ref<V> ref);

    void onCacheOut(Cache<K, V> cache, K key, Ref<V> ref);
}
