package net.ihiroky.reservoir;

import javax.management.MXBean;

/**
 * This class defines a interface of Queue MBean.
 *
 * @author Hiroki Itoh
 */
@MXBean
public interface QueueMBean {

    /**
     * Returns a name of this queue.
     * @return a name of this queue
     */
    String getName();

    /**
     * Returns the number of elements in this queue.
     * @return the number of elements in this queue
     */
    int size();
}
