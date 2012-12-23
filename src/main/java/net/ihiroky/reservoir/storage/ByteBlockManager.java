package net.ihiroky.reservoir.storage;

import java.util.List;

/**
 * Created on 12/10/31, 12:03
 *
 * @author Hiroki Itoh
 */
public interface ByteBlockManager {
    ByteBlock allocate();

    void free();

    boolean hasFreeBlock();

    List<Number> freeBlockListView();

    byte get(long index) throws Exception;

    String getName();

    long getBlocks();

    long getAllocatedBlocks();
}
