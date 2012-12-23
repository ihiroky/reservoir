package net.ihiroky.reservoir;

import net.ihiroky.reservoir.storage.BulkInfo;
import net.ihiroky.reservoir.storage.ByteBufferStorageAccessor;
import net.ihiroky.reservoir.storage.ByteBufferInfo;
import net.ihiroky.reservoir.storage.FileStorageAccessor;
import net.ihiroky.reservoir.storage.FileInfo;
import net.ihiroky.reservoir.storage.HeapStorageAccessor;
import net.ihiroky.reservoir.storage.MemoryMappedFileStorageAccessor;
import net.ihiroky.reservoir.storage.RejectedAllocationHandler;
import net.ihiroky.reservoir.storage.RejectedAllocationPolicy;
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
            <K, V> StorageAccessor<K, V> create() {
                return new HeapStorageAccessor<K, V>();
            }
        },
        BYTE_BUFFER {
            @Override
            <K, V> StorageAccessor<K, V> create() {
                return new ByteBufferStorageAccessor<K, V>();
            }
        },
        MEMORY_MAPPED_FILE {
            @Override
            <K, V> StorageAccessor<K, V> create() {
                return new MemoryMappedFileStorageAccessor<K, V>();
            }
        },
        FILE {
            @Override
            <K, V> StorageAccessor<K, V> create() {
                return new FileStorageAccessor<K, V>();
            }
        },;

        abstract <K, V> StorageAccessor<K, V> create();
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

    public static ByteBufferStorageAccessorBuilder newByteBufferCacheAccessorBuilder() {
        return new ByteBufferStorageAccessorBuilder();
    }

    public static FileStorageAccessorBuilder newFileCacheAccessorBuilder() {
        return new FileStorageAccessorBuilder();
    }

    public static MemoryMappedFileStorageAccessorBuilder newMemoryMappedFileCacheAccessorBuilder() {
        return new MemoryMappedFileStorageAccessorBuilder();
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
        ByteBufferStorageAccessor<K, V> cacheAccessor = new ByteBufferStorageAccessor<K, V>();
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
        ByteBufferStorageAccessor<K, V> cacheAccessor = new ByteBufferStorageAccessor<K, V>();
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

    private static abstract class StorageAccessorBuilder<T extends StorageAccessorBuilder<T>> {
        int blockSize;
        int partitionsHint;
        RejectedAllocationHandler rejectedAllocationHandler;
        Class<?> coderClass;

        static final int MIN_BYTES_PER_BLOCK = 8;
        static final int DEFAULT_BLOCK_SIZE = 512;
        static final int DEFAULT_PARTITIONS = 1;

        private StorageAccessorBuilder() {
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

        abstract <K, V> StorageAccessor<K, V> build(String name);
    }

    public static final class ByteBufferStorageAccessorBuilder
            extends StorageAccessorBuilder<ByteBufferStorageAccessorBuilder> {
        boolean direct;
        int usagePercent;
        List<ByteBufferInfo> byteBufferInfoList;

        static final int DEFAULT_USAGE_PERCENT = 90;
        private ByteBufferStorageAccessorBuilder() {
        }

        public void clear() {
            super.clear();
            direct = false;
            usagePercent = DEFAULT_USAGE_PERCENT;
            byteBufferInfoList = new ArrayList<ByteBufferInfo>();
        }

        @Override
        @SuppressWarnings("unchecked")
        <K, V> StorageAccessor<K, V> build(String name) {
            Coder<V> coder;
            try {
                coder = (Coder<V>) coderClass.newInstance();
            } catch (Exception e) {
                throw new RuntimeException("[build] coderClass " + coderClass + " is not a subtype of " + Coder.class);
            }

            if ( ! byteBufferInfoList.isEmpty()) {
                ByteBufferStorageAccessor<K, V> accessor = new ByteBufferStorageAccessor<K, V>();
                accessor.prepare(name, blockSize, coder, byteBufferInfoList, rejectedAllocationHandler);
                return accessor;
            }

            BulkInfo bulkInfo = new BulkInfo(getMaxDirectMemorySize() / 100 * usagePercent, blockSize, partitionsHint);
            ByteBufferStorageAccessor<K, V> accessor = new ByteBufferStorageAccessor<K, V>();
            accessor.prepare(name, direct, coder, bulkInfo, rejectedAllocationHandler);
            return accessor;
        }

        public ByteBufferStorageAccessorBuilder direct(boolean d) {
            this.direct = d;
            return this;
        }

        public ByteBufferStorageAccessorBuilder usagePercent(int percent) {
            if (percent <= 0) {
                throw new IllegalArgumentException("[usagePercent] percent must be > 0");
            }
            this.usagePercent = percent;
            return this;
        }

        public ByteBufferStorageAccessorBuilder byteBufferInfo(boolean direct, int capacity) {
            if (capacity < MIN_BYTES_PER_BLOCK) {
                throw new IllegalStateException("[byteBufferInfo] capacity must be >= " + MIN_BYTES_PER_BLOCK);
            }
            byteBufferInfoList.add(new ByteBufferInfo(direct, capacity));
            return this;
        }
    }

    public static class FileStorageAccessorBuilder extends StorageAccessorBuilder<FileStorageAccessorBuilder> {

        File directory;
        FileInfo.Mode mode;
        long totalSize;
        List<FileInfo> fileInfoList = new ArrayList<FileInfo>();

        static final File DEFAULT_DIRECTORY = new File(".");

        private FileStorageAccessorBuilder() {
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
        <K, V> StorageAccessor<K, V> build(String name) {
            Coder<V> coder;
            try {
                coder = (Coder<V>) coderClass.newInstance();
            } catch (Exception e) {
                throw new RuntimeException("[build] coderClass " + coderClass + " is not a subtype of " + Coder.class);
            }

            try {
                if ( ! fileInfoList.isEmpty()) {
                    FileStorageAccessor<K, V> accessor = createInstance();
                    accessor.prepare(name, blockSize, coder, fileInfoList, rejectedAllocationHandler);
                    return accessor;
                }

                BulkInfo bulkInfo = new BulkInfo(totalSize, blockSize, partitionsHint);
                FileStorageAccessor<K, V> accessor = createInstance();
                accessor.prepare(name, coder, directory, mode, bulkInfo, rejectedAllocationHandler);
                return accessor;
            } catch (IOException ioe) {
                throw new RuntimeException("failed to prepare StorageAccessor.", ioe);
            }
        }

        protected <K, V> FileStorageAccessor<K, V> createInstance() {
            return new FileStorageAccessor<K, V>();
        }

        public FileStorageAccessorBuilder directory(File dir) {
            if (dir == null) {
                throw new NullPointerException("[directory] dir");
            }
            this.directory = dir;
            return this;
        }

        public FileStorageAccessorBuilder mode(FileInfo.Mode m) {
            if (m == null) {
                throw new NullPointerException("[mode] m");
            }
            this.mode = m;
            return this;
        }

        public FileStorageAccessorBuilder totalSize(long ts) {
            if (ts < MIN_BYTES_PER_BLOCK) {
                throw new IllegalArgumentException("[totalSize] ts must be >= " + MIN_BYTES_PER_BLOCK);
            }
            this.totalSize = ts;
            return this;
        }

        public FileStorageAccessorBuilder fileInfo(String path, long size, FileInfo.Mode m) {
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

    public static class MemoryMappedFileStorageAccessorBuilder extends FileStorageAccessorBuilder {

        private MemoryMappedFileStorageAccessorBuilder() {
        }

        @Override
        protected <K, V> FileStorageAccessor<K, V> createInstance() {
            return new MemoryMappedFileStorageAccessor<K, V>();
        }
    }

    private static class Builder<T extends Builder<T>> {

        Properties props;
        String name;
        CacheAccessorType cacheAccessorType;
        StorageAccessorBuilder<?> storageAccessorBuilder;

        Builder() {
            clear();
        }

        public void clear() {
            props = new Properties();
            name = randomName();
            cacheAccessorType = CacheAccessorType.HEAP;
            storageAccessorBuilder = null;
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

        public T cacheAccessorBuilder(StorageAccessorBuilder<?> cab) {
            if (cab == null) {
                throw new NullPointerException("[storageAccessorBuilder] cab");
            }
            this.storageAccessorBuilder = cab;
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
            if (storageAccessorBuilder != null) {
                StorageAccessor<K, V> accessor = storageAccessorBuilder.build(name);
                return new BasicCache<K, V>(name, index, accessor);
            }

            StorageAccessor<K, V> storageAccessor = cacheAccessorType.create();
            storageAccessor.prepare(name, props);

            logger.debug("[build] name : {}", name);
            logger.debug("[build] maxCacheSize : {}", maxCacheSize);
            logger.debug("[build] initialCacheSize : {}", initialCacheSize);
            logger.debug("[build] index : {}", index);
            logger.debug("[build] storageAccessor : {}", storageAccessor);
            logger.debug("[build] properties : {}", props);
            return new BasicCache<K, V>(name, index, storageAccessor);
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
            if (storageAccessorBuilder != null) {
                StorageAccessor<Object, E> storageAccessor = storageAccessorBuilder.build(name);
                return new BasicQueue<E>(name, storageAccessor);
            }

            StorageAccessor<Object, E> storageAccessor = cacheAccessorType.create();
            storageAccessor.prepare(name, props);

            logger.debug("[build] name : {}", name);
            logger.debug("[build] storageAccessor : {}", storageAccessor);
            logger.debug("[build] properties : {}", props);
            return new BasicQueue<E>(name, storageAccessor);
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
            if (storageAccessorBuilder != null) {
                StorageAccessor<Object, E> storageAccessor = storageAccessorBuilder.build(name);
                return new BasicBlockingQueue<E>(name, storageAccessor, capacity);
            }

            StorageAccessor<Object, E> storageAccessor = cacheAccessorType.create();
            storageAccessor.prepare(name, props);

            logger.debug("[build] name : {}", name);
            logger.debug("[build] capacity : {}", capacity);
            logger.debug("[build] storageAccessor : {}", storageAccessor);
            logger.debug("[build] properties : {}", props);
            return new BasicBlockingQueue<E>(name, storageAccessor, capacity);
        }


    }

}
