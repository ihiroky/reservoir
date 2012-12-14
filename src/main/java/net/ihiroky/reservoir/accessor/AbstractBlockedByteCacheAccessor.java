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
public abstract class AbstractBlockedByteCacheAccessor<K, V>
        implements CacheAccessor<K, V>, BlockedByteCacheAccessorMBean {

    private String name;
    private volatile ByteBlockManager[] byteBlockManagers;
    private int blockSize;
    private long wholeBlocks;
    private Coder.Encoder<V> encoder;
    private Coder.Decoder<V> decoder;

    private RejectedAllocationHandler rejectedAllocationHandler = RejectedAllocationPolicy.ABORT;
    private final Object freeWaitMutex = new Object();

    private Logger logger = LoggerFactory.getLogger(AbstractBlockedByteCacheAccessor.class);

    private static final ByteBuffer EMPTY_BUFFER = ByteBuffer.allocate(0); // automatically readonly.
    private static final List<ByteBlock> EMPTY_LIST = Collections.emptyList();

    private ByteBlock allocate(K key, int listPosition) throws InterruptedException {
        for (boolean isContinue = true; isContinue; ) {
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
            isContinue = rejectedAllocationHandler.handle(this);
        }
        return null;
    }

    void waitOnFreeWaitMutex() throws InterruptedException {
        // allow spurious wake up.
        synchronized (freeWaitMutex) {
            freeWaitMutex.wait();
        }
    }

    void addByteBlockManager(ByteBlockManager byteBlockManager) {
        synchronized (freeWaitMutex) {
            int currentLength = this.byteBlockManagers.length;
            ByteBlockManager[] newManagers = new ByteBlockManager[currentLength + 1];
            System.arraycopy(this.byteBlockManagers, 0, newManagers, 0, currentLength);
            newManagers[currentLength] = byteBlockManager;
            this.byteBlockManagers = newManagers;
            freeWaitMutex.notifyAll();
        }
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

    @Override
    public String getRejectedAllocationHandlerName() {
        return (rejectedAllocationHandler != null) ? rejectedAllocationHandler.toString() : "";
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
            return decoder.decode(byteBuffer);
        }

        boolean update(K key, V value) {
            ByteBuffer encoded = encoder.encode(value);
            return flush(key, encoded);
        }

        void free() {
            WriteLock writeLock = writeLock();
            writeLock.lock();
            try {
                freeWithoutWriteLock();
            } finally {
                writeLock.unlock();
            }
        }

        private void freeWithoutWriteLock() {
            for (ByteBlock block : blockList) {
                block.free();
            }
            synchronized (freeWaitMutex) {
                freeWaitMutex.notifyAll();
            }
            bytes = 0;
            blockList = EMPTY_LIST;
        }

        private void freeTailWithoutLock(int listPosition) {
            for (int i = blockList.size() - 1; i >= listPosition; i--) {
                blockList.remove(i).free();
            }
            synchronized (freeWaitMutex) {
                freeWaitMutex.notifyAll();
            }
        }

        private ByteBuffer asByteBuffer() {
            byte[] result;
            ReadLock readLock = readLock();
            readLock.lock();
            try {
                int length = bytes;
                if (length == 0) {
                    return EMPTY_BUFFER;
                }
                int offset = 0;
                result = new byte[length];
                for (ByteBlock block : blockList) {
                    offset += block.get(0, result, offset, length - offset);
                    if (offset == length) {
                        break;
                    }
                }
            } finally {
                readLock.unlock();
            }
            return ByteBuffer.wrap(result);
        }

        private boolean flush(K key, ByteBuffer byteBuffer) {
            int listPosition = 0;
            int offset = 0;
            byte[] input = byteBuffer.array();
            int inputLength = byteBuffer.remaining();
            ByteBlock block;
            WriteLock writeLock = writeLock();
            writeLock.lock();
            try {
                if (blockList == EMPTY_LIST) {
                    blockList = new ArrayList<ByteBlock>();
                }
                bytes = inputLength;
                while (offset < inputLength) {
                    if (listPosition < blockList.size()) {
                        block = blockList.get(listPosition);
                    } else {
                        block = allocate(key, listPosition);
                        if (block == null) {
                            freeWithoutWriteLock();
                            return false;
                        }
                        blockList.add(block);
                    }
                    listPosition++;
                    offset += block.put(0, input, offset, inputLength - offset);
                }
                freeTailWithoutLock(listPosition);
            } catch (InterruptedException ie) {
                freeWithoutWriteLock();
                throw new RuntimeException("ByteBlock allocation is failed by InterruptedException.", ie);
            } catch (RuntimeException re) {
                freeWithoutWriteLock();
                throw re;
            } finally {
                writeLock.unlock();
            }
            return true;
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

    private boolean updateEntry(K key, V value, Index<K, Ref<V>> index) {
        if (key == null) {
            return false;
        }

        BlockedByteRef ref = new BlockedByteRef();
        @SuppressWarnings("unchecked")
        BlockedByteRef oldRef = (BlockedByteRef) index.putIfAbsent(key, ref);
        if (oldRef != null) {
            ref = oldRef;
        }
        return ref.update(key, value);
    }

    @Override
    public Ref<V> create(K key, V value) {
        BlockedByteRef ref = new BlockedByteRef();
        return ref.update(key, value) ? ref : null;
    }

    @Override
    public boolean update(K key, V value, Index<K, Ref<V>> index) {
        return updateEntry(key, value, index);
    }

    @Override
    public void update(Map<K, V> keyValues, Index<K, Ref<V>> index) {
        Iterator<Map.Entry<K, V>> iterator = keyValues.entrySet().iterator();
        Map.Entry<K, V> entry;
        while (iterator.hasNext()) {
            entry = iterator.next();
            if (!updateEntry(entry.getKey(), entry.getValue(), index)) {
                Collection<Object> failedKeys = new ArrayList<Object>();
                failedKeys.add(entry.getKey());
                while (iterator.hasNext()) {
                    failedKeys.add(iterator.next().getKey());
                }
                throw new NoEnoughFreeBlockException("failed to update blocks.", failedKeys);
            }
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

    static RejectedAllocationHandler createRejectedAllocationHandler(String value) {
        if (value == null) {
            return RejectedAllocationPolicy.ABORT;
        }
        try {
            return RejectedAllocationPolicy.valueOf(value);
        } catch (IllegalArgumentException ignored) {
            // The value is not a member of RejectedAllocationHandler.
            // Try to instantiate as a sub class name of RejectedAllocationHandler.
        }
        try {
            Class<?> c = Class.forName(value);
            return (RejectedAllocationHandler) c.newInstance();
        } catch (Exception e) {
            throw new RuntimeException("failed to create RejectedAllocationHandler : " + value, e);
        }
    }

    protected void prepare(String name, ByteBlockManager[] byteBlockManagers,
                           int blockSize, Coder<V> coder, RejectedAllocationHandler handler) {
        this.name = name;
        this.byteBlockManagers = byteBlockManagers;
        this.blockSize = blockSize;
        this.decoder = coder.createDecoder();
        this.encoder = coder.createEncoder();
        if (handler != null) {
            this.rejectedAllocationHandler = handler;
        }

        long wholeBlocks = 0;
        for (ByteBlockManager bbb : byteBlockManagers) {
            wholeBlocks += bbb.getBlocks();
        }
        this.wholeBlocks = wholeBlocks;

        logger.info("[prepare] name: {}", name);
        logger.info("[prepare] byteBlockManagers: {}", Arrays.toString(byteBlockManagers));
        logger.info("[prepare] blockSize: {}", blockSize);
        logger.info("[prepare] coder: {}", coder);
        logger.info("[prepare] wholeBlock: {}", wholeBlocks);
        logger.info("[prepare] rejectedAllocationHandler: {}", rejectedAllocationHandler);

        MBeanSupport.registerMBean(this, name);
        for (ByteBlockManager bbb : byteBlockManagers) {
            MBeanSupport.registerMBean(bbb, bbb.getName());
        }
    }

    public int getBlockSize() {
        return blockSize;
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
