package net.ihiroky.reservoir;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Created on 12/10/01, 10:26
 *
 * @author Hiroki Itoh
 */
public class MockCacheEventListener<K, V> implements CacheEventListener<K, V> {

    enum Method {
        PUT,
        REMOVE,
        CACHE_OUT,
    }

    static class Args<K, V> {
        Cache<K, V> cache;
        Map.Entry<K, V> entry;
        Method method;

        Args(Method method, Cache<K, V> cache, K key, Ref<V> ref) {
            this.method = method;
            this.cache = cache;
            this.entry = Pair.newImmutableEntry(key, ref.value());
        }
    }

    List<Args<K, V>> argsList = Collections.synchronizedList(new ArrayList<Args<K, V>>());

    @Override
    public void onPut(Cache<K, V> cache, K key, Ref<V> ref) {
        argsList.add(new Args<K, V>(Method.PUT, cache, key, ref));
    }

    @Override
    public void onRemove(Cache<K, V> cache, K key, Ref<V> ref) {
        argsList.add(new Args<K, V>(Method.REMOVE, cache, key, ref));
    }

    @Override
    public void onCacheOut(Cache<K, V> cache, K key, Ref<V> ref) {
        argsList.add(new Args<K, V>(Method.CACHE_OUT, cache, key, ref));
    }
}
