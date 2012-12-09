package net.ihiroky.reservoir.accessor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 * Created on 12/10/31, 16:51
 *
 * @author Hiroki Itoh
 */
public class MemoryMappedFileCacheAccessor<K, V>
        extends FileCacheAccessor<K, V> implements MemoryMappedFileCacheAccessorMBean {

    private Collection<MappedByteBuffer> mappedByteBuffers = Collections.emptyList();
    private Logger logger = LoggerFactory.getLogger(FileCacheAccessor.class);

    @Override
    protected long getMaxPartitionSize(int blockSize) {
        return ByteBufferCacheAccessor.maxPartitionSize(blockSize);
    }

    @Override
    protected ByteBlockManager createInstance(
            String name, File file, RandomAccessFile randomAccessFile, int blockSize) throws IOException {
        long size = randomAccessFile.length();
        final int maxPartitionSize = ByteBufferCacheAccessor.maxPartitionSize(blockSize);
        if (size > maxPartitionSize) {
            size = maxPartitionSize;
            randomAccessFile.setLength(size);
            logger.warn("[createInstance] truncate {} to {} because of the MemoryByteBuffer limitation.",
                    file, size);
        }
        MappedByteBuffer byteBuffer = randomAccessFile.getChannel().map(FileChannel.MapMode.READ_WRITE, 0L, size);
        synchronized (this) {
            if (mappedByteBuffers.isEmpty()) {
                mappedByteBuffers = new ArrayList<MappedByteBuffer>();
            }
            mappedByteBuffers.add(byteBuffer);
        }
        BlockedByteBuffer blockedByteBuffer = new BlockedByteBuffer(byteBuffer, blockSize, super.freeWaitMutex);
        blockedByteBuffer.setName(name.concat(file.getName()));
        return blockedByteBuffer;
    }

    @Override
    public void dispose() {
        super.dispose();
        mappedByteBuffers = Collections.emptyList();
    }

    public void sync() {
        synchronized (this) {
            for (MappedByteBuffer mappedByteBuffer : mappedByteBuffers) {
                mappedByteBuffer.force();
            }
        }
    }
}
