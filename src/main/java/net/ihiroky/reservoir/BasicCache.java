package net.ihiroky.reservoir;

import net.ihiroky.reservoir.index.ConcurrentLinkedHashMapIndex;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * A {@link net.ihiroky.reservoir.Cache} implementation that uses {@link net.ihiroky.reservoir.Index} to manage
 * keys and {@link net.ihiroky.reservoir.CacheAccessor} to store values. All operations are thread-safe if
 * {@link net.ihiroky.reservoir.Index} and {@link net.ihiroky.reservoir.CacheAccessor} are thread-safe.
 *
 * @param <K> the type of keys maintained by this cache.
 * @param <V> the type of mapped values.
 * @author Hiroki Itoh
 */
public class BasicCache<K, V> extends AbstractCache<K, V> implements CacheMBean {

    /** Manages keys and key-value mapping. */
    private Index<K, Ref<V>> index;

    /** stores values to a specified storage.*/
    private CacheAccessor<K, V> cacheAccessor;

    /**
     * {@link net.ihiroky.reservoir.IndexEventListener} to handle cache out and
     * {@link net.ihiroky.reservoir.CompoundCache} operations.
     */
    private RefIndexEventListener refIndexEventListener;

    /**
     * Constructs this object. This cache is registered to the platform MBean server.
     * @param name a name of this cache.
     * @param index an index to manage keys and key-value mapping.
     * @param cacheAccessor a cacheAccessor to store values.
     */
    BasicCache(String name, Index<K, Ref<V>> index, CacheAccessor<K, V> cacheAccessor) {
        super(name);
        this.index = index;
        this.cacheAccessor = cacheAccessor;
        this.refIndexEventListener = new RefIndexEventListener();
        index.setEventListener(refIndexEventListener);
        MBeanSupport.registerMBean(this, getName());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public V get(K key) {
        Ref<V> ref = index.get(key);
        return (ref != null) ? ref.value() : null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<K, V> get(Collection<K> keySet) {
        Collection<Map.Entry<K, Ref<V>>> refs = index.get(keySet);
        Map<K, V> result = new HashMap<K, V>(refs.size());
        for (Map.Entry<K, Ref<V>> entry : refs) {
            result.put(entry.getKey(), entry.getValue().value());
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void put(K key, V value) {
        cacheAccessor.update(key, value, index);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void putAll(Map<K, V> keyValues) {
        cacheAccessor.update(keyValues, index);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public V remove(K key) {
        Ref<V> ref = index.remove(key);
        if (ref == null) {
            return null;
        }
        V value = ref.value();
        cacheAccessor.remove(key, ref);
        return value;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<K, V> remove(Collection<K> keys) {
        Collection<Map.Entry<K, Ref<V>>> refEntries = index.remove(keys);
        Map<K, V> result = new HashMap<K, V>(refEntries.size());
        for (Map.Entry<K, Ref<V>> refEntry : refEntries) {
            result.put(refEntry.getKey(), refEntry.getValue().value());
        }
        cacheAccessor.remove(refEntries);
        return result;
    }

    /**
     * {@inheritDoc}
     */

    @Override
    public void delete(K key) {
        Ref<V> ref = index.remove(key);
        if (ref != null) {
            cacheAccessor.remove(key, ref);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete(Collection<K> keys) {
        Collection<Map.Entry<K, Ref<V>>> refEntries = index.remove(keys);
        cacheAccessor.remove(refEntries);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean containsKey(K key) {
        return index.contains(key);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clear() {
        index.clear();
    }

    /**
     * {@inheritDoc}
     * In addition, MBean registered by the constructor is unregistered.
     */
    @Override
    public void dispose() {
        cacheAccessor.dispose();
        MBeanSupport.unregisterMBean(this, getName());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setIndexEventListener(IndexEventListener<K, Ref<V>> indexEventListener) {
        IndexEventListener<K, Ref<V>> nullListener = nullIndexEventListener();
        refIndexEventListener.nextListener = (indexEventListener != null) ? indexEventListener : nullListener;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean hasConcurrentIndex() {
        return index instanceof ConcurrentLinkedHashMapIndex;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int size() {
        return index.size();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getCacheAccessorClassName() {
        return cacheAccessor.getClass().getName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getIndexClassName() {
        return index.getClass().getName();
    }

    /**
     * {@inheritDoc}
     */
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

    /**
     * An iterator implementation of this cache entries.
     * @param <E> the type of an iteration element.
     */
    private abstract class RefEntryIterator<E> implements Iterator<Map.Entry<K, E>> {

        Iterator<Map.Entry<K, Ref<V>>> base = index.entrySet().iterator();

        /**
         * Returns true if this cache has a next entry.
         * @return true if this cache has a next entry.
         */
        @Override
        public boolean hasNext() {
            return base.hasNext();
        }

        /**
         * Removes a current entry.
         */
        @Override
        public void remove() {
            base.remove();
        }
    }

    /**
     * An event listener to handle events of {@link net.ihiroky.reservoir.Index} held by this cache.
     * On receiving events, {@link net.ihiroky.reservoir.Ref} containing the {@code value} passed to methods
     * is created and is passed to cache event listeners held by this cache. And a next index event listener
     * is called if set in the cache.
     */
    private class RefIndexEventListener implements IndexEventListener<K, Ref<V>> {

        /** {@link net.ihiroky.reservoir.IndexEventListener} to be called after this listener. */
        IndexEventListener<K, Ref<V>> nextListener = nullIndexEventListener();

        /**
         * {@inheritDoc}
         */
        @Override
        public void onPut(Index<K, Ref<V>> index, K key, Ref<V> value) {
            CacheRef<V> cacheRef = new CacheRef<V>(value);
            for (CacheEventListener<K, V> eventListener : eventListenerIterable()) {
                eventListener.onPut(BasicCache.this, key, cacheRef);
            }
            nextListener.onPut(index, key, cacheRef);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onRemove(Index<K, Ref<V>> index, K key, Ref<V> value) {
            CacheRef<V> cacheRef = new CacheRef<V>(value);
            for (CacheEventListener<K, V> eventListener : eventListenerIterable()) {
                eventListener.onRemove(BasicCache.this, key, cacheRef);
            }
            nextListener.onRemove(index, key, cacheRef);
        }

        /**
         * {@inheritDoc}
         *  In addition, the value allocation is released.
         */
        @Override
        public boolean onCacheOut(Index<K, Ref<V>> index, K key, Ref<V> value) {
            CacheRef<V> cacheRef = new CacheRef<V>(value);
            for (CacheEventListener<K, V> eventListener : eventListenerIterable()) {
                eventListener.onCacheOut(BasicCache.this, key, cacheRef);
            }
            boolean result = nextListener.onCacheOut(index, key, cacheRef);
            cacheAccessor.remove(key, value); // cacheAccessor requires raw ref.
            return result;
        }
    }

    /**
     * Restricts to call the original {@code Ref}.
     * @param <V> the type of a value.
     */
    private static class CacheRef<V> implements Ref<V> {

        /** an original ref. */
        Ref<V> ref;

        /** a value stored in the {@code ref}. */
        V cache;

        CacheRef(Ref<V> ref) {
            this.ref = ref;
        }

        /**
         * {@inheritDoc}
         */
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
