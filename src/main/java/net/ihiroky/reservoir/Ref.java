package net.ihiroky.reservoir;

/**
 * A {@code Ref} is a reference of a stored object that is maintained by {@link net.ihiroky.reservoir.StorageAccessor}.
 *
 * @author Hiroki Itoh
 */
public interface Ref<V> {

    /**
     * Returns a value referenced by this instance.
     * @return a value referenced by this instance
     */
    V value();

}
