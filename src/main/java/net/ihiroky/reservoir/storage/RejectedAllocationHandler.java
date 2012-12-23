package net.ihiroky.reservoir.storage;

/**
 * @author Hiroki Itoh
 */
public interface RejectedAllocationHandler {
    boolean handle(AbstractBlockedByteStorageAccessor<?, ?> accessor) throws InterruptedException;

    abstract class AllocateByteBlockManager implements RejectedAllocationHandler {
        @Override
        public boolean handle(AbstractBlockedByteStorageAccessor<?, ?> accessor) throws InterruptedException {
            ByteBlockManager byteBlockManager = createByteBlockManager(accessor);
            accessor.addByteBlockManager(byteBlockManager);
            return true;
        }

        public abstract ByteBlockManager createByteBlockManager(AbstractBlockedByteStorageAccessor<?, ?> accessor);
    }
}
