package net.ihiroky.reservoir.index;

import net.ihiroky.reservoir.Index;
import net.ihiroky.reservoir.IndexEventListener;
import net.ihiroky.reservoir.Pair;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created on 12/10/12, 16:39
 *
 * @author Hiroki Itoh
 */
public class ConcurrentLinkedHashMapIndex<K, V> implements Index<K, V> {

    private ConcurrentLinkedHashMap<K, V> map;
    private AtomicLong capacity;
    private IndexEventListener<K, V> eventListener;

    public ConcurrentLinkedHashMapIndex(
            long initialCapacity, long capacity, int concurrentLevel, boolean accessOrder) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be greater than 0.");
        }

        this.eventListener = SimpleIndex.nullEventListener();
        this.capacity = new AtomicLong(capacity);
        map = new ConcurrentLinkedHashMap<K, V>(initialCapacity, 0.75f, concurrentLevel, accessOrder) {
            @Override
            protected RemoveEldestPolicy removeEldestEntry(Map.Entry<K, V> eldestEntry) {
                if (size() > ConcurrentLinkedHashMapIndex.this.capacity.get()) {
                    return eventListener.onDiscard(ConcurrentLinkedHashMapIndex.this,
                            eldestEntry.getKey(), eldestEntry.getValue()) ? RemoveEldestPolicy.REMOVE : RemoveEldestPolicy.READY_TO_REMOVE;
                }
                return RemoveEldestPolicy.DO_NOTHING;
            }
        };

    }

    @Override
    public V get(K key) {
        return map.get(key);
    }

    @Override
    public Collection<Map.Entry<K, V>> get(Collection<K> keys) {
        Collection<Map.Entry<K, V>> result = new ArrayList<Map.Entry<K, V>>(keys.size());
        for (K key : keys) {
            V value = map.get(key);
            if (value != null) {
                result.add(Pair.newImmutableEntry(key, value));
            }
        }
        return result;
    }

    private V putOne(K key, V value) {
        V oldValue = map.put(key, value);
        eventListener.onPut(this, key, value);
        return oldValue;
    }

    @Override
    public V put(K key, V value) {
        return putOne(key, value);
    }

    @Override
    public V putIfAbsent(K key, V value) {
        V oldValue = map.putIfAbsent(key, value);
        if (oldValue == null) {
            eventListener.onPut(this, key, value);
        }
        return oldValue;
    }

    @Override
    public void put(Collection<Map.Entry<K, V>> keyValues) {
        for (Map.Entry<K, V> entry : keyValues) {
            putOne(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public V remove(K key) {
        V value = map.remove(key);
        if (value != null) {
            eventListener.onRemove(this, key, value);
        }
        return value;
    }

    @Override
    public boolean removeSilently(K key, V value) {
        return map.remove(key, value);
    }

    @Override
    public Collection<Map.Entry<K, V>> remove(Collection<K> keys) {
        Collection<Map.Entry<K, V>> result = new ArrayList<Map.Entry<K, V>>(keys.size());
        V value;
        for (K key : keys) {
            value = map.remove(key);
            if (value != null) {
                eventListener.onRemove(this, key, value);
                result.add(Pair.newImmutableEntry(key, value));
            }
        }
        return result;
    }

    @Override
    public boolean containsKey(K key) {
        return map.containsKey(key);
    }

    @Override
    public boolean containsAllKeys(Collection<K> keys) {
        for (K key : keys) {
            if (!map.containsKey(key)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        return map.entrySet();
    }

    @Override
    public void clear() {

        for (Map.Entry<K, V> entry : map.entrySet()) {
            eventListener.onRemove(this, entry.getKey(), entry.getValue());
        }
        map.clear();
    }

    @Override
    public void setEventListener(IndexEventListener<K, V> eventListener) {
        IndexEventListener<K, V> nullEventListener = SimpleIndex.nullEventListener();
        this.eventListener = (eventListener != null) ? eventListener : nullEventListener;
    }

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public int maxSize() {
        long c = capacity.get();
        return (c > Integer.MAX_VALUE) ? Integer.MAX_VALUE : (int) c;
    }

    @Override
    public String toString() {
        return map.toString();
    }
}
