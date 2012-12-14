package net.ihiroky.reservoir.accessor;

import net.ihiroky.reservoir.CacheAccessor;
import net.ihiroky.reservoir.Index;
import net.ihiroky.reservoir.Ref;

import java.util.Collection;
import java.util.Map;
import java.util.Properties;

/**
 * Created on 12/09/26, 14:59
 *
 * @author Hiroki Itoh
 */
public class HeapCacheAccessor<K, V> implements CacheAccessor<K, V> {

    static class HeapRef<V> implements Ref<V> {

        V value;

        HeapRef(V value) {
            this.value = value;
        }

        @Override
        public V value() {
            return value;
        }

        @Override
        public int hashCode() {
            return (value != null) ? value.hashCode() : 0;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof Ref)) {
                return false;
            }
            @SuppressWarnings("unchecked") Ref<V> that = (Ref<V>) obj;
            return this.value().equals(that.value());
        }
    }

    private void updateEntry(K key, V value, Index<K, Ref<V>> index) {
        if (key == null) {
            return;
        }
        HeapRef<V> ref = new HeapRef<V>(value);
        @SuppressWarnings("unchecked")
        HeapRef<V> oldRef = (HeapRef<V>) index.putIfAbsent(key, ref);
        if (oldRef != null) {
            ref = oldRef;
        }
        ref.value = value;
    }

    @Override
    public Ref<V> create(K key, V value) {
        return new HeapRef<V>(value);
    }

    @Override
    public boolean update(K key, V value, Index<K, Ref<V>> index) {
        updateEntry(key, value, index);
        return true;
    }

    @Override
    public void update(Map<K, V> keyValues, Index<K, Ref<V>> index) {
        for (Map.Entry<K, V> keyValue : keyValues.entrySet()) {
            updateEntry(keyValue.getKey(), keyValue.getValue(), index);
        }
    }

    @Override
    public void remove(K key, Ref<V> ref) {
    }

    @Override
    public void remove(Collection<Map.Entry<K, Ref<V>>> refs) {
    }

    @Override
    public void prepare(String name, Properties props) {
    }

    @Override
    public void dispose() {
    }
}
