package net.ihiroky.reservoir.storage;

import net.ihiroky.reservoir.Coder;
import net.ihiroky.reservoir.PropertiesSupport;
import net.ihiroky.reservoir.Reservoir;
import net.ihiroky.reservoir.coder.SerializableCoder;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

/**
 * Created on 12/09/27, 18:38
 *
 * @author Hiroki Itoh
 */
public class ByteBufferStorageAccessor<K, V> extends AbstractBlockedByteStorageAccessor<K, V> {

    private static final String KEY_DIRECT = "reservoir.ByteBufferStorageAccessor.direct";
    private static final String KEY_SIZE = "reservoir.ByteBufferStorageAccessor.size";
    private static final String KEY_USAGE_PERCENT = "reservoir.ByteBufferStorageAccessor.usagePercent";
    private static final String KEY_BLOCK_SIZE = "reservoir.ByteBufferStorageAccessor.blockSize";
    private static final String KEY_PARTITIONS = "reservoir.ByteBufferStorageAccessor.partitions";
    private static final String KEY_CODER = "reservoir.ByteBufferStorageAccessor.coder";
    private static final String KEY_REJECTED_ALLOCATION_HANDLER =
            "reservoir.ByteBufferStorageAccessor.rejectedAllocationHandler";

    private static final String KEY_PARTITION_PREFIX = "partition.";
    private static final String KEY_DIRECT_SUFFIX = ".direct";
    private static final String KEY_CAPACITY_SUFFIX = ".capacity";

    private static final boolean DEFAULT_DIRECT = false;
    private static final long DEFAULT_SIZE = 32 * 1024 * 1024;
    private static final int DEFAULT_BLOCK_SIZE = 512;
    private static final int DEFAULT_PARTITIONS = 1;

    static int maxPartitionSize(int blockSize, boolean direct) {
        return ((direct ? Reservoir.getMaxDirectBufferCapacity() : Integer.MAX_VALUE) / blockSize) * blockSize;
    }

    public void prepare(String name, int blockSize, Coder<V> coder, Collection<ByteBufferInfo> byteBufferInfos,
                        RejectedAllocationHandler rejectedAllocationHandler) {
        if (name == null) {
            throw new NullPointerException("name must not be null.");
        }
        if (coder == null) {
            throw new NullPointerException("coder must not be null.");
        }

        BlockedByteBuffer[] bbbArray = new BlockedByteBuffer[byteBufferInfos.size()];
        int count = 0;
        for (ByteBufferInfo byteBufferInfo : byteBufferInfos) {
            ByteBuffer byteBuffer = byteBufferInfo.direct
                    ? ByteBuffer.allocateDirect(byteBufferInfo.capacity) : ByteBuffer.allocate(byteBufferInfo.capacity);
            bbbArray[count++] = new BlockedByteBuffer(byteBuffer, blockSize);
        }
        prepare(name, bbbArray, blockSize, coder, rejectedAllocationHandler);
    }

    public void prepare(final String name, final boolean direct, Coder<V> coder,
                        BulkInfo bulkInfo, RejectedAllocationHandler rejectedAllocationHandler) {
        if (name == null) {
            throw new NullPointerException("name must not be null.");
        }
        if (coder == null) {
            throw new NullPointerException("coder must not be null.");
        }
        if (bulkInfo == null) {
            throw new NullPointerException("bulkInfo must not be null.");
        }
        if (name.length() == 0) {
            throw new IllegalArgumentException("name must not be empty.");
        }

        BulkInfo.ByteBlockManagerAllocator allocator = new BulkInfo.ByteBlockManagerAllocator() {
            @Override
            public ByteBlockManager allocate(long size, int blockSize, int index) {
                ByteBuffer byteBuffer = direct ?
                        ByteBuffer.allocateDirect((int)size) : ByteBuffer.allocate((int)size);
                BlockedByteBuffer bbb = new BlockedByteBuffer(byteBuffer, blockSize);
                bbb.setName(name + '-' + String.format("%05d", index));
                return bbb;
            }
        };
        int maxPartitionSize = maxPartitionSize(bulkInfo.blockSize, direct);
        ByteBlockManager[] managers = bulkInfo.allocate(allocator, maxPartitionSize);
        prepare(name, managers, bulkInfo.blockSize, coder, rejectedAllocationHandler);
    }

    @Override
    public void prepare(String name, Properties props) {

        int blockSize = PropertiesSupport.intValue(props, KEY_BLOCK_SIZE, DEFAULT_BLOCK_SIZE);
        final Coder<V> coder = PropertiesSupport.newInstance(props, KEY_CODER, SerializableCoder.class);
        coder.init(props);
        final String rah = props.getProperty(KEY_REJECTED_ALLOCATION_HANDLER);

        Collection<ByteBufferInfo> byteBufferInfos = parseByteBufferInfo(props, blockSize);
        if (byteBufferInfos.size() > 0) {
            prepare(name, blockSize, coder, byteBufferInfos, createRejectedAllocationHandler(rah));
            return;
        }

        boolean direct = PropertiesSupport.booleanValue(props, KEY_DIRECT, DEFAULT_DIRECT);
        int usagePercent = PropertiesSupport.intValue(props, KEY_USAGE_PERCENT, 0);
        long size = (usagePercent <= 0)
                ? PropertiesSupport.longValue(props, KEY_SIZE, DEFAULT_SIZE)
                : Reservoir.getMaxDirectMemorySize() / 100L * usagePercent;
        int partitions = PropertiesSupport.intValue(props, KEY_PARTITIONS, DEFAULT_PARTITIONS);
        BulkInfo bulkInfo = new BulkInfo(size, blockSize, partitions);
        prepare(name, direct, coder, bulkInfo, createRejectedAllocationHandler(rah));
    }

    private Collection<ByteBufferInfo> parseByteBufferInfo(Properties props, int blockSize) {
        String byPartitionPrefix = PropertiesSupport.key(ByteBufferStorageAccessor.class, KEY_PARTITION_PREFIX);
        Map<Integer, ByteBufferInfo> infoMap = new TreeMap<Integer, ByteBufferInfo>();
        for (String key : props.stringPropertyNames()) {
            if (!key.startsWith(byPartitionPrefix)) {
                continue;
            }
            int idEndIndex = key.indexOf('.', byPartitionPrefix.length());
            if (idEndIndex == -1) {
                continue;
            }
            String value = props.getProperty(key);
            try {
                int id = Integer.parseInt(key.substring(byPartitionPrefix.length(), idEndIndex));
                ByteBufferInfo byteBufferInfo = infoMap.get(id);
                if (byteBufferInfo == null) {
                    byteBufferInfo = new ByteBufferInfo();
                    infoMap.put(id, byteBufferInfo);
                }
                if (key.endsWith(KEY_DIRECT_SUFFIX)) {
                    byteBufferInfo.direct = Boolean.parseBoolean(value);
                } else if (key.endsWith(KEY_CAPACITY_SUFFIX)) {
                    byteBufferInfo.capacity = Integer.parseInt(value);
                }
            } catch (NumberFormatException nfe) {
                throw new IllegalArgumentException("failed to parse property. key:" + key + ", value:" + value, nfe);
            }
        }
        for (Map.Entry<Integer, ByteBufferInfo> entry : infoMap.entrySet()) {
            ByteBufferInfo byteBufferInfo = entry.getValue();
            int maxPartitionSize = maxPartitionSize(blockSize, byteBufferInfo.direct);
            if (byteBufferInfo.capacity > maxPartitionSize(blockSize, byteBufferInfo.direct)) {
                throw new IllegalArgumentException(
                        "Partition size violation. Max capacity per ByteBuffer is "
                                + maxPartitionSize + " at index " + entry.getKey());
            }
        }
        return infoMap.isEmpty()
                ? Collections.<ByteBufferInfo>emptyList() : new ArrayList<ByteBufferInfo>(infoMap.values());
    }

    public static void main(String[] args) {
        long capacity = 3L * 1024 * 1024 * 1024;
        int blockSize = 512;
        Properties props = PropertiesSupport.builder()
                .put("reservoir.ByteBufferStorageAccessor.direct", "true")
                .put("reservoir.ByteBufferStorageAccessor.size", Long.toString(capacity))
                .put("reservoir.ByteBufferStorageAccessor.blockSize", Integer.toString(blockSize))
                .put("reservoir.ByteBufferStorageAccessor.partitions", "1")
                .put("reservoir.ByteBufferStorageAccessor.coder", "net.ihiroky.reservoir.coder.StringCoder")
                .build();

        ByteBufferStorageAccessor<Object, String> a = new ByteBufferStorageAccessor<Object, String>();
        a.prepare(ByteBufferStorageAccessor.class.getName() + "#main", props);
        long expected = capacity / blockSize;
        System.out.println("whole blocks  : " + a.getWholeBlocks());
        System.out.println("logical blocks: " + expected);
        assert a.getWholeBlocks() == expected;
    }
}
