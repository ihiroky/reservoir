package net.ihiroky.reservoir;

import net.ihiroky.reservoir.index.ConcurrentLinkedHashMapIndex;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created on 12/09/18, 20:17
 *
 * @author Hiroki Itoh
 */
public class BasicCache<K, V> extends AbstractCache<K, V> implements CacheMBean {

    private Index<K, Ref<V>> index;
    private CacheAccessor<K, V> cacheAccessor;
    private RefIndexEventListener refIndexEventListener;

    BasicCache(String name, Index<K, Ref<V>> index, CacheAccessor<K, V> cacheAccessor) {
        super(name);
        this.index = index;
        this.cacheAccessor = cacheAccessor;
        this.refIndexEventListener = new RefIndexEventListener();
        index.setEventListener(refIndexEventListener);
        MBeanSupport.registerMBean(this, getName());
    }

    @Override
    public V get(K key) {
        Ref<V> ref = index.get(key);
        return (ref != null) ? ref.value() : null;
    }

    @Override
    public Map<K, V> get(Collection<K> keySet) {
        Collection<Map.Entry<K, Ref<V>>> refs = index.get(keySet);
        Map<K, V> result = new HashMap<K, V>(refs.size());
        for (Map.Entry<K, Ref<V>> entry : refs) {
            result.put(entry.getKey(), entry.getValue().value());
        }
        return result;
    }

    @Override
    public void put(K key, V value) {
        cacheAccessor.update(key, value, index);
    }

    @Override
    public void put(Map<K, V> keyValues) {
        cacheAccessor.update(keyValues, index);
    }

    @Override
    public void remove(K key) {
        Ref<V> ref = index.remove(key);
        cacheAccessor.remove(key, ref);
    }

    @Override
    public void remove(Collection<K> keys) {
        Collection<Map.Entry<K, Ref<V>>> refEntries = index.remove(keys);
        cacheAccessor.remove(refEntries);
    }

    @Override
    public V poll(K key) {
        Ref<V> ref = index.remove(key);
        if (ref == null) {
            return null;
        }
        V value = ref.value();
        cacheAccessor.remove(key, ref);
        return value;
    }

    @Override
    public Map<K, V> poll(Collection<K> keys) {
        Collection<Map.Entry<K, Ref<V>>> refEntries = index.remove(keys);
        Map<K, V> result = new HashMap<K, V>(refEntries.size());
        for (Map.Entry<K, Ref<V>> refEntry : refEntries) {
            result.put(refEntry.getKey(), refEntry.getValue().value());
        }
        cacheAccessor.remove(refEntries);
        return result;
    }

    @Override
    public boolean containsKey(K key) {
        return index.contains(key);
    }

    @Override
    public void clear() {
        index.clear();
    }

    @Override
    public void dispose() {
        cacheAccessor.dispose();
        MBeanSupport.unregisterMBean(this, getName());
    }

    @Override
    protected void setIndexEventListener(IndexEventListener<K, Ref<V>> indexEventListener) {
        IndexEventListener<K, Ref<V>> nullListener = nullIndexEventListener();
        refIndexEventListener.nextListener = (indexEventListener != null) ? indexEventListener : nullListener;
    }

    @Override
    protected boolean hasConcurrentIndex() {
        return index instanceof ConcurrentLinkedHashMapIndex;
    }

    @Override
    public int size() {
        return index.size();
    }

    @Override
    public String getCacheAccessorClassName() {
        return cacheAccessor.getClass().getName();
    }

    @Override
    public String getIndexClassName() {
        return index.getClass().getName();
    }

    @Override
    public Iterator<Map.Entry<K, V>> iterator() {
        return new RefEntryIterator<V>() {
            @Override
            public Map.Entry<K, V> next() {
                Map.Entry<K, Ref<V>> entry = base.next();
                return Pair.newImmutableEntry(entry.getKey(), entry.getValue().value());
            }
        };
    }

    private abstract class RefEntryIterator<E> implements Iterator<Map.Entry<K, E>> {

        Iterator<Map.Entry<K, Ref<V>>> base = index.entrySet().iterator();

        @Override
        public boolean hasNext() {
            return base.hasNext();
        }

        @Override
        public void remove() {
            base.remove();
        }
    }

    private class RefIndexEventListener implements IndexEventListener<K, Ref<V>> {

        IndexEventListener<K, Ref<V>> nextListener = nullIndexEventListener();

        @Override
        public void onPut(Index<K, Ref<V>> index, K key, Ref<V> value) {
            CacheRef<V> cacheRef = new CacheRef<V>(value);
            for (CacheEventListener<K, V> eventListener : eventListenerIterable()) {
                eventListener.onPut(BasicCache.this, key, cacheRef);
            }
            nextListener.onPut(index, key, cacheRef);
        }

        @Override
        public void onRemove(Index<K, Ref<V>> index, K key, Ref<V> value) {
            CacheRef<V> cacheRef = new CacheRef<V>(value);
            for (CacheEventListener<K, V> eventListener : eventListenerIterable()) {
                eventListener.onRemove(BasicCache.this, key, cacheRef);
            }
            nextListener.onRemove(index, key, cacheRef);
        }

        @Override
        public boolean onCacheOut(Index<K, Ref<V>> index, K key, Ref<V> value) {
            CacheRef<V> cacheRef = new CacheRef<V>(value);
            for (CacheEventListener<K, V> eventListener : eventListenerIterable()) {
                eventListener.onCacheOut(BasicCache.this, key, cacheRef);
            }
            boolean result = nextListener.onCacheOut(index, key, cacheRef);
            cacheAccessor.remove(key, value); // cacheAccessor must use raw ref.
            return result;
        }
    }

    static class CacheRef<V> implements Ref<V> {

        Ref<V> ref;
        V cache;

        CacheRef(Ref<V> ref) {
            this.ref = ref;
        }

        @Override
        public V value() {
            synchronized (this) {
                if (cache == null) {
                    cache = ref.value();
                }
            }
            return cache;
        }
    }
}
