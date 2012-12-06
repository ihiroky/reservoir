package net.ihiroky.reservoir;

import net.ihiroky.reservoir.accessor.ByteBufferCacheAccessor;
import net.ihiroky.reservoir.accessor.FileCacheAccessor;
import net.ihiroky.reservoir.accessor.HeapCacheAccessor;
import net.ihiroky.reservoir.accessor.MemoryMappedFileCacheAccessor;
import net.ihiroky.reservoir.index.ConcurrentFIFOIndex;
import net.ihiroky.reservoir.index.ConcurrentLRUIndex;
import net.ihiroky.reservoir.index.FIFOIndex;
import net.ihiroky.reservoir.index.LRUIndex;
import net.ihiroky.reservoir.index.SimpleIndex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

/**
 * @author Hiroki Itoh
 */
public final class Reservoir {

    private Reservoir() {
        throw new AssertionError(Reservoir.class.getName() + " can't be instantiated.");
    }

    public enum IndexType {
        SIMPLE {
            @Override
            <K, V> Index<K, Ref<V>> create(long initialSize, long maxSize) {
                return new SimpleIndex<K, Ref<V>>(toInt(initialSize));
            }
        },
        LRU {
            @Override
            <K, V> Index<K, Ref<V>> create(long initialSize, long maxSize) {
                return new ConcurrentLRUIndex<K, Ref<V>>(initialSize, maxSize);
            }
        },
        FIFO {
            @Override
            <K, V> Index<K, Ref<V>> create(long initialSize, long maxSize) {
                return new ConcurrentFIFOIndex<K, Ref<V>>(initialSize, maxSize);
            }
        },
        FRAGILE_LRU {
            @Override
            <K, V> Index<K, Ref<V>> create(long initialSize, long maxSize) {
                return new LRUIndex<K, Ref<V>>(toInt(initialSize), toInt(maxSize));
            }
        },
        FRAGILE_FIFO {
            @Override
            <K, V> Index<K, Ref<V>> create(long initialSize, long maxSize) {
                return new FIFOIndex<K, Ref<V>>(toInt(initialSize), toInt(maxSize));
            }
        },;

        int toInt(long value) {
            return (value > Integer.MAX_VALUE) ? Integer.MAX_VALUE : (int) value;
        }

        abstract <K, V> Index<K, Ref<V>> create(long initialSize, long maxSize);
    }

    public enum CacheAccessorType {
        HEAP {
            @Override
            <K, V> CacheAccessor<K, V> create() {
                return new HeapCacheAccessor<K, V>();
            }
        },
        BYTE_BUFFER {
            @Override
            <K, V> CacheAccessor<K, V> create() {
                return new ByteBufferCacheAccessor<K, V>();
            }
        },
        MEMORY_MAPPED_FILE {
            @Override
            <K, V> CacheAccessor<K, V> create() {
                return new MemoryMappedFileCacheAccessor<K, V>();
            }
        },
        FILE {
            @Override
            <K, V> CacheAccessor<K, V> create() {
                return new FileCacheAccessor<K, V>();
            }
        },;

        abstract <K, V> CacheAccessor<K, V> create();
    }

    public static CacheBuilder newCacheBuilder() {
        return new CacheBuilder();
    }

    public static QueueBuilder newQueueBuilder() {
        return new QueueBuilder();
    }

    public static BlockingQueueBuilder newBlockingQueueBuilder() {
        return new BlockingQueueBuilder();
    }

    private static String randomName() {
        return String.valueOf((long) (Math.random() * Long.MAX_VALUE));
    }

    /**
     * Creates off heap cache.
     * <p/>
     * the cache to create is named by {@code String.valueOf(Double.doubleToLongBits(Math.random()))}.
     * To allocate a large memory, use {@code -XX:MaxDirectMemorySize} VM option.
     *
     * @param size       bytes to allocate as off heap cache (direct byte buffer).
     * @param blockSize  a unit bytes to allocate.
     * @param partitions the number of ByteBuffer instance. This is a hint.
     * @param coder      an instance of {@code Coder}.
     * @param <K>        key
     * @param <V>        value
     * @return
     */
    public static <K, V> BasicCache<K, V> createOffHeapCache(
            long size, int blockSize, int partitions, Coder<V> coder) {

        String name = randomName();
        Index<K, Ref<V>> index = IndexType.SIMPLE.create(16, -1);
        ByteBufferCacheAccessor<K, V> cacheAccessor = new ByteBufferCacheAccessor<K, V>();
        cacheAccessor.prepare(name, true, size, blockSize, partitions, coder);
        return new BasicCache<K, V>(name, index, cacheAccessor);
    }

    /**
     * Creates off heap cache.
     * <p/>
     * To allocate a large memory, use {@code -XX:MaxDirectMemorySize} VM option.
     *
     * @param name       a name of the cache to create.
     * @param size       bytes to allocate as off heap cache (direct byte buffer).
     * @param blockSize  a unit bytes to allocate.
     * @param partitions the number of ByteBuffer instance. This is a hint.
     * @param coder      an instance of {@code Coder}.
     * @param <K>        key
     * @param <V>        value
     * @return
     */
    public static <K, V> BasicCache<K, V> createOffHeapCache(
            String name, long size, int blockSize, int partitions, Coder<V> coder) {

        Index<K, Ref<V>> index = IndexType.SIMPLE.create(16, -1);
        ByteBufferCacheAccessor<K, V> cacheAccessor = new ByteBufferCacheAccessor<K, V>();
        cacheAccessor.prepare(name, true, size, blockSize, partitions, coder);
        return new BasicCache<K, V>(name, index, cacheAccessor);
    }

    private static class Builder<T extends Builder<T>> {

        protected Properties props;
        protected String name;
        protected CacheAccessorType cacheAccessorType;

        Builder() {
            clear();
        }

        public void clear() {
            props = new Properties();
            name = randomName();
            cacheAccessorType = CacheAccessorType.HEAP;
        }

        public T property(String key, String value) {
            if (key != null && value != null) {
                props.setProperty(key, value);
            }
            @SuppressWarnings("unchecked") T t = (T) this;
            return t;
        }

        public T property(Class<?> cls, String keyInClass, String value) {
            if (cls != null && keyInClass != null && value != null) {
                props.setProperty(PropertiesSupport.key(cls, keyInClass), value);
            }
            @SuppressWarnings("unchecked") T t = (T) this;
            return t;
        }

        public T name(String name) {
            if (name != null) {
                this.name = name;
            }
            @SuppressWarnings("unchecked") T t = (T) this;
            return t;
        }

        public T cacheAccessorType(CacheAccessorType cacheAccessorType) {
            if (cacheAccessorType != null) {
                this.cacheAccessorType = cacheAccessorType;
            }
            @SuppressWarnings("unchecked") T t = (T) this;
            return t;
        }
    }

    public static class CacheBuilder extends Builder<CacheBuilder> {

        private long initialCacheSize;
        private long maxCacheSize;
        private IndexType indexType;

        private Logger logger = LoggerFactory.getLogger(CacheBuilder.class);

        CacheBuilder() {
        }

        @Override
        public void clear() {
            super.clear();
            maxCacheSize = Long.MAX_VALUE;
            indexType = IndexType.LRU;
        }

        public CacheBuilder indexType(IndexType indexType) {
            if (indexType != null) {
                this.indexType = indexType;
            }
            return this;
        }

        public CacheBuilder maxCacheSize(long maxSize) {
            if (maxSize > 0) {
                this.maxCacheSize = maxSize;
            }
            return this;
        }

        public CacheBuilder initialCacheSize(long initialSize) {
            if (initialSize > 0) {
                this.initialCacheSize = initialSize;
            }
            return this;
        }

        public <K, V> BasicCache<K, V> build() {
            if (maxCacheSize < initialCacheSize) {
                maxCacheSize = initialCacheSize;
            }
            Index<K, Ref<V>> index = indexType.create(initialCacheSize, maxCacheSize);
            CacheAccessor<K, V> cacheAccessor = cacheAccessorType.create();
            cacheAccessor.prepare(name, props);

            logger.debug("[build] name : {}", name);
            logger.debug("[build] maxCacheSize : {}", maxCacheSize);
            logger.debug("[build] initialCacheSize : {}", initialCacheSize);
            logger.debug("[build] index : {}", index);
            logger.debug("[build] cacheAccessor : {}", cacheAccessor);
            return new BasicCache<K, V>(name, index, cacheAccessor);
        }
    }

    public static class QueueBuilder extends Builder<QueueBuilder> {

        private Logger logger = LoggerFactory.getLogger(QueueBuilder.class);

        QueueBuilder() {
        }

        public <E> BasicQueue<E> build() {
            CacheAccessor<Object, E> cacheAccessor = cacheAccessorType.create();
            cacheAccessor.prepare(name, props);

            logger.debug("[build] name : {}", name);
            logger.debug("[build] cacheAccessor : {}", cacheAccessor);
            return new BasicQueue<E>(name, cacheAccessor);
        }
    }

    public static class BlockingQueueBuilder extends Builder<QueueBuilder> {

        private int capacity = Integer.MAX_VALUE;

        private Logger logger = LoggerFactory.getLogger(QueueBuilder.class);

        BlockingQueueBuilder() {
        }

        @Override
        public void clear() {
            super.clear();
            capacity = Integer.MAX_VALUE;
        }

        public BlockingQueueBuilder capacity(int c) {
            this.capacity = c;
            return this;
        }

        public <E> BasicBlockingQueue<E> build() {
            CacheAccessor<Object, E> cacheAccessor = cacheAccessorType.create();
            cacheAccessor.prepare(name, props);

            logger.debug("[build] name : {}", name);
            logger.debug("[build] cacheAccessor : {}", cacheAccessor);
            return new BasicBlockingQueue<E>(name, cacheAccessor, capacity);
        }


    }

}
