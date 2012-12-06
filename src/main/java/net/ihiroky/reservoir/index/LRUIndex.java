package net.ihiroky.reservoir.index;

/**
 * Created on 12/09/18, 16:01
 *
 * @author Hiroki Itoh
 */
public class LRUIndex<K, V> extends LinkedHashMapIndex<K, V> {

    public LRUIndex() {
        super(8192, Integer.MAX_VALUE, true);
    }

    public LRUIndex(int initialCapacity, int capacity) {
        super(initialCapacity, capacity, true);
    }
}
