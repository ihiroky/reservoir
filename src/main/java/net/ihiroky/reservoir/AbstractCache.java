package net.ihiroky.reservoir;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Pattern;

/**
 * A cache (key-value store) that stores values in a space prepared by {@link CacheAccessor}. The key is held in
 * Java heap. The reference of the element store is held by {@link net.ihiroky.reservoir.Cache}, which defines the
 * cache algorithm.
 *
 * TODO bytes direct get/put
 *
 * @param <K> the type of keys maintained by this cache.
 * @param <V> the type of mapped values.
 * @author Hiroki Itoh
 */
public abstract class AbstractCache<K, V> implements Cache<K, V> {

    /** A name of this cache. */
    private final String name;

    /** Listeners to listen this cache events. */
    private List<CacheEventListener<K, V>> eventListenerList;

    /** Changes a key to String. */
    private StringResolver<K> keyResolver = nullStringKeyResolver();

    /**
     * Returns {@link net.ihiroky.reservoir.IndexEventListener#NULL_LISTENER} with a specified type.
     * @param <K> the type of a key.
     * @param <V> the type of a value.
     * @return {@link net.ihiroky.reservoir.IndexEventListener#NULL_LISTENER} with a specified type.
     */
    @SuppressWarnings("unchecked")
    static <K, V> IndexEventListener<K, Ref<V>> nullIndexEventListener() {
        return (IndexEventListener<K, Ref<V>>) IndexEventListener.NULL_LISTENER;
    }

    /**
     * Returns {@link net.ihiroky.reservoir.StringResolver#NULL} with a specified type.
     * @param <K> the type of a key.
     * @return {@link net.ihiroky.reservoir.StringResolver#NULL} with a specified type.
     */
    @SuppressWarnings("unchecked")
    static <K> StringResolver<K> nullStringKeyResolver() {
        return (StringResolver<K>) StringResolver.NULL;
    }

    /**
     * Constructs this instance.
     * @param name A name of this cache.
     */
    AbstractCache(String name) {
        this.name = String.valueOf(name);
        this.eventListenerList = new CopyOnWriteArrayList<CacheEventListener<K, V>>();
        this.keyResolver = nullStringKeyResolver();
    }

    /**
     * Returns {@link net.ihiroky.reservoir.CacheEventListener}s.
     * @return {@link net.ihiroky.reservoir.CacheEventListener}s.
     */
    protected Iterable<CacheEventListener<K, V>> eventListenerIterable() {
        return eventListenerList;
    }

    /**
     * Sets {@link net.ihiroky.reservoir.IndexEventListener} to chain to.
     * @param indexEventListener {@link net.ihiroky.reservoir.IndexEventListener} to chain to.
     */
    abstract protected void setIndexEventListener(IndexEventListener<K, Ref<V>> indexEventListener);

    /**
     * Returns true if an index used by this cache is concurrent.
     * @return true if an index used by this cache is concurrent.
     */
    abstract protected boolean hasConcurrentIndex();

    /**
     * {@inheritDoc}
     */
    @Override
    public void addEventListener(CacheEventListener<K, V> eventListener) {
        if (eventListener != null) {
            eventListenerList.add(eventListener);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeEventListener(CacheEventListener<K, V> eventListener) {
        eventListenerList.remove(eventListener);
    }

    /**
     * Clears {@link net.ihiroky.reservoir.CacheEventListener} managed in this cache.
     */
    protected void clearEventListener() {
        eventListenerList.clear();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getStringKeyResolverClassName() {
        return (keyResolver != null) ? keyResolver.getClass().getName() : null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String referEntry(String key) {
        K k = keyResolver.resolve(key);
        V value = (k != null) ? get(k) : null;
        return (value != null) ? String.valueOf(value) : null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeEntry(String key) {
        K k = keyResolver.resolve(key);
        if (k != null) {
            remove(k);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean containsEntry(String key) {
        K k = keyResolver.resolve(key);
        return (k != null) && containsKey(k);
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public void setStringKeyResolver(StringResolver<K> resolver) {
        keyResolver = (resolver != null) ? resolver : (StringResolver<K>) nullStringKeyResolver();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeTo(OutputStream outputStream, StreamingCoder<K, V> coder) throws Exception {
        coder.write(Pattern.compile(".*"), this, outputStream);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void readFrom(InputStream inputStream, StreamingCoder<K, V> coder) throws Exception {
        coder.read(this, inputStream);
    }
}
