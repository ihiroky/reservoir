package net.ihiroky.reservoir;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * An implementation of {@link net.ihiroky.reservoir.AbstractBasicQueue} based on {@code LinkedBlockingQueue}.
 * This class supports {@code BlockingQueue} operations.
 *
 * {@inheritDoc}
 *
 * @param <E> the type of elements held in this collection.
 * @author Hiroki Itoh
 */
public class BasicBlockingQueue<E> extends AbstractBasicQueue<E, LinkedBlockingQueue<Ref<E>>>
        implements BlockingQueue<E> {

    /**
     * A object that creates a critical session for elements space allocation and release.
     */
    private final Object mutex = new Object();

    /**
     * Creates this instance.
     * @param name a name of this queue.
     * @param storageAccessor an elements space allocator.
     */
    BasicBlockingQueue(String name, StorageAccessor<Object, E> storageAccessor) {
        super(name, storageAccessor, new LinkedBlockingQueue<Ref<E>>());
    }

    /**
     * Creates this instance.
     * @param name a name of this queue.
     * @param storageAccessor an elements space allocator.
     * @param capacity a
     */
    BasicBlockingQueue(String name, StorageAccessor<Object, E> storageAccessor, int capacity) {
        super(name, storageAccessor, new LinkedBlockingQueue<Ref<E>>(capacity));
    }

    /**
     * Inserts a specified element at the tail of this queue, waiting if necessary for queue or storageAccessor
     * space to become available.
     * @param e an element to insert.
     * @throws InterruptedException if interrupted while waiting.
     */
    @Override
    public void put(E e) throws InterruptedException {
        if (e == null) {
            throw new NullPointerException("e must not be null.");
        }
        Ref<E> ref = storageAccessor.create(null, e);
        if (ref == null) {
            // wait until a space allocation gets successful.
            synchronized (mutex) {
                while (ref == null) {
                    mutex.wait();
                    ref = storageAccessor.create(null, e);
                }
            }
        }
        try {
            refQueue.put(ref);
        } catch (InterruptedException ie) {
            storageAccessor.remove(null, ref);
            throw ie;
        }
    }

    /**
     * Inserts the specified element at the tail of this queue, waiting if necessary up to the specified wait time
     * for queue or storageAccessor space to become available.
     *
     * @param e an element to insert.
     * @param timeout how long to wait before giving up, in units of
     *        <tt>unit</tt>
     * @param unit a <tt>TimeUnit</tt> determining how to interpret the
     *        <tt>timeout</tt> parameter
     * @return {@code true} if successful, or {@code false} if the specified waiting time elapses before space
     *        is available.
     *
     * @throws InterruptedException
     */
    @Override
    public boolean offer(E e, long timeout, TimeUnit unit) throws InterruptedException {
        if (e == null) {
            throw new NullPointerException("e must not be null.");
        }
        if (unit == null) {
            throw new NullPointerException("unit must not be null.");
        }

        Ref<E> ref = storageAccessor.create(null, e);
        long timeoutMillis = unit.toMillis(timeout);
        if (ref == null) {
            // wait until a space allocation gets success in the specified times.
            long now = System.currentTimeMillis();
            long startWait = now;
            synchronized (mutex) {
                while (ref == null && timeoutMillis > 0) {
                    mutex.wait(timeoutMillis);
                    now = System.currentTimeMillis();
                    timeoutMillis -= (now - startWait);
                    startWait = now;
                    ref = storageAccessor.create(null, e);
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
            storageAccessor.remove(null, ref);
            return false;
        } catch (InterruptedException ie) {
            storageAccessor.remove(null, ref);
            throw ie;
        }
    }

    /**
     * Retrieves and removes the head of this queue, waiting if necessary until an element becomes available.
     *
     * @return the head of this queue.
     * @throws InterruptedException if interrupted while waiting.
     */
    @Override
    public E take() throws InterruptedException {
        Ref<E> ref = refQueue.take();
        E result = ref.value();
        storageAccessor.remove(null, ref);
        synchronized (mutex) {
            mutex.notifyAll();
        }
        return result;
    }

    /**
     * Retrieves and removes the head of this queue, waiting up to the specified wait time if necessary
     * for an element to become available.
     *
     * @param timeout how long to wait before giving up, in units of
     *        <tt>unit</tt>
     * @param unit a <tt>TimeUnit</tt> determining how to interpret the
     *        <tt>timeout</tt> parameter
     * @return the head of this queue, or <tt>null</tt> if the
     *         specified waiting time elapses before an element is available
     * @throws InterruptedException if interrupted while waiting
     */
    @Override
    public E poll(long timeout, TimeUnit unit) throws InterruptedException {
        Ref<E> ref = refQueue.poll(timeout, unit);
        if (ref == null) {
            return null;
        }
        E result = ref.value();
        storageAccessor.remove(null, ref);
        synchronized (mutex) {
            mutex.notifyAll();
        }
        return result;
    }

    /**
     * Returns the number of additional elements that this queue can ideally (in the absence of memory or resource
     * constraints) accept without blocking.
     *
     * @return a remaining capacity.
     */
    @Override
    public int remainingCapacity() {
        return refQueue.remainingCapacity();
    }

    /**
     * Removes all available elements from this queue and adds them to the given collection.
     * @param c a collection to transfer elements into.
     * @return the number of elements transferred.
     */
    @Override
    public int drainTo(Collection<? super E> c) {
        List<Ref<E>> refList = new ArrayList<Ref<E>>();
        int result = refQueue.drainTo(refList);
        for (Ref<E> ref : refList) {
            c.add(ref.value());
            storageAccessor.remove(null, ref);
        }
        if (result > 0) {
            synchronized (mutex) {
                mutex.notifyAll();
            }
        }
        return result;
    }

    /**
     * Removes at most the given number of available elements from this queue and adds them to the given collection.
     * @param c a collection to transfer.
     * @param maxElements the maximum number of elements.
     * @return the number of elements transferred.
     */
    @Override
    public int drainTo(Collection<? super E> c, int maxElements) {
        List<Ref<E>> refList = new ArrayList<Ref<E>>(maxElements);
        int result = refQueue.drainTo(refList, maxElements);
        for (Ref<E> ref : refList) {
            c.add(ref.value());
            storageAccessor.remove(null, ref);
        }
        if (result > 0) {
            synchronized (mutex) {
                mutex.notifyAll();
            }
        }
        return result;
    }

    /**
     * {@inheritDoc}
     * @return the head of this queue, or null if this queue is empty.
     */
    @Override
    public E poll() {
        Ref<E> ref = refQueue.poll();
        if (ref == null) {
            return null;
        }
        E e = ref.value();
        storageAccessor.remove(null, ref);
        synchronized (mutex) {
            mutex.notifyAll();
        }
        return e;
    }
}
