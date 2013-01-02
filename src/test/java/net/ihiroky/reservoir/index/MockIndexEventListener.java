package net.ihiroky.reservoir.index;

import net.ihiroky.reservoir.Index;
import net.ihiroky.reservoir.IndexEventListener;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Created on 12/09/26, 18:03
 *
 * @author Hiroki Itoh
 */
class MockIndexEventListener<K, V> implements IndexEventListener<K, V> {

    enum Method {
        PUT,
        PUTS,
        REMOVE,
        REMOVES,
        CACHE_OUT,
        CACHE_OUTS,
    }

    static class Args<K, V> {
        Method method;
        Index<K, V> index;
        K key;
        V value;
        Collection<Map.Entry<K, V>> entries;

        Args(Method method, Index<K, V> index, K key, V value) {
            this.method = method;
            this.index = index;
            this.key = key;
            this.value = value;
        }

        Args(Method method, Index<K, V> index, Collection<Map.Entry<K, V>> entries) {
            this.method = method;
            this.index = index;
            this.entries = entries;
        }
    }

    List<Args<K, V>> argsList = Collections.synchronizedList(new ArrayList<Args<K, V>>());

    @Override
    public void onPut(Index<K, V> index, K key, V value) {
        argsList.add(new Args<K, V>(Method.PUT, index, key, value));
    }

    @Override
    public void onRemove(Index<K, V> index, K key, V value) {
        argsList.add(new Args<K, V>(Method.REMOVE, index, key, value));
    }

    @Override
    public boolean onDiscard(Index<K, V> index, K key, V value) {
        argsList.add(new Args<K, V>(Method.CACHE_OUT, index, key, value));
        return true;
    }
}
