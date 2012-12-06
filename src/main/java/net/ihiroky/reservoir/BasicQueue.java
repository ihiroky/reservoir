package net.ihiroky.reservoir;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created on 12/10/16, 23:33
 *
 * @author Hiroki Itoh
 */
public class BasicQueue<E> extends AbstractBasicQueue<E, ConcurrentLinkedQueue<Ref<E>>> {


    BasicQueue(String name, CacheAccessor<Object, E> cacheAccessor) {
        super(name, cacheAccessor, new ConcurrentLinkedQueue<Ref<E>>());
    }
}
