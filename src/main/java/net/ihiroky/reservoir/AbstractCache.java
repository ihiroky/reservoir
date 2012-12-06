package net.ihiroky.reservoir;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Pattern;

/**
 * Created on 12/10/03, 0:44
 * TODO bytes direct get/put
 *
 * @author Hiroki Itoh
 */
public abstract class AbstractCache<K, V> implements Cache<K, V> {

    private final String name;
    private List<CacheEventListener<K, V>> eventListenerList;
    private StringResolver<K> keyResolver;

    @SuppressWarnings("unchecked")
    static <K, V> IndexEventListener<K, Ref<V>> nullIndexEventListener() {
        return (IndexEventListener<K, Ref<V>>) IndexEventListener.NULL_LISTENER;
    }

    @SuppressWarnings("unchecked")
    static <K> StringResolver<K> nullStringKeyResolver() {
        return (StringResolver<K>) StringResolver.NULL;
    }

    AbstractCache(String name) {
        this.name = String.valueOf(name);
        this.eventListenerList = new CopyOnWriteArrayList<CacheEventListener<K, V>>();
        this.keyResolver = nullStringKeyResolver();
    }

    protected Iterable<CacheEventListener<K, V>> eventListenerIterable() {
        return eventListenerList;
    }

    abstract protected void setIndexEventListener(IndexEventListener<K, Ref<V>> indexEventListener);

    abstract protected boolean hasConcurrentIndex();

    @Override
    public void addEventListener(CacheEventListener<K, V> eventListener) {
        if (eventListener != null) {
            eventListenerList.add(eventListener);
        }
    }

    @Override
    public void removeEventListener(CacheEventListener<K, V> eventListener) {
        eventListenerList.remove(eventListener);
    }

    protected void clearEventListener() {
        eventListenerList.clear();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getStringKeyResolverClassName() {
        return (keyResolver != null) ? keyResolver.getClass().getName() : null;
    }

    @Override
    public String referEntry(String key) {
        K k = keyResolver.resolve(key);
        V value = (k != null) ? get(k) : null;
        return (value != null) ? String.valueOf(value) : null;
    }

    @Override
    public void removeEntry(String key) {
        K k = keyResolver.resolve(key);
        if (k != null) {
            remove(k);
        }
    }

    @Override
    public boolean containsEntry(String key) {
        K k = keyResolver.resolve(key);
        return (k != null) && containsKey(k);
    }

    public K resolveKey(String stringKey) {
        return keyResolver.resolve(stringKey);
    }

    @SuppressWarnings("unchecked")
    public void setStringKeyResolver(StringResolver<K> resolver) {
        keyResolver = (resolver != null) ? resolver : (StringResolver<K>) nullStringKeyResolver();
    }

    @Override
    public void writeTo(OutputStream outputStream, StreamingCoder<K, V> coder) throws Exception {
        coder.write(Pattern.compile(".*"), this, outputStream);
    }

    @Override
    public void readFrom(InputStream inputStream, StreamingCoder<K, V> coder) throws Exception {
        coder.read(this, inputStream);
    }
}
