package net.ihiroky.reservoir.index;

import net.ihiroky.reservoir.Index;
import net.ihiroky.reservoir.IndexEventListener;
import net.ihiroky.reservoir.Pair;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Cache Index implementation with LinkedHashMap.
 * <p/>
 * Null key and value are not allowed.
 * Note that this implementation is not synchronized.
 * <p/>
 * Created on 12/09/18, 15:04
 *
 * @author Hiroki Itoh
 */
public class LinkedHashMapIndex<K, V> implements Index<K, V> {

    private LinkedHashMap<K, V> index;
    private int capacity;
    private IndexEventListener<K, V> eventListener;

    protected LinkedHashMapIndex(int initialCapacity, int capacity, boolean accessOrder) {
        if (initialCapacity <= 0) {
            throw new IllegalArgumentException("initialCapacity must be greater than 0.");
        }
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be greater than 0.");
        }
        if (initialCapacity > capacity) {
            throw new IllegalArgumentException("capacity must be equal or greater than initialCapacity.");
        }
        this.capacity = capacity;
        this.eventListener = SimpleIndex.nullEventListener();
        this.index = new LinkedHashMap<K, V>(initialCapacity, 0.75f, accessOrder) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                return (size() > LinkedHashMapIndex.this.capacity)
                        && eventListener.onCacheOut(LinkedHashMapIndex.this, eldest.getKey(), eldest.getValue());
            }
        };
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
        // TODO eviction if shrink.
    }

    @Override
    public V get(K key) {
        return index.get(key);
    }

    @Override
    public Collection<Map.Entry<K, V>> get(Collection<K> keys) {
        if (keys == null) {
            return null;
        }

        Map<K, V> indexMap = this.index;
        Collection<Map.Entry<K, V>> result = new ArrayList<Map.Entry<K, V>>(keys.size());
        for (K key : keys) {
            V value = indexMap.get(key);
            if (value != null) {
                result.add(new AbstractMap.SimpleImmutableEntry<K, V>(key, value));
            }
        }
        return result;
    }

    @Override
    public V put(K key, V value) {
        if (key == null || value == null) {
            return null;
        }

        V old = index.put(key, value);
        eventListener.onPut(this, key, value);
        return old;
    }

    @Override
    public V putIfAbsent(K key, V value) {
        V oldValue = get(key);
        if (oldValue == null) {
            put(key, value);
        }
        return oldValue;
    }

    @Override
    public void put(Collection<Map.Entry<K, V>> keyValues) {
        if (keyValues == null) {
            return;
        }

        Map<K, V> indexMap = this.index;
        K key;
        V value;
        for (Map.Entry<K, V> entry : keyValues) {
            key = entry.getKey();
            value = entry.getValue();
            indexMap.put(key, value);
            eventListener.onPut(this, key, value);
        }
    }

    @Override
    public V remove(K key) {
        V value = index.remove(key);
        if (value != null) {
            eventListener.onRemove(this, key, value);
        }
        return value;
    }

    @Override
    public void removeSilently(K key, V value) {
        if (value.equals(index.get(key))) {
            index.remove(key);
        }
    }

    @Override
    public Collection<Map.Entry<K, V>> remove(Collection<K> keySet) {
        if (keySet == null) {
            return Collections.emptyList();
        }

        Map<K, V> indexMap = this.index;
        Collection<Map.Entry<K, V>> removed = new ArrayList<Map.Entry<K, V>>(keySet.size());
        V value;
        for (K key : keySet) {
            value = indexMap.remove(key);
            if (value != null) {
                eventListener.onRemove(this, key, value);
                removed.add(Pair.newImmutableEntry(key, value));
            }
        }
        removed = Collections.unmodifiableCollection(removed);
        return removed;
    }

    @Override
    public boolean contains(K key) {
        return index.containsKey(key);
    }

    @Override
    public boolean containsAll(Collection<K> keySet) {
        Map<K, V> indexMap = this.index;
        for (K key : keySet) {
            if (!indexMap.containsKey(key)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        return index.entrySet();
    }

    @Override
    public void clear() {
        IndexEventListener<K, V> listener = eventListener;
        for (Map.Entry<K, V> entry : index.entrySet()) {
            listener.onRemove(this, entry.getKey(), entry.getValue());
        }
        index.clear();
    }

    @Override
    public void setEventListener(IndexEventListener<K, V> eventListener) {
        IndexEventListener<K, V> nullEventListener = SimpleIndex.nullEventListener();
        this.eventListener = (eventListener != null) ? eventListener : nullEventListener;
    }

    @Override
    public int size() {
        return index.size();
    }

    @Override
    public int maxSize() {
        return capacity;
    }
}
