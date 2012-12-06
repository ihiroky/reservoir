package net.ihiroky.reservoir.index;

/**
 * Created on 12/09/18, 16:02
 *
 * @author Hiroki Itoh
 */
public class FIFOIndex<K, V> extends LinkedHashMapIndex<K, V> {

    public FIFOIndex() {
        super(8192, Integer.MAX_VALUE, false);
    }

    public FIFOIndex(int initialCapacity, int capacity) {
        super(initialCapacity, capacity, false);
    }
}
