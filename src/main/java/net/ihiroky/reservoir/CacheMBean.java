package net.ihiroky.reservoir;

import javax.management.MXBean;

/**
 * Created on 12/10/17, 17:45
 *
 * @author Hiroki Itoh
 */
@MXBean
public interface CacheMBean {

    /**
     * Returns a name of this cache.
     * @return a name of this cache.
     */
    String getName();

    /**
     * Returns the number of entries in this cache. This is not a MBean property because of a cost of counting up.
     * @return the number of entries.
     */
    int size();

    /**
     * Returns a name of class {@link StorageAccessor} in this cache.
     * @return a name of class {@link StorageAccessor}.
     */
    String getCacheAccessorClassName();

    /**
     * Returns a name of class {@link net.ihiroky.reservoir.Index} in this cache.
     * @return a name of class {@link net.ihiroky.reservoir.Index}.
     */
    String getIndexClassName();

    /**
     * Returns a name of class {@link net.ihiroky.reservoir.StringResolver}
     * to change key to {@code String} in this cache.
     * @return a name of class {@link net.ihiroky.reservoir.StringResolver}.
     */
    String getStringKeyResolverClassName();

    /**
     * Returns a {@code String} value mapped by a key. This method works correctly if and only if an appropriate
     * {@link net.ihiroky.reservoir.StringResolver} for the key is set.
     * @param key a key resolved by a string key resolver.
     * @return a {@code String} value mapped by a key.
     */
    String referEntry(String key);

    /**
     * Removes an entry associated with a key. This method works correctly if and only if an appropriate
     * {@link net.ihiroky.reservoir.StringResolver} for the key is set.
     * @param key a key resolved by a string key resolver.
     */
    void removeEntry(String key);

    /**
     * Returns true if this cache contains an entry associated with a key. This method works correctly
     * if and only if an appropriate {@link net.ihiroky.reservoir.StringResolver} for the key is set.
     * @param key a key resolved by a string key resolver.
     * @return true if this cache contains an entry associated with a key.
     */
    boolean containsEntry(String key);
}
