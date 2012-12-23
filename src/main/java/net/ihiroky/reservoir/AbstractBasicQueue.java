package net.ihiroky.reservoir;

import java.util.AbstractQueue;
import java.util.Iterator;
import java.util.Queue;

/**
 * A queue that stores elements in a space prepared by {@link net.ihiroky.reservoir.StorageAccessor}. The reference of the element store is
 * held by {@code java.util.Queue}, which defines the queueing algorithm. Elements storage is managed by
 * {@link net.ihiroky.reservoir.StorageAccessor} specified in constructors.
 *
 * @param <E> the type of elements held in this collection.
 * @param <Q> the type of a queue hold references that point to element's store.
 * @author Hiroki Itoh
 */
abstract class AbstractBasicQueue<E, Q extends Queue<Ref<E>>> extends AbstractQueue<E> implements QueueMBean {

    /** A name of this queue. */
    private String name;

    /** A manager that allocates and releases elements store. */
    protected StorageAccessor<Object, E> storageAccessor;

    /** A {@code java.util.Queue} implementation that holds references of the elements store. */
    protected Q refQueue;

    /**
     * A constructor for use by subclass. This instance is registered on platform MBean server by this constructor.
     *
     * @param name a name of this queue.
     * @param storageAccessor a manager that allocates and releases elements store.
     * @param queue a {@code java.util.Queue} implementation that holds references of the elements store.
     */
    AbstractBasicQueue(String name, StorageAccessor<Object, E> storageAccessor, Q queue) {
        if (storageAccessor == null) {
            throw new NullPointerException("storageAccessor must not be null.");
        }
        if (name == null) {
            throw new NullPointerException("name must not be null.");
        }
        if (name.length() == 0) {
            throw new IllegalArgumentException("name must not be empty.");
        }

        this.name = name;
        this.storageAccessor = storageAccessor;
        this.refQueue = queue;
        MBeanSupport.registerMBean(this, name);
    }

    /**
     * Invalidates this instance. Elements and their stores is released and MBean registered by the constructor is
     * unregistered.
     */
    public void dispose() {
        refQueue.clear();
        storageAccessor.dispose();
        MBeanSupport.unregisterMBean(this, name);
    }

    /**
     * Returns an iterator over the elements in this queue in proper sequence.
     * The elements will be returned in order defined by the type of {@code Q}.
     *
     * @return an iterator over the elements in this queue in proper sequence.
     */
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
                storageAccessor.remove(null, current);
            }
        };
    }

    /**
     * Returns the number of elements in this instance.
     * @return the number of elements in this instance.
     */
    @Override
    public int size() {
        return refQueue.size();
    }

    /**
     * Inserts the specified element at the tail of this queue.
     *
     * @param e an element to insert.
     * @return true if the element store is successfully allocated and the element is inserted in the queue.
     */
    @Override
    public boolean offer(E e) {
        if (e == null) {
            throw new NullPointerException("e must not be null.");
        }

        Ref<E> ref = storageAccessor.create(null, e);
        if (ref != null) {
            if (refQueue.offer(ref)) {
                return true;
            }
            storageAccessor.remove(null, ref);
        }
        return false;
    }

    /**
     * Retrieves and removes an element at the head of this queue.
     *
     * @return an element which is retrieved and removed or null if this queue is empty.
     */
    @Override
    public E poll() {
        Ref<E> ref = refQueue.poll();
        if (ref == null) {
            return null;
        }
        E e = ref.value();
        storageAccessor.remove(null, ref);
        return e;
    }

    /**
     * Retrieves, not removed, an element at the head of this queue. The element is not removed.
     * @return an element to retrieve.
     */
    @Override
    public E peek() {
        Ref<E> ref = refQueue.peek();
        return (ref != null) ? ref.value() : null;
    }

    /**
     * Check if this queue is empty or not.
     * @return true if this queue is empty.
     */
    @Override
    public boolean isEmpty() {
        return refQueue.isEmpty();
    }

    /**
     * Returns a name of this queue.
     * @return a name of this queue.
     */
    @Override
    public String getName() {
        return name;
    }
}
