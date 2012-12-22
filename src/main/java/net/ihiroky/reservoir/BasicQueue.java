package net.ihiroky.reservoir;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * An implementation of {@link net.ihiroky.reservoir.AbstractBasicQueue} using {@code java.util.ConcurrentLinkedQueue}.
 *
 * @author Hiroki Itoh
 */
public class BasicQueue<E> extends AbstractBasicQueue<E, ConcurrentLinkedQueue<Ref<E>>> {


    /**
     * Constructs this object.
     * @param name a name of this queue.
     * @param cacheAccessor  a manager that allocates and releases elements store.
     */
    BasicQueue(String name, CacheAccessor<Object, E> cacheAccessor) {
        super(name, cacheAccessor, new ConcurrentLinkedQueue<Ref<E>>());
    }
}
