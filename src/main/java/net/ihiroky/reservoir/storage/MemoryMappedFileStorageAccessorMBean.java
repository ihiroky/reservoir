package net.ihiroky.reservoir.storage;

import javax.management.MXBean;

/**
 * Created on 12/10/31, 18:43
 *
 * @author Hiroki Itoh
 */
@MXBean
public interface MemoryMappedFileStorageAccessorMBean extends BlockedByteStorageAccessorMBean {
    void sync();
}
