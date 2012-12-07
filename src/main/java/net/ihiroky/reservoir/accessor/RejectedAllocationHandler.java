package net.ihiroky.reservoir.accessor;

/**
 * @author Hiroki Itoh
 */
public interface RejectedAllocationHandler {
    void handle(AbstractBlockedByteCacheAccessor<?, ?> accessor) throws InterruptedException;

    abstract class AllocateByteBlockManager implements RejectedAllocationHandler {
        @Override
        public void handle(AbstractBlockedByteCacheAccessor<?, ?> accessor) throws InterruptedException {
            ByteBlockManager byteBlockManager = createByteBlockManager(accessor, accessor.freeWaitMutex);
            accessor.addByteBlockManager(byteBlockManager);
        }

        public abstract ByteBlockManager createByteBlockManager(
                AbstractBlockedByteCacheAccessor<?, ?> accessor, Object freeWaitMutex);
    }
}
