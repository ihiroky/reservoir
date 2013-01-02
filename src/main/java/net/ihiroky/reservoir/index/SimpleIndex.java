package net.ihiroky.reservoir.index;

import net.ihiroky.reservoir.Index;
import net.ihiroky.reservoir.IndexEventListener;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Created on 12/10/05, 10:47
 *
 * @author Hiroki Itoh
 */
public class SimpleIndex<K, V> implements Index<K, V> {

    private ConcurrentMap<K, V> index;
    private IndexEventListener<K, V> eventListener;

    private static final int DEFAULT_INITIAL_CAPACITY = 16;

    @SuppressWarnings("unchecked")
    static <K, V> IndexEventListener<K, V> nullEventListener() {
        return (IndexEventListener<K, V>) IndexEventListener.NULL_LISTENER;
    }

    public SimpleIndex() {
        this(DEFAULT_INITIAL_CAPACITY);
    }

    public SimpleIndex(int initialCapacity) {
        index = new ConcurrentHashMap<K, V>(
                (initialCapacity != Integer.MAX_VALUE && initialCapacity > 0) ? initialCapacity : DEFAULT_INITIAL_CAPACITY);
        eventListener = nullEventListener();
    }

    @Override
    public V get(K key) {
        if (key == null) {
            return null;
        }
        return index.get(key);
    }

    @Override
    public Collection<Map.Entry<K, V>> get(Collection<K> keys) {
        if (keys == null) {
            return Collections.emptyList();
        }
        Collection<Map.Entry<K, V>> result = new ArrayList<Map.Entry<K, V>>(keys.size());
        for (K key : keys) {
            V value = index.get(key);
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
        if (key == null || value == null) {
            throw new NullPointerException();
        }
        V oldValue = index.putIfAbsent(key, value);
        if (oldValue == null) {
            eventListener.onPut(this, key, value);
        }
        return oldValue;
    }

    @Override
    public void put(Collection<Map.Entry<K, V>> keyValues) {
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
        if (key == null) {
            return null;
        }
        V value = index.remove(key);
        if (value != null) {
            eventListener.onRemove(this, key, value);
        }
        return value;
    }

    @Override
    public boolean removeSilently(K key, V value) {
        return (key != null && value != null) && index.remove(key, value);
    }

    @Override
    public Collection<Map.Entry<K, V>> remove(Collection<K> keys) {
        if (keys == null) {
            return Collections.emptyList();
        }
        Map<K, V> indexMap = index;
        Collection<Map.Entry<K, V>> result = new ArrayList<Map.Entry<K, V>>(keys.size());
        for (K key : keys) {
            V value = indexMap.remove(key);
            if (value != null) {
                eventListener.onRemove(this, key, value);
                result.add(new AbstractMap.SimpleImmutableEntry<K, V>(key, value));
            }
        }
        return result;
    }

    @Override
    public boolean containsKey(K key) {
        return index.containsKey(key);
    }

    @Override
    public boolean containsAllKeys(Collection<K> keySet) {
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
        for (Map.Entry<K, V> entry : index.entrySet()) {
            eventListener.onRemove(this, entry.getKey(), entry.getValue());
        }
        index.clear();

    }

    @Override
    public void setEventListener(IndexEventListener<K, V> eventListener) {
        IndexEventListener<K, V> nullListener = nullEventListener();
        this.eventListener = (eventListener != null) ? eventListener : nullListener;
    }

    @Override
    public int size() {
        return index.size();
    }

    @Override
    public int maxSize() {
        return Integer.MAX_VALUE;
    }
}
