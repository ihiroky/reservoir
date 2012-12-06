package net.ihiroky.reservoir;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Created on 12/09/18, 14:58
 * <p/>
 * TODO timed expiration index.
 *
 * @author Hiroki Itoh
 */
public interface Index<K, V> {

    V get(K key);

    Collection<Map.Entry<K, V>> get(Collection<K> keys);

    V put(K key, V value);

    V putIfAbsent(K key, V value);

    void put(Collection<Map.Entry<K, V>> keyValues);

    V remove(K key);

    void removeSilently(K key, V value);

    Collection<Map.Entry<K, V>> remove(Collection<K> keys);

    boolean contains(K key);

    boolean containsAll(Collection<K> keys);

    Set<Map.Entry<K, V>> entrySet();

    void clear();

    void setEventListener(IndexEventListener<K, V> eventListener);

    int size();

    int maxSize();
}
