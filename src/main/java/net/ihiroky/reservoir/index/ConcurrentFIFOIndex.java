package net.ihiroky.reservoir.index;

/**
 * Created on 12/10/15, 19:14
 *
 * @author Hiroki Itoh
 */
public class ConcurrentFIFOIndex<K, V> extends ConcurrentLinkedHashMapIndex<K, V> {

    /**
     * TODO specify concurrentLevel.
     *
     * @param initialCapacity
     * @param capacity
     */
    public ConcurrentFIFOIndex(long initialCapacity, long capacity) {
        super(initialCapacity, capacity, 16, false);
    }
}
