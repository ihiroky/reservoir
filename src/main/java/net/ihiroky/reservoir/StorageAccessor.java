package net.ihiroky.reservoir;

import java.util.Collection;
import java.util.Map;
import java.util.Properties;

/**
 * Created on 12/09/18, 18:58
 *
 * @author Hiroki Itoh
 */
public interface StorageAccessor<K, V> {

    Ref<V> create(K key, V value);

    boolean update(K key, V value, Index<K, Ref<V>> index);

    void update(Map<K, V> keyValues, Index<K, Ref<V>> index);

    void remove(K key, Ref<V> ref);

    void remove(Collection<Map.Entry<K, Ref<V>>> refs);

    void prepare(String name, Properties props);

    void dispose();
}