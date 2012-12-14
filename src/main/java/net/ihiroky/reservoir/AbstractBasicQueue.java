package net.ihiroky.reservoir;

import java.util.AbstractQueue;
import java.util.Iterator;
import java.util.Queue;

/**
 * Created on 12/10/16, 23:33
 *
 * @author Hiroki Itoh
 */
abstract class AbstractBasicQueue<E, Q extends Queue<Ref<E>>> extends AbstractQueue<E> implements QueueMBean {

    private String name;
    protected CacheAccessor<Object, E> cacheAccessor;
    protected Q refQueue;

    AbstractBasicQueue(String name, CacheAccessor<Object, E> cacheAccessor, Q queue) {
        if (cacheAccessor == null) {
            throw new NullPointerException("cacheAccessor must not be null.");
        }
        if (name == null) {
            throw new NullPointerException("name must not be null.");
        }
        if (name.length() == 0) {
            throw new IllegalArgumentException("name must not be empty.");
        }

        this.name = name;
        this.cacheAccessor = cacheAccessor;
        this.refQueue = queue;
        MBeanSupport.registerMBean(this, name);
    }

    public void dispose() {
        cacheAccessor.dispose();
        MBeanSupport.unregisterMBean(this, name);
    }

    @Override
    public Iterator<E> iterator() {
        return new Iterator<E>() {
            Iterator<Ref<E>> base = refQueue.iterator();
            Ref<E> current = null;

            @Override
            public boolean hasNext() {
                return base.hasNext();
            }

            @Override
            public E next() {
                current = base.next();
                return current.value();
            }

            @Override
            public void remove() {
                base.remove();
                cacheAccessor.remove(null, current);
            }
        };
    }

    @Override
    public int size() {
        return refQueue.size();
    }

    @Override
    public boolean offer(E e) {
        if (e == null) {
            throw new NullPointerException("e must not be null.");
        }

        Ref<E> ref = cacheAccessor.create(null, e);
        if (ref != null) {
            if (refQueue.offer(ref)) {
                return true;
            }
            cacheAccessor.remove(null, ref);
        }
        return false;
    }

    @Override
    public E poll() {
        Ref<E> ref = refQueue.poll();
        if (ref == null) {
            return null;
        }
        E e = ref.value();
        cacheAccessor.remove(null, ref);
        return e;
    }

    @Override
    public E peek() {
        Ref<E> ref = refQueue.peek();
        return (ref != null) ? ref.value() : null;
    }

    @Override
    public boolean isEmpty() {
        return refQueue.isEmpty();
    }

    @Override
    public String getName() {
        return name;
    }
}
