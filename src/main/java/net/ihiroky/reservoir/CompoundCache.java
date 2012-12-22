package net.ihiroky.reservoir;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created on 12/10/02, 16:51
 *
 * @author Hiroki Itoh
 */
public final class CompoundCache<K, V> extends AbstractCache<K, V> implements CacheMBean {

    private AbstractCache<K, V> mainCache;
    private AbstractCache<K, V> subCache;
    private boolean promoteOnGet;
    private final ExecutorService demoteExecutor;

    public CompoundCache(String name,
                         AbstractCache<K, V> mainCache, AbstractCache<K, V> subCache) {
        this(name, mainCache, subCache, false, false);
    }

    public CompoundCache(String name,
                         AbstractCache<K, V> mainCache, AbstractCache<K, V> subCache, boolean promoteOnGet, boolean demoteBehind) {
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

    @Override
    public Map<K, V> get(Collection<K> keySet) {
        Map<K, V> retrieved = mainCache.get(keySet);
        int retrievedSize = retrieved.size();
        int requiredSize = keySet.size();
        if (retrievedSize == requiredSize) {
            return retrieved;
        }

        Collection<K> leftKeys;
        if (retrievedSize == 0) {
            leftKeys = keySet;
        } else {
            int left = requiredSize - retrievedSize;
            leftKeys = new ArrayList<K>(left);
            for (K key : keySet) {
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

    @Override
    public void put(K key, V value) {
        mainCache.put(key, value);
    }

    @Override
    public void putAll(Map<K, V> keyValues) {
        mainCache.putAll(keyValues);
    }

    @Override
    public V remove(K key) {
        V result = mainCache.remove(key);
        if (result == null) {
            result = subCache.remove(key);
        }
        return result;
    }

    @Override
    public Map<K, V> remove(Collection<K> keys) {
        Map<K, V> result0 = mainCache.remove(keys);
        Map<K, V> result1 = subCache.remove(keys);
        result0.putAll(result1);
        return result0;
    }

    @Override
    public void delete(K key) {
        mainCache.delete(key);
        subCache.delete(key);
    }

    @Override
    public void delete(Collection<K> keys) {
        mainCache.delete(keys);
        subCache.delete(keys);
    }

    @Override
    public boolean containsKey(K key) {
        return mainCache.containsKey(key) || subCache.containsKey(key);
    }

    @Override
    public Iterator<Map.Entry<K, V>> iterator() {
        return new Iterator<Map.Entry<K, V>>() {
            Iterator<Map.Entry<K, V>> base = mainCache.iterator();
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

    @Override
    public void clear() {
        mainCache.clear();
        subCache.clear();
    }

    @Override
    public void dispose() {
        subCache.dispose();
        mainCache.dispose();
        if (demoteExecutor != null) {
            demoteExecutor.shutdownNow();
        }
        MBeanSupport.unregisterMBean(this, getName());
    }

    @Override
    public int size() {
        return mainCache.size() + subCache.size();
    }

    @Override
    public String getCacheAccessorClassName() {
        return "main:" + mainCache.getCacheAccessorClassName() + ", sub:" + subCache.getCacheAccessorClassName();
    }

    @Override
    public String getIndexClassName() {
        return "main:" + mainCache.getIndexClassName() + ", sub:" + subCache.getIndexClassName();
    }

    @Override
    protected void setIndexEventListener(IndexEventListener<K, Ref<V>> indexEventListener) {
        // demote entry from subCache.
        subCache.setIndexEventListener(indexEventListener);
    }

    @Override
    protected boolean hasConcurrentIndex() {
        return subCache.hasConcurrentIndex();
    }

    private class DemoteEventListener implements IndexEventListener<K, Ref<V>> {

        @Override
        public void onPut(Index<K, Ref<V>> index, K key, Ref<V> ref) {
        }

        @Override
        public void onRemove(Index<K, Ref<V>> index, K key, Ref<V> ref) {
        }

        @Override
        public boolean onCacheOut(final Index<K, Ref<V>> index, final K key, final Ref<V> ref) {
            final V value = ref.value();
            if (demoteExecutor == null) {
                subCache.put(key, value); // TODO put bytes directly if same Coder is used.
                return true;
            }

            demoteExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    // LinkedHashMapIndex doesn't support this code.
                    subCache.put(key, value); // TODO put bytes directly if same Coder is used.
                    index.removeSilently(key, ref);
                }
            });
            return false;
        }
    }
}
