package net.ihiroky.reservoir;

import javax.management.MXBean;

/**
 * Created on 12/10/18, 18:34
 *
 * @author Hiroki Itoh
 */
@MXBean
public interface QueueMBean {

    String getName();

    int size();
}
