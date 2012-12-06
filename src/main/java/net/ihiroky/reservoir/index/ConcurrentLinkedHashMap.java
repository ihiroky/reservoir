package net.ihiroky.reservoir.index;

import java.util.AbstractCollection;
import java.util.AbstractQueue;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Created on 12/10/12, 22:49
 *
 * @author Hiroki Itoh
 */
public class ConcurrentLinkedHashMap<K, V> implements ConcurrentMap<K, V> {

    private final AtomicReferenceArray<Segment> segmentArray;
    private final int segmentShift;
    private final int segmentMask;
    private final int initialSegmentCapacity;
    private final float loadFactor;
    private final OrderQueue<K, V> orderQueue;
    private final boolean accessOrder;
    private Set<K> keySet;
    private Collection<V> values;
    private Set<Entry<K, V>> entrySet;

    private static final int DEFAULT_INITIAL_CAPACITY = 16;
    private static final float DEFAULT_LOAD_FACTOR = 0.75f;
    private static final int DEFAULT_CONCURRENCY_LEVEL = 16;
    private static final int MAXIMUM_CAPACITY = 1 << 30;
    private static final int MIN_SEGMENT_CAPACITY = 2;
    private static final int MAX_SEGMENTS = 1 << 16;

    public enum RemoveEldestPolicy {
        REMOVE {
            @Override
            <K, V> void execute(ConcurrentLinkedHashMap<K, V> map, Node<K, V> eldestNode) {
                map.remove(eldestNode.key, eldestNode.value);
            }
        },
        READY_TO_REMOVE {
            @Override
            <K, V> void execute(ConcurrentLinkedHashMap<K, V> map, Node<K, V> eldestNode) {
                eldestNode.readyToRemove = true;
            }
        },
        DO_NOTHING {
            @Override
            <K, V> void execute(ConcurrentLinkedHashMap<K, V> map, Node<K, V> eldestNode) {
            }
        },;

        abstract <K, V> void execute(ConcurrentLinkedHashMap<K, V> map, Node<K, V> eldestNode);
    }

    public ConcurrentLinkedHashMap() {
        this(DEFAULT_INITIAL_CAPACITY, DEFAULT_LOAD_FACTOR, DEFAULT_CONCURRENCY_LEVEL);
    }

    public ConcurrentLinkedHashMap(long initialCapacity, float loadFactor, int concurrencyLevel) {
        this(initialCapacity, loadFactor, concurrencyLevel, false);
    }

    public ConcurrentLinkedHashMap(long initialCapacity, float loadFactor,
                                   int concurrencyLevel, boolean accessOrder) {

        if (loadFactor <= 0 || initialCapacity < 0 || concurrencyLevel <= 0) {
            throw new IllegalArgumentException();
        }

        if (concurrencyLevel > MAX_SEGMENTS) {
            concurrencyLevel = MAX_SEGMENTS;
        }
        this.loadFactor = loadFactor;
        this.orderQueue = new OrderQueue<K, V>();
        this.accessOrder = accessOrder;

        int segmentShift = 0;
        int segments = 1;
        while (segments < concurrencyLevel) {
            ++segmentShift;
            segments <<= 1;
        }
        this.segmentShift = 32 - segmentShift;
        this.segmentMask = segments - 1;
        if (initialCapacity > MAXIMUM_CAPACITY)
            initialCapacity = MAXIMUM_CAPACITY;
        int c = (int) (initialCapacity / segments);
        if (c * segments < initialCapacity) {
            c++;
        }
        int initialSegmentCapacity = MIN_SEGMENT_CAPACITY;
        while (initialSegmentCapacity < c) {
            initialSegmentCapacity <<= 1;
        }
        this.initialSegmentCapacity = initialSegmentCapacity;
        this.segmentArray = new AtomicReferenceArray<Segment>(segments);
    }

    private Segment ensureSegmentFor(K key) {
        int index = indexFor(key);
        Segment segment;
        if ((segment = segmentArray.get(index)) == null) {
            if ((segment = segmentArray.get(index)) == null) {
                // store a new segment if not already present.
                Segment newSegment = new Segment(initialSegmentCapacity, loadFactor);
                while ((segment = segmentArray.get(index)) == null) {
                    if (segmentArray.compareAndSet(index, null, newSegment)) {
                        segment = newSegment;
                        break;
                    }
                }
            }
        }
        return segment;
    }

    private static int hash(int h) {
        // Spread bits to regularize both segment and index locations,
        // using variant of single-word Wang/Jenkins hash.
        h += (h << 15) ^ 0xffffcd7d;
        h ^= (h >>> 10);
        h += (h << 3);
        h ^= (h >>> 6);
        h += (h << 2) + (h << 14);
        return h ^ (h >>> 16);
    }

    private Segment segmentFor(Object key) {
        int h = hash(key.hashCode());
        return segmentArray.get((h >>> segmentShift) & segmentMask);
    }

    private int indexFor(Object key) {
        int h = hash(key.hashCode());
        return (h >>> segmentShift) & segmentMask;
    }

    @Override
    public int size() {
        long size = 0;
        int segments = segmentArray.length();
        Segment segment;
        for (int i = 0; i < segments; i++) {
            segment = segmentArray.get(i);
            if (segment != null) {
                size += segment.size();
            }
        }
        return (size > Integer.MAX_VALUE) ? Integer.MAX_VALUE : (int) size;
    }

    @Override
    public boolean isEmpty() {
        int segments = segmentArray.length();
        Segment segment;
        for (int i = 0; i < segments; i++) {
            segment = segmentArray.get(i);
            if (segment != null && segment.size() > 0) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean containsKey(Object key) {
        if (key == null) {
            throw new NullPointerException("key must not be null.");
        }

        Segment segment = segmentFor(key);
        return (segment != null) && segment.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        if (value == null) {
            throw new NullPointerException("value must not be null.");
        }

        Segment segment;
        int segments = segmentArray.length();
        for (int i = 0; i < segments; i++) {
            segment = segmentArray.get(i);
            if (segment != null && segment.containsValue(value)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public V get(Object key) {
        if (key == null) {
            throw new NullPointerException("key must not be null.");
        }

        Segment segment = segmentFor(key);
        return (segment != null) ? segment.get(key) : null;
    }

    @Override
    public V put(K key, V value) {
        if (key == null || value == null) {
            throw new NullPointerException("key or value must not be null.");
        }


        Segment segment = ensureSegmentFor(key);
        return segment.put(key, value);
    }

    protected RemoveEldestPolicy removeEldestEntry(Entry<K, V> eldestEntry) {
        return RemoveEldestPolicy.DO_NOTHING;
    }

    @Override
    public V remove(Object key) {
        if (key == null) {
            throw new NullPointerException("key must not be null.");
        }

        Segment segment = segmentFor(key);
        return (segment != null) ? segment.remove(key) : null;
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        if (m == null) {
            throw new NullPointerException("m must not be null.");
        }

        Segment segment;
        K key;
        V value;
        for (Map.Entry<? extends K, ? extends V> entry : m.entrySet()) {
            key = entry.getKey();
            value = entry.getValue();
            if (key == null || value != null) {
                throw new NullPointerException("key or value must not be null.");
            }
            segment = ensureSegmentFor(key);
            segment.put(key, entry.getValue());
        }
    }

    @Override
    public void clear() {
        Segment segment;
        int segments = segmentArray.length();
        for (int i = 0; i < segments; i++) {
            segment = segmentArray.get(i);
            if (segment != null) {
                segment.clear();
            }
        }
    }

    @Override
    public Set<K> keySet() {
        return (keySet != null) ? keySet : (keySet = new KeySet());
    }

    @Override
    public Collection<V> values() {
        return (values != null) ? values : (values = new Values());
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        return (entrySet != null) ? entrySet : (entrySet = new EntrySet());
    }

    @Override
    public V putIfAbsent(K key, V value) {
        if (key == null || value == null) {
            throw new NullPointerException("key or value must not be null.");
        }

        Segment segment = ensureSegmentFor(key);
        return segment.putIfAbsent(key, value);
    }

    @Override
    public boolean remove(Object key, Object value) {
        if (key == null || value == null) {
            throw new NullPointerException("key or value must not be null.");
        }

        Segment segment = segmentFor(key);
        return (segment != null) && segment.remove(key, value);
    }

    @Override
    public boolean replace(K key, V oldValue, V newValue) {
        if (key == null || oldValue == null || newValue == null) {
            throw new NullPointerException("key, oldValue or newValue must not be null.");
        }

        Segment segment = segmentFor(key);
        return (segment != null) && segment.replace(key, oldValue, newValue);
    }

    @Override
    public V replace(K key, V value) {
        if (key == null || value == null) {
            throw new NullPointerException("key or value must not be null.");
        }

        Segment segment = segmentFor(key);
        return (segment != null) ? segment.replace(key, value) : null;
    }

    @Override
    public String toString() {
        int length = segmentArray.length();
        StringBuilder b = new StringBuilder();
        b.append('[');
        for (int i = 0; i < length; i++) {
            Segment segment = segmentArray.get(i);
            if (segment != null) {
                segment.appendTo(b);
            }
        }
        int len = b.length();
        if (len > 1) {
            b.delete(len - 2, len); // last ", "
        }
        return b.append(']').toString();
    }

    public Queue<Entry<K, V>> orderQueue() {
        return orderQueue;
    }

    private abstract class AllSegmentIterator<E> implements Iterator<E> {

        private int segmentIndex;
        Iterator<Entry<K, V>> segmentEntryIterator = nextEntryIterator();
        private Entry<K, V> currentEntry;

        Iterator<Entry<K, V>> nextEntryIterator() {
            AtomicReferenceArray<Segment> array = segmentArray;
            int length = array.length();
            Segment segment = null;
            while (segmentIndex < length
                    && (segment = segmentArray.get(segmentIndex++)) == null) {
            }
            if (segmentIndex > length || segment == null) {
                return null;
            }
            List<Entry<K, V>> entryList;
            Lock lock = segment.readLock();
            lock.lock();
            try {
                entryList = new ArrayList<Entry<K, V>>(segment.entrySet());
            } finally {
                lock.unlock();
            }
            return entryList.iterator();
        }

        Entry<K, V> nextEntry() {
            return (currentEntry = segmentEntryIterator.next());
        }

        @Override
        public boolean hasNext() {
            for (Iterator<Entry<K, V>> subIterator = segmentEntryIterator;
                 subIterator != null;
                 subIterator = segmentEntryIterator = nextEntryIterator()) {
                if (subIterator.hasNext()) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public void remove() {
            Segment segment = segmentArray.get(segmentIndex - 1);
            segment.remove(currentEntry.getKey());
        }
    }

    private class KeySet extends AbstractSet<K> {
        public Iterator<K> iterator() {
            return new AllSegmentIterator<K>() {
                @Override
                public K next() {
                    return nextEntry().getKey();
                }
            };
        }

        public int size() {
            return ConcurrentLinkedHashMap.this.size();
        }

        public boolean isEmpty() {
            return ConcurrentLinkedHashMap.this.isEmpty();
        }

        public boolean contains(Object o) {
            return ConcurrentLinkedHashMap.this.containsKey(o);
        }

        public boolean remove(Object o) {
            return ConcurrentLinkedHashMap.this.remove(o) != null;
        }

        public void clear() {
            ConcurrentLinkedHashMap.this.clear();
        }
    }

    private class Values extends AbstractCollection<V> {
        public Iterator<V> iterator() {
            return new AllSegmentIterator<V>() {
                @Override
                public V next() {
                    return nextEntry().getValue();
                }
            };
        }

        public int size() {
            return ConcurrentLinkedHashMap.this.size();
        }

        public boolean isEmpty() {
            return ConcurrentLinkedHashMap.this.isEmpty();
        }

        public boolean contains(Object o) {
            return ConcurrentLinkedHashMap.this.containsValue(o);
        }

        public void clear() {
            ConcurrentLinkedHashMap.this.clear();
        }
    }

    private class EntrySet extends AbstractSet<Entry<K, V>> {
        public Iterator<Map.Entry<K, V>> iterator() {
            return new AllSegmentIterator<Entry<K, V>>() {
                @Override
                public Entry<K, V> next() {
                    return nextEntry();
                }
            };
        }

        public boolean contains(Object o) {
            if (!(o instanceof Map.Entry))
                return false;
            Map.Entry<?, ?> e = (Map.Entry<?, ?>) o;
            V v = ConcurrentLinkedHashMap.this.get(e.getKey());
            return v != null && v.equals(e.getValue());
        }

        public boolean remove(Object o) {
            if (!(o instanceof Map.Entry))
                return false;
            Map.Entry<?, ?> e = (Map.Entry<?, ?>) o;
            return ConcurrentLinkedHashMap.this.remove(e.getKey()) != null;
        }

        public int size() {
            return ConcurrentLinkedHashMap.this.size();
        }

        public boolean isEmpty() {
            return ConcurrentLinkedHashMap.this.isEmpty();
        }

        public void clear() {
            ConcurrentLinkedHashMap.this.clear();
        }
    }

    private class Segment extends ReentrantReadWriteLock implements ConcurrentMap<K, V> {

        volatile Map<K, Node<K, V>> map;
        Collection<V> values;
        Set<Entry<K, V>> entrySet;

        Segment(int initialCapacity, float loadFactor) {
            map = new HashMap<K, Node<K, V>>(initialCapacity, loadFactor);
        }

        @Override
        public int size() {
            return map.size();
        }

        @Override
        public boolean isEmpty() {
            return map.isEmpty();
        }

        @Override
        public boolean containsKey(Object key) {
            boolean containsKey;

            Lock lock = readLock();
            lock.lock();
            try {
                containsKey = map.containsKey(key);
            } finally {
                lock.unlock();
            }
            return containsKey;
        }

        @Override
        public boolean containsValue(Object value) {
            boolean containsValue = false;

            Lock lock = readLock();
            lock.lock();
            try {
                for (Node<K, V> node : map.values()) {
                    if (node.value.equals(value)) {
                        containsValue = true;
                        break;
                    }
                }
            } finally {
                lock.unlock();
            }
            return containsValue;
        }

        @Override
        public V get(Object key) {
            V value = null;

            Lock lock = readLock();
            lock.lock();
            try {
                Node<K, V> node = map.get(key);
                if (node != null) {
                    value = node.value;
                    if (accessOrder) {
                        orderQueue.promote(node);
                    }
                }
            } finally {
                lock.unlock();
            }
            return value;
        }

        @Override
        public V put(K key, V value) {
            Node<K, V> newNode = new Node<K, V>(key, value);
            V oldValue;

            RemoveEldestPolicy removeEldestPolicy = null;
            Lock lock = writeLock();
            lock.lock();
            try {
                Node<K, V> oldNode = map.put(key, newNode);
                if (oldNode != null) {
                    oldValue = oldNode.value;
                    orderQueue.removeAndOffer(oldNode, newNode);
                    return oldValue;
                }
                oldValue = null;
                orderQueue.offer(newNode);
            } finally {
                lock.unlock();
            }
            Node<K, V> eldestNode = orderQueue.peekValid();
            removeEldestPolicy = removeEldestEntry(eldestNode);
            removeEldestPolicy.execute(ConcurrentLinkedHashMap.this, eldestNode);
            return oldValue;
        }

        @Override
        public V remove(Object key) {
            V oldValue = null;

            Lock lock = writeLock();
            lock.lock();
            try {
                Node<K, V> node = map.remove(key);
                if (node != null) {
                    oldValue = node.value;
                    orderQueue.remove(node);
                }
            } finally {
                lock.unlock();
            }
            return oldValue;
        }

        @Override
        public void putAll(Map<? extends K, ? extends V> m) {
            for (Map.Entry<? extends K, ? extends V> entry : m.entrySet()) {
                put(entry.getKey(), entry.getValue());
            }
        }

        @Override
        public void clear() {
            Lock lock = writeLock();
            lock.lock();
            try {
                map.clear();
                orderQueue.clear();
            } finally {
                lock.unlock();
            }
        }

        @Override
        public Set<K> keySet() {
            return map.keySet();
        }

        @Override
        public Collection<V> values() {
            return (values != null) ? values : (values = new Values());
        }

        @Override
        public Set<Entry<K, V>> entrySet() {
            return (entrySet != null) ? entrySet : (entrySet = new EntrySet());
        }

        @Override
        public V putIfAbsent(K key, V value) {
            Node<K, V> newNode = new Node<K, V>(key, value);
            V oldValue = null;

            RemoveEldestPolicy removeEldestPolicy = null;
            Lock lock = writeLock();
            lock.lock();
            try {
                Node<K, V> oldNode = map.get(key);
                if (oldNode != null) {
                    oldValue = oldNode.value;
                    if (accessOrder) {
                        orderQueue.promote(oldNode);
                    }
                    return oldValue;
                }
                map.put(key, newNode);
                orderQueue.offer(newNode);
            } finally {
                lock.unlock();
            }
            Node<K, V> eldestNode = orderQueue.peekValid();
            removeEldestPolicy = removeEldestEntry(eldestNode);
            removeEldestPolicy.execute(ConcurrentLinkedHashMap.this, eldestNode);
            return oldValue;
        }

        @Override
        public boolean remove(Object key, Object value) {
            boolean result;

            Lock lock = writeLock();
            lock.lock();
            try {
                Node<K, V> node = map.get(key);
                if (node != null && node.value.equals(value)) {
                    map.remove(key);
                    orderQueue.remove(node);
                    result = true;
                } else {
                    result = false;
                }
            } finally {
                lock.unlock();
            }
            return result;
        }

        @Override
        public boolean replace(K key, V oldValue, V newValue) {
            boolean result = false;

            Lock lock = writeLock();
            lock.lock();
            try {
                Node<K, V> node = map.get(key);
                if (node != null && node.value.equals(oldValue)) {
                    Node<K, V> newNode = new Node<K, V>(key, newValue);
                    map.put(key, newNode);
                    orderQueue.removeAndOffer(node, newNode);
                    result = true;
                }
            } finally {
                lock.unlock();
            }
            return result;
        }

        @Override
        public V replace(K key, V value) {
            V oldValue = null;

            Lock lock = writeLock();
            lock.lock();
            try {
                Node<K, V> oldNode = map.get(key);
                if (oldNode != null) {
                    Node<K, V> newNode = new Node<K, V>(key, value);
                    map.put(key, newNode);
                    orderQueue.removeAndOffer(oldNode, newNode);
                    oldValue = oldNode.value;
                }
            } finally {
                lock.unlock();
            }
            return oldValue;
        }

        @Override
        public String toString() {
            String result;

            Lock lock = readLock();
            lock.lock();
            try {
                result = map.toString();
            } finally {
                lock.unlock();
            }
            return result;
        }

        private void appendTo(StringBuilder b) {
            Lock lock = readLock();
            lock.lock();
            try {
                for (Node<K, V> node : map.values()) {
                    b.append(node.key).append('=').append(node.value).append(',').append(' ');
                }
            } finally {
                lock.unlock();
            }
        }

        private class Values extends AbstractCollection<V> {
            @Override
            public Iterator<V> iterator() {
                return new ValueIterator();
            }

            @Override
            public int size() {
                return Segment.this.size();
            }

            @Override
            public void clear() {
                Segment.this.clear();
            }

            @Override
            public boolean contains(Object obj) {
                return Segment.this.containsValue(obj);
            }
        }

        private class ValueIterator implements Iterator<V> {

            Iterator<Node<K, V>> iterator = Segment.this.map.values().iterator();
            Node<K, V> currentNode;

            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public V next() {
                currentNode = iterator.next();
                return currentNode.value;
            }

            @Override
            public void remove() {
                if (currentNode != null) {
                    Segment.this.remove(currentNode.key);
                    currentNode = null;
                }
            }
        }

        private class EntrySet extends AbstractSet<Entry<K, V>> {
            @Override
            public Iterator<Entry<K, V>> iterator() {
                return new EntryIterator();
            }

            @Override
            public int size() {
                return Segment.this.size();
            }

            @Override
            public void clear() {
                Segment.this.clear();
            }

            @Override
            public boolean contains(Object obj) {
                if (!(obj instanceof Entry)) {
                    return false;
                }
                @SuppressWarnings("unchecked") Entry<K, V> that = (Entry<K, V>) obj;
                Node<K, V> node = Segment.this.map.get(that.getKey());
                return (node != null) && (node.value.equals(that.getValue()));
            }

            @Override
            public boolean remove(Object obj) {
                if (!(obj instanceof Entry)) {
                    return false;
                }
                @SuppressWarnings("unchecked") Entry<K, V> that = (Entry<K, V>) obj;
                return Segment.this.remove(that.getKey()) != null;
            }
        }

        private class EntryIterator implements Iterator<Entry<K, V>> {

            Iterator<Node<K, V>> iterator = Segment.this.map.values().iterator();
            Node<K, V> currentNode;

            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public Entry<K, V> next() {
                return (currentNode = iterator.next());
            }

            @Override
            public void remove() {
                if (currentNode != null) {
                    Segment.this.remove(currentNode.key);
                    currentNode = null;
                }
            }
        }
    }

    protected static class Node<K, V> implements Entry<K, V> {
        final K key;
        V value;
        Node<K, V> prev;
        Node<K, V> next;
        volatile boolean readyToRemove;

        Node(K key, V value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public K getKey() {
            return key;
        }

        @Override
        public V getValue() {
            return value;
        }

        @Override
        public V setValue(V value) {
            V oldValue = this.value;
            this.value = value;
            return oldValue;
        }

        private static boolean eq(Object o1, Object o2) {
            return (o1 != null) ? o1.equals(o2) : o2 == null;
        }

        @Override
        public boolean equals(Object object) {
            if (!(object instanceof Node)) {
                return false;
            }
            @SuppressWarnings("unchecked") Node<K, V> that = (Node<K, V>) object;
            return eq(this.key, that.key) && eq(this.value, that.value)
                    && this.next == that.next && this.prev == that.prev;
        }

        @Override
        public int hashCode() {
            return ((key != null) ? key.hashCode() : 0)
                    ^ ((value != null) ? value.hashCode() : 0);
        }

        @Override
        public String toString() {
            return key + "=" + value;
        }

    }

    // TODO check if 'readyToRemove' node exists for a long time.
    // TODO use non-blockng algorighm link ConcurrentLinkedQueue.
    protected static class OrderQueue<K, V> extends AbstractQueue<Entry<K, V>> {
        Node<K, V> head;
        Node<K, V> tail;

        OrderQueue() {
        }

        private void linkLast(Node<K, V> node) {
            Node<K, V> t = tail;
            node.prev = t;
            tail = node;
            if (t == null) {
                head = node;
            } else {
                t.next = node;
            }
        }

        private void unlink(Node<K, V> node) {
            Node<K, V> next = node.next;
            Node<K, V> prev = node.prev;
            if (prev == null) {
                head = next;
            } else {
                prev.next = next;
                node.prev = null;
            }
            if (next == null) {
                tail = prev;
            } else {
                next.prev = prev;
                node.next = null;
            }
        }

        private Node<K, V> unlinkHead() {
            Node<K, V> h = head;
            if (h == null) {
                return null;
            }
            Node<K, V> next = h.next;
            head = next;
            if (next != null) {
                next.prev = null;
            } else {
                tail = null;
            }
            h.next = null; // for gc
            return h;
        }

        @Override
        public Iterator<Entry<K, V>> iterator() {
            return new OrderQueueIterator(null);
        }

        @Override
        public int size() {
            long size = 0;
            synchronized (this) {
                for (Node<K, V> n = head; n != null; n = n.next) {
                    size++;
                }
            }
            return (size > Integer.MAX_VALUE) ? Integer.MAX_VALUE : (int) size;
        }

        public void offer(Node<K, V> node) {
            synchronized (this) {
                linkLast(node);
            }
        }

        @Override
        public boolean offer(Entry<K, V> entry) {
            offer(new Node<K, V>(entry.getKey(), entry.getValue()));
            return true;
        }

        @Override
        public Node<K, V> poll() {
            synchronized (this) {
                return unlinkHead();
            }
        }

        @Override
        public Node<K, V> peek() {
            synchronized (this) {
                return head;
            }
        }

        public Node<K, V> peekValid() {
            Node<K, V> node;
            synchronized (this) {
                node = head;
                while (node != null && node.readyToRemove) {
                    node = node.next;
                }
            }
            return node;
        }

        public void remove(Node<K, V> node) {
            synchronized (this) {
                unlink(node);
            }
        }

        public void removeAndOffer(Node<K, V> remove, Node<K, V> offer) {
            synchronized (this) {
                unlink(remove);
                offer(offer);
            }
        }

        public void promote(Node<K, V> node) {
            synchronized (this) {
                if (node == tail) {
                    return;
                }
                unlink(node);
                linkLast(node);
            }
        }

        @Override
        public void clear() {
            synchronized (this) {
                head = tail = null;
            }
        }

        boolean contains(Node<K, V> node) {
            return node != null && (node.next != null || node.prev != null);
        }

        public Iterator<Entry<K, V>> iterator(final ConcurrentLinkedHashMap<K, V> map) {
            return new OrderQueueIterator(map);
        }


        private class OrderQueueIterator implements Iterator<Entry<K, V>> {
            Node<K, V> current;
            final ConcurrentLinkedHashMap<K, V> map;

            OrderQueueIterator(ConcurrentLinkedHashMap<K, V> map) {
                Node<K, V> current = new Node<K, V>(null, null);
                current.next = OrderQueue.this.head;

                this.current = current;
                this.map = map;
            }

            @Override
            public boolean hasNext() {
                synchronized (OrderQueue.this) {
                    return (current != null) && (current.next != null);
                }
            }

            @Override
            public Entry<K, V> next() {
                if (current == null) {
                    return null;
                }
                Node<K, V> result;
                synchronized (OrderQueue.this) {
                    result = current.next;
                }
                current = result;
                return result;
            }

            @Override
            public void remove() {
                if (map == null) {
                    throw new UnsupportedOperationException();
                }
                map.remove(current.key);
            }
        }
    }
}
