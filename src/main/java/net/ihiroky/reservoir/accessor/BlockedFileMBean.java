package net.ihiroky.reservoir.accessor;

import javax.management.MXBean;

/**
 * Created on 12/10/31, 16:43
 *
 * @author Hiroki Itoh
 */
@MXBean
public interface BlockedFileMBean {
    String getName();

    String getFilePath();

    long getBytesPerBlock();

    long getFileLength();

    long getLength();

    long getBlocks();

    long getAllocatedBlocks();
}
