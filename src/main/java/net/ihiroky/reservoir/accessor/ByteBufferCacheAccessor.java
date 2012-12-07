package net.ihiroky.reservoir.accessor;

import net.ihiroky.reservoir.Coder;
import net.ihiroky.reservoir.PropertiesSupport;
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
public class ByteBufferCacheAccessor<K, V> extends AbstractBlockedByteCacheAccessor<K, V> {

    private static final String KEY_DIRECT = "reservoir.ByteBufferCacheAccessor.direct";
    private static final String KEY_SIZE = "reservoir.ByteBufferCacheAccessor.size";
    private static final String KEY_BLOCK_SIZE = "reservoir.ByteBufferCacheAccessor.blockSize";
    private static final String KEY_PARTITIONS = "reservoir.ByteBufferCacheAccessor.partitions";
    private static final String KEY_CODER = "reservoir.ByteBufferCacheAccessor.coder";

    private static final String KEY_PARTITION_PREFIX = "partition.";
    private static final String KEY_DIRECT_SUFFIX = ".direct";
    private static final String KEY_CAPACITY_SUFFIX = ".capacity";

    private static final boolean DEFAULT_DIRECT = false;
    private static final long DEFAULT_SIZE = 512 * 1024 * 1024;
    private static final int DEFAULT_BLOCK_SIZE = 512;
    private static final int DEFAULT_PARTITIONS = 16;

    public void prepare(String name, int blockSize, Coder<V> coder, Collection<ByteBufferInfo> byteBufferInfos) {
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
            bbbArray[count++] = new BlockedByteBuffer(byteBuffer, blockSize, super.freeWaitMutex);
        }
        prepare(name, bbbArray, blockSize, coder, null);
    }

    public void prepare(String name, boolean direct, long size, int blockSize, int partitionsHint, Coder<V> coder) {
        if (name == null) {
            throw new NullPointerException("name must not be null.");
        }
        if (coder == null) {
            throw new NullPointerException("coder must not be null.");
        }
        if (size < blockSize
                || blockSize < BlockedByteBuffer.MIN_BYTES_PER_BLOCK
                || partitionsHint <= 0
                || name.length() == 0) {
            throw new IllegalArgumentException();
        }


        final long partitionSizeHint = size / partitionsHint;
        final int maxPartitionSize = Integer.MAX_VALUE - blockSize + 1; // 2^31 - blockSize
        long left = size;
        int partitionSize = (partitionSizeHint < maxPartitionSize) ? (int) partitionSizeHint : maxPartitionSize;
        if (partitionSize < blockSize) {
            partitionSize = blockSize;
        }
        int partitions = (int) (size / partitionSize + ((size % partitionSize != 0) ? 1 : 0));
        BlockedByteBuffer[] bbbArray = new BlockedByteBuffer[partitions];
        for (int i = 0; i < partitions; i++) {
            int bbbSize = (left >= partitionSize) ? partitionSize : (int) left;
            if (bbbSize < blockSize) {
                bbbSize = blockSize;
            }
            ByteBuffer byteBuffer = direct ?
                    ByteBuffer.allocateDirect(bbbSize) : ByteBuffer.allocate(bbbSize);
            bbbArray[i] = new BlockedByteBuffer(byteBuffer, blockSize, super.freeWaitMutex);
            bbbArray[i].setName(name + '-' + String.format("%5d", i));
            left -= bbbSize;
        }

        prepare(name, bbbArray, blockSize, coder, null); // TODO RejectedAllocationHandler setting.
    }

    @Override
    public void prepare(String name, Properties props) {

        int blockSize = PropertiesSupport.intValue(props, KEY_BLOCK_SIZE, DEFAULT_BLOCK_SIZE);
        @SuppressWarnings("unchecked")
        Coder<V> coder = (Coder<V>) PropertiesSupport.newInstance(props, KEY_CODER, SerializableCoder.class);
        coder.init(props);

        Collection<ByteBufferInfo> byteBufferInfos = parseByteBufferInfo(props);
        if (byteBufferInfos.size() > 0) {
            prepare(name, blockSize, coder, byteBufferInfos);
            return;
        }

        boolean direct = PropertiesSupport.booleanValue(props, KEY_DIRECT, DEFAULT_DIRECT);
        long size = PropertiesSupport.longValue(props, KEY_SIZE, DEFAULT_SIZE);
        int partitions = PropertiesSupport.intValue(props, KEY_PARTITIONS, DEFAULT_PARTITIONS);
        prepare(name, direct, size, blockSize, partitions, coder);
    }

    private Collection<ByteBufferInfo> parseByteBufferInfo(Properties props) {
        String byPartitionPrefix = PropertiesSupport.key(ByteBufferCacheAccessor.class, KEY_PARTITION_PREFIX);
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
        return infoMap.isEmpty()
                ? Collections.<ByteBufferInfo>emptyList() : new ArrayList<ByteBufferInfo>(infoMap.values());
    }
}
