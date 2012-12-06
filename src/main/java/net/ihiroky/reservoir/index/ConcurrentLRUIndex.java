package net.ihiroky.reservoir.index;

/**
 * Created on 12/10/15, 19:13
 *
 * @author Hiroki Itoh
 */
public class ConcurrentLRUIndex<K, V> extends ConcurrentLinkedHashMapIndex<K, V> {

    /**
     * TODO specify concurrentLevel.
     *
     * @param initialCapacity
     * @param capacity
     */
    public ConcurrentLRUIndex(long initialCapacity, long capacity) {
        super(initialCapacity, capacity, 16, true);
    }
}
