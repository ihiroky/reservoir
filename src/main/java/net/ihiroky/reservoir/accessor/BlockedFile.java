package net.ihiroky.reservoir.accessor;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Created on 12/09/28, 9:31
 *
 * @author Hiroki Itoh
 */
public class BlockedFile implements ByteBlockManager, BlockedFileMBean {

    private String name;
    private final String filePath;
    private final RandomAccessFile randomAccessFile;
    private final long bytesPerBlock;
    private long freeHeadIndex;
    private long freeTailIndex;
    private final long maxLength;
    private final long maxBlocks;
    private volatile long allocatedBlocks;
    private final Object freeWaitMutex;

    private static final int INVALID_INDEX = -1;
    static final int MIN_BYTES_PER_BLOCK = 8;

    public BlockedFile(String filePath, RandomAccessFile randomAccessFile,
                       int bytesPerBlock, Object freeWaitMutex) throws IOException {
        if (randomAccessFile == null) {
            throw new NullPointerException("byteBuffer must not be null.");
        }
        if (freeWaitMutex == null) {
            throw new NullPointerException("freeWaitMutex must not be null.");
        }
        if (bytesPerBlock < MIN_BYTES_PER_BLOCK) {
            throw new IllegalArgumentException("bytesPerBlock must be >= 4.");
        }
        long capacity = randomAccessFile.length();
        if (capacity < bytesPerBlock) {
            throw new IllegalArgumentException("file must be larger than bytesPerBlock.");
        }

        this.filePath = filePath;
        this.randomAccessFile = randomAccessFile;
        this.bytesPerBlock = bytesPerBlock;
        long blocks = capacity / bytesPerBlock;
        this.maxBlocks = blocks;
        this.maxLength = blocks * bytesPerBlock;
        this.freeHeadIndex = 0;
        this.freeTailIndex = maxLength - bytesPerBlock;
        this.freeWaitMutex = freeWaitMutex;
    }

    void setName(String name) {
        this.name = name;
    }

    public long maxBlockSize() {
        return bytesPerBlock;
    }

    public ByteBlock allocate() {
        long block;
        synchronized (this) {
            if (freeHeadIndex == INVALID_INDEX) {
                return null;
            }
            block = freeHeadIndex / bytesPerBlock;
            if (freeHeadIndex != freeTailIndex) {
                try {
                    freeHeadIndex = nextIndex(freeHeadIndex);
                } catch (IOException ioe) {
                    throw new RuntimeException(ioe);
                }
            } else {
                freeHeadIndex = freeTailIndex = INVALID_INDEX;
            }
            allocatedBlocks++;
        }
        return new Block(block);
    }

    private synchronized void free(long blockIndex) {
        long index = blockIndex * bytesPerBlock;
        if (freeTailIndex != INVALID_INDEX) {
            long magic = (index - freeTailIndex - bytesPerBlock) % maxLength;
            try {
                synchronized (randomAccessFile) {
                    randomAccessFile.seek(freeTailIndex);
                    randomAccessFile.writeLong(magic);
                }
            } catch (IOException ioe) {
                throw new RuntimeException(ioe);
            }
            freeTailIndex = index;
        } else {
            freeHeadIndex = freeTailIndex = index;
        }
        allocatedBlocks--;
        synchronized (freeWaitMutex) {
            freeWaitMutex.notifyAll();
        }
    }

    public synchronized void free() {
        freeHeadIndex = freeTailIndex = 0;
        allocatedBlocks = 0;
    }

    private long nextIndex(long index) throws IOException {
        long next;
        synchronized (randomAccessFile) {
            randomAccessFile.seek(index);
            next = randomAccessFile.readLong();
        }
        return (index + next + bytesPerBlock) % maxLength;
    }

    public synchronized List<Number> freeBlockListView() {
        try {
            List<Number> list = new LinkedList<Number>();
            for (long index = freeHeadIndex; index != freeTailIndex; ) {
                list.add(index / bytesPerBlock);
                index = nextIndex(index);
            }
            if (freeTailIndex != INVALID_INDEX) {
                list.add(freeTailIndex / bytesPerBlock);
            }
            return list;
        } catch (IOException ioe) {
            return Collections.emptyList();
        }
    }

    @Override
    public byte get(long index) throws Exception {
        int b;
        synchronized (randomAccessFile) {
            randomAccessFile.seek(index);
            b = randomAccessFile.read();
        }
        return (b != -1) ? (byte) (b & 0xFF) : -1;
    }

    public synchronized boolean hasFreeBlock() {
        return (freeHeadIndex != INVALID_INDEX);
    }

    public String getName() {
        return name;
    }

    @Override
    public String getFilePath() {
        return filePath;
    }

    @Override
    public long getFileLength() {
        try {
            return randomAccessFile.length();
        } catch (IOException ioe) {
            return -1;
        }
    }

    @Override
    public long getBytesPerBlock() {
        return bytesPerBlock;
    }

    @Override
    public long getLength() {
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
        return "name:" + name + ", path:" + filePath + ", maxLength:" + maxLength + ", maxBlocks:" + maxBlocks
                + ", allocatedBlocks:" + allocatedBlocks;
    }

    public class Block implements ByteBlock {

        private long blockIndex;

        private static final int INVALID = -1;

        public Block(long blockIndex) {
            this.blockIndex = blockIndex;
        }

        public long getBlockIndex() {
            return blockIndex;
        }

        public void free() {
            long b;
            synchronized (this) {
                if (blockIndex == INVALID) {
                    return;
                }
                b = blockIndex;
                blockIndex = INVALID;
            }
            BlockedFile.this.free(b);
        }

        public long capacity() {
            return bytesPerBlock;
        }

        public int get(int position) {
            if (position < 0 || position >= bytesPerBlock) {
                throw new IndexOutOfBoundsException("position:" + position + ", capacity:" + bytesPerBlock);
            }
            long bufferOffset = bytesPerBlock * blockIndex + position;
            int read = -1;
            try {
                synchronized (randomAccessFile) {
                    if (blockIndex != INVALID) {
                        randomAccessFile.seek(bufferOffset);
                        read = randomAccessFile.read();
                    }
                }
            } catch (IOException ignored) {
            }
            return read;
        }

        public int get(int position, byte[] bytes, int offset, int length) {
            if (position < 0 || position > bytesPerBlock) {
                throw new IndexOutOfBoundsException("position:" + position + ", capacity:" + bytesPerBlock);
            }
            int left = (int) (bytesPerBlock - position);
            int read = (length <= left) ? length : left;
            long bufferOffset = blockIndex * bytesPerBlock + position;
            try {
                synchronized (randomAccessFile) {
                    if (blockIndex != INVALID) {
                        randomAccessFile.seek(bufferOffset);
                        randomAccessFile.read(bytes, offset, read);
                    } else {
                        read = -1;
                    }
                }
            } catch (IOException ioe) {
                read = -1;
            }
            return read;
        }

        public int put(int position, int b) {
            if (position < 0 || position >= bytesPerBlock) {
                throw new IndexOutOfBoundsException("position:" + position + ", capacity:" + bytesPerBlock);
            }
            long bufferOffset = blockIndex * bytesPerBlock + position;
            int read = 1;
            try {
                synchronized (randomAccessFile) {
                    if (blockIndex != INVALID) {
                        randomAccessFile.seek(bufferOffset);
                        randomAccessFile.write(b);
                    } else {
                        read = -1;
                    }
                }
            } catch (IOException ioe) {
                read = -1;
            }
            return read;
        }

        public int put(int position, byte[] bytes, int offset, int length) {
            if (position < 0 || (position + length) > bytesPerBlock) {
                throw new IndexOutOfBoundsException("position:" + position + ", length:" + length + ", capacity:" + bytesPerBlock);
            }
            int written = (int) ((length <= bytesPerBlock) ? length : bytesPerBlock);
            long bufferOffset = blockIndex * bytesPerBlock + position;
            try {
                synchronized (randomAccessFile) {
                    if (blockIndex != INVALID) {
                        randomAccessFile.seek(bufferOffset);
                        randomAccessFile.write(bytes, offset, written);
                    } else {
                        written = -1;
                    }
                }
            } catch (IOException ioe) {
                written = -1;
            }
            return written;
        }
    }
}
