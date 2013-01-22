package net.ihiroky.reservoir.storage;

import net.ihiroky.reservoir.Reservoir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Hiroki Itoh
 */
public class BulkInfo {

    final long size;
    final int blockSize;
    final int partitions;
    Logger logger = LoggerFactory.getLogger(BulkInfo.class);

    public BulkInfo(long size, int blockSize, int partitions) {
        if (size < blockSize
                || blockSize < BlockedByteBuffer.MIN_BYTES_PER_BLOCK) {
            throw new IllegalArgumentException();
        }

        this.size = size;
        this.blockSize = blockSize;
        this.partitions = partitions;
    }

    ByteBlockManager[] allocate(ByteBlockManagerAllocator allocator, long maxPartitionSize) {
        long partitionSize;
        int actualPartitions;
        if (partitions > 0) {
            long partitionSizeHint = size / partitions;
            if (partitionSizeHint > maxPartitionSize) {
                partitionSizeHint = maxPartitionSize;
            }
            if (partitionSizeHint < blockSize) {
                partitionSizeHint = blockSize;
            }
            actualPartitions = (int)(size / partitionSizeHint);
            partitionSize = (partitionSizeHint / blockSize) * blockSize;
        } else {
            int cap = Reservoir.getMaxDirectBufferCapacity();
            actualPartitions = (int)(size / cap) + (((size % cap) == 0) ? 0 : 1);
            long partitionSizeHint = size / actualPartitions;
            if (partitionSizeHint < blockSize) {
                partitionSizeHint = blockSize;
            }
            partitionSize = (partitionSizeHint / blockSize) * blockSize;
        }

        logger.info("[allocate] partitionSize: {}, actualPartitions: {}", partitionSize, actualPartitions);

        ByteBlockManager[] byteBlockManagers = new ByteBlockManager[actualPartitions];
        for (int i = 0; i < actualPartitions; i++) {
            byteBlockManagers[i] = allocator.allocate(partitionSize, blockSize, i);
        }
        return byteBlockManagers;
    }

    public interface ByteBlockManagerAllocator {
        ByteBlockManager allocate(long size, int blockSize, int index);
    }

    @Override
    public String toString() {
        return "size:" + size + ", blockSize:" + blockSize + ", partitions:" + partitions;
    }
}
