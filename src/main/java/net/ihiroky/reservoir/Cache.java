package net.ihiroky.reservoir;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

/**
 * Created on 12/09/18, 20:15
 * <p/>
 * TODO keySet, entrySet implementation.
 *
 * @author Hiroki Itoh
 */
public interface Cache<K, V> extends CacheMBean, Iterable<Map.Entry<K, V>> {

    V get(K key);

    Map<K, V> get(Collection<K> keys);

    void put(K key, V value);

    void put(Map<K, V> keyValues);

    void remove(K key);

    void remove(Collection<K> keys);

    V poll(K key);

    Map<K, V> poll(Collection<K> keys);

    boolean containsKey(K key);

    Iterator<Map.Entry<K, V>> iterator();

    void clear();

    void dispose();

    void addEventListener(CacheEventListener<K, V> eventListener);

    void removeEventListener(CacheEventListener<K, V> eventListener);

    void setStringKeyResolver(StringResolver<K> resolver);

    int size();

    String getName();

    void writeTo(OutputStream outputStream, StreamingCoder<K, V> coder) throws Exception;

    void readFrom(InputStream inputStream, StreamingCoder<K, V> coder) throws Exception;
}
