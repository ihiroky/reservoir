package net.ihiroky.reservoir;

import java.util.Collection;
import java.util.Map;
import java.util.Properties;

/**
 * Provides for methods to access a storage like files, sets of bytes and so on. The storage is managed by this class
 * and accessed by {@link net.ihiroky.reservoir.Ref} interface.
 *
 * @param <K> the type of objects related to the value that is stored
 * @param <V> the type of value stored in the managed storage
 * @author Hiroki Itoh
 */
public interface StorageAccessor<K, V> {

    /**
     * Creates {@link net.ihiroky.reservoir.Ref} to access a storage managed by this object.
     *
     * @param key an object related to the {@code value}
     * @param value a value stored in the managed storage, accessed by the returned {@code Ref}
     * @return the object that is used to access the stored value
     */
    Ref<V> create(K key, V value);

    /**
     * Updates {@link net.ihiroky.reservoir.Ref} with a specified {@code value}. If {@code Ref} is held by a specified
     * {@code index}, the {@code value} is stored in the {@code Ref}. Otherwise, create a new {@code Ref}.
     *
     * @param key a key associated to the {@code value} by the {@code index}
     * @param value a value stored in the managed storage
     * @param index an object that manages key - {@code Ref} mappings
     * @return true if the {@code value} is successfully updated
     */
    boolean update(K key, V value, Index<K, Ref<V>> index);

    /**
     * Updates {@link} with specified values mapped by {@code keyValues}. If {@code Ref}s are held by a specified
     * {@code index}, the {@code value}s are stored in the {@code Ref}. Otherwise, create new {@code Ref}s.
     *
     * @param keyValues mappings that holds values to be stored and keys associated with these values
     * @param index an object that manages key - {@code Ref} mappings
     */
    void update(Map<K, V> keyValues, Index<K, Ref<V>> index);

    /**
     * Releases a value accessed through a specified {@link net.ihiroky.reservoir.Ref}.
     *
     * @param key an object related to a value accessed through the {@code ref}
     * @param ref the object that is used to access the stored value
     */
    void remove(K key, Ref<V> ref);

    /**
     * Releases values accessed through specified {@link net.ihiroky.reservoir.Ref}s.
     *
     * @param refs a collection that holds the {@code Ref}s and keys associated with the {@code Refs}.
     */
    void remove(Collection<Map.Entry<K, Ref<V>>> refs);

    /**
     * Prepares to use this object. The managed storage is set up and got ready to be accessed.
     * This method is required to be called earlier than any other methods in this object.
     *
     * @param name a name of the managed storage
     * @param props properties that have set up parameters
     */
    void prepare(String name, Properties props);

    /**
     * Puts off the managed storage.
     * This method required to be called if this object is no longer needed.
     */
    void dispose();
}
