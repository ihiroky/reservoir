package net.ihiroky.reservoir.accessor;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;

/**
 * Created on 12/09/28, 9:31
 *
 * @author Hiroki Itoh
 */
public class BlockedByteBuffer implements BlockedByteBufferMBean, ByteBlockManager {

    private String name;
    private final ByteBuffer byteBuffer;
    private final int bytesPerBlock;
    private int freeHeadIndex;
    private int freeTailIndex;
    private final int maxLength;
    private final int maxBlocks;
    private volatile int allocatedBlocks;

    private static final int INVALID_INDEX = -1;
    static final int MIN_BYTES_PER_BLOCK = 4;

    public BlockedByteBuffer(ByteBuffer byteBuffer, int bytesPerBlock) {
        if (byteBuffer == null) {
            throw new NullPointerException("byteBuffer must not be null.");
        }
        if (bytesPerBlock < MIN_BYTES_PER_BLOCK) {
            throw new IllegalArgumentException("bytesPerBlock must be >= 4.");
        }
        if (byteBuffer.capacity() < bytesPerBlock) {
            throw new IllegalArgumentException("byteBuffer must be larger than bytesPerBlock.");
        }

        this.byteBuffer = byteBuffer;
        this.bytesPerBlock = bytesPerBlock;
        int blocks = byteBuffer.capacity() / bytesPerBlock;
        this.maxBlocks = blocks;
        this.maxLength = blocks * bytesPerBlock;
        this.freeHeadIndex = 0;
        this.freeTailIndex = maxLength - bytesPerBlock;
    }

    void setName(String name) {
        this.name = name;
    }

    @Override
    public ByteBlock allocate() {
        int block;
        synchronized (this) {
            if (freeHeadIndex == INVALID_INDEX) {
                return null;
            }
            block = freeHeadIndex / bytesPerBlock;
            if (freeHeadIndex != freeTailIndex) {
                freeHeadIndex = nextIndex(freeHeadIndex);
            } else {
                freeHeadIndex = freeTailIndex = INVALID_INDEX;
            }
            allocatedBlocks++;
        }
        return new Block(block);
    }

    private synchronized void free(int blockIndex) {
        int index = blockIndex * bytesPerBlock;
        if (freeTailIndex != INVALID_INDEX) {
            int magic = (int) (((long) index - freeTailIndex - bytesPerBlock) % maxLength);
            synchronized (byteBuffer) {
                byteBuffer.putInt(freeTailIndex, magic);
            }
            freeTailIndex = index;
        } else {
            freeHeadIndex = freeTailIndex = index;
        }
        allocatedBlocks--;
    }

    @Override
    public synchronized void free() {
        freeHeadIndex = freeTailIndex = 0;
        allocatedBlocks = 0;
    }

    private int nextIndex(int index) {
        int next;
        synchronized (byteBuffer) {
            next = byteBuffer.getInt(index);
        }
        return (int) (((long) index + next + bytesPerBlock) % maxLength);
    }

    @Override
    public synchronized List<Number> freeBlockListView() {
        List<Number> list = new LinkedList<Number>();
        for (int index = freeHeadIndex; index != freeTailIndex; ) {
            list.add(index / bytesPerBlock);
            index = nextIndex(index);
        }
        if (freeTailIndex != INVALID_INDEX) {
            list.add(freeTailIndex / bytesPerBlock);
        }
        return list;
    }

    @Override
    public byte get(long index) {
        return byteBuffer.get((int) index);
    }

    public ByteBuffer byteBufferView() {
        synchronized (byteBuffer) {
            return byteBuffer.asReadOnlyBuffer();
        }
    }

    @Override
    public synchronized boolean hasFreeBlock() {
        return (freeHeadIndex != INVALID_INDEX);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public int getCapacity() {
        return byteBuffer.capacity();
    }

    @Override
    public int getLimit() {
        return byteBuffer.limit();
    }

    @Override
    public int getBytesPerBlock() {
        return bytesPerBlock;
    }

    @Override
    public int getLength() {
        return maxLength;
    }

    @Override
    public long getBlocks() {
        return maxBlocks;
    }

    @Override
    public long getAllocatedBlocks() {
        return allocatedBlocks;
    }

    @Override
    public String toString() {
        return "name:" + name + ", maxLength:" + maxLength + ", maxBlocks:" + maxBlocks
                + ", allocatedBlocks:" + allocatedBlocks;
    }

    public class Block implements ByteBlock {

        private int blockIndex;

        private static final int INVALID = -1;

        public Block(int blockIndex) {
            this.blockIndex = blockIndex;
        }

        @Override
        public long getBlockIndex() {
            return blockIndex;
        }

        @Override
        public void free() {
            int b;
            synchronized (this) {
                if (blockIndex == INVALID) {
                    return;
                }
                b = blockIndex;
                blockIndex = INVALID;
            }
            BlockedByteBuffer.this.free(b);
        }

        @Override
        public long capacity() {
            return bytesPerBlock;
        }

        @Override
        public int get(int position) {
            if (position < 0 || position >= bytesPerBlock) {
                throw new IndexOutOfBoundsException("position:" + position + ", capacity:" + bytesPerBlock);
            }
            int bufferOffset = bytesPerBlock * blockIndex + position;
            int read;
            synchronized (byteBuffer) {
                read = (blockIndex != INVALID) ? (byteBuffer.get(bufferOffset) & 0xFF) : -1;
            }
            return read;
        }

        @Override
        public int get(int position, byte[] bytes, int offset, int length) {
            if (position < 0 || position > bytesPerBlock) {
                throw new IndexOutOfBoundsException("position:" + position + ", capacity:" + bytesPerBlock);
            }
            int left = bytesPerBlock - position;
            int read = (length <= left) ? length : left;
            int bufferOffset = blockIndex * bytesPerBlock + position;
            boolean valid;
            synchronized (byteBuffer) {
                if (valid = (blockIndex != INVALID)) {
                    byteBuffer.position(bufferOffset);
                    byteBuffer.get(bytes, offset, read);
                }
            }
            return valid ? read : -1;
        }

        @Override
        public int put(int position, int b) {
            if (position < 0 || position >= bytesPerBlock) {
                throw new IndexOutOfBoundsException("position:" + position + ", capacity:" + bytesPerBlock);
            }
            int bufferOffset = blockIndex * bytesPerBlock + position;
            boolean valid;
            synchronized (byteBuffer) {
                if (valid = (blockIndex != INVALID)) {
                    byteBuffer.put(bufferOffset, (byte) b);
                }
            }
            return valid ? 1 : -1;
        }

        @Override
        public int put(int position, byte[] bytes, int offset, int length) {
            if (position < 0 || (position + length) > bytesPerBlock) {
                throw new IndexOutOfBoundsException("position:" + position + ", length:" + length + ", capacity:" + bytesPerBlock);
            }
            int written = (length <= bytesPerBlock) ? length : bytesPerBlock;
            int bufferOffset = blockIndex * bytesPerBlock + position;
            boolean valid;
            synchronized (byteBuffer) {
                if (valid = (blockIndex != INVALID)) {
                    byteBuffer.position(bufferOffset);
                    byteBuffer.put(bytes, offset, written);
                }
            }
            return valid ? written : -1;
        }
    }
}
