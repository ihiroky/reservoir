package net.ihiroky.reservoir.storage;

/**
 * @author Hiroki Itoh
 */
public class BulkInfo {

    final long size;
    final int blockSize;
    final int partitionsHint;

    public BulkInfo(long size, int blockSize, int partitionsHint) {
        if (size < blockSize
                || blockSize < BlockedByteBuffer.MIN_BYTES_PER_BLOCK
                || partitionsHint <= 0) {
            throw new IllegalArgumentException();
        }

        this.size = size;
        this.blockSize = blockSize;
        this.partitionsHint = partitionsHint;
    }

    ByteBlockManager[] allocate(ByteBlockManagerAllocator allocator, long maxPartitionSize) {
        final long partitionSizeHint = size / partitionsHint;
        long left = size;
        long partitionSize = (partitionSizeHint < maxPartitionSize) ? (int) partitionSizeHint : maxPartitionSize;
        if (partitionSize < blockSize) {
            partitionSize = blockSize;
        }

        int partitions = (int) (size / partitionSize + ((size % partitionSize != 0) ? 1 : 0));
        ByteBlockManager[] byteBlockManagers = new ByteBlockManager[partitions];
        for (int i = 0; i < partitions; i++) {
            long bbbSize = (left >= partitionSize) ? partitionSize : left;
            if (bbbSize < blockSize) {
                bbbSize = blockSize;
            }
            byteBlockManagers[i] = allocator.allocate(bbbSize, blockSize, i);
            left -= bbbSize;
        }
        return byteBlockManagers;
    }

    public interface ByteBlockManagerAllocator {
        ByteBlockManager allocate(long size, int blockSize, int index);
    }

    @Override
    public String toString() {
        return "size:" + size + ", blockSize:" + blockSize + ", partitionsHint:" + partitionsHint;
    }
}
