package net.ihiroky.reservoir.accessor;

import net.ihiroky.reservoir.CacheAccessor;
import net.ihiroky.reservoir.Coder;
import net.ihiroky.reservoir.Index;
import net.ihiroky.reservoir.MBeanSupport;
import net.ihiroky.reservoir.Ref;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Created on 12/09/27, 18:38
 *
 * @author Hiroki Itoh
 */
public abstract class AbstractBlockedByteCacheAccessor<K, V> implements CacheAccessor<K, V>, BlockedByteCacheAccessorMBean {

    private String name;
    private ByteBlockManager[] byteBlockManagers;
    private int blockSize;
    private long wholeBlocks;
    private Coder.Encoder<V> encoder;
    private Coder.Decoder<V> decoder;

    private Logger logger = LoggerFactory.getLogger(AbstractBlockedByteCacheAccessor.class);

    private static final ByteBuffer EMPTY_BUFFER = ByteBuffer.allocate(0).asReadOnlyBuffer();
    private static final List<ByteBlock> EMPTY_LIST = Collections.emptyList();

    private ByteBlock allocate(K key, int listPosition) {
        ByteBlockManager[] bbbArray = byteBlockManagers;
        int length = bbbArray.length;

        int bbbIndex = positiveHash(key, listPosition) % length;
        ByteBlock newBlock = bbbArray[bbbIndex].allocate();
        if (newBlock != null) {
            return newBlock;
        }
        for (int i = 1; i < length; i++) {
            newBlock = bbbArray[(bbbIndex + i) % length].allocate();
            if (newBlock != null) {
                return newBlock;
            }
        }
        throw new IllegalStateException("no free block.");
    }

    private static int positiveHash(Object key, int listPosition) {
        int result = 17;
        if (key != null) {
            result = 31 * result + key.hashCode();
        }
        result = 31 * result + listPosition;
        return result & 0x7FFFFFFF;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public long getAllocatedBlocks() {
        long result = 0;
        for (ByteBlockManager bbb : byteBlockManagers) {
            result += bbb.getAllocatedBlocks();
        }
        return result;
    }

    @Override
    public long getWholeBlocks() {
        return wholeBlocks;
    }

    @Override
    public int getPartitions() {
        return byteBlockManagers.length;
    }

    @Override
    public String getEncoderClassName() {
        return (encoder != null) ? encoder.getClass().getName() : "";
    }

    @Override
    public String getDecoderClassName() {
        return (decoder != null) ? decoder.getClass().getName() : "";
    }

    private class BlockedByteRef extends ReentrantReadWriteLock implements Ref<V> {

        List<ByteBlock> blockList;
        int bytes;

        BlockedByteRef() {
            blockList = EMPTY_LIST;
        }

        @Override
        public V value() {
            ByteBuffer byteBuffer = asByteBuffer();
            return byteBuffer.hasRemaining() ? decoder.decode(byteBuffer) : null;
        }

        void update(K key, V value) {
            ByteBuffer encoded = encoder.encode(value);
            flush(key, encoded);
        }

        void free() {
            WriteLock writeLock = writeLock();
            writeLock.lock();
            try {
                for (ByteBlock block : blockList) {
                    block.free();
                }
                bytes = 0;
                blockList.clear();
            } finally {
                writeLock.unlock();
            }
        }

        private ByteBuffer asByteBuffer() {
            byte[] buffer = new byte[blockSize];
            int read;
            ByteBuffer bb;
            ReadLock readLock = readLock();
            readLock.lock();
            try {
                int length = bytes;
                if (length == 0) {
                    return EMPTY_BUFFER;
                }
                bb = ByteBuffer.allocate(length);
                for (ByteBlock block : blockList) {
                    // TODO direct copy
                    read = block.get(0, buffer, 0, length);
                    bb.put(buffer, 0, read);
                    if ((length -= read) == 0) {
                        break;
                    }
                }
            } finally {
                readLock.unlock();
            }
            bb.flip();
            return bb;
        }

        private void flush(K key, ByteBuffer byteBuffer) {
            byte[] buffer = new byte[blockSize];
            int listPosition = 0;
            int length;
            int inputLength = byteBuffer.remaining();
            ByteBlock block;
            WriteLock writeLock = writeLock();
            writeLock.lock();
            try {
                if (blockList == EMPTY_LIST) {
                    blockList = new ArrayList<ByteBlock>();
                }
                bytes = inputLength;
                while (byteBuffer.hasRemaining()) {
                    length = (byteBuffer.remaining() >= buffer.length) ? buffer.length : byteBuffer.remaining();
                    // TODO direct copy
                    byteBuffer.get(buffer, 0, length);
                    if (listPosition < blockList.size()) {
                        block = blockList.get(listPosition);
                    } else {
                        block = allocate(key, listPosition);
                        blockList.add(block);
                    }
                    listPosition++;
                    block.put(0, buffer, 0, length);
                }
                for (int i = blockList.size() - 1; i >= listPosition; i--) {
                    blockList.remove(i).free();
                }
            } catch (RuntimeException re) {
                for (Iterator<ByteBlock> i = blockList.iterator(); i.hasNext(); ) {
                    i.next().free();
                    i.remove();
                }
                throw re;
            } finally {
                writeLock.unlock();
            }
        }

        @Override
        public String toString() {
            StringBuilder b = new StringBuilder();
            ReadLock lock = readLock();
            lock.lock();
            try {
                if (blockList.size() == 0) {
                    return "[]";
                }
                b.append('[').append(blockList.get(0).getBlockIndex());
                for (int i = 1; i < blockList.size(); i++) {
                    b.append(',').append(blockList.get(i).getBlockIndex());
                }
            } finally {
                lock.unlock();
            }
            return b.append(']').toString();
        }
    }

    private void updateEntry(K key, V value, Index<K, Ref<V>> index) {
        if (key == null) {
            return;
        }

        BlockedByteRef ref = new BlockedByteRef();
        @SuppressWarnings("unchecked")
        BlockedByteRef oldRef = (BlockedByteRef) index.putIfAbsent(key, ref);
        if (oldRef != null) {
            ref = oldRef;
        }
        ref.update(key, value);
    }

    @Override
    public Ref<V> create(K key, V value) {
        BlockedByteRef ref = new BlockedByteRef();
        ref.update(key, value);
        return ref;
    }

    @Override
    public void update(K key, V value, Index<K, Ref<V>> index) {
        updateEntry(key, value, index);
    }

    @Override
    public void update(Map<K, V> keyValues, Index<K, Ref<V>> index) {
        Iterator<Map.Entry<K, V>> iterator = keyValues.entrySet().iterator();
        Map.Entry<K, V> entry = null;
        try {
            while (iterator.hasNext()) {
                entry = iterator.next();
                updateEntry(entry.getKey(), entry.getValue(), index);
            }
        } catch (IllegalStateException ise) {
            Collection<Object> failedKeys = new ArrayList<Object>();
            failedKeys.add(entry.getKey()); // entry is surely non null.
            while (iterator.hasNext()) {
                failedKeys.add(iterator.next().getKey());
            }
            throw new NoEnoughFreeBlockException(
                    "failed to update blocks.", ise, failedKeys);
        }
    }

    @Override
    public void remove(K key, Ref<V> ref) {
        if (ref != null) {
            @SuppressWarnings("unchecked") BlockedByteRef blockedByteRef = (BlockedByteRef) ref;
            blockedByteRef.free();
        }
    }

    @Override
    public void remove(Collection<Map.Entry<K, Ref<V>>> refs) {
        for (Map.Entry<K, Ref<V>> entry : refs) {
            @SuppressWarnings("unchecked") BlockedByteRef ref = (BlockedByteRef) entry.getValue();
            if (ref != null) {
                ref.free();
            }
        }
    }

    protected void prepare(String name, ByteBlockManager[] byteBlockManagers, int blockSize, Coder<V> coder) {
        this.name = name;
        this.byteBlockManagers = byteBlockManagers;
        this.blockSize = blockSize;
        this.decoder = coder.createDecoder();
        this.encoder = coder.createEncoder();

        long wholeBlocks = 0;
        for (ByteBlockManager bbb : byteBlockManagers) {
            wholeBlocks += bbb.getBlocks();
        }
        this.wholeBlocks = wholeBlocks;

        logger.info("[prepare] name: {}", name);
        logger.info("[prepare] byteBlockManagers: {}", Arrays.toString(byteBlockManagers));
        logger.info("[prepare] blockSize: {}", blockSize);
        logger.info("[prepare] coder: {}", coder);
        logger.info("[prepare] whileBlock: {}", wholeBlocks);

        MBeanSupport.registerMBean(this, name);
        for (ByteBlockManager bbb : byteBlockManagers) {
            MBeanSupport.registerMBean(bbb, bbb.getName());
        }
    }

    @Override
    public void dispose() {
        MBeanSupport.unregisterMBean(this, getName());
        if (byteBlockManagers != null) {
            for (ByteBlockManager bbb : byteBlockManagers) {
                bbb.free();
                MBeanSupport.unregisterMBean(bbb, bbb.getName());
            }
        }
    }
}
