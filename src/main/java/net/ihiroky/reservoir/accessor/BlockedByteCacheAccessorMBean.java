package net.ihiroky.reservoir.accessor;

import javax.management.MXBean;

/**
 * Created on 12/10/17, 18:08
 *
 * @author Hiroki Itoh
 */
@MXBean
public interface BlockedByteCacheAccessorMBean {

    String getName();

    long getAllocatedBlocks();

    long getWholeBlocks();

    int getPartitions();

    String getEncoderClassName();

    String getDecoderClassName();
}
