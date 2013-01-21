package net.ihiroky.reservoir.storage;

/**
 * @author Hiroki Itoh
 */
public class BulkInfo {

    final long size;
    final int blockSize;
    final int partitions;

    public BulkInfo(long size, int blockSize, int partitions) {
        if (size < blockSize
                || blockSize < BlockedByteBuffer.MIN_BYTES_PER_BLOCK
                || partitions <= 0) {
            throw new IllegalArgumentException();
        }

        this.size = size;
        this.blockSize = blockSize;
        this.partitions = partitions;
    }

    ByteBlockManager[] allocate(ByteBlockManagerAllocator allocator, long maxPartitionSize) {
        long partitionSizeHint = size / partitions;
        if (partitionSizeHint > maxPartitionSize) {
            partitionSizeHint = maxPartitionSize;
        }
        if (partitionSizeHint < blockSize) {
            partitionSizeHint = blockSize;
        }
        long partitionSize = (partitionSizeHint / blockSize) * blockSize;
        ByteBlockManager[] byteBlockManagers = new ByteBlockManager[partitions];
        for (int i = 0; i < partitions; i++) {
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
