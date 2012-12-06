package net.ihiroky.reservoir.accessor;

import java.util.Collection;
import java.util.Collections;

/**
 * Created on 12/10/03, 15:57
 *
 * @author Hiroki Itoh
 */
public class NoEnoughFreeBlockException extends IllegalStateException {

    private Collection<Object> failedKeys;

    public NoEnoughFreeBlockException(String s, Throwable t, Collection<Object> failedKeys) {
        super(s, t);
        this.failedKeys = (failedKeys != null) ?
                Collections.unmodifiableCollection(failedKeys) : Collections.<Object>emptyList();
    }

    public Collection<Object> failedKeys() {
        return failedKeys;
    }
}
