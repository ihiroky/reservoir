package net.ihiroky.reservoir.storage;

import javax.management.MXBean;

/**
 * Created on 12/10/17, 18:15
 *
 * @author Hiroki Itoh
 */
@MXBean
public interface BlockedByteBufferMBean {

    String getName();

    int getCapacity();

    int getLimit();

    int getBytesPerBlock();

    int getLength();

    long getBlocks();

    long getAllocatedBlocks();
}
