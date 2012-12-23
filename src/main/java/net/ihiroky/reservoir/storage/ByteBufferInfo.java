package net.ihiroky.reservoir.storage;

/**
 * @author Hiroki Itoh
 */
public class ByteBufferInfo {

    boolean direct;
    int capacity;

    private static final int DEFAULT_CAPACITY = 1048576;

    public ByteBufferInfo(boolean direct, int capacity) {
        this.direct = direct;
        this.capacity = capacity;
    }

    ByteBufferInfo() {
        direct = false;
        capacity = DEFAULT_CAPACITY;
    }
}
