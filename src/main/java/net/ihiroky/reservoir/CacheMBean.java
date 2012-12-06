package net.ihiroky.reservoir;

import javax.management.MXBean;

/**
 * Created on 12/10/17, 17:45
 *
 * @author Hiroki Itoh
 */
@MXBean
public interface CacheMBean {

    String getName();

    int size();

    String getCacheAccessorClassName();

    String getIndexClassName();

    String getStringKeyResolverClassName();

    String referEntry(String key);

    void removeEntry(String key);

    boolean containsEntry(String key);
}
