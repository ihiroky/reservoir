package net.ihiroky.reservoir;

import net.ihiroky.reservoir.accessor.BulkInfo;
import net.ihiroky.reservoir.accessor.ByteBufferCacheAccessor;
import net.ihiroky.reservoir.accessor.ByteBufferInfo;
import net.ihiroky.reservoir.accessor.FileCacheAccessor;
import net.ihiroky.reservoir.accessor.FileInfo;
import net.ihiroky.reservoir.accessor.HeapCacheAccessor;
import net.ihiroky.reservoir.accessor.MemoryMappedFileCacheAccessor;
import net.ihiroky.reservoir.accessor.RejectedAllocationHandler;
import net.ihiroky.reservoir.accessor.RejectedAllocationPolicy;
import net.ihiroky.reservoir.coder.SerializableCoder;
import net.ihiroky.reservoir.index.ConcurrentFIFOIndex;
import net.ihiroky.reservoir.index.ConcurrentLRUIndex;
import net.ihiroky.reservoir.index.FIFOIndex;
import net.ihiroky.reservoir.index.LRUIndex;
import net.ihiroky.reservoir.index.SimpleIndex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.ArrayList;
import java.util.List;
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

    public static ByteBufferCacheAccessorBuilder newByteBufferCacheAccessorBuilder() {
        return new ByteBufferCacheAccessorBuilder();
    }

    public static FileCacheAccessorBuilder newFileCacheAccessorBuilder() {
        return new FileCacheAccessorBuilder();
    }

    public static MemoryMappedFileCacheAccessorBuilder newMemoryMappedFileCacheAccessorBuilder() {
        return new MemoryMappedFileCacheAccessorBuilder();
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
        cacheAccessor.prepare(name, true, coder, new BulkInfo(size, blockSize, partitions),
                RejectedAllocationPolicy.ABORT);
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
        cacheAccessor.prepare(name, true, coder, new BulkInfo(size, blockSize, partitions),
                RejectedAllocationPolicy.ABORT);
        return new BasicCache<K, V>(name, index, cacheAccessor);
    }

    static final long UNIT_K = 1024;
    static final long UNIT_M = 1024 * UNIT_K;
    static final long UNIT_G = 1024 * UNIT_M;
    static final long DEFAULT_DIRECT_MEMORY_SIZE = 64 * UNIT_M; // JVM default value for direct byte buffer.

    private static class LazyInitializationFieldHolder {
        static long MAX_DIRECT_MEMORY_SIZE = parseMaxDirectMemoryValue();

        static final String KEY_MAX_DIRECT_MEMORY_SIZE = "-XX:MaxDirectMemorySize";

        static long parseMaxDirectMemoryValue() {
            RuntimeMXBean bean = ManagementFactory.getRuntimeMXBean();
            for (String inputArgument : bean.getInputArguments()) {
                if (inputArgument.startsWith(KEY_MAX_DIRECT_MEMORY_SIZE)) {
                    int eq = inputArgument.indexOf('=');
                    if (eq == -1) {

                    }
                    int tail = inputArgument.length() - 1;
                    char unitMark = Character.toUpperCase(inputArgument.charAt(tail));
                    long value = (unitMark >= 0 && unitMark <= 9)
                            ? Long.parseLong(inputArgument.substring(eq + 1))
                            : Long.parseLong(inputArgument.substring(eq + 1, tail));
                    switch (unitMark) {
                        case 'K':
                            return value * UNIT_K;
                        case 'M':
                            return value * UNIT_M;
                        case 'G':
                            return value * UNIT_G;
                        default:
                            return value;
                    }
                }
            }
            return DEFAULT_DIRECT_MEMORY_SIZE;
        }
    }

    public static long getMaxDirectMemorySize() {
        return LazyInitializationFieldHolder.MAX_DIRECT_MEMORY_SIZE;
    }

    private static abstract class CacheAccessorBuilder<T extends CacheAccessorBuilder<T>> {
        int blockSize;
        int partitionsHint;
        RejectedAllocationHandler rejectedAllocationHandler;
        Class<?> coderClass;

        static final int MIN_BYTES_PER_BLOCK = 8;
        static final int DEFAULT_BLOCK_SIZE = 512;
        static final int DEFAULT_PARTITIONS = 1;

        private CacheAccessorBuilder() {
            clear();
        }

        public void clear() {
            blockSize = DEFAULT_BLOCK_SIZE;
            partitionsHint = DEFAULT_PARTITIONS;
            rejectedAllocationHandler = RejectedAllocationPolicy.ABORT;
            coderClass = SerializableCoder.class;
        }

        @SuppressWarnings("unchecked")
        private T t() {
            return (T) this;
        }

        public T blockSize(int bs) {
            if (bs < MIN_BYTES_PER_BLOCK) {
                throw new IllegalArgumentException("[blockSize] bs must be >= 8");
            }
            this.blockSize = bs;
            return t();
        }

        public T partitionsHint(int partitions) {
            if (partitions < 1) {
                throw new IllegalArgumentException("[partitionsHint] partitions");
            }
            this.partitionsHint = partitions;
            return t();
        }

        public T rejectedAllocationHandler(RejectedAllocationHandler rah) {
            if (rah == null) {
                throw new NullPointerException("[rejectedAllocationHandler] handler");
            }
            this.rejectedAllocationHandler = rah;
            return t();
        }

        public T coderClass(Class<?> cc) {
            if (cc == null) {
                throw new NullPointerException("[coderClass] cc");
            }
            this.coderClass = cc;
            return t();
        }

        abstract <K, V> CacheAccessor<K, V> build(String name);
    }

    public static final class ByteBufferCacheAccessorBuilder
            extends CacheAccessorBuilder<ByteBufferCacheAccessorBuilder> {
        boolean direct;
        int usagePercent;
        List<ByteBufferInfo> byteBufferInfoList;

        static final int DEFAULT_USAGE_PERCENT = 90;
        private ByteBufferCacheAccessorBuilder() {
        }

        public void clear() {
            super.clear();
            direct = false;
            usagePercent = DEFAULT_USAGE_PERCENT;
            byteBufferInfoList = new ArrayList<ByteBufferInfo>();
        }

        @Override
        @SuppressWarnings("unchecked")
        <K, V> CacheAccessor<K, V> build(String name) {
            Coder<V> coder;
            try {
                coder = (Coder<V>) coderClass.newInstance();
            } catch (Exception e) {
                throw new RuntimeException("[build] coderClass " + coderClass + " is not a subtype of " + Coder.class);
            }

            if ( ! byteBufferInfoList.isEmpty()) {
                ByteBufferCacheAccessor<K, V> accessor = new ByteBufferCacheAccessor<K, V>();
                accessor.prepare(name, blockSize, coder, byteBufferInfoList, rejectedAllocationHandler);
                return accessor;
            }

            BulkInfo bulkInfo = new BulkInfo(getMaxDirectMemorySize() / 100 * usagePercent, blockSize, partitionsHint);
            ByteBufferCacheAccessor<K, V> accessor = new ByteBufferCacheAccessor<K, V>();
            accessor.prepare(name, direct, coder, bulkInfo, rejectedAllocationHandler);
            return accessor;
        }

        public ByteBufferCacheAccessorBuilder direct(boolean d) {
            this.direct = d;
            return this;
        }

        public ByteBufferCacheAccessorBuilder usagePercent(int percent) {
            if (percent <= 0) {
                throw new IllegalArgumentException("[usagePercent] percent must be > 0");
            }
            this.usagePercent = percent;
            return this;
        }

        public ByteBufferCacheAccessorBuilder byteBufferInfo(boolean direct, int capacity) {
            if (capacity < MIN_BYTES_PER_BLOCK) {
                throw new IllegalStateException("[byteBufferInfo] capacity must be >= " + MIN_BYTES_PER_BLOCK);
            }
            byteBufferInfoList.add(new ByteBufferInfo(direct, capacity));
            return this;
        }
    }

    public static class FileCacheAccessorBuilder extends CacheAccessorBuilder<FileCacheAccessorBuilder> {

        File directory;
        FileInfo.Mode mode;
        long totalSize;
        List<FileInfo> fileInfoList = new ArrayList<FileInfo>();

        static final File DEFAULT_DIRECTORY = new File(".");

        private FileCacheAccessorBuilder() {
        }

        @Override
        public void clear() {
            super.clear();
            directory = DEFAULT_DIRECTORY;
            mode = FileInfo.Mode.READ_WRITE;
            totalSize = DEFAULT_DIRECT_MEMORY_SIZE;
        }

        @Override
        @SuppressWarnings("unchecked")
        <K, V> CacheAccessor<K, V> build(String name) {
            Coder<V> coder;
            try {
                coder = (Coder<V>) coderClass.newInstance();
            } catch (Exception e) {
                throw new RuntimeException("[build] coderClass " + coderClass + " is not a subtype of " + Coder.class);
            }

            try {
                if ( ! fileInfoList.isEmpty()) {
                    FileCacheAccessor<K, V> accessor = createInstance();
                    accessor.prepare(name, blockSize, coder, fileInfoList, rejectedAllocationHandler);
                    return accessor;
                }

                BulkInfo bulkInfo = new BulkInfo(totalSize, blockSize, partitionsHint);
                FileCacheAccessor<K, V> accessor = createInstance();
                accessor.prepare(name, coder, directory, mode, bulkInfo, rejectedAllocationHandler);
                return accessor;
            } catch (IOException ioe) {
                throw new RuntimeException("failed to prepare CacheAccessor.", ioe);
            }
        }

        protected <K, V> FileCacheAccessor<K, V> createInstance() {
            return new FileCacheAccessor<K, V>();
        }

        public FileCacheAccessorBuilder directory(File dir) {
            if (dir == null) {
                throw new NullPointerException("[directory] dir");
            }
            this.directory = dir;
            return this;
        }

        public FileCacheAccessorBuilder mode(FileInfo.Mode m) {
            if (m == null) {
                throw new NullPointerException("[mode] m");
            }
            this.mode = m;
            return this;
        }

        public FileCacheAccessorBuilder totalSize(long ts) {
            if (ts < MIN_BYTES_PER_BLOCK) {
                throw new IllegalArgumentException("[totalSize] ts must be >= " + MIN_BYTES_PER_BLOCK);
            }
            this.totalSize = ts;
            return this;
        }

        public FileCacheAccessorBuilder fileInfo(String path, long size, FileInfo.Mode m) {
            if (path == null) {
                throw new NullPointerException("[fileInfo] path");
            }
            if (size < MIN_BYTES_PER_BLOCK) {
                throw new IllegalArgumentException("[fileInfo] size must be >= " + MIN_BYTES_PER_BLOCK);
            }
            if (m == null) {
                throw new NullPointerException("[fileInfo] m");
            }
            this.fileInfoList.add(new FileInfo(path, size, m));
            return this;
        }
    }

    public static class MemoryMappedFileCacheAccessorBuilder extends FileCacheAccessorBuilder {

        private MemoryMappedFileCacheAccessorBuilder() {
        }

        @Override
        protected <K, V> FileCacheAccessor<K, V> createInstance() {
            return new MemoryMappedFileCacheAccessor<K, V>();
        }
    }

    private static class Builder<T extends Builder<T>> {

        Properties props;
        String name;
        CacheAccessorType cacheAccessorType;
        CacheAccessorBuilder<?> cacheAccessorBuilder;

        Builder() {
            clear();
        }

        public void clear() {
            props = new Properties();
            name = randomName();
            cacheAccessorType = CacheAccessorType.HEAP;
            cacheAccessorBuilder = null;
        }

        @SuppressWarnings("unchecked")
        private T t() {
            return (T) this;
        }

        public T property(String key, String value) {
            if (key == null) {
                throw new NullPointerException("[property] key");
            }
            if (value == null) {
                throw new NullPointerException("[property] value");
            }
            props.setProperty(key, value);
            return t();
        }

        public T property(Class<?> cls, String keyInClass, String value) {
            if (cls == null) {
                throw new NullPointerException("[property] cls");
            }
            if (keyInClass == null) {
                throw new NullPointerException("[property] keyInClass");
            }
            if (value == null) {
                throw new NullPointerException("[property] value");
            }
            props.setProperty(PropertiesSupport.key(cls, keyInClass), value);
            return t();
        }

        public T name(String name) {
            if (name == null) {
                throw new NullPointerException("[name] name");
            }
            this.name = name;
            return t();
        }

        public T cacheAccessorType(CacheAccessorType cat) {
            if (cat == null) {
                throw new NullPointerException("[cacheAccessorType] cat");
            }
            this.cacheAccessorType = cat;
            return t();
        }

        public T cacheAccessorBuilder(CacheAccessorBuilder<?> cab) {
            if (cab == null) {
                throw new NullPointerException("[cacheAccessorBuilder] cab");
            }
            this.cacheAccessorBuilder = cab;
            return t();
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

        public CacheBuilder indexType(IndexType it) {
            if (it == null) {
                throw new NullPointerException("[indexType] it");
            }
            this.indexType = it;
            return this;
        }

        public CacheBuilder maxCacheSize(long maxSize) {
            if (maxSize <= 0) {
                throw new IllegalArgumentException("[maxCacheSize] maxSize must be > 0");
            }
            this.maxCacheSize = maxSize;
            return this;
        }

        public CacheBuilder initialCacheSize(long initialSize) {
            if (initialSize <= 0) {
                throw new IllegalArgumentException("[initialCacheSize] initialSize must be > 0");
            }
            this.initialCacheSize = initialSize;
            return this;
        }

        public <K, V> BasicCache<K, V> build() {
            if (name == null || name.length() == 0) {
                throw new IllegalStateException("[build] name is null or empty.");
            }
            if (maxCacheSize < initialCacheSize) {
                maxCacheSize = initialCacheSize;
            }
            Index<K, Ref<V>> index = indexType.create(initialCacheSize, maxCacheSize);
            if (cacheAccessorBuilder != null) {
                CacheAccessor<K, V> accessor = cacheAccessorBuilder.build(name);
                return new BasicCache<K, V>(name, index, accessor);
            }

            CacheAccessor<K, V> cacheAccessor = cacheAccessorType.create();
            cacheAccessor.prepare(name, props);

            logger.debug("[build] name : {}", name);
            logger.debug("[build] maxCacheSize : {}", maxCacheSize);
            logger.debug("[build] initialCacheSize : {}", initialCacheSize);
            logger.debug("[build] index : {}", index);
            logger.debug("[build] cacheAccessor : {}", cacheAccessor);
            logger.debug("[build] properties : {}", props);
            return new BasicCache<K, V>(name, index, cacheAccessor);
        }
    }

    public static class QueueBuilder extends Builder<QueueBuilder> {

        private Logger logger = LoggerFactory.getLogger(QueueBuilder.class);

        QueueBuilder() {
        }

        public <E> BasicQueue<E> build() {
            if (name == null || name.length() == 0) {
                throw new IllegalStateException("[build] name is null or empty.");
            }
            if (cacheAccessorBuilder != null) {
                CacheAccessor<Object, E> cacheAccessor = cacheAccessorBuilder.build(name);
                return new BasicQueue<E>(name, cacheAccessor);
            }

            CacheAccessor<Object, E> cacheAccessor = cacheAccessorType.create();
            cacheAccessor.prepare(name, props);

            logger.debug("[build] name : {}", name);
            logger.debug("[build] cacheAccessor : {}", cacheAccessor);
            logger.debug("[build] properties : {}", props);
            return new BasicQueue<E>(name, cacheAccessor);
        }
    }

    public static class BlockingQueueBuilder extends Builder<BlockingQueueBuilder> {

        private int capacity = Integer.MAX_VALUE;

        private Logger logger = LoggerFactory.getLogger(BlockingQueueBuilder.class);

        BlockingQueueBuilder() {
        }

        @Override
        public void clear() {
            super.clear();
            capacity = Integer.MAX_VALUE;
        }

        public BlockingQueueBuilder capacity(int c) {
            if (c < 1) {
                throw new IllegalArgumentException("[capacity] c must be >= 1");
            }
            this.capacity = c;
            return this;
        }

        public <E> BasicBlockingQueue<E> build() {
            if (name == null || name.length() == 0) {
                throw new IllegalStateException("[build] name is null or empty.");
            }
            if (cacheAccessorBuilder != null) {
                CacheAccessor<Object, E> cacheAccessor = cacheAccessorBuilder.build(name);
                return new BasicBlockingQueue<E>(name, cacheAccessor, capacity);
            }

            CacheAccessor<Object, E> cacheAccessor = cacheAccessorType.create();
            cacheAccessor.prepare(name, props);

            logger.debug("[build] name : {}", name);
            logger.debug("[build] capacity : {}", capacity);
            logger.debug("[build] cacheAccessor : {}", cacheAccessor);
            logger.debug("[build] properties : {}", props);
            return new BasicBlockingQueue<E>(name, cacheAccessor, capacity);
        }


    }

}
