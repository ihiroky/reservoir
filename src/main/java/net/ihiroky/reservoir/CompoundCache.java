package net.ihiroky.reservoir;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A {@link net.ihiroky.reservoir.Cache} implementation that consists of a main cache and a sub cache.
 *
 * This cache accesses the main cache first and then the sub cache on a get and put operations.
 * The main cache is only accessed on the get operation if an entry exists in the main cache; otherwise,
 * the sub cache is accessed. And so as the put operation (if an entry can store in the main cache or not).
 * On remove and count operation, both the main and sub cache sequentially. The main cache often has a fast
 * and size limited storage, and the sub cache has a slow and large one.
 *
 * Two flags control entries transfer between the main and sub cache. The {@code promoteOnGet} controls that
 * an entry is transferred from the sub to main cache on the get operation if the entry exists only
 * in the sub cache. The {@code demoteBehind} controls whether or not to use a dedicated thread that supports
 * a entry transfer to push out from the main to sub cache on the put operation because of the main cache size
 * limitation if available.
 *
 * @author Hiroki Itoh
 */
public final class CompoundCache<K, V> extends AbstractCache<K, V> implements CacheMBean {

    /** a main cache */
    private AbstractCache<K, V> mainCache;

    /** a sub cache */
    private AbstractCache<K, V> subCache;

    /** a flag controls a entry transfer between the sub to main cache on the get operation */
    private boolean promoteOnGet;

    /** an executor to execute an entry demotion */
    private final ExecutorService demoteExecutor;

    /**
     * Constructs this object. In other words, this constructor behaves exactly as if it simply
     * performs the call {@code CompoundCache(name, mainCache, subCache, false, false)}.
     *
     * @param name a name of this cache.
     * @param mainCache a main cache.
     * @param subCache a sub cache.
     */
    public CompoundCache(String name,
                         AbstractCache<K, V> mainCache, AbstractCache<K, V> subCache) {
        this(name, mainCache, subCache, false, false);
    }

    /**
     * Constructs this object with flags. This cache is registered to the platform MBean server.
     *
     * @param name a name of this cache
     * @param mainCache a main cache.
     * @param subCache a sub cache.
     * @param promoteOnGet true if entries can be transferred from sub to main on the get operation.
     * @param demoteBehind true if use a dedicate thread on demoting entries fro main to sub on the put operation.
     */
    public CompoundCache(String name, AbstractCache<K, V> mainCache, AbstractCache<K, V> subCache,
                         boolean promoteOnGet, boolean demoteBehind) {
        super(name);
        if (mainCache == null) {
            throw new NullPointerException("mainCache must not be null.");
        }
        if (subCache == null) {
            throw new NullPointerException("subCache must not be null.");
        }

        mainCache.clearEventListener();
        subCache.clearEventListener();
        mainCache.setIndexEventListener(new DemoteEventListener());

        this.mainCache = mainCache;
        this.subCache = subCache;
        this.promoteOnGet = promoteOnGet;
        if (demoteBehind && (!mainCache.hasConcurrentIndex())) {
            throw new IllegalArgumentException("mainCache must have concurrent index when demoteBehind is true.");
        }
        demoteExecutor = demoteBehind ?
                Executors.newSingleThreadExecutor(new CountThreadFactory("CompoundCacheDemotion")) : null;

        MBeanSupport.registerMBean(this, getName());
    }

    /**
     * {@inheritDoc}
     * The main cache is accessed first, and then the sub cache accessed if the main cache does not have
     * a mapped value specified with the associated key. If {@code promoteOnGet} gets true in the constructor,
     * a entry can be transferred from sub to main if the entry exists only in sub.
     */
    @Override
    public V get(K key) {
        V value = mainCache.get(key);
        if (value != null) {
            return value;
        }
        value = subCache.get(key);
        if (promoteOnGet && value != null) {
            mainCache.put(key, value);
            subCache.delete(key);
        }
        return value;
    }

    /**
     * {@inheritDoc}
     * The main cache is accessed first, and then the sub cache accessed if the main cache does not have
     * all mapped values specified with the associated keys. If {@code promoteOnGet} gets true in the constructor,
     * a entry can be transferred from sub to main if the entry exists only in sub.
     */
    @Override
    public Map<K, V> get(Collection<K> keys) {
        Map<K, V> retrieved = mainCache.get(keys);
        int retrievedSize = retrieved.size();
        int requiredSize = keys.size();
        if (retrievedSize == requiredSize) {
            return retrieved;
        }

        Collection<K> leftKeys;
        if (retrievedSize == 0) {
            leftKeys = keys;
        } else {
            int left = requiredSize - retrievedSize;
            leftKeys = new ArrayList<K>(left);
            for (K key : keys) {
                if (!retrieved.containsKey(key)) {
                    leftKeys.add(key);
                    if (--left == 0) {
                        break;
                    }
                }
            }
        }
        Map<K, V> result = subCache.get(leftKeys);
        if (promoteOnGet && result.size() > 0) {
            mainCache.putAll(result);
            subCache.delete(leftKeys);
        }
        result.putAll(retrieved);
        return result;
    }

    /**
     * {@inheritDoc}
     * If the main cache is size limited, cache-out entries are transferred from main to sub.
     */
    @Override
    public void put(K key, V value) {
        mainCache.put(key, value);
    }

    /**
     * {@inheritDoc}
     * If the main cache is size limited, cache-out entries are transferred from main to sub.
     */
    @Override
    public void putAll(Map<K, V> keyValues) {
        mainCache.putAll(keyValues);
    }

    /**
     * {@inheritDoc}
     * An entry specified with {@code key} is removed from both main and sub.
     */
    @Override
    public V remove(K key) {
        V result = mainCache.remove(key);
        if (result == null) {
            result = subCache.remove(key);
        }
        return result;
    }

    /**
     * {@inheritDoc}
     * Entries specified with {@code keys} are removed from both main and sub.
     */
    @Override
    public Map<K, V> remove(Collection<K> keys) {
        Map<K, V> result0 = mainCache.remove(keys);
        Map<K, V> result1 = subCache.remove(keys);
        result0.putAll(result1);
        return result0;
    }

    /**
     * {@inheritDoc}
     * An entry specified with {@code key} is removed from both main and sub.
     */
    @Override
    public void delete(K key) {
        mainCache.delete(key);
        subCache.delete(key);
    }

    /**
     * {@inheritDoc}
     * Entries specified with {@code keys} are removed from both main and sub.
     */
    @Override
    public void delete(Collection<K> keys) {
        mainCache.delete(keys);
        subCache.delete(keys);
    }

    /**
     * {@inheritDoc}
     * @return true if either main or sub has a mapping specified with {@code key}
     */
    @Override
    public boolean containsKey(K key) {
        return mainCache.containsKey(key) || subCache.containsKey(key);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Iterator<Map.Entry<K, V>> iterator() {
        return new Iterator<Map.Entry<K, V>>() {

            /** an iterator over the cache entries */
            Iterator<Map.Entry<K, V>> base = mainCache.iterator();

            /** flag whether the current base iterator is for main or not */
            boolean main = true;

            @Override
            public boolean hasNext() {
                if (main) {
                    if (base.hasNext()) {
                        return true;
                    }
                    base = subCache.iterator();
                    main = false;
                }
                return base.hasNext();
            }

            @Override
            public Map.Entry<K, V> next() {
                return base.next();
            }

            @Override
            public void remove() {
                base.remove();
            }
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clear() {
        mainCache.clear();
        subCache.clear();
    }

    /**
     * Disposes main cache, sub cache. A demoting thread is stopped if enabled. The registration to platform Mbean
     * server is also unregistered.
     *
     */
    @Override
    public void dispose() {
        subCache.dispose();
        mainCache.dispose();
        if (demoteExecutor != null) {
            demoteExecutor.shutdownNow();
        }
        MBeanSupport.unregisterMBean(this, getName());
    }

    /**
     * {@inheritDoc}
     * @return the number of total entries in main and sub cache
     */
    @Override
    public int size() {
        return mainCache.size() + subCache.size();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getCacheAccessorClassName() {
        return "main:" + mainCache.getCacheAccessorClassName() + ", sub:" + subCache.getCacheAccessorClassName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getIndexClassName() {
        return "main:" + mainCache.getIndexClassName() + ", sub:" + subCache.getIndexClassName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setIndexEventListener(IndexEventListener<K, Ref<V>> indexEventListener) {
        // demote entry from subCache.
        subCache.setIndexEventListener(indexEventListener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean hasConcurrentIndex() {
        return subCache.hasConcurrentIndex();
    }

    /**
     * Listens index events to control the entry demotion.
     */
    private class DemoteEventListener implements IndexEventListener<K, Ref<V>> {

        /**
         * {@inheritDoc}
         * Do nothing.
         */
        @Override
        public void onPut(Index<K, Ref<V>> index, K key, Ref<V> ref) {
        }

        /**
         * {@inheritDoc}
         * Do nothing.
         */
        @Override
        public void onRemove(Index<K, Ref<V>> index, K key, Ref<V> ref) {
        }

        /**
         * {@inheritDoc}
         * Executes the entry demotion.
         */
        @Override
        public boolean onDiscard(final Index<K, Ref<V>> index, final K key, final Ref<V> ref) {
            final V value = ref.value();
            if (demoteExecutor == null) {
                subCache.put(key, value); // TODO put bytes directly if the same Coder is used.
                return true;
            }

            demoteExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    // LinkedHashMapIndex doesn't support this code.
                    subCache.put(key, value); // TODO put bytes directly if the same Coder is used.
                    index.removeSilently(key, ref);
                }
            });
            return false;
        }
    }
}
