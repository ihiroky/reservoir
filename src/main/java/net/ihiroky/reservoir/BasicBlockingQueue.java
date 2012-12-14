package net.ihiroky.reservoir;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * @author Hiroki Itoh
 */
public class BasicBlockingQueue<E> extends AbstractBasicQueue<E, LinkedBlockingQueue<Ref<E>>>
        implements BlockingQueue<E> {

    private final Object mutex = new Object();

    BasicBlockingQueue(String name, CacheAccessor<Object, E> cacheAccessor) {
        super(name, cacheAccessor, new LinkedBlockingQueue<Ref<E>>());
    }

    BasicBlockingQueue(String name, CacheAccessor<Object, E> cacheAccessor, int capacity) {
        super(name, cacheAccessor, new LinkedBlockingQueue<Ref<E>>(capacity));
    }

    @Override
    public void put(E e) throws InterruptedException {
        if (e == null) {
            throw new NullPointerException("e must not be null.");
        }
        Ref<E> ref = cacheAccessor.create(null, e);
        if (ref == null) {
            synchronized (mutex) {
                while (ref == null) {
                    mutex.wait();
                    ref = cacheAccessor.create(null, e);
                }
            }
        }
        try {
            refQueue.put(ref);
        } catch (InterruptedException ie) {
            cacheAccessor.remove(null, ref);
            throw ie;
        }
    }

    @Override
    public boolean offer(E e, long timeout, TimeUnit unit) throws InterruptedException {
        if (e == null) {
            throw new NullPointerException("e must not be null.");
        }
        if (unit == null) {
            throw new NullPointerException("unit must not be null.");
        }

        Ref<E> ref = cacheAccessor.create(null, e);
        long timeoutMillis = unit.toMillis(timeout);
        if (ref == null) {
            long now = System.currentTimeMillis();
            long startWait = now;
            synchronized (mutex) {
                while (ref == null && timeoutMillis > 0) {
                    mutex.wait(timeoutMillis);
                    now = System.currentTimeMillis();
                    timeoutMillis -= (now - startWait);
                    startWait = now;
                    ref = cacheAccessor.create(null, e);
                }
            }
            if (ref == null) {
                return false;
            }
        }
        try {
            if (refQueue.offer(ref, timeoutMillis, TimeUnit.MILLISECONDS)) {
                return true;
            }
            cacheAccessor.remove(null, ref);
            return false;
        } catch (InterruptedException ie) {
            cacheAccessor.remove(null, ref);
            throw ie;
        }
    }

    @Override
    public E take() throws InterruptedException {
        Ref<E> ref = refQueue.take();
        E result = ref.value();
        cacheAccessor.remove(null, ref);
        synchronized (mutex) {
            mutex.notifyAll();
        }
        return result;
    }

    @Override
    public E poll(long timeout, TimeUnit unit) throws InterruptedException {
        Ref<E> ref = refQueue.poll(timeout, unit);
        if (ref == null) {
            return null;
        }
        E result = ref.value();
        cacheAccessor.remove(null, ref);
        synchronized (mutex) {
            mutex.notifyAll();
        }
        return result;
    }

    @Override
    public int remainingCapacity() {
        return refQueue.remainingCapacity();
    }

    @Override
    public int drainTo(Collection<? super E> c) {
        List<Ref<E>> refList = new ArrayList<Ref<E>>();
        int result = refQueue.drainTo(refList);
        for (Ref<E> ref : refList) {
            c.add(ref.value());
            cacheAccessor.remove(null, ref);
        }
        if (result > 0) {
            synchronized (mutex) {
                mutex.notifyAll();
            }
        }
        return result;
    }

    @Override
    public int drainTo(Collection<? super E> c, int maxElements) {
        List<Ref<E>> refList = new ArrayList<Ref<E>>(maxElements);
        int result = refQueue.drainTo(refList, maxElements);
        for (Ref<E> ref : refList) {
            c.add(ref.value());
            cacheAccessor.remove(null, ref);
        }
        if (result > 0) {
            synchronized (mutex) {
                mutex.notifyAll();
            }
        }
        return result;
    }

    @Override
    public E poll() {
        Ref<E> ref = refQueue.poll();
        if (ref == null) {
            return null;
        }
        E e = ref.value();
        cacheAccessor.remove(null, ref);
        synchronized (mutex) {
            mutex.notifyAll();
        }
        return e;
    }
}
