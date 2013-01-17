package net.ihiroky.reservoir;

import net.ihiroky.reservoir.coder.SerializableCoder;
import net.ihiroky.reservoir.index.ConcurrentFIFOIndex;
import net.ihiroky.reservoir.index.ConcurrentLRUIndex;
import net.ihiroky.reservoir.index.FIFOIndex;
import net.ihiroky.reservoir.index.LRUIndex;
import net.ihiroky.reservoir.index.SimpleIndex;
import net.ihiroky.reservoir.storage.BulkInfo;
import net.ihiroky.reservoir.storage.ByteBufferInfo;
import net.ihiroky.reservoir.storage.ByteBufferStorageAccessor;
import net.ihiroky.reservoir.storage.FileInfo;
import net.ihiroky.reservoir.storage.FileStorageAccessor;
import net.ihiroky.reservoir.storage.HeapStorageAccessor;
import net.ihiroky.reservoir.storage.MemoryMappedFileStorageAccessor;
import net.ihiroky.reservoir.storage.RejectedAllocationHandler;
import net.ihiroky.reservoir.storage.RejectedAllocationPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Provides for factory methods to create instances of {@link net.ihiroky.reservoir.BasicCache},
 * {@link net.ihiroky.reservoir.BasicQueue}, {@link net.ihiroky.reservoir.BasicBlockingQueue}.
 *
 * @author Hiroki Itoh
 */
public final class Reservoir {

    private Reservoir() {
        throw new AssertionError(Reservoir.class.getName() + " can't be instantiated.");
    }

    /**
     * An index type used by {@link net.ihiroky.reservoir.BasicCache}
     */
    public enum IndexType {
        /** A type of index that has no size limitation */
        SIMPLE {
            /**
             * {@inheritDoc}
             */
            @Override
            <K, V> Index<K, Ref<V>> create(long initialSize, long maxSize) {
                return new SimpleIndex<K, Ref<V>>(toInt(initialSize));
            }
        },
        /** A type of index that has size limitation with LRU algorithm */
        LRU {
            /**
             * {@inheritDoc}
             */
            @Override
            <K, V> Index<K, Ref<V>> create(long initialSize, long maxSize) {
                return new ConcurrentLRUIndex<K, Ref<V>>(initialSize, maxSize);
            }
        },
        /** A type of index that has size limitation with FIFO algorithm */
        FIFO {
            /**
             * {@inheritDoc}
             */
            @Override
            <K, V> Index<K, Ref<V>> create(long initialSize, long maxSize) {
                return new ConcurrentFIFOIndex<K, Ref<V>>(initialSize, maxSize);
            }
        },
        /** A type of index that has size limitation with LRU algorithm, not synchronized */
        FRAGILE_LRU {
            /**
             * {@inheritDoc}
             */
            @Override
            <K, V> Index<K, Ref<V>> create(long initialSize, long maxSize) {
                return new LRUIndex<K, Ref<V>>(toInt(initialSize), toInt(maxSize));
            }
        },
        /** A type of  index that has size limitation with FIFO algorithm, not synchronized */
        FRAGILE_FIFO {
            /**
             * {@inheritDoc}
             * Returns the instance of {@link net.ihiroky.reservoir.index.FIFOIndex}.
             */
            @Override
            <K, V> Index<K, Ref<V>> create(long initialSize, long maxSize) {
                return new FIFOIndex<K, Ref<V>>(toInt(initialSize), toInt(maxSize));
            }
        },
        ;

        /**
         * Converts a specified long value to int. If {@code value} is larger than {@code Integer.MAX_VALUE},
         * Returns {@code Integer.MAX_VALUE}.
         *
         * @param value a long value to be converted to int
         * @return the int value
         */
        int toInt(long value) {
            return (value > Integer.MAX_VALUE) ? Integer.MAX_VALUE : (int) value;
        }

        /**
         * Creates {@link net.ihiroky.reservoir.Index}.
         *
         * @param initialSize an initial index size if configurable.
         * @param maxSize an max size of the index if configurable.
         * @param <K> the type of the keys
         * @param <V> the type of mapped values
         * @return the instance of {@link net.ihiroky.reservoir.Index}
         */
        abstract <K, V> Index<K, Ref<V>> create(long initialSize, long maxSize);
    }

    /**
     * A storage accessor type to create an instance of {@link net.ihiroky.reservoir.StorageAccessor}.
     */
    public enum StorageAccessorType {
        /** A type of a heap storage accessor */
        HEAP {
            /**
             * {@inheritDoc}
             */
            @Override
            <K, V> StorageAccessor<K, V> create() {
                return new HeapStorageAccessor<K, V>();
            }
        },
        /** A type of {@code java.nio.ByteBuffer} storage accessor */
        BYTE_BUFFER {
            /**
             * {@inheritDoc}
             */
            @Override
            <K, V> StorageAccessor<K, V> create() {
                return new ByteBufferStorageAccessor<K, V>();
            }
        },
        /** A type of {@code java.nio.MappedByteBuffer} storage accessor */
        MEMORY_MAPPED_FILE {
            /**
             * {@inheritDoc}
             */
            @Override
            <K, V> StorageAccessor<K, V> create() {
                return new MemoryMappedFileStorageAccessor<K, V>();
            }
        },
        /** A type of {@code java.io.RandomAccessFile} storage accessor */
        FILE {
            /**
             * {@inheritDoc}
             */
            @Override
            <K, V> StorageAccessor<K, V> create() {
                return new FileStorageAccessor<K, V>();
            }
        },;

        /**
         * Creates an instance of {@link net.ihiroky.reservoir.StorageAccessor}.
         * @param <K> the type of the keys.
         * @param <V> the type of the values to be stored.
         * @return
         */
        abstract <K, V> StorageAccessor<K, V> create();
    }

    /**
     * Creates a builder to create {@link net.ihiroky.reservoir.BasicCache}.
     * @return the cache builder
     */
    public static CacheBuilder newCacheBuilder() {
        return new CacheBuilder();
    }

    /**
     * Creates a builder to create {@link net.ihiroky.reservoir.BasicQueue}.
     * @return the queue builder
     */
    public static QueueBuilder newQueueBuilder() {
        return new QueueBuilder();
    }

    /**
     * Creates a builder to create {@link net.ihiroky.reservoir.BasicBlockingQueue}.
     * @return the blocking queue builder
     */
    public static BlockingQueueBuilder newBlockingQueueBuilder() {
        return new BlockingQueueBuilder();
    }

    /**
     * Creates a builder to create {@link net.ihiroky.reservoir.storage.ByteBufferStorageAccessor}.
     * @return the byte buffer storage accessor builder
     */
    public static ByteBufferStorageAccessorBuilder newByteBufferCacheAccessorBuilder() {
        return new ByteBufferStorageAccessorBuilder();
    }

    /**
     * Creates a builder to create {@link net.ihiroky.reservoir.storage.FileStorageAccessor}.
     * @return the file storage accessor builder
     */
    public static FileStorageAccessorBuilder newFileCacheAccessorBuilder() {
        return new FileStorageAccessorBuilder();
    }

    /**
     * Creates a builder to create {@link net.ihiroky.reservoir.storage.MemoryMappedFileStorageAccessor}.
     * @return the memory mapped file storage accessor builder
     */
    public static MemoryMappedFileStorageAccessorBuilder newMemoryMappedFileCacheAccessorBuilder() {
        return new MemoryMappedFileStorageAccessorBuilder();
    }

    private static String randomName() {
        return String.valueOf((long) (Math.random() * Long.MAX_VALUE));
    }

    /**
     * Creates an off heap cache.
     * <p/>
     * the cache to create is named by {@code String.valueOf(Double.doubleToLongBits(Math.random()))}.
     * To allocate a large memory, use {@code -XX:MaxDirectMemorySize} VM option.
     *
     * @param size       bytes to allocate as off heap cache (direct byte buffer)
     * @param blockSize  a unit bytes to allocate
     * @param partitions the number of ByteBuffer instance. This is a hint
     * @param coder      an instance of {@code Coder}
     * @param <K>        the type of the keys
     * @param <V>        the type of the mapped values
     * @return the off heap cache
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
     * @param name       a name of the cache to create
     * @param size       bytes to allocate as off heap cache (direct byte buffer)
     * @param blockSize  a unit bytes to allocate
     * @param partitions the number of ByteBuffer instance. This is a hint
     * @param coder      an instance of {@code Coder}
     * @param <K>        the type of the keys
     * @param <V>        the type of mapped values
     * @return the off heap cache
     */
    public static <K, V> BasicCache<K, V> createOffHeapCache(
            String name, long size, int blockSize, int partitions, Coder<V> coder) {

        Index<K, Ref<V>> index = IndexType.SIMPLE.create(16, -1);
        ByteBufferStorageAccessor<K, V> cacheAccessor = new ByteBufferStorageAccessor<K, V>();
        cacheAccessor.prepare(name, true, coder, new BulkInfo(size, blockSize, partitions),
                RejectedAllocationPolicy.ABORT);
        return new BasicCache<K, V>(name, index, cacheAccessor);
    }

    /** The unit of K */
    static final long UNIT_K = 1024;

    /** Thre unit of M */
    static final long UNIT_M = 1024 * UNIT_K;

    /** The unit of G */
    static final long UNIT_G = 1024 * UNIT_M;

    /**  JVM default value for direct byte buffer */
    static final long DEFAULT_DIRECT_MEMORY_SIZE = 64 * UNIT_M;

    /**
     * Provides for variables which is lazily initialized.
     * In short, lazy initialization holder class idiom.
     *
     * TODO set DEFAULT_PAGE_SIZE from configurations.
     */
    private static class LazyInitializationFieldHolder {

        static long maxDirectMemorySize = getMaxDirectMemorySize();
        static int maxDirectBufferCapacity = maxDirectBufferCapacity();

        private static final String KEY_MAX_DIRECT_MEMORY_SIZE = "-XX:MaxDirectMemorySize";
        private static final int DEFAULT_PAGE_SIZE = 4096; // TODO set by configuration
        private static final int USABLE_DIRECT_MEMORY_ON_32 = 1856 * 1024 * 1024;

        // the max value of MaxDirectMemorySize on Solaris10 32 bit jvm is 2^31 - 1 bytes.
        // the max value of usable MaxDirectMemorySize on Solaris10 32 bit jvm is 1856MB.

        /**
         * Returns the max direct memory size. If sun.misc.VM is available,
         * use system setting; otherwise use the value of JVM option or DEFAULT_DIRECT_MEMORY_SIZE.
         * @return the max direct memory size.
         */
        static long getMaxDirectMemorySize() {
            try {
                return getSystemMaxDirectMemorySize();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return parseArgumentMaxDirectMemorySize();
        }

        /**
         * Returns direct memory size that {@code java.nio.ByteBuffer#allocateDirect(int)} is successfully called.
         * @return direct memory size per {@code java.nio.ByteBuffer} instance
         */
        static int maxDirectBufferCapacity() {
            int pageSize = getPageSize();
            if (is32Bit()) {
                int allocatableSize = (int) maxDirectMemorySize - pageSize;
                return (allocatableSize <= USABLE_DIRECT_MEMORY_ON_32) ? allocatableSize : USABLE_DIRECT_MEMORY_ON_32;
            }
            return (maxDirectMemorySize <= Integer.MAX_VALUE)
                    ? (int) maxDirectMemorySize - pageSize : Integer.MAX_VALUE - pageSize;
        }

        /**
         * Returns true if jvm architecture is 32 bits.
         * @return true if jvm architecture is 32 bits
         */
        static boolean is32Bit() {
            String s = System.getProperty("sun.arch.data.model");
            if (s != null) {
                return s.equals("32");
            }
            s = System.getProperty("java.vm.name");
            if (s != null) {
                return !s.contains("64");
            }
            s = System.getProperty("os.arch");
            if (s != null) {
                return !s.contains("64");
            }
            throw new RuntimeException("failed to judge jvm architecture.");
        }

        /**
         * Returns the value of the JVM option "-XX:MaxDirectMemorySize".
         * @return the value of the JVM option "-XX:MaxDirectMemorySize"
         */
        static long parseArgumentMaxDirectMemorySize() {
            RuntimeMXBean bean = ManagementFactory.getRuntimeMXBean();
            for (String inputArgument : bean.getInputArguments()) {
                if (inputArgument.startsWith(KEY_MAX_DIRECT_MEMORY_SIZE)) {
                    int eq = inputArgument.indexOf('=');
                    if (eq == -1) {
                        return DEFAULT_DIRECT_MEMORY_SIZE;
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

        /**
         * Returns max direct memory size configured by the system.
         * @return max direct memory size configured by the system
         */
        static long getSystemMaxDirectMemorySize() {
            try {
                Class<?> c = Class.forName("sun.misc.VM");
                Method m = c.getDeclaredMethod("maxDirectMemory");
                return (Long) m.invoke(null);
            } catch (Exception e) {
                throw new RuntimeException("failed to call sun.misc.VM#maxDirectMemory()", e);
            }
        }

        /**
         * Returns the page size. If {@code java.nio.Bits#pageSize()} is available, use system setting;
         * otherwise use {@code DEFAULT_PAGE_SIZE}.
         * @return the page size
         */
        static int getPageSize() {
            try {
                return getSystemPageSize();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return DEFAULT_PAGE_SIZE;
        }

        /**
         * Returns the system page size
         * @return
         */
        static int getSystemPageSize() {
            try {
                Class<?> c = Class.forName("java.nio.Bits");
                Method m = c.getDeclaredMethod("pageSize");
                m.setAccessible(true);
                return (Integer) m.invoke(null);
            } catch (Exception e) {
                throw new RuntimeException("failed to call java.nio.Bits#pageSize()", e);
            }
        }
    }

    /**
     * Returns the value of the JVM option "-XX:MaxDirectMemorySize".
     * @return the value of the JVM option "-XX:MaxDirectMemorySize"
     */
    public static long getMaxDirectMemorySize() {
        return LazyInitializationFieldHolder.maxDirectMemorySize;
    }

    /**
     * Returns the capacity per direct buffer.
     * @return the capacity per direct buffer
     */
    public static int getMaxDirectBufferCapacity() {
        return LazyInitializationFieldHolder.maxDirectBufferCapacity;
    }

    /**
     * A base implementation of a builder to create an instance of {@link net.ihiroky.reservoir.StorageAccessor}.
     * @param <T> the type of the sub class
     */
    private static abstract class StorageAccessorBuilder<T extends StorageAccessorBuilder<T>> {

        /** a unit size managed by a storage manager */
        int blockSize;

        /** the hint number of storage used by a storage manager */
        int partitionsHint;

        /** a handler to control that a storage gets full */
        RejectedAllocationHandler rejectedAllocationHandler;

        /** a class object to control serialization and deserialization */
        Class<?> coderClass;

        /** the minimum number of {@code blockSize} */
        static final int MIN_BYTES_PER_BLOCK = 8;

        /** the default {@code blockSize} */
        static final int DEFAULT_BLOCK_SIZE = 512;

        /** the default {@code partitionsHint} */
        static final int DEFAULT_PARTITIONS = 1;

        private StorageAccessorBuilder() {
            reset();
        }

        /**
         * Resets parameters.
         * TODO document default values.
         */
        public void reset() {
            blockSize = DEFAULT_BLOCK_SIZE;
            partitionsHint = DEFAULT_PARTITIONS;
            rejectedAllocationHandler = RejectedAllocationPolicy.ABORT;
            coderClass = SerializableCoder.class;
        }

        /**
         * Casts this instance to T.
         * @return this instance that the type is T
         */
        @SuppressWarnings("unchecked")
        private T t() {
            return (T) this;
        }

        /**
         * Sets the {@code blockSize}.
         * @param bs blockSize to be set
         * @return this instance
         */
        public T blockSize(int bs) {
            if (bs < MIN_BYTES_PER_BLOCK) {
                throw new IllegalArgumentException("[blockSize] bs must be >= 8");
            }
            this.blockSize = bs;
            return t();
        }

        /**
         * Sets the {@code partitionsHint}.
         * @param partitions partitionsHint to be set
         * @return this instance
         */
        public T partitionsHint(int partitions) {
            if (partitions < 1) {
                throw new IllegalArgumentException("[partitionsHint] partitions");
            }
            this.partitionsHint = partitions;
            return t();
        }

        /**
         * Sets the {@code rejectedAllocationHandler}.
         * @param rah rejectedAllocationHandler to be set
         * @return this instance
         */
        public T rejectedAllocationHandler(RejectedAllocationHandler rah) {
            if (rah == null) {
                throw new NullPointerException("[rejectedAllocationHandler] handler");
            }
            this.rejectedAllocationHandler = rah;
            return t();
        }

        /**
         * Sets {@code coderClass}.
         * @param cc coderClass to be set
         * @return this instance
         */
        public T coderClass(Class<?> cc) {
            if (cc == null) {
                throw new NullPointerException("[coderClass] cc");
            }
            this.coderClass = cc;
            return t();
        }

        /**
         * Creates the instance of {@link net.ihiroky.reservoir.StorageAccessor} with specified parameters
         * by this object.
         * @param name a name of a storage accessor to build
         * @param <K> the type of the keys
         * @param <V> the type of the values to be stored
         * @return the instance of {@link net.ihiroky.reservoir.StorageAccessor}
         */
        abstract <K, V> StorageAccessor<K, V> build(String name);
    }

    /**
     * A builder to create {@link net.ihiroky.reservoir.storage.ByteBufferStorageAccessor}.
     */
    public static final class ByteBufferStorageAccessorBuilder
            extends StorageAccessorBuilder<ByteBufferStorageAccessorBuilder> {

        /** true if the direct byte buffer is used */
        boolean direct;

        /** an usage rate against the max direct memory size */
        int usagePercent;

        /**
         * a list of {@link net.ihiroky.reservoir.storage.ByteBufferInfo} to define the base
         * {@code java.nio.ByteBuffer}
         * */
        List<ByteBufferInfo> byteBufferInfoList;

        /** the default usage rate */
        static final int DEFAULT_USAGE_PERCENT = 90;

        private ByteBufferStorageAccessorBuilder() {
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void reset() {
            super.reset();
            direct = false;
            usagePercent = DEFAULT_USAGE_PERCENT;
            byteBufferInfoList = new ArrayList<ByteBufferInfo>();
        }

        /**
         * {@inheritDoc}
         */
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

        /**
         * Sets true if the direct byte buffer is used.
         * @param d true if the direct byte buffer is used.
         * @return this instance
         */
        public ByteBufferStorageAccessorBuilder direct(boolean d) {
            this.direct = d;
            return this;
        }

        /**
         * Sets the usage rate against max direct memory size.
         * @param percent the usage rate against max direct memory size
         * @return this instance
         */
        public ByteBufferStorageAccessorBuilder usagePercent(int percent) {
            if (percent <= 0) {
                throw new IllegalArgumentException("[usagePercent] percent must be > 0");
            }
            this.usagePercent = percent;
            return this;
        }

        /**
         * Adds a {@code java.nio.ByteBuffer} definition.
         * @param direct true if direct byte buffer is used by the definition
         * @param capacity a capacity of the {@code java.nio.ByteBuffer} definition
         * @return this instance
         */
        public ByteBufferStorageAccessorBuilder byteBufferInfo(boolean direct, int capacity) {
            if (capacity < MIN_BYTES_PER_BLOCK) {
                throw new IllegalStateException("[byteBufferInfo] capacity must be >= " + MIN_BYTES_PER_BLOCK);
            }
            byteBufferInfoList.add(new ByteBufferInfo(direct, capacity));
            return this;
        }
    }

    /**
     * A builder to create {@link net.ihiroky.reservoir.storage.FileStorageAccessor}.
     */
    public static class FileStorageAccessorBuilder extends StorageAccessorBuilder<FileStorageAccessorBuilder> {

        /** a directory in which base files is stored */
        File directory;

        /** file access mode */
        FileInfo.Mode mode;

        /** a size of {@code FileStorageAccessor} */
        long totalSize;

        /** a list of {@link net.ihiroky.reservoir.storage.FileInfo} to define the base file */
        List<FileInfo> fileInfoList = new ArrayList<FileInfo>();

        /** default directory in which base files is stored */
        static final File DEFAULT_DIRECTORY = new File(".");

        private FileStorageAccessorBuilder() {
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public final void reset() {
            super.reset();
            directory = DEFAULT_DIRECTORY;
            mode = FileInfo.Mode.READ_WRITE;
            totalSize = DEFAULT_DIRECT_MEMORY_SIZE;
        }

        /**
         * {@inheritDoc}
         */
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

        /**
         * Creates an instance of {@link net.ihiroky.reservoir.storage.FileStorageAccessor}.
         * @param <K> the type of key
         * @param <V> the type of values to be stored
         * @return the instance of {@code FileStorageAccessor}
         */
        protected <K, V> FileStorageAccessor<K, V> createInstance() {
            return new FileStorageAccessor<K, V>();
        }

        /**
         * Sets the {@code directory}.
         * @param dir a directory to be set.
         * @return this instance
         */
        public FileStorageAccessorBuilder directory(File dir) {
            if (dir == null) {
                throw new NullPointerException("[directory] dir");
            }
            this.directory = dir;
            return this;
        }

        /**
         * Sets the {@code mode}.
         * @param m a file access mode to be set
         * @return this instance
         */
        public FileStorageAccessorBuilder mode(FileInfo.Mode m) {
            if (m == null) {
                throw new NullPointerException("[mode] m");
            }
            this.mode = m;
            return this;
        }

        /**
         * Sets the {@code totalSize}.
         * @param ts a size of {@code FileStorageAccessor}
         * @return this instance
         */
        public FileStorageAccessorBuilder totalSize(long ts) {
            if (ts < MIN_BYTES_PER_BLOCK) {
                throw new IllegalArgumentException("[totalSize] ts must be >= " + MIN_BYTES_PER_BLOCK);
            }
            this.totalSize = ts;
            return this;
        }

        /**
         * Adds a base file definition.
         * @param path a path to the base file
         * @param size a size of the base file
         * @param m a file access mode of the base file
         * @return this instance
         */
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

    /**
     * A builder to creates {@link net.ihiroky.reservoir.storage.MemoryMappedFileStorageAccessor}.
     */
    public static class MemoryMappedFileStorageAccessorBuilder extends FileStorageAccessorBuilder {

        private MemoryMappedFileStorageAccessorBuilder() {
        }

        /**
         * {@inheritDoc}
         * Returns {@link net.ihiroky.reservoir.storage.MemoryMappedFileStorageAccessor}.
         */
        @Override
        protected <K, V> FileStorageAccessor<K, V> createInstance() {
            return new MemoryMappedFileStorageAccessor<K, V>();
        }
    }

    /**
     * A base implementation of builders to create a cache and a queue.
     * @param <T> the type of the instance
     */
    private static class Builder<T extends Builder<T>> {

        /** properties to initialize an object to create */
        Properties props;

        /** a name of an object to create */
        String name;

        /** a storage accessor type to create an instance of {@code StorageAccessor} */
        StorageAccessorType storageAccessorType;

        /** a storage accessor builder */
        StorageAccessorBuilder<?> storageAccessorBuilder;

        Builder() {
            reset();
        }

        /**
         * Resets parameters.
         * TODO document default values
         */
        public void reset() {
            props = new Properties();
            name = randomName();
            storageAccessorType = StorageAccessorType.HEAP;
            storageAccessorBuilder = null;
        }

        /**
         * Casts this instance to T.
         * @return this instance that the type is T
         */
        @SuppressWarnings("unchecked")
        private T t() {
            return (T) this;
        }

        /**
         * Sets a initialization property.
         * @param key a key of the property
         * @param value a value of the property
         * @return this instance
         */
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

        /**
         * Sets a initialization property.
         * @param cls a class object that holds properties
         * @param keyInClass a name of the properties in the {@code cls}
         * @param value a value to be set
         * @return this instance
         */
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

        /**
         * Sets a name of an object to be created.
         * @param name a name of an object to be created
         * @return a name of an object to be created
         */
        public T name(String name) {
            if (name == null) {
                throw new NullPointerException("[name] name");
            }
            this.name = name;
            return t();
        }

        /**
         * Sets a type of {@code StorageAccessor}.
         * @param sat a type of {@code StorageAccessor}
         * @return a type of {@code StorageAccessor}
         */
        public T storageAccessorType(StorageAccessorType sat) {
            if (sat == null) {
                throw new NullPointerException("[storageAccessorType] cat");
            }
            this.storageAccessorType = sat;
            return t();
        }

        /**
         * Sets a builder to create an instance of {@code StorageAccessor}.
         * @param sab a builder to create an instance of {@code StorageAccessor}
         * @return a builder to create an instance of {@code StorageAccessor}
         */
        public T storageAccessorBuilder(StorageAccessorBuilder<?> sab) {
            if (sab == null) {
                throw new NullPointerException("[storageAccessorBuilder] cab");
            }
            this.storageAccessorBuilder = sab;
            return t();
        }
    }

    /**
     * A builder to create {@link net.ihiroky.reservoir.BasicCache}.
     */
    public static class CacheBuilder extends Builder<CacheBuilder> {

        /** initial size of the cache */
        private long initialCacheSize;

        /** max size of the cache */
        private long maxCacheSize;

        /** index type that determine an index of the cache */
        private IndexType indexType;

        private Logger logger = LoggerFactory.getLogger(CacheBuilder.class);

        /**
         *
         */
        CacheBuilder() {
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void reset() {
            super.reset();
            maxCacheSize = Long.MAX_VALUE;
            indexType = IndexType.LRU;
        }

        /**
         * Sets the {@code indexType}.
         * @param it a index type to be set
         * @return this instance
         */
        public CacheBuilder indexType(IndexType it) {
            if (it == null) {
                throw new NullPointerException("[indexType] it");
            }
            this.indexType = it;
            return this;
        }

        /**
         * Sets the {@code maxSize}
         * @param maxSize cache max size to be set
         * @return this instance
         */
        public CacheBuilder maxCacheSize(long maxSize) {
            if (maxSize <= 0) {
                throw new IllegalArgumentException("[maxCacheSize] maxSize must be > 0");
            }
            this.maxCacheSize = maxSize;
            return this;
        }

        /**
         * Sets the {@code initialCacheSize}.
         * @param initialSize cache initial size to be set
         * @return this instance
         */
        public CacheBuilder initialCacheSize(long initialSize) {
            if (initialSize <= 0) {
                throw new IllegalArgumentException("[initialCacheSize] initialSize must be > 0");
            }
            this.initialCacheSize = initialSize;
            return this;
        }

        /**
         * Creates an instance of {@code BasicCache} with specified parameters in this object.
         * @param <K> the type of keys maintained by the cache
         * @param <V> the type of mapped values
         * @return the instance of {@code BasicCache}
         */
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

            StorageAccessor<K, V> storageAccessor = storageAccessorType.create();
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

    /**
     * A builder to create {@link net.ihiroky.reservoir.BasicQueue}.
     */
    public static class QueueBuilder extends Builder<QueueBuilder> {

        private Logger logger = LoggerFactory.getLogger(QueueBuilder.class);

        QueueBuilder() {
        }

        /**
         * {@inheritDoc}
         */
        public <E> BasicQueue<E> build() {
            if (name == null || name.length() == 0) {
                throw new IllegalStateException("[build] name is null or empty.");
            }
            if (storageAccessorBuilder != null) {
                StorageAccessor<Object, E> storageAccessor = storageAccessorBuilder.build(name);
                return new BasicQueue<E>(name, storageAccessor);
            }

            StorageAccessor<Object, E> storageAccessor = storageAccessorType.create();
            storageAccessor.prepare(name, props);

            logger.debug("[build] name : {}", name);
            logger.debug("[build] storageAccessor : {}", storageAccessor);
            logger.debug("[build] properties : {}", props);
            return new BasicQueue<E>(name, storageAccessor);
        }
    }

    /**
     * A builder to create {@link net.ihiroky.reservoir.BasicBlockingQueue}.
     */
    public static class BlockingQueueBuilder extends Builder<BlockingQueueBuilder> {

        /** a capacity of a queue to be created */
        private int capacity = Integer.MAX_VALUE;

        private Logger logger = LoggerFactory.getLogger(BlockingQueueBuilder.class);

        BlockingQueueBuilder() {
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void reset() {
            super.reset();
            capacity = Integer.MAX_VALUE;
        }

        /**
         * Sets the {@code capacity}.
         * @param c a capacity to be set
         * @return this instance
         */
        public BlockingQueueBuilder capacity(int c) {
            if (c < 1) {
                throw new IllegalArgumentException("[capacity] c must be >= 1");
            }
            this.capacity = c;
            return this;
        }

        /**
         * {@inheritDoc}
         */
        public <E> BasicBlockingQueue<E> build() {
            if (name == null || name.length() == 0) {
                throw new IllegalStateException("[build] name is null or empty.");
            }
            if (storageAccessorBuilder != null) {
                StorageAccessor<Object, E> storageAccessor = storageAccessorBuilder.build(name);
                return new BasicBlockingQueue<E>(name, storageAccessor, capacity);
            }

            StorageAccessor<Object, E> storageAccessor = storageAccessorType.create();
            storageAccessor.prepare(name, props);

            logger.debug("[build] name : {}", name);
            logger.debug("[build] capacity : {}", capacity);
            logger.debug("[build] storageAccessor : {}", storageAccessor);
            logger.debug("[build] properties : {}", props);
            return new BasicBlockingQueue<E>(name, storageAccessor, capacity);
        }
    }
}
