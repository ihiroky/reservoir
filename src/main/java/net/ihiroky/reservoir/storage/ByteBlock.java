package net.ihiroky.reservoir.storage;

/**
 * Created on 12/10/31, 12:04
 *
 * @author Hiroki Itoh
 */
public interface ByteBlock {
    void free();

    long getBlockIndex();

    long capacity();

    int get(int position);

    int get(int position, byte[] bytes, int offset, int length);

    int put(int position, int b);

    int put(int position, byte[] bytes, int offset, int length);
}
